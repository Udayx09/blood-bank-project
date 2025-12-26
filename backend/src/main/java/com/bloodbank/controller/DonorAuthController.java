package com.bloodbank.controller;

import com.bloodbank.entity.Donor;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.security.DonorPrincipal;
import com.bloodbank.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Donor Authentication Controller
 * Handles phone+password login for donors
 */
@RestController
@RequestMapping("/api/donor/auth")
@CrossOrigin(origins = "*")
public class DonorAuthController {

    private final DonorRepository donorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public DonorAuthController(DonorRepository donorRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider) {
        this.donorRepository = donorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Login with phone and password
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String password = request.get("password");

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Phone number is required"));
        }

        // Try to find donor with different phone formats
        Donor donor = donorRepository.findByPhone(phone).orElse(null);

        // If not found, try with 91 prefix (Indian phone format)
        if (donor == null && !phone.startsWith("91") && phone.length() == 10) {
            donor = donorRepository.findByPhone("91" + phone).orElse(null);
        }
        // If not found with prefix, try without it
        if (donor == null && phone.startsWith("91") && phone.length() == 12) {
            donor = donorRepository.findByPhone(phone.substring(2)).orElse(null);
        }

        if (donor == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "No account found with this phone number"));
        }

        // Check if donor has set a password
        if (donor.getPassword() == null || donor.getPassword().isEmpty()) {
            // Donor needs to set password first
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("requiresPasswordSetup", true);
            response.put("donorId", donor.getId());
            response.put("message", "Please set a password to continue");
            return ResponseEntity.ok(response);
        }

        // Donor has password - verify it
        if (password == null || password.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Password is required"));
        }

        if (!passwordEncoder.matches(password, donor.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid password"));
        }

        // Generate JWT token for donor
        String token = jwtTokenProvider.generateDonorToken(donor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("donor", convertToDonorDto(donor));

        return ResponseEntity.ok(response);
    }

    /**
     * Register new donor with password
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        String phone = (String) request.get("phone");
        String password = (String) request.get("password");
        String name = (String) request.get("name");
        String bloodType = (String) request.get("bloodType");
        String city = (String) request.get("city");
        String dateOfBirth = (String) request.get("dateOfBirth");
        Integer weight = request.get("weight") != null ? ((Number) request.get("weight")).intValue() : null;

        // Validation
        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Phone number is required"));
        }
        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Password must be at least 6 characters"));
        }
        if (name == null || name.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Name is required"));
        }
        if (bloodType == null || bloodType.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Blood type is required"));
        }
        if (dateOfBirth == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Date of birth is required"));
        }

        // Check if phone already exists
        if (donorRepository.existsByPhone(phone)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Phone number already registered"));
        }

        // Create donor
        Donor donor = new Donor();
        donor.setPhone(phone);
        donor.setPassword(passwordEncoder.encode(password));
        donor.setName(name);
        donor.setBloodType(bloodType);
        donor.setCity(city != null ? city : "solapur");
        donor.setDateOfBirth(LocalDate.parse(dateOfBirth));
        donor.setWeight(weight != null ? weight : 50);
        donor.setIsVerified(true); // Auto-verify since we have password
        donor.setIsAvailableForContact(true);
        donor.setCreatedAt(LocalDateTime.now());
        donor.setUpdatedAt(LocalDateTime.now());

        donor = donorRepository.save(donor);

        // Generate JWT token
        String token = jwtTokenProvider.generateDonorToken(donor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("donor", convertToDonorDto(donor));

        return ResponseEntity.ok(response);
    }

    /**
     * Set password for existing donor (first-time password setup)
     */
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(@RequestBody Map<String, Object> request) {
        Long donorId = request.get("donorId") != null ? ((Number) request.get("donorId")).longValue() : null;
        String phone = (String) request.get("phone");
        String password = (String) request.get("password");

        if (password == null || password.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Password must be at least 6 characters"));
        }

        Donor donor = null;
        if (donorId != null) {
            donor = donorRepository.findById(donorId).orElse(null);
        } else if (phone != null) {
            donor = donorRepository.findByPhone(phone).orElse(null);
        }

        if (donor == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Donor not found"));
        }

        // Set password
        donor.setPassword(passwordEncoder.encode(password));
        donor.setUpdatedAt(LocalDateTime.now());
        donorRepository.save(donor);

        // Generate JWT token
        String token = jwtTokenProvider.generateDonorToken(donor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("donor", convertToDonorDto(donor));

        return ResponseEntity.ok(response);
    }

    /**
     * Get authenticated donor profile
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal DonorPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        Donor donor = donorRepository.findById(principal.getId()).orElse(null);
        if (donor == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Donor not found"));
        }

        return ResponseEntity.ok(Map.of("success", true, "donor", convertToDonorDto(donor)));
    }

    private Map<String, Object> convertToDonorDto(Donor donor) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", donor.getId());
        dto.put("name", donor.getName());
        dto.put("phone", donor.getPhone());
        dto.put("bloodType", donor.getBloodType());
        dto.put("city", donor.getCity());
        dto.put("weight", donor.getWeight());
        dto.put("dateOfBirth", donor.getDateOfBirth() != null ? donor.getDateOfBirth().toString() : null);
        dto.put("lastDonationDate",
                donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : null);
        dto.put("isVerified", donor.getIsVerified());
        dto.put("isAvailableForContact", donor.getIsAvailableForContact());
        dto.put("eligible", donor.isEligible());
        dto.put("daysUntilEligible", donor.getDaysUntilEligible());
        dto.put("age", donor.getAge());
        dto.put("profilePhoto", donor.getProfilePhoto());
        return dto;
    }

    // ===================== PASSWORD RESET =====================

    /**
     * Send OTP for password reset (forgot password)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Phone number is required"));
        }

        // Normalize and find donor
        String normalizedPhone = normalizePhone(phone);
        Donor donor = donorRepository.findByPhone(normalizedPhone).orElse(null);

        // If not found, try with 91 prefix
        if (donor == null && !phone.startsWith("91") && phone.length() == 10) {
            normalizedPhone = "91" + phone;
            donor = donorRepository.findByPhone(normalizedPhone).orElse(null);
        }

        if (donor == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "No account found with this phone number"));
        }

        // Generate simple 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

        // Store OTP in temporary storage
        donor.setResetOtp(otp);
        donor.setResetOtpExpiry(LocalDateTime.now().plusMinutes(10));
        donorRepository.save(donor);

        // Send OTP via WhatsApp service
        try {
            sendPasswordResetOtp(normalizedPhone, otp);
        } catch (Exception e) {
            System.out.println("WhatsApp notification failed, OTP: " + otp);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "OTP sent to your WhatsApp");
        response.put("phone", normalizedPhone);

        return ResponseEntity.ok(response);
    }

    /**
     * Send OTP via WhatsApp service
     */
    private void sendPasswordResetOtp(String phoneNumber, String otp) {
        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            String jsonBody = String.format("{\"phoneNumber\":\"%s\",\"otp\":\"%s\"}", phoneNumber, otp);
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:3001/api/whatsapp/send-donor-otp"))
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
            client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println("Password Reset OTP sent via WhatsApp to " + phoneNumber);
        } catch (Exception e) {
            System.out.println("Failed to send WhatsApp OTP: " + e.getMessage());
        }
    }

    /**
     * Reset password with OTP verification
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String otp = request.get("otp");
        String newPassword = request.get("newPassword");

        if (phone == null || phone.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Phone number is required"));
        }
        if (otp == null || otp.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "OTP is required"));
        }
        if (newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Password must be at least 6 characters"));
        }

        // Find donor
        String normalizedPhone = normalizePhone(phone);
        Donor donor = donorRepository.findByPhone(normalizedPhone).orElse(null);

        if (donor == null && !phone.startsWith("91") && phone.length() == 10) {
            normalizedPhone = "91" + phone;
            donor = donorRepository.findByPhone(normalizedPhone).orElse(null);
        }

        if (donor == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "No account found with this phone number"));
        }

        // Verify OTP
        if (donor.getResetOtp() == null || !donor.getResetOtp().equals(otp)) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid OTP"));
        }

        if (donor.getResetOtpExpiry() == null || LocalDateTime.now().isAfter(donor.getResetOtpExpiry())) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "OTP has expired"));
        }

        // Update password
        donor.setPassword(passwordEncoder.encode(newPassword));
        donor.setResetOtp(null);
        donor.setResetOtpExpiry(null);
        donor.setUpdatedAt(LocalDateTime.now());
        donorRepository.save(donor);

        // Generate JWT token
        String token = jwtTokenProvider.generateDonorToken(donor.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Password reset successful");
        response.put("token", token);
        response.put("donor", convertToDonorDto(donor));

        return ResponseEntity.ok(response);
    }

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            return cleaned;
        }
        if (cleaned.length() == 10) {
            return "91" + cleaned;
        }
        return cleaned;
    }
}
