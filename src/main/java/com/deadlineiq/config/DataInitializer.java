package com.deadlineiq.config;

import com.deadlineiq.model.*;
import com.deadlineiq.repository.DeadlineRepository;
import com.deadlineiq.repository.OpportunityRepository;
import com.deadlineiq.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Initializes essential demo accounts and sample academic data in MySQL if tables are empty.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeadlineRepository deadlineRepository;
    private final OpportunityRepository opportunityRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, DeadlineRepository deadlineRepository, OpportunityRepository opportunityRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.deadlineRepository = deadlineRepository;
        this.opportunityRepository = opportunityRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            System.out.println("🌱 Initializing default campus users in MySQL database...");

            User student = new User(
                    null,
                    "Sai Jayanth",
                    "student@deadlineiq.com",
                    passwordEncoder.encode("password123"),
                    Role.STUDENT,
                    "CSE",
                    null,
                    "3rd Year",
                    "Section A"
            );
            userRepository.save(student);

            User faculty = new User(
                    null,
                    "Dr. R. Sharma",
                    "faculty@deadlineiq.com",
                    passwordEncoder.encode("password123"),
                    Role.FACULTY,
                    "CSE",
                    "Java",
                    null,
                    null
            );
            userRepository.save(faculty);

            User coordinator = new User(
                    null,
                    "Prof. K. Venkatesh",
                    "coordinator@deadlineiq.com",
                    passwordEncoder.encode("password123"),
                    Role.COORDINATOR,
                    "ECE",
                    null,
                    null,
                    null
            );
            userRepository.save(coordinator);

            User admin = new User(
                    null,
                    "System Administrator",
                    "admin@deadlineiq.com",
                    passwordEncoder.encode("password123"),
                    Role.ADMIN,
                    "CSE",
                    null,
                    null,
                    null
            );
            userRepository.save(admin);

            System.out.println("✅ Seed users created in MySQL (student, faculty, coordinator, admin).");
        }

        if (deadlineRepository.count() == 0) {
            Deadline d1 = new Deadline(
                    null,
                    "Operating Systems CPU Scheduling Simulation",
                    "Implement FCFS, Round Robin, and Priority scheduling algorithms.",
                    Category.ASSIGNMENT,
                    "faculty@deadlineiq.com",
                    LocalDate.now().plusDays(2),
                    Priority.HIGH,
                    "CSE 3-1",
                    "Section A"
            );
            deadlineRepository.save(d1);

            Deadline d2 = new Deadline(
                    null,
                    "DBMS Phase 2 Normalization & Schema Architecture",
                    "Deliver normalized ER schemas up to BCNF with DDL scripts.",
                    Category.PROJECT,
                    "faculty@deadlineiq.com",
                    LocalDate.now().plusDays(5),
                    Priority.HIGH,
                    "CSE 3-1",
                    "Section A"
            );
            deadlineRepository.save(d2);

            Deadline d3 = new Deadline(
                    null,
                    "Machine Learning Midterm Exam",
                    "Covers linear regression, backprop, and CNN architectures.",
                    Category.EXAM,
                    "faculty@deadlineiq.com",
                    LocalDate.now().plusDays(9),
                    Priority.MEDIUM,
                    "CSE 3-1",
                    "All"
            );
            deadlineRepository.save(d3);
        }

        if (opportunityRepository.count() == 0) {
            Opportunity o1 = new Opportunity(
                    null,
                    "Software Engineering Intern - Summer 2027",
                    "Google",
                    "INTERNSHIP",
                    "Build world-class distributed cloud infrastructure and AI systems with Google engineering teams.",
                    "CGPA >= 7.5, B.Tech CSE / IT",
                    LocalDate.now().plusDays(14),
                    LocalDate.now().plusDays(30),
                    "coordinator@deadlineiq.com"
            );
            opportunityRepository.save(o1);

            Opportunity o2 = new Opportunity(
                    null,
                    "Microsoft Imagine Cup & AI Hackathon 2026",
                    "Microsoft",
                    "HACKATHON",
                    "Compete to build groundbreaking startups and intelligent applications powered by Microsoft AI.",
                    "All Enrolled University Students",
                    LocalDate.now().plusDays(8),
                    LocalDate.now().plusDays(20),
                    "coordinator@deadlineiq.com"
            );
            opportunityRepository.save(o2);
        }
    }
}
