package com.bloodbank.controller;

import com.bloodbank.dto.BloodInventoryDto;
import com.bloodbank.dto.DonorDto;
import com.bloodbank.dto.ReservationDto;
import com.bloodbank.entity.Reservation;
import com.bloodbank.security.BankPrincipal;
import com.bloodbank.service.DonorService;
import com.bloodbank.service.InventoryService;
import com.bloodbank.service.ReservationService;
import com.bloodbank.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bank")
public class BankPortalController {

    private static final Logger log = LoggerFactory.getLogger(BankPortalController.class);

    private final ReservationService reservationService;
    private final InventoryService inventoryService;
    private final WhatsAppService whatsAppService;
    private final DonorService donorService;

    public BankPortalController(ReservationService reservationService,
            InventoryService inventoryService,
            WhatsAppService whatsAppService,
            DonorService donorService) {
        this.reservationService = reservationService;
        this.inventoryService = inventoryService;
        this.whatsAppService = whatsAppService;
        this.donorService = donorService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
            @AuthenticationPrincipal BankPrincipal principal) {

        try {
            if (principal == null) {
                log.warn("No principal found for /stats request");
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("error", "Not authenticated");
                return ResponseEntity.status(401).body(error);
            }

            Long bankId = principal.getId();
            log.debug("Getting stats for bank ID: {}", bankId);

            Map<String, Object> reservationStats = new HashMap<>();
            try {
                Object[] stats = reservationService.getStatsByBankId(bankId);
                log.info("Stats query returned for bankId {}: {}", bankId, java.util.Arrays.toString(stats));

                // Handle the case where query returns [[values]] instead of [values]
                if (stats != null && stats.length > 0 && stats[0] instanceof Object[]) {
                    stats = (Object[]) stats[0];
                    log.info("Unwrapped nested array: {}", java.util.Arrays.toString(stats));
                }

                if (stats != null && stats.length >= 5) {
                    reservationStats.put("total", stats[0] != null ? ((Number) stats[0]).intValue() : 0);
                    reservationStats.put("pending", stats[1] != null ? ((Number) stats[1]).intValue() : 0);
                    reservationStats.put("confirmed", stats[2] != null ? ((Number) stats[2]).intValue() : 0);
                    reservationStats.put("completed", stats[3] != null ? ((Number) stats[3]).intValue() : 0);
                    reservationStats.put("cancelled", stats[4] != null ? ((Number) stats[4]).intValue() : 0);
                } else {
                    reservationStats.put("total", 0);
                    reservationStats.put("pending", 0);
                    reservationStats.put("confirmed", 0);
                    reservationStats.put("completed", 0);
                    reservationStats.put("cancelled", 0);
                }
            } catch (Exception e) {
                log.warn("Error getting reservation stats: {}", e.getMessage());
                reservationStats.put("total", 0);
                reservationStats.put("pending", 0);
                reservationStats.put("confirmed", 0);
                reservationStats.put("completed", 0);
                reservationStats.put("cancelled", 0);
            }

            List<BloodInventoryDto> inventory = inventoryService.getInventoryByBankId(bankId);
            int totalUnits = inventory != null ? inventory.stream()
                    .mapToInt(BloodInventoryDto::getUnitsAvailable)
                    .sum() : 0;

            Map<String, Object> inventoryData = new HashMap<>();
            inventoryData.put("total", totalUnits);
            inventoryData.put("byType", inventory != null ? inventory.stream()
                    .map(inv -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("type", inv.getBloodType());
                        item.put("units", inv.getUnitsAvailable());
                        return item;
                    })
                    .collect(Collectors.toList()) : List.of());

            List<ReservationDto> recentReservations = reservationService.getRecentByBankId(bankId);
            List<Map<String, Object>> recentActivity = recentReservations != null ? recentReservations.stream()
                    .map(r -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", r.getId());
                        item.put("patientName", r.getPatientName());
                        item.put("bloodType", r.getBloodType());
                        item.put("unitsNeeded", r.getUnitsNeeded());
                        item.put("status", r.getStatus());
                        item.put("createdAt", r.getCreatedAt());
                        return item;
                    })
                    .collect(Collectors.toList()) : List.of();

