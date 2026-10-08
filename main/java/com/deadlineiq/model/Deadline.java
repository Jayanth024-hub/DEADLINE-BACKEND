package com.deadlineiq.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * JPA Entity representing a Deadline in DeadlineIQ, persisted in MySQL table 'deadlines'.
 */
@Entity
@Table(name = "deadlines")
public class Deadline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column
    private String createdBy;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeadlineStatus status;

    @Column
    private String targetClass;

    @Column
    private String targetSection;

    @Column(nullable = false)
    private boolean completed;

    public Deadline() {}

    public Deadline(Long id, String title, String description, Category category, String createdBy,
                    LocalDate dueDate, Priority priority, String targetClass, String targetSection) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.createdBy = createdBy;
        this.dueDate = dueDate;
        this.priority = priority;
        this.targetClass = targetClass;
        this.targetSection = targetSection;
        this.completed = false;
        this.status = calculateStatus(dueDate, false);
    }

    @PrePersist
    @PreUpdate
    private void autoUpdateStatus() {
        this.status = calculateStatus(this.dueDate, this.completed);
    }

    /**
     * Pure Java Date/Time business logic to calculate urgency.
     */
    public static DeadlineStatus calculateStatus(LocalDate dueDate, boolean completed) {
        if (completed) {
            return DeadlineStatus.COMPLETED;
        }
        if (dueDate == null) {
            return DeadlineStatus.UPCOMING;
        }
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            return DeadlineStatus.OVERDUE;
        } else if (dueDate.isEqual(today)) {
            return DeadlineStatus.DUE_TODAY;
        } else if (dueDate.isBefore(today.plusDays(3)) || dueDate.isEqual(today.plusDays(3))) {
            return DeadlineStatus.DUE_SOON;
        } else {
            return DeadlineStatus.UPCOMING;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        this.status = calculateStatus(dueDate, this.completed);
    }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public DeadlineStatus getStatus() {
        return calculateStatus(this.dueDate, this.completed);
    }

    public void setStatus(DeadlineStatus status) { this.status = status; }

    public String getTargetClass() { return targetClass; }
    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }

    public String getTargetSection() { return targetSection; }
    public void setTargetSection(String targetSection) { this.targetSection = targetSection; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) {
        this.completed = completed;
        this.status = calculateStatus(this.dueDate, completed);
    }
}
