package com.deadlineiq.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a Career/Placement Opportunity, persisted in MySQL table 'opportunities'.
 */
@Entity
@Table(name = "opportunities")
public class Opportunity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String organization;

    @Column
    private String type;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String eligibility;

    @Column
    private LocalDate registrationDeadline;

    @Column
    private LocalDate eventDate;

    @Column
    private String createdBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "opportunity_registrations", joinColumns = @JoinColumn(name = "opportunity_id"))
    @Column(name = "student_email")
    private List<String> registeredStudentEmails = new ArrayList<>();

    public Opportunity() {}

    public Opportunity(Long id, String title, String organization, String type, String description,
                       String eligibility, LocalDate registrationDeadline, LocalDate eventDate, String createdBy) {
        this.id = id;
        this.title = title;
        this.organization = organization;
        this.type = type;
        this.description = description;
        this.eligibility = eligibility;
        this.registrationDeadline = registrationDeadline;
        this.eventDate = eventDate;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEligibility() { return eligibility; }
    public void setEligibility(String eligibility) { this.eligibility = eligibility; }

    public LocalDate getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDate registrationDeadline) { this.registrationDeadline = registrationDeadline; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public List<String> getRegisteredStudentEmails() { return registeredStudentEmails; }
    public void setRegisteredStudentEmails(List<String> registeredStudentEmails) { this.registeredStudentEmails = registeredStudentEmails; }

    public void registerStudent(String email) {
        if (email != null && !registeredStudentEmails.contains(email.toLowerCase())) {
            registeredStudentEmails.add(email.toLowerCase());
        }
    }

    public boolean isStudentRegistered(String email) {
        if (email == null) return false;
        return registeredStudentEmails.contains(email.toLowerCase());
    }

    public int getApplicantCount() {
        return registeredStudentEmails != null ? registeredStudentEmails.size() : 0;
    }
}
