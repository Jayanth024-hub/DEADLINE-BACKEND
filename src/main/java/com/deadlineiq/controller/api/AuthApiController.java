package com.deadlineiq.controller.api;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST API Controller for Authentication (Used by React Frontend).
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3001", "http://localhost:3000", "http://127.0.0.1:3001", "http://127.0.0.1:3000"}, allowCredentials = "true")
public class AuthApiController {

    private final UserService userService;

    public AuthApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || email.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Please provide both email and password."));
        }

        Optional<User> userOpt = userService.authenticate(email.trim(), password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Start web session
            session.setAttribute("currentUser", user);

            // Generate API token
            String apiToken = userService.generateApiToken(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Authentication successful");
            response.put("token", apiToken);
            response.put("role", user.getRole().name());

            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("name", user.getName());
            userDetails.put("email", user.getEmail());
            userDetails.put("department", user.getDepartment());
            userDetails.put("subject", user.getSubject());
            userDetails.put("year", user.getYear());
            userDetails.put("section", user.getSection());
            userDetails.put("role", user.getRole().name());
            response.put("user", userDetails);
            response.put("redirectUrl", getDashboardUrl(user.getRole()));

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Invalid email or password. Please try again.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            // 1. Basic field validation
            if (user.getName() == null || user.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Full name is required."));
            }
            if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Email address is required."));
            }
            if (!user.getEmail().contains("@") || !user.getEmail().contains(".")) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Please enter a valid email address."));
            }
            if (user.getPassword() == null || user.getPassword().length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Password must be at least 6 characters long."));
            }

            // 2. Default role to STUDENT if not provided
            if (user.getRole() == null) {
                user.setRole(Role.STUDENT);
            }

            // 3. Department is required for ALL roles
            if (user.getDepartment() == null || user.getDepartment().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Department is required."));
            }
            String dept = user.getDepartment().trim();
            if (!java.util.Set.of("CSE", "ECE", "EEE", "AIML", "Cyber Security").contains(dept)) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid department. Allowed departments: CSE, ECE, EEE, AIML, Cyber Security."));
            }
            user.setDepartment(dept);

            // 4. Role-specific validation & sanitization
            if (user.getRole() == Role.FACULTY) {
                if (user.getSubject() == null || user.getSubject().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Teaching subject is required for faculty."));
                }
                user.setSubject(user.getSubject().trim());
                user.setYear(null);
                user.setSection(null);
            } else if (user.getRole() == Role.STUDENT) {
                if (user.getYear() == null || user.getYear().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Academic year is required for students."));
                }
                if (user.getSection() == null || user.getSection().trim().isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Section is required for students."));
                }
                user.setYear(user.getYear().trim());
                user.setSection(user.getSection().trim());
                user.setSubject(null);
            } else {
                // COORDINATOR, ADMIN
                user.setSubject(null);
                user.setYear(null);
                user.setSection(null);
            }

            // 5. Check for duplicate email
            if (userService.findByEmail(user.getEmail().trim()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(
                        Map.of("success", false, "error", "An account with this email already exists.")
                );
            }

            // 5. Persist user in MySQL
            boolean registered = userService.register(user);
            if (registered) {
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("message", "Account registered successfully. You may now sign in.");
                return ResponseEntity.status(HttpStatus.CREATED).body(res);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                        Map.of("success", false, "error", "Failed to register user. Please try again.")
                );
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("success", false, "error", "Registration error: " + e.getMessage())
            );
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");

        if (user == null) {
            String token = request.getHeader("Authorization");
            if (token != null) {
                user = userService.getUserByToken(token.trim()).orElse(null);
                if (user != null) {
                    session.setAttribute("currentUser", user);
                }
            }
        }

        if (user != null) {
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("name", user.getName());
            userDetails.put("email", user.getEmail());
            userDetails.put("role", user.getRole().name());
            userDetails.put("department", user.getDepartment());
            userDetails.put("subject", user.getSubject());
            userDetails.put("year", user.getYear() != null ? user.getYear() : "");
            userDetails.put("section", user.getSection() != null ? user.getSection() : "");
            return ResponseEntity.ok(userDetails);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpSession session) {
        String token = request.getHeader("Authorization");
        if (token != null) {
            userService.invalidateToken(token.trim());
        }
        session.invalidate();
        return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
    }

    private String getDashboardUrl(Role role) {
        switch (role) {
            case STUDENT: return "/student/dashboard";
            case FACULTY: return "/faculty/dashboard";
            case COORDINATOR: return "/coordinator/dashboard";
            case ADMIN: return "/admin/dashboard";
            default: return "/";
        }
    }
}
