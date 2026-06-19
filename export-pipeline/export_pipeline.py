"""
A1C Challenge — Data Export Pipeline (Zenodo CC0)

Connects to the PostgreSQL database, applies the strict anonymization and
transformation rules from Section 8 of the Data Schema, generates a JSON-LD
data dictionary, and stages the artifacts to AWS S3 for the Zenodo delivery
script to pick up.

Privacy rules enforced here (Section 8.1 / 8.2):
  - token  -> randomized sequential participant_id (breaks enrollment-order inference)
  - enrolled_at_week/year -> coarse cohort_quarter
  - tribal_nation -> small-n aggregation (recode if distinct n < 10)
  - drop all free-text and device-fingerprinting fields

Run:
    python export_pipeline.py

Requires (Section: Phase 5 of the infrastructure checklist):
    pip install pandas psycopg2-binary boto3 numpy
"""

import os
import json
import numpy as np
import pandas as pd
import psycopg2
import boto3
from botocore.exceptions import NoCredentialsError, ClientError
from datetime import datetime

# ==============================================================================
# 1. CONFIGURATION
# ==============================================================================
DB_CONFIG = {
    'host': os.getenv('DB_HOST', 'localhost'),
    'port': os.getenv('DB_PORT', '5432'),
    'dbname': os.getenv('DB_NAME', 'a1c_challenge'),
    'user': os.getenv('DB_USER', 'export_user'),  # Ensure this user has SELECT ONLY
    'password': os.getenv('DB_PASSWORD'),
}

S3_BUCKET = os.getenv('S3_BUCKET', 'a1c-challenge-open-data-staging')
S3_PREFIX = os.getenv('S3_PREFIX', 'exports/v1/')
TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")


# ==============================================================================
# 2. DATABASE EXTRACTION (Normalized Per-Table)
# ==============================================================================
def extract_tables():
    """
    Extracts each table independently to avoid column collisions, sparse NULL
    padding, and fragile UNION ALL logic.
    """
    print("Connecting to database and extracting normalized tables...")
    conn = psycopg2.connect(**DB_CONFIG)
    try:
        df_participants = pd.read_sql("SELECT * FROM participants", conn)
        df_checkins = pd.read_sql("SELECT * FROM checkins", conn)
        df_milestones = pd.read_sql("SELECT * FROM milestones", conn)

        print(f"Extracted: {len(df_participants)} participants, "
              f"{len(df_checkins)} checkins, {len(df_milestones)} milestones.")
        return df_participants, df_checkins, df_milestones
    finally:
        conn.close()


# ==============================================================================
# 3. ANONYMIZATION & TRANSFORMATION
# ==============================================================================
def anonymize_tables(df_participants, df_checkins, df_milestones):
    print("Applying anonymization and transformation rules...")

    # --- Rule 1: Randomized Sequential Participant ID (Sec 8.2) ---
    unique_tokens = df_participants['token'].unique()
    np.random.shuffle(unique_tokens)  # Breaks any enrollment sequence correlation
    token_to_id = {token: idx + 1 for idx, token in enumerate(unique_tokens)}

    # Apply mapping to all tables to maintain referential integrity
    df_participants['participant_id'] = df_participants['token'].map(token_to_id)
    df_checkins['participant_id'] = df_checkins['token'].map(token_to_id)
    df_milestones['participant_id'] = df_milestones['token'].map(token_to_id)

    # --- Rule 2: Cohort Quarter (Participants only) ---
    def get_cohort_quarter(row):
        year = row['enrolled_at_year']
        week = row['enrolled_at_week']
        if week <= 13:
            return f"{year}-Q1"
        elif week <= 26:
            return f"{year}-Q2"
        elif week <= 39:
            return f"{year}-Q3"
        else:
            return f"{year}-Q4"

    df_participants['cohort_quarter'] = df_participants.apply(get_cohort_quarter, axis=1)

    # --- Rule 3: Small-N Tribal Nation Aggregation (Sec 8.2) ---
    # Count DISTINCT participants, not rows, to avoid inflating counts due to the
    # 1-to-many relationship with weekly check-ins.
    valid_tribal_mask = df_participants['tribal_nation'].notna()
    if valid_tribal_mask.any():
        unique_participant_nations = df_participants.loc[
            valid_tribal_mask, ['participant_id', 'tribal_nation']
        ].drop_duplicates()
        counts = unique_participant_nations['tribal_nation'].value_counts()
        small_n_nations = counts[counts < 10].index

        df_participants.loc[valid_tribal_mask, 'tribal_nation'] = \
            df_participants.loc[valid_tribal_mask, 'tribal_nation'].apply(
                lambda x: 'Indigenous — nation not reported' if x in small_n_nations else x
            )

    # --- Rule 4: Drop Excluded Fields (Per Table) ---
    # Participants
    df_participants.drop(
        columns=['token', 'enrolled_at_week', 'enrolled_at_year'],
        inplace=True, errors='ignore'
    )
    # Checkins
    df_checkins.drop(
        columns=['token', 'checkin_id', 'submitted_at_day_offset'],
        inplace=True, errors='ignore'
    )
    # Milestones
    df_milestones.drop(
        columns=['token', 'milestone_id'],
        inplace=True, errors='ignore'
    )

    # Global Drop: Free text and fingerprinting fields
    drop_cols = ['cgm_device', 'cannabis_profile_notes', 'free_text_note']
    for df in (df_participants, df_checkins, df_milestones):
        df.drop(columns=[c for c in drop_cols if c in df.columns],
                inplace=True, errors='ignore')

    # Reorder columns to put participant_id first in all tables
    out = []
    for df in (df_participants, df_checkins, df_milestones):
        cols = ['participant_id'] + [c for c in df.columns if c != 'participant_id']
        out.append(df[cols])
    df_participants, df_checkins, df_milestones = out

    print("Anonymization complete.")
    return df_participants, df_checkins, df_milestones


