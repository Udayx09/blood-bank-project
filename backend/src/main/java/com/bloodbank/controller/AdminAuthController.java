package com.bloodbank.controller;

import com.bloodbank.entity.Admin;
import com.bloodbank.repository.AdminRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/admin")
public class AdminAuthController {

    private static final Logger log = LoggerFactory.getLogger(AdminAuthController.class);

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${admin.default.password:udaysproject18}")
    private String defaultAdminPassword;

    public AdminAuthController(AdminRepository adminRepository) {
        this.adminRepository = adminRepository;
    }

    @PostConstruct
    public void initDefaultAdmin() {
        // Create default admin if not exists
        if (!adminRepository.existsByUsername("admin")) {
            Admin admin = new Admin();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(defaultAdminPassword));
            admin.setEmail("admin@bloodbank.com");
            adminRepository.save(admin);
            log.info("Created default admin user");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        Map<String, Object> response = new HashMap<>();

        if (username == null || password == null) {
            response.put("success", false);
            response.put("error", "Username and password are required");
            return ResponseEntity.badRequest().body(response);
        }

        return adminRepository.findByUsername(username)
                .map(admin -> {
                    if (passwordEncoder.matches(password, admin.getPasswordHash())) {
                        // Generate JWT token
                        String token = generateToken(admin);
                        response.put("success", true);
                        response.put("token", token);
                        response.put("admin", Map.of(
                                "id", admin.getId(),
                                "username", admin.getUsername(),
                                "email", admin.getEmail()));
                        log.info("Admin login successful: {}", username);
                        return ResponseEntity.ok(response);
                    } else {
                        response.put("success", false);
                        response.put("error", "Invalid password");
                        log.warn("Admin login failed - wrong password: {}", username);
                        return ResponseEntity.status(401).body(response);
                    }
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("error", "Admin not found");
                    log.warn("Admin login failed - not found: {}", username);
                    return ResponseEntity.status(401).body(response);
                });
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        Map<String, Object> response = new HashMap<>();

        if (username == null || oldPassword == null || newPassword == null) {
            response.put("success", false);
            response.put("error", "All fields are required");
            return ResponseEntity.badRequest().body(response);
        }

        return adminRepository.findByUsername(username)
                .map(admin -> {
                    if (passwordEncoder.matches(oldPassword, admin.getPasswordHash())) {
                        admin.setPasswordHash(passwordEncoder.encode(newPassword));
                        adminRepository.save(admin);
                        response.put("success", true);
                        response.put("message", "Password changed successfully");
                        return ResponseEntity.ok(response);
                    } else {
                        response.put("success", false);
                        response.put("error", "Current password is incorrect");
                        return ResponseEntity.status(401).body(response);
                    }
                })
                .orElseGet(() -> {
                    response.put("success", false);
                    response.put("error", "Admin not found");
                    return ResponseEntity.status(404).body(response);
                });
    }

    private String generateToken(Admin admin) {
        Key key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .setSubject(admin.getUsername())
                .claim("adminId", admin.getId())
                .claim("type", "admin")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
