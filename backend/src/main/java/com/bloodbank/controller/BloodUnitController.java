package com.bloodbank.controller;

import com.bloodbank.entity.BloodBank;
import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.BloodUnit.BloodComponent;
import com.bloodbank.entity.BloodUnit.UnitStatus;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.DonationRepository;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Blood Unit Management with Component Tracking
 */
@RestController
@RequestMapping("/api/bank/units")
@CrossOrigin(origins = "*")
public class BloodUnitController {

    private static final Logger log = LoggerFactory.getLogger(BloodUnitController.class);

    private final BloodUnitRepository bloodUnitRepository;
    private final BloodBankRepository bloodBankRepository;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final JwtTokenProvider jwtTokenProvider;

    public BloodUnitController(BloodUnitRepository bloodUnitRepository,
            BloodBankRepository bloodBankRepository,
            DonorRepository donorRepository,
            DonationRepository donationRepository,
            JwtTokenProvider jwtTokenProvider) {
        this.bloodUnitRepository = bloodUnitRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Get all blood component types
     */
    @GetMapping("/components")
    public ResponseEntity<Map<String, Object>> getComponents() {
        List<Map<String, Object>> components = Arrays.stream(BloodComponent.values())
                .map(c -> {
                    Map<String, Object> comp = new HashMap<>();
                    comp.put("code", c.name());
                    comp.put("name", c.getDisplayName());
                    comp.put("shelfLifeDays", c.getShelfLifeDays());
                    return comp;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", components);
        return ResponseEntity.ok(response);
    }

    /**
     * Get all units for the authenticated blood bank
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUnits(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String bloodType,
            @RequestParam(required = false) String component) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        List<BloodUnit> units;
        if (status != null && !status.isEmpty()) {
            UnitStatus unitStatus = UnitStatus.valueOf(status.toUpperCase());
            units = bloodUnitRepository.findByBloodBankIdAndStatusOrderByExpiryDateAsc(bankId, unitStatus);
        } else {
            units = bloodUnitRepository.findByBloodBankIdOrderByExpiryDateAsc(bankId);
        }

        // Apply filters
        if (bloodType != null && !bloodType.isEmpty()) {
            units = units.stream()
                    .filter(u -> u.getBloodType().equals(bloodType))
                    .collect(Collectors.toList());
        }
        if (component != null && !component.isEmpty()) {
            BloodComponent comp = BloodComponent.valueOf(component.toUpperCase());
            units = units.stream()
                    .filter(u -> u.getComponent() == comp)
                    .collect(Collectors.toList());
        }

        List<Map<String, Object>> data = units.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", data, "count", data.size()));
    }

    /**
     * Get units expiring soon
     */
    @GetMapping("/expiring")
    public ResponseEntity<Map<String, Object>> getExpiringUnits(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "7") int days) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        List<BloodUnit> units = bloodUnitRepository.findExpiringWithinDays(bankId, days);
        List<Map<String, Object>> data = units.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", data, "count", data.size()));
    }

    /**
     * Get expiry summary stats
     */
    @GetMapping("/expiry-summary")
    public ResponseEntity<Map<String, Object>> getExpirySummary(
            @RequestHeader("Authorization") String authHeader) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        LocalDate today = LocalDate.now();

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalAvailable", bloodUnitRepository.countByBloodBankIdAndStatus(bankId, UnitStatus.AVAILABLE));
        summary.put("expiringIn3Days", bloodUnitRepository.countExpiringBetween(bankId, today, today.plusDays(3)));
        summary.put("expiringIn7Days", bloodUnitRepository.countExpiringBetween(bankId, today, today.plusDays(7)));
        summary.put("expiringIn14Days", bloodUnitRepository.countExpiringBetween(bankId, today, today.plusDays(14)));

        // Critical: platelets expiring soon (not already expired)
        LocalDate threeDaysLater = today.plusDays(3);
        List<BloodUnit> criticalPlateletUnits = bloodUnitRepository
                .findByBloodBankIdAndStatusOrderByExpiryDateAsc(bankId, UnitStatus.AVAILABLE);
        long criticalPlatelets = criticalPlateletUnits.stream()
                .filter(u -> u.getComponent() == BloodComponent.PLATELETS_RDP || u.getComponent() == BloodComponent.SDP)
                .filter(u -> u.getExpiryDate() != null &&
                        !u.getExpiryDate().isBefore(today) &&
                        !u.getExpiryDate().isAfter(threeDaysLater))
                .count();
        summary.put("criticalPlatelets", criticalPlatelets);

        return ResponseEntity.ok(Map.of("success", true, "data", summary));
    }

