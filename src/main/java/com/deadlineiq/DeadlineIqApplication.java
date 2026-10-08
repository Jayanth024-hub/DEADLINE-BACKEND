package com.deadlineiq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DeadlineIQ - Academic & Career Deadline Management System
 * Main entry point for the Spring Boot application.
 */
@SpringBootApplication(scanBasePackages = {"com.deadlineiq", "com.controller", "com.service", "com.repository", "com.entity", "com.dto", "com"})
@EntityScan(basePackages = {"com.deadlineiq.model", "com.entity"})
@EnableJpaRepositories(basePackages = {"com.deadlineiq.repository", "com.repository"})
public class DeadlineIqApplication {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(DeadlineIqApplication.class, args);
        
        System.out.println("==================================================================");
        System.out.println("🚀  DeadlineIQ Project Started Successfully!");
        System.out.println("📁  Location: D:\\DEADLINEIQ");
        System.out.println("🤖  AI Chatbot + Memory + RAG Engine: Active");
        System.out.println("==================================================================");
    }

    private static void loadDotEnv() {
        java.io.File[] candidateLocations = new java.io.File[] {
            new java.io.File(".env"),
            new java.io.File("backend/demo/.env"),
            new java.io.File("D:/DEADLINEIQ/backend/demo/.env"),
            new java.io.File(System.getProperty("user.dir"), ".env")
        };

        for (java.io.File envFile : candidateLocations) {
            if (envFile.exists() && envFile.isFile()) {
                try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile, java.nio.charset.StandardCharsets.UTF_8))) {
                    String line;
                    int loadedCount = 0;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            if (val != null) {
                                if ("PINECONE_INDEX_NAME".equalsIgnoreCase(key)) {
                                    val = val.toLowerCase();
                                }
                                System.setProperty(key, val);
                                loadedCount++;
                            }
                        }
                    }
                    System.out.println("🌱 Loaded " + loadedCount + " environment variables from .env: " + envFile.getAbsolutePath());
                    break;
                } catch (Exception e) {
                    System.err.println("⚠️ Warning: could not parse .env file: " + e.getMessage());
                }
            }
        }
    }
}
