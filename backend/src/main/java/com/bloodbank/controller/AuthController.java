package com.bloodbank.controller;

import com.bloodbank.dto.AuthRequest;
import com.bloodbank.security.BankPrincipal;
import com.bloodbank.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/bank/register")
    public ResponseEntity<AuthRequest.AuthResponse> register(
            @RequestBody Map<String, Object> request) {

        String phone = (String) request.get("phone");
        String password = (String) request.get("password");
        Object bloodBankIdObj = request.get("bloodBankId");

        if (phone == null || password == null || bloodBankIdObj == null) {
            return ResponseEntity.badRequest().body(
                    AuthRequest.AuthResponse.builder()
                            .success(false)
                            .message("Phone, password, and bloodBankId are required")
                            .build());
        }

        Long bloodBankId;
        if (bloodBankIdObj instanceof Integer) {
            bloodBankId = ((Integer) bloodBankIdObj).longValue();
        } else if (bloodBankIdObj instanceof Long) {
            bloodBankId = (Long) bloodBankIdObj;
        } else {
            bloodBankId = Long.parseLong(bloodBankIdObj.toString());
        }

        AuthRequest.AuthResponse response = authService.register(phone, password, bloodBankId);

        if (response.isSuccess()) {
            return ResponseEntity.status(201).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/bank/login")
    public ResponseEntity<AuthRequest.AuthResponse> login(
            @RequestBody Map<String, String> request) {

        try {
            String phone = request.get("phone");
            String password = request.get("password");

            log.debug("Login attempt for phone: {}", phone);

            if (phone == null || password == null) {
                return ResponseEntity.badRequest().body(
                        AuthRequest.AuthResponse.builder()
                                .success(false)
                                .message("Phone and password are required")
                                .build());
            }

            AuthRequest.AuthResponse response = authService.login(phone, password);

            if (response.isSuccess()) {
                log.info("Login successful for phone: {}", phone);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for phone: {}", phone);
                return ResponseEntity.status(401).body(response);
            }
        } catch (Exception e) {
            log.error("Error during login: ", e);
            return ResponseEntity.status(500).body(
                    AuthRequest.AuthResponse.builder()
                            .success(false)
                            .message("An error occurred during login: " + e.getMessage())
                            .build());
        }
    }

    @GetMapping("/bank/me")
    public ResponseEntity<Map<String, Object>> getCurrentBank(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Not authenticated");
            return ResponseEntity.status(401).body(error);
        }

        return authService.getBankFromToken(principal.getId())
                .map(bank -> {
                    Map<String, Object> bankData = new HashMap<>();
                    bankData.put("id", bank.getId());
                    bankData.put("name", bank.getName());
                    bankData.put("email", bank.getEmail());
                    bankData.put("address", bank.getAddress());
                    bankData.put("city", bank.getCity());
                    bankData.put("phone", bank.getPhone());
                    bankData.put("is_open", bank.getIsOpen());

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("bank", bankData);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Bank not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    // Temporary debug endpoint - DELETE after fixing the issue
    @PostMapping("/bank/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String newPassword = request.get("password");

        if (phone == null || newPassword == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Phone and password are required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = authService.resetPassword(phone, newPassword);
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
