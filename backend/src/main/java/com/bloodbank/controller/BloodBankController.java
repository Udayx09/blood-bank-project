package com.bloodbank.controller;

import com.bloodbank.dto.BloodBankDto;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.service.BloodBankService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/blood-banks")
public class BloodBankController {

    private final BloodBankService bloodBankService;

    public BloodBankController(BloodBankService bloodBankService) {
        this.bloodBankService = bloodBankService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllBloodBanks() {
        List<BloodBankDto> bloodBanks = bloodBankService.getAllBloodBanks();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", bloodBanks.size());
        response.put("data", bloodBanks);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchByBloodType(
            @RequestParam(required = false) String bloodType) {

        if (bloodType == null || bloodType.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Blood type is required. Use ?bloodType=A+");
            return ResponseEntity.badRequest().body(error);
        }

        List<BloodBankDto> bloodBanks = bloodBankService.searchByBloodType(bloodType);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("bloodType", bloodType);
        response.put("count", bloodBanks.size());
        response.put("data", bloodBanks);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getBloodTypes() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", bloodBankService.getBloodTypes());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getBloodBankById(@PathVariable Long id) {
        return bloodBankService.getBloodBankById(id)
                .map(bank -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", bank);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Blood bank not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createBloodBank(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String address = (String) request.get("address");
        String city = (String) request.get("city");
        String phone = (String) request.get("phone");

        if (name == null || address == null || city == null || phone == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Missing required fields: name, address, city, phone");
            return ResponseEntity.badRequest().body(error);
        }

        BloodBank bloodBank = BloodBank.builder()
                .name(name)
                .address(address)
                .city(city)
                .phone(phone)
                .email((String) request.get("email"))
                .rating(request.get("rating") != null ? new BigDecimal(request.get("rating").toString()) : null)
                .isOpen(request.get("isOpen") != null ? (Boolean) request.get("isOpen") : true)
                .latitude(request.get("latitude") != null ? new BigDecimal(request.get("latitude").toString()) : null)
                .longitude(
                        request.get("longitude") != null ? new BigDecimal(request.get("longitude").toString()) : null)
                .build();

        BloodBank created = bloodBankService.createBloodBank(bloodBank);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Blood bank created successfully");
        response.put("data", created);

        return ResponseEntity.status(201).body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateBloodBank(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {

        BloodBank updates = BloodBank.builder()
                .name((String) request.get("name"))
                .address((String) request.get("address"))
                .city((String) request.get("city"))
                .phone((String) request.get("phone"))
                .email((String) request.get("email"))
                .rating(request.get("rating") != null ? new BigDecimal(request.get("rating").toString()) : null)
                .isOpen(request.get("isOpen") != null ? (Boolean) request.get("isOpen") : null)
                .latitude(request.get("latitude") != null ? new BigDecimal(request.get("latitude").toString()) : null)
                .longitude(
                        request.get("longitude") != null ? new BigDecimal(request.get("longitude").toString()) : null)
                .build();

        return bloodBankService.updateBloodBank(id, updates)
                .map(bank -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Blood bank updated successfully");
                    response.put("data", bank);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Blood bank not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @PutMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleStatus(@PathVariable Long id) {
        return bloodBankService.toggleStatus(id)
                .map(bank -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Blood bank is now " + (bank.getIsOpen() ? "open" : "closed"));
                    response.put("data", bank);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Blood bank not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBloodBank(@PathVariable Long id) {
        return bloodBankService.getBloodBankById(id)
                .map(bank -> {
                    boolean deleted = bloodBankService.deleteBloodBank(id);
                    if (deleted) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Blood bank deleted successfully");
                        response.put("data", bank);
                        return ResponseEntity.ok(response);
                    } else {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("error", "Failed to delete blood bank");
                        return ResponseEntity.status(500).body(error);
                    }
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Blood bank not found");
                    return ResponseEntity.status(404).body(error);
                });
    }
}