    /**
     * Add new blood unit
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addUnit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            String bloodType = (String) request.get("bloodType");
            String componentStr = (String) request.get("component");
            String collectionDateStr = (String) request.get("collectionDate");
            String unitNumber = (String) request.get("unitNumber");
            Long donorId = request.get("donorId") != null ? Long.valueOf(request.get("donorId").toString()) : null;

            // Validation
            if (bloodType == null || componentStr == null || collectionDateStr == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing required fields"));
            }

            BloodComponent component = BloodComponent.valueOf(componentStr.toUpperCase());
            LocalDate collectionDate = LocalDate.parse(collectionDateStr);

            // Generate unit number if not provided
            if (unitNumber == null || unitNumber.isEmpty()) {
                unitNumber = generateUnitNumber(bankId);
            }

            // Check for duplicate unit number
            if (bloodUnitRepository.existsByUnitNumber(unitNumber)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Unit number already exists"));
            }

            BloodBank bank = bloodBankRepository.findById(bankId).orElse(null);
            if (bank == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Blood bank not found"));
            }

            BloodUnit unit = new BloodUnit();
            unit.setBloodBank(bank);
            unit.setBloodType(bloodType.toUpperCase());
            unit.setComponent(component);
            unit.setCollectionDate(collectionDate);
            unit.setUnitNumber(unitNumber);
            unit.setStatus(UnitStatus.AVAILABLE);

            // Link donor if provided
            if (donorId != null) {
                Donor donor = donorRepository.findById(donorId).orElse(null);
                if (donor != null) {
                    unit.setDonor(donor);
                }
            }

            bloodUnitRepository.save(unit);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Blood unit added successfully",
                    "data", convertToDto(unit)));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Record a donation - creates multiple blood units from one donation
     */
    @PostMapping("/record-donation")
    public ResponseEntity<Map<String, Object>> recordDonation(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            String bloodType = (String) request.get("bloodType");
            String collectionDateStr = (String) request.get("collectionDate");
            @SuppressWarnings("unchecked")
            List<String> componentsList = (List<String>) request.get("components");
            Long donorId = request.get("donorId") != null ? Long.valueOf(request.get("donorId").toString()) : null;

            // Walk-in donor details
            String donorName = (String) request.get("donorName");
            String donorPhone = (String) request.get("donorPhone");
            String donorDateOfBirthStr = (String) request.get("donorDateOfBirth");

            if (bloodType == null || collectionDateStr == null || componentsList == null || componentsList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Missing required fields"));
            }

            LocalDate collectionDate = LocalDate.parse(collectionDateStr);

            BloodBank bank = bloodBankRepository.findById(bankId).orElse(null);
            if (bank == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Blood bank not found"));
            }

            Donor donor = null;
            if (donorId != null) {
                donor = donorRepository.findById(donorId).orElse(null);
            } else if (donorName != null && donorPhone != null && donorDateOfBirthStr != null) {
                // Walk-in donor registration
                String normalizedPhone = normalizePhone(donorPhone);
                donor = donorRepository.findByPhone(normalizedPhone).orElse(null);

                if (donor == null) {
                    // Create new donor
                    donor = new Donor();
                    donor.setName(donorName);
                    donor.setPhone(normalizedPhone);
                    donor.setBloodType(bloodType);
                    donor.setDateOfBirth(LocalDate.parse(donorDateOfBirthStr));
                    donor.setCity(bank.getCity()); // Use bank's city
                    donor.setWeight(50); // Default weight
                    donor.setIsVerified(false); // Not OTP verified
                    donor.setLastDonationDate(collectionDate);
                    donor = donorRepository.save(donor);
                    log.info("New walk-in donor registered: {} ({})", donorName, normalizedPhone);
                } else {
                    // Update existing donor's last donation date
                    donor.setLastDonationDate(collectionDate);
                    donorRepository.save(donor);
                    log.info("Existing donor found: {} - updating last donation", donor.getName());
                }
            }

            List<Map<String, Object>> createdUnits = new ArrayList<>();

            for (int i = 0; i < componentsList.size(); i++) {
                String componentStr = componentsList.get(i);
                BloodComponent component = BloodComponent.valueOf(componentStr.toUpperCase());

                BloodUnit unit = new BloodUnit();
                unit.setBloodBank(bank);
                unit.setBloodType(bloodType.toUpperCase());
                unit.setComponent(component);
                unit.setCollectionDate(collectionDate);
                unit.setUnitNumber(generateUnitNumber(bankId));
                unit.setStatus(UnitStatus.AVAILABLE);
                if (donor != null) {
                    unit.setDonor(donor);
                }

                bloodUnitRepository.save(unit);
                createdUnits.add(convertToDto(unit));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", createdUnits.size() + " blood units created from donation");
            response.put("data", createdUnits);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Update unit status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        BloodUnit unit = bloodUnitRepository.findById(id).orElse(null);
        if (unit == null || !unit.getBloodBank().getId().equals(bankId)) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", "Unit not found"));
        }

        try {
            String statusStr = request.get("status");
            UnitStatus newStatus = UnitStatus.valueOf(statusStr.toUpperCase());

            // Check if unit is expired
            if (unit.isExpired()) {
                // If expired, only allow setting to EXPIRED or DISCARDED
                if (newStatus == UnitStatus.RESERVED || newStatus == UnitStatus.USED
                        || newStatus == UnitStatus.AVAILABLE) {
                    // Auto-update status to EXPIRED if not already
                    if (unit.getStatus() != UnitStatus.EXPIRED && unit.getStatus() != UnitStatus.DISCARDED) {
                        unit.setStatus(UnitStatus.EXPIRED);
                        bloodUnitRepository.save(unit);
                    }
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "error",
                            "Cannot change expired blood unit to " + newStatus + ". Unit expired on "
                                    + unit.getExpiryDate(),
                            "data", convertToDto(unit)));
                }
            }

