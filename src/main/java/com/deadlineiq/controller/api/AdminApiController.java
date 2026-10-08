package com.deadlineiq.controller.api;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API Controller for Institutional Campus User Administration.
 * Connects directly to MySQL database 'deadlineiq' table 'users'.
 */
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AdminApiController {

    private final UserService userService;

    public AdminApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> response = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("name", u.getName());
            map.put("email", u.getEmail());
            map.put("role", u.getRole() == Role.ADMIN ? "ADMINISTRATOR" : u.getRole().name());
            map.put("department", u.getDepartment());
            map.put("subject", u.getSubject());
            map.put("section", u.getSection());
            map.put("academicYear", u.getYear());
            map.put("avatar", u.getName() != null && u.getName().length() >= 2 ? u.getName().substring(0, 2).toUpperCase() : "U");
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/users")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String email = payload.get("email");
        String roleStr = payload.get("role");
        String department = payload.get("department");
        String section = payload.get("section");
        String year = payload.get("academicYear");
        String subject = payload.get("subject");
        String password = payload.get("password");

        if (name == null || name.trim().isEmpty() || email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name and Email are required."));
        }

        Role role = Role.STUDENT;
        if (roleStr != null) {
            String cleanRole = roleStr.trim().toUpperCase();
            if (cleanRole.equals("ADMINISTRATOR") || cleanRole.equals("ADMIN")) role = Role.ADMIN;
            else if (cleanRole.equals("FACULTY")) role = Role.FACULTY;
            else if (cleanRole.equals("COORDINATOR")) role = Role.COORDINATOR;
        }

        if (password == null || password.trim().isEmpty()) {
            password = "password123";
        }

        User user = new User(
                null,
                name.trim(),
                email.trim().toLowerCase(),
                password,
                role,
                department != null ? department.trim() : "CSE",
                subject != null ? subject.trim() : null,
                year != null ? year.trim() : null,
                section != null ? section.trim() : null
        );

        boolean success = userService.register(user);
        if (success) {
            Map<String, Object> res = new HashMap<>();
            res.put("id", user.getId());
            res.put("name", user.getName());
            res.put("email", user.getEmail());
            res.put("role", user.getRole().name());
            res.put("department", user.getDepartment());
            res.put("section", user.getSection());
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } else {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "User with email already exists."));
        }
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable("id") Long id) {
        boolean deleted = userService.deleteUser(id);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true, "message", "User deleted successfully from MySQL"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userService.getAllUsers().size());
        stats.put("students", userService.countByRole(Role.STUDENT));
        stats.put("faculty", userService.countByRole(Role.FACULTY));
        stats.put("coordinators", userService.countByRole(Role.COORDINATOR));
        stats.put("admins", userService.countByRole(Role.ADMIN));
        return ResponseEntity.ok(stats);
    }
}
