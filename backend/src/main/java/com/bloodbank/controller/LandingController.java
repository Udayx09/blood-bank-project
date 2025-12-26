package com.bloodbank.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class LandingController {

    private static final Logger log = LoggerFactory.getLogger(LandingController.class);

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("message", "Blood Bank API is running");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Blood Bank API");
        response.put("version", "1.0.0");

        Map<String, String> endpoints = new HashMap<>();
        endpoints.put("health", "/api/health");
        endpoints.put("bloodBanks", "/api/blood-banks");
        endpoints.put("search", "/api/blood-banks/search?bloodType=A+");
        endpoints.put("reservations", "/api/reservations");
        response.put("endpoints", endpoints);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/landing")
    public ResponseEntity<Map<String, Object>> getLandingContent() {
        Map<String, Object> content = new HashMap<>();

        Map<String, Object> hero = new HashMap<>();
        hero.put("badge", "🩸 Saving Lives Together");
        hero.put("title", "Every Drop Counts, Every Donor Matters");
        hero.put("quote", "The blood you donate gives someone another chance at life.");

        List<Map<String, String>> stats = Arrays.asList(
                Map.of("number", "10K+", "label", "Lives Saved"),
                Map.of("number", "500+", "label", "Blood Banks"),
                Map.of("number", "24/7", "label", "Support"));
        hero.put("stats", stats);
        content.put("hero", hero);

        content.put("testimonials", getTestimonialsList());

        Map<String, String> contact = new HashMap<>();
        contact.put("email", "admin@bloodbankproject.com");
        contact.put("phone", "+1-234-567-8900");
        contact.put("address", "123 Donation Lane, Bloodville, Healthcare State, 45678");
        content.put("contact", contact);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", content);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/testimonials")
    public ResponseEntity<Map<String, Object>> getTestimonials() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", getTestimonialsList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/contact")
    public ResponseEntity<Map<String, Object>> submitContact(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");

        log.info("Contact form received from: {} ({})", name, email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Thank you for contacting us! We'll get back to you soon.");

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> getTestimonialsList() {
        return Arrays.asList(
                Map.of(
                        "id", 1L,
                        "quote", "This platform helped me find the exact blood type I needed.",
                        "author", "Sarah K.",
                        "initials", "SK",
                        "role", "Blood Recipient's Family",
                        "featured", false),
                Map.of(
                        "id", 2L,
                        "quote", "As a regular donor, I love how easy it is to track my donations.",
                        "author", "Michael J.",
                        "initials", "MJ",
                        "role", "Regular Donor",
                        "featured", true),
                Map.of(
                        "id", 3L,
                        "quote", "It has transformed how we manage emergency blood requirements.",
                        "author", "Dr. Rachel M.",
                        "initials", "DR",
                        "role", "Hospital Administrator",
                        "featured", false));
    }
}
