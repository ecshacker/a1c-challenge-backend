package com.a1cchallenge.repository;

import com.a1cchallenge.entity.StudyConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudyConfigRepository extends JpaRepository<StudyConfig, Short> {
    Optional<StudyConfig> findById(Short id);
}
