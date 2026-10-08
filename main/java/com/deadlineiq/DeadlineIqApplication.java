package com.deadlineiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * DeadlineIQ - Academic & Career Deadline Management System
 * Main entry point for the Spring Boot application.
 */
@SpringBootApplication(scanBasePackages = {"com.deadlineiq", "com.controller", "com.service", "com.repository", "com.entity", "com.dto", "com"})
@EntityScan(basePackages = {"com.deadlineiq.model", "com.entity"})
@EnableJpaRepositories(basePackages = {"com.deadlineiq.repository", "com.repository"})
public class DeadlineIqApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeadlineIqApplication.class, args);
        
        System.out.println("==================================================================");
        System.out.println("🚀  DeadlineIQ Project Started Successfully!");
        System.out.println("🌐  Local URL: http://localhost:8080");
        System.out.println("📁  Location: E:\\DeadlineIQ_Project");
        System.out.println("🤖  AI Chatbot + Memory + RAG Engine: Active");
        System.out.println("==================================================================");
    }
}