# ==============================================================================
# 4. JSON-LD DATA DICTIONARY GENERATION
# ==============================================================================
def generate_json_ld(df_participants, df_checkins, df_milestones):
    print("Generating JSON-LD data dictionary...")

    # Representative metadata mapping (expand as needed for production)
    schema_metadata = {
        "participant_id": {"label": "Anonymous Participant ID", "dataType": "Integer",
                           "description": "Sequential integer, randomized order."},
        "cohort_quarter": {"label": "Cohort Quarter", "dataType": "String",
                           "description": "Quarter of enrollment (e.g., 2026-Q3)."},
        "tribal_nation": {"label": "Tribal Nation", "dataType": "String",
                          "description": "Aggregated to nation name only if n>=10, "
                                         "otherwise 'Indigenous — nation not reported'."},
        "baseline_a1c": {"label": "Baseline A1C", "dataType": "Decimal", "unit": "%",
                         "description": "Enrollment A1C."},
        "delta_a1c": {"label": "Delta A1C", "dataType": "Decimal", "unit": "%",
                      "description": "Milestone A1C minus Baseline A1C. Negative = improvement."},
        "delta_fructosamine": {"label": "Delta Fructosamine", "dataType": "Decimal", "unit": "µmol/L",
                               "description": "Milestone fructosamine minus baseline fructosamine. "
                                              "Negative = improvement."},
        "adherence_tier": {"label": "Adherence Tier", "dataType": "String",
                           "description": "enrolled_only, full_cohort, partial, per_protocol, or high_adherence."},
        "combined_compliance_score": {"label": "Weekly Compliance Score", "dataType": "Decimal",
                                       "description": "(hemp_days + cannabis_days) / 14."},
        "cecd_composite_baseline": {"label": "CECD Composite Baseline", "dataType": "Decimal",
                                    "description": "Mean of 6 wellbeing dimensions at enrollment."},
    }

    def build_variables(df, table_name):
        variables = []
        for col in df.columns:
            meta = schema_metadata.get(col, {
                "label": col.replace('_', ' ').title(),
                "dataType": str(df[col].dtype),
                "description": f"Study variable from {table_name}.",
            })
            variables.append({
                "@type": "PropertyValue",
                "name": col,
                "description": meta.get("description"),
                "dataType": meta.get("dataType"),
                "unitText": meta.get("unit"),
            })
        return variables

    json_ld = {
        "@context": "http://schema.org/",
        "@type": "Dataset",
        "name": "A1C Challenge Open Data Export",
        "license": "https://creativecommons.org/publicdomain/zero/1.0/",
        "version": "1.0",
        "datePublished": datetime.now().date().isoformat(),
        "variableMeasured": build_variables(df_participants, "participants")
        + build_variables(df_checkins, "checkins")
        + build_variables(df_milestones, "milestones"),
    }
    return json_ld


# ==============================================================================
# 5. LOCAL EXPORT & S3 STAGING
# ==============================================================================
def stage_to_s3(df_participants, df_checkins, df_milestones, json_ld):
    participants_file = f"participants_{TIMESTAMP}.csv"
    checkins_file = f"checkins_{TIMESTAMP}.csv"
    milestones_file = f"milestones_{TIMESTAMP}.csv"
    jsonld_file = f"data_dictionary_{TIMESTAMP}.jsonld"

    print(f"Saving {participants_file}, {checkins_file}, {milestones_file}, "
          f"and {jsonld_file} locally...")
    df_participants.to_csv(participants_file, index=False)
    df_checkins.to_csv(checkins_file, index=False)
    df_milestones.to_csv(milestones_file, index=False)
    with open(jsonld_file, 'w') as f:
        json.dump(json_ld, f, indent=2)

    s3 = boto3.client('s3')
    try:
        print(f"Staging to s3://{S3_BUCKET}/{S3_PREFIX}...")
        s3.upload_file(participants_file, S3_BUCKET, f"{S3_PREFIX}{participants_file}")
        s3.upload_file(checkins_file, S3_BUCKET, f"{S3_PREFIX}{checkins_file}")
        s3.upload_file(milestones_file, S3_BUCKET, f"{S3_PREFIX}{milestones_file}")
        s3.upload_file(jsonld_file, S3_BUCKET, f"{S3_PREFIX}{jsonld_file}")
        print("Successfully staged all files to S3. Ready for Zenodo delivery script.")

        # Cleanup local files
        os.remove(participants_file)
        os.remove(checkins_file)
        os.remove(milestones_file)
        os.remove(jsonld_file)

    except (NoCredentialsError, ClientError) as e:
        print(f"Failed to upload to S3: {e}")
        print("Local files retained for manual upload.")


# ==============================================================================
# MAIN EXECUTION
# ==============================================================================
if __name__ == "__main__":
    # 1. Extract
    df_p, df_c, df_m = extract_tables()

    if df_p.empty:
        print("No data found in database. Exiting.")
        raise SystemExit(0)

    # 2. Transform & Anonymize
    anon_p, anon_c, anon_m = anonymize_tables(df_p, df_c, df_m)

    # 3. Generate Data Dictionary
    json_ld_data = generate_json_ld(anon_p, anon_c, anon_m)

    # 4. Stage to S3
    stage_to_s3(anon_p, anon_c, anon_m, json_ld_data)
