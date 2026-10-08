package com.deadlineiq.repository;

import com.deadlineiq.model.Opportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for Opportunity entity in MySQL.
 */
@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
}
