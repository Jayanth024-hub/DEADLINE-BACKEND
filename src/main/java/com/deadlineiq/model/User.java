package com.deadlineiq.model;

import jakarta.persistence.*;

/**
 * JPA Entity representing a User in DeadlineIQ, persisted in MySQL table 'users'.
 * Department and Subject are separate fields:
 * - department: engineering department (CSE, ECE, EEE, AIML, Cyber Security)
 * - subject: teaching subject for FACULTY (NULL for other roles)
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @com.fasterxml.jackson.annotation.JsonProperty(access = com.fasterxml.jackson.annotation.JsonProperty.Access.WRITE_ONLY)
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column
    private String department;

    @Column
    private String subject;

    @Column(name = "academic_year")
    private String year;

    @Column
    private String section;

    public User() {}

    public User(Long id, String name, String email, String password, Role role, String department, String subject, String year, String section) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
        this.subject = subject;
        this.year = year;
        this.section = section;
    }

    public User(Long id, String name, String email, String password, Role role, String department, String year, String section) {
        this(id, name, email, password, role, department, null, year, section);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }
}
