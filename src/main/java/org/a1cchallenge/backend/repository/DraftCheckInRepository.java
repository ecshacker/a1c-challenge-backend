package com.a1cchallenge.repository;

import com.a1cchallenge.entity.DraftCheckInEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DraftCheckInRepository extends JpaRepository<DraftCheckInEntity, UUID> {

    Optional<DraftCheckInEntity> findByTokenAndStudyWeek(String token, Integer studyWeek);

    // Section 5: deleted on check-in submit
    @Modifying
    @Query("DELETE FROM DraftCheckInEntity d WHERE d.token = :token AND d.studyWeek = :studyWeek")
    void deleteByTokenAndStudyWeek(@Param("token") String token, @Param("studyWeek") Integer studyWeek);

    /**
     * Section 5 purge: delete drafts whose study_week ended more than ~14 days
     * (2 study weeks) before the current global study week. The current global
     * week is derived from study_config.launch_date in the calling job.
     */
    @Modifying
    @Query("DELETE FROM DraftCheckInEntity d WHERE d.studyWeek <= :purgeBeforeWeek")
    int purgeDraftsBeforeWeek(@Param("purgeBeforeWeek") Integer purgeBeforeWeek);
}