            unit.setStatus(newStatus);
            bloodUnitRepository.save(unit);

            return ResponseEntity.ok(Map.of("success", true, "message", "Status updated", "data", convertToDto(unit)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Invalid status"));
        }
    }

    /**
     * Delete unit
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteUnit(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        BloodUnit unit = bloodUnitRepository.findById(id).orElse(null);
        if (unit == null || !unit.getBloodBank().getId().equals(bankId)) {
            return ResponseEntity.status(404).body(Map.of("success", false, "error", "Unit not found"));
        }

        bloodUnitRepository.delete(unit);
        return ResponseEntity.ok(Map.of("success", true, "message", "Unit deleted"));
    }

    /**
     * Mark expired units as expired (can be called periodically)
     */
    @PostMapping("/mark-expired")
    public ResponseEntity<Map<String, Object>> markExpiredUnits(
            @RequestHeader("Authorization") String authHeader) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        List<BloodUnit> expiredUnits = bloodUnitRepository.findExpiredUnits(bankId, LocalDate.now());
        int count = 0;
        for (BloodUnit unit : expiredUnits) {
            unit.setStatus(UnitStatus.EXPIRED);
            bloodUnitRepository.save(unit);
            count++;
        }

        return ResponseEntity.ok(Map.of("success", true, "message", count + " units marked as expired"));
    }

    // Helper methods
    private Long getBankIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            return null;
        }
        return jwtTokenProvider.getBankIdFromToken(token);
    }

    private String generateUnitNumber(Long bankId) {
        long count = bloodUnitRepository.countByBloodBankId(bankId);
        return String.format("%03d", count + 1);
    }

    private Map<String, Object> convertToDto(BloodUnit unit) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", unit.getId());
        dto.put("unitNumber", unit.getUnitNumber());
        dto.put("bloodType", unit.getBloodType());
        dto.put("component", unit.getComponent().name());
        dto.put("componentName", unit.getComponent().getDisplayName());
        dto.put("collectionDate", unit.getCollectionDate().toString());
        dto.put("expiryDate", unit.getExpiryDate().toString());
        dto.put("status", unit.getStatus().name());
        dto.put("daysUntilExpiry", unit.getDaysUntilExpiry());
        dto.put("hoursUntilExpiry", unit.getHoursUntilExpiry());
        dto.put("expiryStatus", unit.getExpiryStatus());
        dto.put("isExpired", unit.isExpired());
        if (unit.getDonor() != null) {
            dto.put("donorId", unit.getDonor().getId());
            dto.put("donorName", unit.getDonor().getName());
        }
        return dto;
    }

    /**
     * Normalize phone number to 12-digit format with country code
     */
    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        return digits;
    }

    // ==================== TWO-STEP DONATION WORKFLOW ====================

    /**
     * Lookup donor by phone number
     */
    @GetMapping("/lookup-donor")
    public ResponseEntity<Map<String, Object>> lookupDonor(
            @RequestParam String phone,
            @RequestHeader("Authorization") String authHeader) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        // Try to find donor with different phone formats
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        Donor donor = donorRepository.findByPhone(cleanPhone).orElse(null);

        // If not found, try with 91 prefix (Indian phone format)
        if (donor == null && !cleanPhone.startsWith("91") && cleanPhone.length() == 10) {
            donor = donorRepository.findByPhone("91" + cleanPhone).orElse(null);
        }
        // If not found with prefix, try without it
        if (donor == null && cleanPhone.startsWith("91") && cleanPhone.length() == 12) {
            donor = donorRepository.findByPhone(cleanPhone.substring(2)).orElse(null);
        }

        Map<String, Object> result = new HashMap<>();

        if (donor != null) {
            result.put("success", true);
            result.put("found", true);
            Map<String, Object> donorDto = new HashMap<>();
            donorDto.put("id", donor.getId());
            donorDto.put("name", donor.getName());
            donorDto.put("phone", donor.getPhone());
            donorDto.put("bloodType", donor.getBloodType());
            donorDto.put("lastDonationDate",
                    donor.getLastDonationDate() != null ? donor.getLastDonationDate().toString() : null);
            donorDto.put("eligible", donor.isEligible());
            donorDto.put("daysUntilEligible", donor.getDaysUntilEligible());
            result.put("donor", donorDto);
        } else {
            result.put("success", true);
            result.put("found", false);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Step 1: Record a donation (just donor + date, no components yet)
     */
    @PostMapping("/record-donation-step1")
    public ResponseEntity<Map<String, Object>> recordDonationStep1(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            String phone = (String) request.get("phone");
            String donationDateStr = (String) request.get("donationDate");
            String bloodType = (String) request.get("bloodType");

            // Walk-in donor details (if new)
            String donorName = (String) request.get("donorName");
            String donorDateOfBirthStr = (String) request.get("donorDateOfBirth");

            if (phone == null || donationDateStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Phone and donation date required"));
            }

            LocalDate donationDate = LocalDate.parse(donationDateStr);
            BloodBank bank = bloodBankRepository.findById(bankId).orElse(null);
            if (bank == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Blood bank not found"));
            }

            String normalizedPhone = normalizePhone(phone);
            Donor donor = donorRepository.findByPhone(normalizedPhone).orElse(null);

            // Create new donor if not found
            if (donor == null) {
                if (donorName == null || donorDateOfBirthStr == null || bloodType == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "error", "New donor requires name, DOB, and blood type"));
                }
                donor = new Donor();
                donor.setName(donorName);
                donor.setPhone(normalizedPhone);
                donor.setBloodType(bloodType);
                donor.setDateOfBirth(LocalDate.parse(donorDateOfBirthStr));
                donor.setCity(bank.getCity());
                donor.setWeight(50);
                donor.setIsVerified(false);
                donor.setLastDonationDate(donationDate);
                donor = donorRepository.save(donor);
                log.info("New walk-in donor registered: {} ({})", donorName, normalizedPhone);
            } else {
                // Check eligibility - must wait 90 days between donations
                if (!donor.isEligible()) {
                    long daysRemaining = donor.getDaysUntilEligible();
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "success", false,
                                    "error",
                                    "Donor is not eligible to donate yet. " + daysRemaining + " days remaining.",
                                    "daysRemaining", daysRemaining));
                }

                // Update existing donor's last donation date
                donor.setLastDonationDate(donationDate);
                if (bloodType != null && !bloodType.isEmpty()) {
                    donor.setBloodType(bloodType);
                }
                donorRepository.save(donor);
            }

            // Create donation record
            Donation donation = new Donation(donor, bank, donationDate);
            donation.setComponentsAdded(false);
            donationRepository.save(donation);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Donation recorded! Add components when ready.");
            result.put("donationId", donation.getId());
            result.put("donorName", donor.getName());
            result.put("bloodType", donor.getBloodType());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error recording donation: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Step 2: Add components to a pending donation
     */
    @PostMapping("/add-components/{donationId}")
    public ResponseEntity<Map<String, Object>> addComponents(
            @PathVariable Long donationId,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> request) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        try {
            @SuppressWarnings("unchecked")
            List<String> componentsList = (List<String>) request.get("components");

            if (componentsList == null || componentsList.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "At least one component required"));
            }

            Donation donation = donationRepository.findById(donationId).orElse(null);
            if (donation == null) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Donation not found"));
            }

            if (donation.getComponentsAdded()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Components already added to this donation"));
            }

            BloodBank bank = bloodBankRepository.findById(bankId).orElse(null);
            Donor donor = donation.getDonor();
            LocalDate collectionDate = donation.getDonationDate();
            String bloodType = donor.getBloodType();

            List<Map<String, Object>> createdUnits = new ArrayList<>();

            for (String componentStr : componentsList) {
                BloodComponent component = BloodComponent.valueOf(componentStr.toUpperCase());

                BloodUnit unit = new BloodUnit();
                unit.setBloodBank(bank);
                unit.setBloodType(bloodType);
                unit.setComponent(component);
                unit.setCollectionDate(collectionDate);
                unit.setUnitNumber(generateUnitNumber(bankId));
                unit.setStatus(UnitStatus.AVAILABLE);
                unit.setDonor(donor);

                bloodUnitRepository.save(unit);
                createdUnits.add(convertToDto(unit));
            }

            // Mark donation as processed
            donation.setComponentsAdded(true);
            donationRepository.save(donation);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Created " + createdUnits.size() + " blood units");
            result.put("units", createdUnits);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error adding components: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get pending donations (no components added yet)
     */
    @GetMapping("/pending-donations")
    public ResponseEntity<Map<String, Object>> getPendingDonations(
            @RequestHeader("Authorization") String authHeader) {

        Long bankId = getBankIdFromToken(authHeader);
        if (bankId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Unauthorized"));
        }

        List<Donation> pending = donationRepository
                .findByBloodBankIdAndComponentsAddedFalseOrderByDonationDateDesc(bankId);

        List<Map<String, Object>> donations = pending.stream().map(d -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", d.getId());
            dto.put("donorName", d.getDonor().getName());
            dto.put("donorPhone", d.getDonor().getPhone());
            dto.put("bloodType", d.getDonor().getBloodType());
            dto.put("donationDate", d.getDonationDate().toString());
            dto.put("createdAt", d.getCreatedAt().toString());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("donations", donations);
        result.put("count", donations.size());

        return ResponseEntity.ok(result);
    }
}
