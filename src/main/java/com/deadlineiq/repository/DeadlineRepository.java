package com.deadlineiq.repository;

import com.deadlineiq.model.Deadline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Deadline entity in MySQL.
 */
@Repository
public interface DeadlineRepository extends JpaRepository<Deadline, Long> {

    List<Deadline> findByTargetSectionIgnoreCaseOrTargetSectionIgnoreCase(String section, String all);

    List<Deadline> findByCreatedByIgnoreCase(String createdBy);
}
