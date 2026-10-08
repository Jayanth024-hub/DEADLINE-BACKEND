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
 * REST API Controller for Authentication (Used by Axios & Postman).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    private final UserService userService;

    public AuthApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpSession session) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        Optional<User> userOpt = userService.authenticate(email, password);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // Start web session (for browser navigation)
            session.setAttribute("currentUser", user);

            // Generate Postman API token
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
            userDetails.put("year", user.getYear());
            userDetails.put("section", user.getSection());
            userDetails.put("role", user.getRole().name());
            response.put("user", userDetails);
            response.put("redirectUrl", getDashboardUrl(user.getRole()));

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT);
        }
        boolean registered = userService.register(user);
        if (registered) {
            Map<String, Object> res = new HashMap<>();
            res.put("success", true);
            res.put("message", "Account registered successfully. You may now sign in.");
            return ResponseEntity.status(HttpStatus.CREATED).body(res);
        } else {
            Map<String, Object> err = new HashMap<>();
            err.put("success", false);
            err.put("error", "An account with this email already exists.");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request, HttpSession session) {
        User user = (User) session.getAttribute("currentUser");

        // Or check header for Postman
        if (user == null) {
            String token = request.getHeader("Authorization");
            if (token != null) {
                user = userService.getUserByToken(token.trim()).orElse(null);
            }
        }

        if (user != null) {
            Map<String, Object> userDetails = new HashMap<>();
            userDetails.put("id", user.getId());
            userDetails.put("name", user.getName());
            userDetails.put("email", user.getEmail());
            userDetails.put("role", user.getRole().name());
            userDetails.put("department", user.getDepartment());
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
            case DEAN: return "/dean/dashboard";
            case ADMIN: return "/admin/dashboard";
            default: return "/";
        }
    }
}
