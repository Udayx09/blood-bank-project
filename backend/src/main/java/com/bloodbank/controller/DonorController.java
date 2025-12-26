package com.bloodbank.controller;

import com.bloodbank.dto.DonorDto;
import com.bloodbank.service.DonorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/donor")
@CrossOrigin(origins = "*")
public class DonorController {

    private final DonorService donorService;

    public DonorController(DonorService donorService) {
        this.donorService = donorService;
    }

    /**
     * Send OTP to phone number
     */
    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");

        if (phone == null || phone.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Phone number is required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = donorService.sendOtp(phone);
        return ResponseEntity.ok(result);
    }

    /**
     * Verify OTP
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String otp = request.get("otp");

        if (phone == null || otp == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Phone and OTP are required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> result = donorService.verifyOtp(phone, otp);
        return ResponseEntity.ok(result);
    }

    /**
     * Register new donor
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> registerDonor(@RequestBody DonorDto donorDto) {
        // Validate required fields
        if (donorDto.getName() == null || donorDto.getName().trim().isEmpty()) {
            return badRequest("Name is required");
        }
        if (donorDto.getPhone() == null || donorDto.getPhone().trim().isEmpty()) {
            return badRequest("Phone number is required");
        }
        if (donorDto.getBloodType() == null || donorDto.getBloodType().trim().isEmpty()) {
            return badRequest("Blood type is required");
        }
        if (donorDto.getDateOfBirth() == null) {
            return badRequest("Date of birth is required");
        }
        if (donorDto.getCity() == null || donorDto.getCity().trim().isEmpty()) {
            return badRequest("City is required");
        }
        if (donorDto.getWeight() == null || donorDto.getWeight() < 50) {
            return badRequest("Weight must be at least 50 kg");
        }

        // Validate age (18-65)
        int age = calculateAge(donorDto.getDateOfBirth());
        if (age < 18 || age > 65) {
            return badRequest("Age must be between 18 and 65 years");
        }

        Map<String, Object> result = donorService.registerDonor(donorDto);
        return ResponseEntity.ok(result);
    }

    /**
     * Get donor profile by phone (after OTP verification)
     */
    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam String phone) {
        Optional<DonorDto> donor = donorService.getDonorByPhone(phone);

        Map<String, Object> result = new HashMap<>();
        if (donor.isPresent()) {
            result.put("success", true);
            result.put("donor", donor.get());
        } else {
            result.put("success", false);
            result.put("error", "Donor not found");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get donor profile by ID
     */
    @GetMapping("/profile/{id}")
    public ResponseEntity<Map<String, Object>> getProfileById(@PathVariable Long id) {
        Optional<DonorDto> donor = donorService.getDonorById(id);

        Map<String, Object> result = new HashMap<>();
        if (donor.isPresent()) {
            result.put("success", true);
            result.put("donor", donor.get());
        } else {
            result.put("success", false);
            result.put("error", "Donor not found");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Update donor profile
     */
    @PutMapping("/profile/{id}")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @PathVariable Long id,
            @RequestBody DonorDto donorDto) {
        Map<String, Object> result = donorService.updateDonor(id, donorDto);
        return ResponseEntity.ok(result);
    }

    /**
     * Record a donation
     */
    @PostMapping("/record-donation")
    public ResponseEntity<Map<String, Object>> recordDonation(@RequestBody Map<String, Object> request) {
        Long donorId = Long.valueOf(request.get("donorId").toString());
        Long bloodBankId = request.get("bloodBankId") != null ? Long.valueOf(request.get("bloodBankId").toString())
                : null;
        LocalDate donationDate = request.get("donationDate") != null
                ? LocalDate.parse(request.get("donationDate").toString())
                : LocalDate.now();

        Map<String, Object> result = donorService.recordDonation(donorId, bloodBankId, donationDate);
        return ResponseEntity.ok(result);
    }

    /**
     * Get donation history
     */
    @GetMapping("/history/{donorId}")
    public ResponseEntity<Map<String, Object>> getDonationHistory(@PathVariable Long donorId) {
        List<Map<String, Object>> history = donorService.getDonationHistory(donorId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("donations", history);
        result.put("totalDonations", history.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Find available donors for blood type in city (for blood banks)
     */
    @GetMapping("/available")
    public ResponseEntity<Map<String, Object>> findAvailableDonors(
            @RequestParam String bloodType,
            @RequestParam String city) {

        List<DonorDto> donors = donorService.findAvailableDonors(bloodType, city);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("donors", donors);
        result.put("count", donors.size());

        return ResponseEntity.ok(result);
    }

    // Helper methods

    private ResponseEntity<Map<String, Object>> badRequest(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        return ResponseEntity.badRequest().body(error);
    }

    private int calculateAge(LocalDate dateOfBirth) {
        return java.time.Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Update donor's contact availability (opt-in/opt-out)
     */
    @PutMapping("/{id}/contact-availability")
    public ResponseEntity<Map<String, Object>> updateContactAvailability(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {
        Boolean available = request.get("available");
        if (available == null) {
            return badRequest("'available' field is required");
        }
        Map<String, Object> result = donorService.updateContactAvailability(id, available);
        return ResponseEntity.ok(result);
    }

    /**
     * Get incoming donation requests for a donor
     */
    @GetMapping("/{donorId}/requests")
    public ResponseEntity<Map<String, Object>> getDonorRequests(@PathVariable Long donorId) {
        List<Map<String, Object>> requests = donorService.getDonorRequests(donorId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("requests", requests);
        result.put("count", requests.size());

        return ResponseEntity.ok(result);
    }

    /**
     * Respond to a donation request (accept or decline)
     */
    @PutMapping("/requests/{requestId}/respond")
    public ResponseEntity<Map<String, Object>> respondToRequest(
            @PathVariable Long requestId,
            @RequestBody Map<String, Object> request) {
        Long donorId = Long.valueOf(request.get("donorId").toString());
        Boolean accept = (Boolean) request.get("accept");

        if (donorId == null || accept == null) {
            return badRequest("donorId and accept are required");
        }

        Map<String, Object> result = donorService.respondToRequest(requestId, donorId, accept);
        return ResponseEntity.ok(result);
    }

    /**
     * Login with phone and password
     */
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> loginWithPassword(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        String password = request.get("password");

        if (phone == null || password == null) {
            return badRequest("Phone and password are required");
        }

        Map<String, Object> result = donorService.loginWithPassword(phone, password);
        return ResponseEntity.ok(result);
    }

    /**
     * Set password for donor (after OTP verification)
     */
    @PostMapping("/{donorId}/set-password")
    public ResponseEntity<Map<String, Object>> setPassword(
            @PathVariable Long donorId,
            @RequestBody Map<String, String> request) {
        String newPassword = request.get("password");

        if (newPassword == null) {
            return badRequest("Password is required");
        }

        Map<String, Object> result = donorService.setPassword(donorId, newPassword);
        return ResponseEntity.ok(result);
    }

    /**
     * Check if donor has password set
     */
    @GetMapping("/{donorId}/has-password")
    public ResponseEntity<Map<String, Object>> hasPassword(@PathVariable Long donorId) {
        boolean hasPassword = donorService.hasPasswordSet(donorId);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("hasPassword", hasPassword);
        return ResponseEntity.ok(result);
    }
}
