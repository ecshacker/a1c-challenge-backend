# The A1C Challenge — Backend

Spring Boot 3 / PostgreSQL backend for the anonymous-by-design A1C Challenge study,
generated from `A1C_Challenge_build-trail.md`.

The build-trail is a bottom-up dev log containing code fragments for every layer
with intentional gaps (deferred accessors, `TODO` field-mappings, a few typos).
This project assembles those fragments into a complete, structurally-consistent
Maven project and fills every gap.

---

## Privacy model (why the code looks the way it does)

The study is **anonymous by design** and IRB-exempt. The only key is a participant
token (`XXXX-XXXX-XXXX`, alphabet `A–H J–N P–Z 2–9`, excluding `O/0/I/1`) held in the
browser's `localStorage`. The backend therefore:

- never stores PII, IP addresses, or wall-clock timestamps tied to a token;
- stores only **offsets** (`submitted_at_day_offset`, `event_week_offset`, …) and
  ISO study weeks;
- writes audit rows under a **SHA-256 hash** of the token, never the token itself;
- disables the Tomcat access log and SQL bind-parameter logging
  (see `application.yml`).

---

## Project layout

```
a1c-backend/
├── pom.xml                         Spring Boot 3.3.5, Java 17 target
└── src/main/
    ├── java/com/a1cchallenge/
    │   ├── A1cChallengeApplication.java     @SpringBootApplication @EnableScheduling
    │   ├── entity/        JPA entities (1 per table) + StudyConfig/StudyStatus
    │   ├── repository/    Spring Data JPA repositories
    │   ├── dto/           request/response records (computed fields server-side only)
    │   ├── validation/    @ValidParticipantToken, @ValidEnrollment + validators
    │   ├── service/       business logic (enrollment, check-in, milestone, audit…)
    │   ├── controller/    REST controllers under /api/v1
    │   ├── exception/     domain exceptions + @RestControllerAdvice handler
    │   └── scheduled/     DraftCleanupJob (nightly draft purge)
    └── resources/
        ├── application.yml                  PostgreSQL + privacy logging config
        └── db/migration/
            ├── V1__core_schema.sql          5 core tables, constraints, trigger
            └── V2__study_config.sql         kill-switch table, seeded PRE_LAUNCH
```

Flyway owns the schema; JPA runs in `ddl-auto: validate` so entities are checked
against the migrations at startup but never mutate them.

---

## REST surface

| Method | Path                              | Notes                                  |
|--------|-----------------------------------|----------------------------------------|
| POST   | `/api/v1/participants`            | enroll → `201` + token (kill-switch gated) |
| GET    | `/api/v1/participants/me`         | `X-Participant-Token` header           |
| PATCH  | `/api/v1/participants/me`         | self-service field updates             |
| PATCH  | `/api/v1/participants/me/baseline`| locked after week 1 closes             |
| POST   | `/api/v1/checkins`                | `200` + soft warnings                  |
| POST   | `/api/v1/checkins/draft`          | upsert draft                           |
| DELETE | `/api/v1/checkins/draft`          | discard draft                          |
| POST   | `/api/v1/milestones`              | `201` (four_week / eight_week)         |
| PUT    | `/api/v1/admin/study-status`      | `X-Admin-Secret` header                |

---

## Defects fixed from the build-trail (faithful to source intent)

| # | Source defect | Resolution |
|---|---------------|------------|
| 1 | Deferred getters/setters across entities/DTOs | Lombok `@Getter`/`@Setter` |
| 2 | `mapEnrollmentFields` left as `TODO` (~60 fields) | Implemented in full |
| 3 | `mapCheckInFields` left as `TODO` | Implemented in full |
| 4 | Typo `cond_ibd_crohns"e` | Corrected to `cond_ibd_crohns` |
| 5 | `hempIntendedDailyG` typed `Integer` | Changed to `BigDecimal` (DDL `DECIMAL(5,1)`) |
| 6 | Malformed `chk_glucose_unit_req` (unclosed paren) | Closed |
| 7 | `MilestoneEntity` CECD composites `nullable=false` vs DDL nullable + `computeCecdComposite` returning `null` | Made `nullable=true` (resolves contradiction) |
| 8 | `purgeDraftsOlderThan14DaysPastWeekEnd()` placeholder | Implemented as `purgeDraftsBeforeWeek(currentGlobalWeek - 2)`, global week derived from `study_config.launch_date` |

---

## Build & run

> **Note:** this project was generated and structurally validated (brace balance,
> package decls, internal-import resolution, and a 6-table entity↔migration column
> diff — all clean), but **was not compiled here** because the generation
> environment has no Maven, no dependency cache, and no network access. Run the
> commands below in a normal environment to compile and test.

```bash
# 1. compile
mvn -q compile

# 2. context-load smoke test (uses H2 in PostgreSQL mode, no real DB needed)
mvn -q test

# 3. run against PostgreSQL
export DB_URL=jdbc:postgresql://localhost:5432/a1c_challenge
export DB_USERNAME=a1c_app
export DB_PASSWORD=...
export ADMIN_SECRET=...           # required to flip the kill-switch
mvn spring-boot:run
```

Flyway applies `V1`/`V2` on first start. The study launches in `PRE_LAUNCH`;
flip it to `OPEN` via `PUT /api/v1/admin/study-status` to allow enrollment.

---

## Out of scope (per build-trail)

- UI / frontend layer ("NOT YET STARTED")
- Python data-export pipeline (separate concern)
