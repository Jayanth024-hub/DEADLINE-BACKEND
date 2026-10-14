package com.deadlineiq.controller.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Lightweight public health check endpoint for production deployment monitoring
 * (e.g. Render, Railway, AWS ECS, Kubernetes probes).
 */
@RestController
@RequestMapping("/api/health")
public class HealthApiController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
