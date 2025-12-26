package com.bloodbank.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Root controller for handling requests to /
 */
@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Blood Bank API");
        response.put("version", "1.0.0");
        response.put("status", "running");
        response.put("apiBase", "/api");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/api/health");
        endpoints.put("bloodBanks", "/api/blood-banks");
        endpoints.put("reservations", "/api/reservations");
        endpoints.put("admin", "/api/admin/stats");
        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }
}
