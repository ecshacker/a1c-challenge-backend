package com.a1cchallenge.repository;

import com.a1cchallenge.entity.ParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipantRepository extends JpaRepository<ParticipantEntity, String> {
    // String is the token (Primary Key)
}
