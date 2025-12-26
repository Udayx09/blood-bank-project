package com.bloodbank.controller;

import com.bloodbank.repository.*;
import com.bloodbank.security.BankPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final ReservationRepository reservationRepository;

    public AnalyticsController(
            DonorRepository donorRepository,
            DonationRepository donationRepository,
            BloodBankRepository bloodBankRepository,
            BloodInventoryRepository bloodInventoryRepository,
            ReservationRepository reservationRepository) {
        this.donorRepository = donorRepository;
        this.donationRepository = donationRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Admin Analytics - System-wide statistics
     */
    @GetMapping("/admin/summary")
    public ResponseEntity<Map<String, Object>> getAdminSummary() {
        Map<String, Object> summary = new HashMap<>();

        // Total counts
        summary.put("totalDonors", donorRepository.count());
        summary.put("totalDonations", donationRepository.count());
        summary.put("totalBloodBanks", bloodBankRepository.count());
        summary.put("totalReservations", reservationRepository.count());

        // Verified donors
        summary.put("verifiedDonors", donorRepository.countByIsVerifiedTrue());

        return ResponseEntity.ok(summary);
    }

    /**
     * Admin Analytics - Blood type distribution
     */
    @GetMapping("/admin/blood-type-distribution")
    public ResponseEntity<Map<String, Object>> getBloodTypeDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        String[] bloodTypes = { "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-" };
        for (String type : bloodTypes) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", type);
            item.put("count", donorRepository.countByBloodTypeAndIsVerifiedTrue(type));
            data.add(item);
        }

        distribution.put("data", data);
        return ResponseEntity.ok(distribution);
    }

    /**
     * Admin Analytics - Recent donations by date
     */
    @GetMapping("/admin/donations-trend")
    public ResponseEntity<Map<String, Object>> getDonationsTrend() {
        Map<String, Object> trend = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        // Get donations for last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", donationRepository.countByDonationDate(date));
            data.add(item);
        }

        trend.put("data", data);
        return ResponseEntity.ok(trend);
    }

    /**
     * Bank Portal Analytics - Bank-specific statistics
     */
    @GetMapping("/bank/summary")
    public ResponseEntity<Map<String, Object>> getBankSummary(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        Map<String, Object> summary = new HashMap<>();

        // Bank-specific counts
        summary.put("totalDonations", donationRepository.countByBloodBankId(bankId));
        summary.put("totalReservations", reservationRepository.countByBloodBankId(bankId));

        // Inventory summary
        int totalUnits = bloodInventoryRepository.findByBloodBankId(bankId)
                .stream()
                .mapToInt(inv -> inv.getUnitsAvailable())
                .sum();
        summary.put("totalInventoryUnits", totalUnits);

        return ResponseEntity.ok(summary);
    }

    /**
     * Bank Portal Analytics - Inventory by blood type
     */
    @GetMapping("/bank/inventory-distribution")
    public ResponseEntity<Map<String, Object>> getBankInventoryDistribution(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        Map<String, Object> distribution = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        var inventories = bloodInventoryRepository.findByBloodBankId(bankId);
        for (var inv : inventories) {
            Map<String, Object> item = new HashMap<>();
            item.put("type", inv.getBloodType());
            item.put("units", inv.getUnitsAvailable());
            data.add(item);
        }

        distribution.put("data", data);
        return ResponseEntity.ok(distribution);
    }

    /**
     * Bank Portal Analytics - Recent donations
     */
    @GetMapping("/bank/donations-trend")
    public ResponseEntity<Map<String, Object>> getBankDonationsTrend(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        Map<String, Object> trend = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        // Get donations for last 7 days
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("count", donationRepository.countByBloodBankIdAndDonationDate(bankId, date));
            data.add(item);
        }

        trend.put("data", data);
        return ResponseEntity.ok(trend);
    }

    /**
     * Bank Portal Analytics - Reservations by status
     */
    @GetMapping("/bank/reservations-stats")
    public ResponseEntity<Map<String, Object>> getBankReservationsStats(
            @AuthenticationPrincipal BankPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        Long bankId = principal.getId();
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        String[] statuses = { "pending", "confirmed", "completed", "cancelled" };
        for (String status : statuses) {
            Map<String, Object> item = new HashMap<>();
            item.put("status", status);
            item.put("count", reservationRepository.countByBloodBankIdAndStatus(bankId, status));
            data.add(item);
        }

        stats.put("data", data);
        return ResponseEntity.ok(stats);
    }

    /**
     * Admin Analytics - Donor registrations trend (last 30 days)
     */
    @GetMapping("/admin/registrations-trend")
    public ResponseEntity<Map<String, Object>> getDonorRegistrationsTrend() {
        Map<String, Object> trend = new HashMap<>();
        List<Map<String, Object>> data = new ArrayList<>();

        // Get registrations for last 30 days (weekly buckets)
        for (int i = 4; i >= 0; i--) {
            LocalDate endDate = LocalDate.now().minusDays(i * 7);
            LocalDate startDate = endDate.minusDays(7);
            Map<String, Object> item = new HashMap<>();
            item.put("week", "Week " + (5 - i));
            item.put("count", donorRepository.countByCreatedAtBetween(
                    startDate.atStartOfDay(), endDate.atTime(23, 59, 59)));
            data.add(item);
        }

        trend.put("data", data);
        return ResponseEntity.ok(trend);
    }
}
