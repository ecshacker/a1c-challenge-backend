package org.a1cchallenge.backend.repository;

import org.a1cchallenge.backend.entity.DraftCheckInEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
     * Section 5 promotion: fetch all drafts whose window has closed so the
     * cleanup job can promote them to submitted check-ins.
     */
    List<DraftCheckInEntity> findAllByStudyWeekLessThanEqual(Integer studyWeek);
}
