package com.deadlineiq.service;

import com.deadlineiq.model.Opportunity;
import com.deadlineiq.repository.OpportunityRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Service managing campus recruitment drives, internships, and hackathons persisted in MySQL.
 */
@Service
public class OpportunityService {

    private final OpportunityRepository opportunityRepository;

    public OpportunityService(OpportunityRepository opportunityRepository) {
        this.opportunityRepository = opportunityRepository;
    }

    @PostConstruct
    public void initDefaultOpportunities() {
        if (opportunityRepository.count() == 0) {
            LocalDate today = LocalDate.now();

            // 1. Placement Drive
            opportunityRepository.save(new Opportunity(null,
                    "TCS National Qualifier Test (NQT) Drive",
                    "Tata Consultancy Services", "Placement",
                    "On-campus recruitment drive for Ninja & Digital software engineering profiles.",
                    "B.Tech (CSE, IT, ECE) • Min CGPA: 7.0 • No active backlogs",
                    today.plusDays(2), today.plusDays(10), "coordinator@campus.edu"));

            // 2. Hackathon
            opportunityRepository.save(new Opportunity(null,
                    "Smart India Hackathon 2026 Campus Round",
                    "Ministry of Education Innovation Cell", "Hackathon",
                    "Internal college evaluation round for problem statements under Smart Automation and HealthTech.",
                    "All B.Tech batches • Teams of 6 members with at least 1 female teammate",
                    today.plusDays(7), today.plusDays(18), "coordinator@campus.edu"));

            // 3. Internship
            opportunityRepository.save(new Opportunity(null,
                    "Infosys Springboard AI & Cloud Internship",
                    "Infosys Limited", "Internship",
                    "Virtual project-based internship on Enterprise Java & Cloud Microservices.",
                    "3rd & 4th Year B.Tech CSE / IT students",
                    today.plusDays(14), today.plusDays(30), "coordinator@campus.edu"));
        }
    }

    public List<Opportunity> getAllOpportunities() {
        return opportunityRepository.findAll();
    }

    public Optional<Opportunity> findById(Long id) {
        if (id == null) return Optional.empty();
        return opportunityRepository.findById(id);
    }

    public Opportunity addOpportunity(Opportunity opp) {
        if (opp.getRegistrationDeadline() == null) {
            opp.setRegistrationDeadline(LocalDate.now().plusDays(7));
        }
        return opportunityRepository.save(opp);
    }

    public boolean registerStudent(Long oppId, String studentEmail) {
        Optional<Opportunity> opt = findById(oppId);
        if (opt.isPresent()) {
            Opportunity opp = opt.get();
            opp.registerStudent(studentEmail);
            opportunityRepository.save(opp);
            return true;
        }
        return false;
    }

    public boolean deleteOpportunity(Long id) {
        if (id != null && opportunityRepository.existsById(id)) {
            opportunityRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
