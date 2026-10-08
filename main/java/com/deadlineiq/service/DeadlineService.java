package com.deadlineiq.service;

import com.deadlineiq.model.Category;
import com.deadlineiq.model.Deadline;
import com.deadlineiq.model.DeadlineStatus;
import com.deadlineiq.model.Priority;
import com.deadlineiq.repository.DeadlineRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service managing all academic & personal deadlines persisted in MySQL via Spring Data JPA.
 */
@Service
public class DeadlineService {

    private final DeadlineRepository deadlineRepository;

    public DeadlineService(DeadlineRepository deadlineRepository) {
        this.deadlineRepository = deadlineRepository;
    }

    @PostConstruct
    public void initDefaultDeadlines() {
        if (deadlineRepository.count() == 0) {
            LocalDate today = LocalDate.now();

            // 1. Due Today
            deadlineRepository.save(new Deadline(null,
                    "CS301: Relational Algebra & SQL Optimization",
                    "Complete questions 1 to 5 on relational algebra transformations and indexed queries.",
                    Category.ASSIGNMENT, "faculty@deadlineiq.com", today, Priority.HIGH, "CSE 3-1", "Section A"));

            // 2. Due Soon (in 2 days)
            deadlineRepository.save(new Deadline(null,
                    "CS302: Process Synchronization Lab Record",
                    "Implement reader-writer and producer-consumer synchronization problems using POSIX semaphores.",
                    Category.LAB, "faculty@deadlineiq.com", today.plusDays(2), Priority.HIGH, "CSE 3-1", "Section A"));

            // 3. Upcoming (in 6 days)
            deadlineRepository.save(new Deadline(null,
                    "CS303: Minimum Spanning Tree Implementation",
                    "Implement Kruskal and Prim algorithms in Java and benchmark performance on random graphs.",
                    Category.PROJECT, "faculty@deadlineiq.com", today.plusDays(6), Priority.MEDIUM, "CSE 3-1", "Section A"));

            // 4. Overdue (2 days ago)
            deadlineRepository.save(new Deadline(null,
                    "CS204: Discrete Mathematics Tutorial Sheet 1",
                    "Graph theory proofs on planar graphs and Euler paths.",
                    Category.ASSIGNMENT, "faculty@deadlineiq.com", today.minusDays(2), Priority.LOW, "CSE 3-1", "Section A"));

            // 5. Academic Certification Deadline
            deadlineRepository.save(new Deadline(null,
                    "AWS Certified Cloud Practitioner Exam Preparation",
                    "Finish practice exams and review cloud security whitepapers.",
                    Category.CERTIFICATION, "academic-cell", today.plusDays(10), Priority.MEDIUM, "Personal", "All"));
        }
    }

    public List<Deadline> getAllDeadlines() {
        List<Deadline> list = deadlineRepository.findAll();
        list.forEach(d -> d.setStatus(d.getStatus()));
        return list;
    }

    public List<Deadline> getDeadlinesForStudent(String section) {
        return getAllDeadlines().stream()
                .filter(d -> "All".equalsIgnoreCase(d.getTargetSection())
                        || "Personal".equalsIgnoreCase(d.getTargetClass())
                        || (section != null && section.equalsIgnoreCase(d.getTargetSection())))
                .collect(Collectors.toList());
    }

    public List<Deadline> getDeadlinesByFaculty(String email) {
        if (email == null) return Collections.emptyList();
        return deadlineRepository.findByCreatedByIgnoreCase(email);
    }

    public Optional<Deadline> findById(Long id) {
        if (id == null) return Optional.empty();
        return deadlineRepository.findById(id);
    }

    public Deadline addDeadline(Deadline deadline) {
        if (deadline.getDueDate() == null) {
            deadline.setDueDate(LocalDate.now().plusDays(3));
        }
        deadline.setStatus(Deadline.calculateStatus(deadline.getDueDate(), false));
        return deadlineRepository.save(deadline);
    }

    public boolean markCompleted(Long id) {
        Optional<Deadline> opt = deadlineRepository.findById(id);
        if (opt.isPresent()) {
            Deadline d = opt.get();
            d.setCompleted(!d.isCompleted());
            deadlineRepository.save(d);
            return true;
        }
        return false;
    }

    public boolean deleteDeadline(Long id) {
        if (id != null && deadlineRepository.existsById(id)) {
            deadlineRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Map<String, Object> getStudentStats(String section) {
        List<Deadline> list = getDeadlinesForStudent(section);
        long total = list.size();
        long completed = list.stream().filter(Deadline::isCompleted).count();
        long pending = total - completed;
        long dueToday = list.stream().filter(d -> !d.isCompleted() && d.getStatus() == DeadlineStatus.DUE_TODAY).count();
        long dueSoon = list.stream().filter(d -> !d.isCompleted() && d.getStatus() == DeadlineStatus.DUE_SOON).count();
        long overdue = list.stream().filter(d -> !d.isCompleted() && d.getStatus() == DeadlineStatus.OVERDUE).count();
        int percentage = total > 0 ? (int) Math.round(((double) completed / total) * 100) : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("dueToday", dueToday);
        stats.put("dueSoon", dueSoon);
        stats.put("overdue", overdue);
        stats.put("percentage", percentage);
        return stats;
    }
}
