package com.deadlineiq.controller.api;

import com.deadlineiq.model.Deadline;
import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.DeadlineService;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for Deadlines (Used by Axios dynamic UI & Postman).
 */
@RestController
@RequestMapping("/api/deadlines")
public class DeadlineApiController {

    private final DeadlineService deadlineService;
    private final UserService userService;

    public DeadlineApiController(DeadlineService deadlineService, UserService userService) {
        this.deadlineService = deadlineService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Deadline>> getDeadlines(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(request, session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (user.getRole() == Role.STUDENT) {
            return ResponseEntity.ok(deadlineService.getDeadlinesForStudent(user.getSection()));
        } else if (user.getRole() == Role.FACULTY) {
            return ResponseEntity.ok(deadlineService.getDeadlinesByFaculty(user.getEmail()));
        } else {
            return ResponseEntity.ok(deadlineService.getAllDeadlines());
        }
    }

    @PostMapping
    public ResponseEntity<?> createDeadline(@RequestBody Deadline deadline, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(request, session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }

        deadline.setCreatedBy(user.getEmail());
        Deadline saved = deadlineService.addDeadline(deadline);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> toggleComplete(@PathVariable("id") Long id) {
        boolean updated = deadlineService.markCompleted(id);
        if (updated) {
            Optional<Deadline> opt = deadlineService.findById(id);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "completed", opt.get().isCompleted(),
                    "status", opt.get().getStatus().name(),
                    "message", "Deadline completion updated"
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Deadline not found"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDeadline(@PathVariable("id") Long id) {
        boolean deleted = deadlineService.deleteDeadline(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Deadline deleted successfully"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Deadline not found"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats(HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(request, session);
        String section = (user != null && user.getSection() != null) ? user.getSection() : "Section A";
        return ResponseEntity.ok(deadlineService.getStudentStats(section));
    }

    private User resolveCurrentUser(HttpServletRequest request, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            String token = request.getHeader("Authorization");
            if (token != null) {
                user = userService.getUserByToken(token.trim()).orElse(null);
            }
        }
        return user;
    }
}