            Map<String, Object> data = new HashMap<>();
            data.put("reservations", reservationStats);
            data.put("inventory", inventoryData);
            data.put("recentActivity", recentActivity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getStats: ", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Failed to load stats: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/reservations")
    public ResponseEntity<Map<String, Object>> getReservations(
            @AuthenticationPrincipal BankPrincipal principal) {

        List<ReservationDto> reservations = reservationService.getReservationsByBankId(principal.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", reservations.size());
        response.put("data", reservations);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/reservations/{id}/status")
    public ResponseEntity<Map<String, Object>> updateReservationStatus(
            @AuthenticationPrincipal BankPrincipal principal,
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null || !Reservation.isValidStatus(status)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Invalid status");
            return ResponseEntity.badRequest().body(error);
        }

        return reservationService.updateStatusForBank(id, principal.getId(), status)
                .map(reservation -> {
                    Map<String, Object> whatsappStatus = whatsAppService.getStatus();
                    boolean isReady = Boolean.TRUE.equals(whatsappStatus.get("isReady"));

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Status updated to " + status);
                    response.put("whatsappNotification", isReady ? "sent" : "not_sent");
                    response.put("data", reservation);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Reservation not found or not authorized");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getInventory(
            @AuthenticationPrincipal BankPrincipal principal) {

        List<BloodInventoryDto> inventory = inventoryService.getInventoryByBankId(principal.getId());

        List<Map<String, Object>> inventoryData = inventory.stream()
                .map(inv -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("blood_type", inv.getBloodType());
                    item.put("units_available", inv.getUnitsAvailable());
                    item.put("collection_date", inv.getCollectionDate());
                    item.put("expiry_date", inv.getExpiryDate());
                    item.put("days_left", inv.getDaysLeft());
                    item.put("expiry_status", inv.getExpiryStatus());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inventoryData);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/inventory")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @AuthenticationPrincipal BankPrincipal principal,
            @RequestBody Map<String, Object> request) {

        String bloodType = (String) request.get("bloodType");
        Object unitsObj = request.get("units");

        if (bloodType == null || unitsObj == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "bloodType and units are required");
            return ResponseEntity.badRequest().body(error);
        }

        int units = ((Number) unitsObj).intValue();
        LocalDate collectionDate = null;
        if (request.get("collectionDate") != null) {
            collectionDate = LocalDate.parse((String) request.get("collectionDate"));
        }

        BloodInventoryDto updated = inventoryService.updateInventory(
                principal.getId(), bloodType, units, collectionDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Inventory updated");
        response.put("data", updated);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/expiring")
    public ResponseEntity<Map<String, Object>> getExpiringBlood(
            @AuthenticationPrincipal BankPrincipal principal) {

        Map<String, Object> expiringData = inventoryService.getExpiringBlood(principal.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", expiringData);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(
            @AuthenticationPrincipal BankPrincipal principal,
            @RequestBody Map<String, Object> request) {

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profile updated");
        response.put("data", request);

        return ResponseEntity.ok(response);
    }

    // ==================== DONOR SEARCH ENDPOINTS ====================

    /**
     * Search for donors available for contact
     */
    @GetMapping("/donors")
    public ResponseEntity<Map<String, Object>> searchDonors(
            @AuthenticationPrincipal BankPrincipal principal,
            @RequestParam(required = false) String bloodType) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        String city = principal.getCity();

        List<DonorDto> donors = donorService.searchDonorsForBank(bankId, bloodType, city);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("donors", donors);
        response.put("count", donors.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Send donation request to a donor
     */
    @PostMapping("/donors/{donorId}/request")
    public ResponseEntity<Map<String, Object>> sendDonationRequest(
            @AuthenticationPrincipal BankPrincipal principal,
            @PathVariable Long donorId) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        Map<String, Object> result = donorService.sendDonationRequest(donorId, bankId);

        if (Boolean.TRUE.equals(result.get("success"))) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    /**
     * Get bank's donation request history
     */
    @GetMapping("/donor-requests")
    public ResponseEntity<Map<String, Object>> getDonorRequestHistory(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        List<Map<String, Object>> history = donorService.getBankRequestHistory(bankId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("requests", history);

        return ResponseEntity.ok(response);
    }
}
