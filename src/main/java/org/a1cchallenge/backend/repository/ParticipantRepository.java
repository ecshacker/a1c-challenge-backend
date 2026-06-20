package org.a1cchallenge.backend.repository;

import org.a1cchallenge.backend.entity.ParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, String> {
    // String is the token (Primary Key)
}
