package com.bloodbank.controller;

import com.bloodbank.dto.ReservationDto;
import com.bloodbank.entity.Donor;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.service.DonorNotificationService;
import com.bloodbank.service.InventoryService;
import com.bloodbank.service.ReservationService;
import com.bloodbank.service.WhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final ReservationService reservationService;
    private final InventoryService inventoryService;
    private final BloodBankRepository bloodBankRepository;
    private final DonorRepository donorRepository;
    private final WhatsAppService whatsAppService;
    private final DonorNotificationService donorNotificationService;

    public AdminController(ReservationService reservationService,
            InventoryService inventoryService,
            BloodBankRepository bloodBankRepository,
            DonorRepository donorRepository,
            WhatsAppService whatsAppService,
            DonorNotificationService donorNotificationService) {
        this.reservationService = reservationService;
        this.inventoryService = inventoryService;
        this.bloodBankRepository = bloodBankRepository;
        this.donorRepository = donorRepository;
        this.whatsAppService = whatsAppService;
        this.donorNotificationService = donorNotificationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> reservationStats = new HashMap<>();
        try {
            Object[] stats = reservationService.getStats();
            if (stats != null && stats.length >= 5) {
                reservationStats.put("total", safeToInt(stats[0]));
                reservationStats.put("pending", safeToInt(stats[1]));
                reservationStats.put("confirmed", safeToInt(stats[2]));
                reservationStats.put("completed", safeToInt(stats[3]));
                reservationStats.put("cancelled", safeToInt(stats[4]));
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

        Map<String, Object> bloodBankStats = new HashMap<>();
        bloodBankStats.put("total", bloodBankRepository.count());

        Map<String, Object> bloodUnitsStats = new HashMap<>();
        bloodUnitsStats.put("total", inventoryService.getTotalUnits());
        bloodUnitsStats.put("byType", inventoryService.getBloodTypeStats());

        Map<String, Object> whatsappStatus = whatsAppService.getStatus();
        Map<String, Object> whatsapp = new HashMap<>();
        whatsapp.put("connected", Boolean.TRUE.equals(whatsappStatus.get("isReady")));
        whatsapp.put("hasQR", Boolean.TRUE.equals(whatsappStatus.get("hasQR")));

        List<Map<String, Object>> lowStockAlerts = inventoryService.getLowStockAlerts();

        List<ReservationDto> allReservations = reservationService.getAllReservations();
        List<Map<String, Object>> recentActivity = allReservations.stream()
                .limit(5)
                .map(r -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", r.getId());
                    item.put("type", "reservation");
                    item.put("patientName", r.getPatientName());
                    item.put("bloodType", r.getBloodType());
                    item.put("status", r.getStatus());
                    item.put("bloodBankName", r.getBloodBankName());
                    item.put("createdAt", r.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("reservations", reservationStats);
        data.put("bloodBanks", bloodBankStats);
        data.put("bloodUnits", bloodUnitsStats);
        data.put("whatsapp", whatsapp);
        data.put("lowStockAlerts", lowStockAlerts);
        data.put("lowStockThreshold", InventoryService.DEFAULT_LOW_STOCK_THRESHOLD);
        data.put("recentActivity", recentActivity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<Map<String, Object>> getLowStockAlerts() {
        List<Map<String, Object>> alerts = inventoryService.getLowStockAlerts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("threshold", InventoryService.DEFAULT_LOW_STOCK_THRESHOLD);
        response.put("count", alerts.size());
        response.put("data", alerts);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/inventory")
    public ResponseEntity<Map<String, Object>> getAllInventory() {
        List<Map<String, Object>> inventory = inventoryService.getAllInventoryGroupedByBank();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", inventory);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/inventory")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @RequestBody Map<String, Object> request) {

        Object bloodBankIdObj = request.get("bloodBankId");
        String bloodType = (String) request.get("bloodType");
        Object unitsObj = request.get("units");

        if (bloodBankIdObj == null || bloodType == null || unitsObj == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "bloodBankId, bloodType, and units are required");
            return ResponseEntity.badRequest().body(error);
        }

        Long bloodBankId;
        if (bloodBankIdObj instanceof Integer) {
            bloodBankId = ((Integer) bloodBankIdObj).longValue();
        } else if (bloodBankIdObj instanceof Long) {
            bloodBankId = (Long) bloodBankIdObj;
        } else {
            bloodBankId = Long.parseLong(bloodBankIdObj.toString());
        }

        int units = ((Number) unitsObj).intValue();

        try {
            var updated = inventoryService.updateInventory(bloodBankId, bloodType, units, null);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inventory updated");
            response.put("data", updated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private int safeToInt(Object value) {
        if (value == null)
            return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    // Endpoint to fix duplicate phone numbers
    @PostMapping("/fix-duplicate-phones")
    public ResponseEntity<Map<String, Object>> fixDuplicatePhones() {
        List<com.bloodbank.entity.BloodBank> allBanks = bloodBankRepository.findAll();
        Map<String, java.util.concurrent.atomic.AtomicInteger> phoneCount = new HashMap<>();
        List<String> fixed = new java.util.ArrayList<>();

        for (com.bloodbank.entity.BloodBank bank : allBanks) {
            String phone = bank.getPhone();
            if (phone != null && !phone.isEmpty()) {
                phoneCount.computeIfAbsent(phone, k -> new java.util.concurrent.atomic.AtomicInteger(0));
                int count = phoneCount.get(phone).incrementAndGet();
                if (count > 1) {
                    // This is a duplicate - make it unique by appending bank ID
                    String newPhone = phone + "_" + bank.getId();
                    bank.setPhone(newPhone);
                    bloodBankRepository.save(bank);
                    fixed.add(bank.getName() + ": " + phone + " -> " + newPhone);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Fixed " + fixed.size() + " duplicate phone numbers");
        response.put("fixed", fixed);
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger blood shortage alert to donors
     */
    @PostMapping("/send-shortage-alert")
    public ResponseEntity<Map<String, Object>> sendShortageAlert(@RequestBody Map<String, String> request) {
        String bloodType = request.get("bloodType");
        String city = request.get("city");
        String bloodBankName = request.get("bloodBankName");

        if (bloodType == null || city == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "bloodType and city are required");
            return ResponseEntity.badRequest().body(error);
        }

        donorNotificationService.sendBloodShortageAlert(bloodType, city, bloodBankName);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Blood shortage alert sent for " + bloodType + " in " + city);
        return ResponseEntity.ok(response);
    }

    /**
     * Trigger manual eligibility check (for testing)
     */
    @PostMapping("/trigger-eligibility-check")
    public ResponseEntity<Map<String, Object>> triggerEligibilityCheck() {
        Map<String, Object> result = donorNotificationService.triggerManualEligibilityCheck();
        return ResponseEntity.ok(result);
    }

    /**
     * Get all donors for admin panel
     */
    @GetMapping("/donors")
    public ResponseEntity<Map<String, Object>> getAllDonors() {
        List<Donor> donors = donorRepository.findAll();

        List<Map<String, Object>> donorList = donors.stream().map(d -> {
            Map<String, Object> dto = new HashMap<>();
            dto.put("id", d.getId());
            dto.put("name", d.getName());
            dto.put("phone", d.getPhone());
            dto.put("bloodType", d.getBloodType());
            dto.put("city", d.getCity());
            dto.put("isVerified", d.getIsVerified());
            dto.put("isAvailableForContact", d.getIsAvailableForContact());
            dto.put("lastDonationDate", d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : null);
            dto.put("createdAt", d.getCreatedAt() != null ? d.getCreatedAt().toString() : null);
            dto.put("eligible", d.isEligible());
            dto.put("daysUntilEligible", d.getDaysUntilEligible());
            return dto;
        }).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", donorList);
        response.put("count", donorList.size());

        return ResponseEntity.ok(response);
    }
}
