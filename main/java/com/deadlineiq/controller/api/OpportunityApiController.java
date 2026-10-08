package com.deadlineiq.controller.api;

import com.deadlineiq.model.Opportunity;
import com.deadlineiq.model.User;
import com.deadlineiq.service.OpportunityService;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller for Campus Placement Drives & Hackathons (Axios & Postman).
 */
@RestController
@RequestMapping("/api/opportunities")
public class OpportunityApiController {

    private final OpportunityService opportunityService;
    private final UserService userService;

    public OpportunityApiController(OpportunityService opportunityService, UserService userService) {
        this.opportunityService = opportunityService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<Opportunity>> getAllOpportunities() {
        return ResponseEntity.ok(opportunityService.getAllOpportunities());
    }

    @PostMapping
    public ResponseEntity<?> createOpportunity(@RequestBody Opportunity opp, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(request, session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        opp.setCreatedBy(user.getEmail());
        Opportunity created = opportunityService.addOpportunity(opp);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/register")
    public ResponseEntity<?> registerForOpportunity(@PathVariable("id") Long id, HttpServletRequest request, HttpSession session) {
        User user = resolveCurrentUser(request, session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Please sign in to register"));
        }

        boolean registered = opportunityService.registerStudent(id, user.getEmail());
        if (registered) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Successfully registered for opportunity!"));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Opportunity not found"));
        }
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
