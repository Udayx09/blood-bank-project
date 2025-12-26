package com.bloodbank.service;

import com.bloodbank.dto.BloodInventoryDto;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.entity.BloodInventory;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.BloodInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final BloodInventoryRepository inventoryRepository;
    private final BloodBankRepository bloodBankRepository;

    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;
    public static final int BLOOD_SHELF_LIFE_DAYS = 42;

    public InventoryService(BloodInventoryRepository inventoryRepository,
            BloodBankRepository bloodBankRepository) {
        this.inventoryRepository = inventoryRepository;
        this.bloodBankRepository = bloodBankRepository;
    }

    public List<BloodInventoryDto> getInventoryByBankId(Long bankId) {
        return inventoryRepository.findByBloodBankIdOrderByBloodType(bankId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BloodInventoryDto updateInventory(Long bankId, String bloodType,
            int units, LocalDate collectionDate) {
        BloodBank bank = bloodBankRepository.findById(bankId)
                .orElseThrow(() -> new RuntimeException("Blood bank not found"));

        LocalDate collection = collectionDate != null ? collectionDate : LocalDate.now();
        LocalDate expiry = collection.plusDays(BLOOD_SHELF_LIFE_DAYS);

        BloodInventory inventory = inventoryRepository
                .findByBloodBankIdAndBloodType(bankId, bloodType)
                .orElse(BloodInventory.builder()
                        .bloodBank(bank)
                        .bloodType(bloodType)
                        .build());

        inventory.setUnitsAvailable(units);
        inventory.setCollectionDate(collection);
        inventory.setExpiryDate(expiry);

        BloodInventory saved = inventoryRepository.save(inventory);
        log.info("Inventory updated: {} {} units at bank {}", bloodType, units, bankId);

        return convertToDto(saved);
    }

    public List<Map<String, Object>> getAllInventoryGroupedByBank() {
        List<BloodBank> banks = bloodBankRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (BloodBank bank : banks) {
            Map<String, Object> bankData = new HashMap<>();
            bankData.put("id", bank.getId());
            bankData.put("name", bank.getName());

            List<BloodInventory> inventories = inventoryRepository
                    .findByBloodBankIdOrderByBloodType(bank.getId());

            List<Map<String, Object>> inventoryList = inventories.stream()
                    .map(inv -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("bloodType", inv.getBloodType());
                        item.put("units", inv.getUnitsAvailable());
                        return item;
                    })
                    .collect(Collectors.toList());

            bankData.put("inventory", inventoryList);
            result.add(bankData);
        }

        return result;
    }

    public Map<String, Object> getExpiringBlood(Long bankId) {
        LocalDate sevenDaysFromNow = LocalDate.now().plusDays(7);
        List<BloodInventory> expiring = inventoryRepository.findExpiringBlood(bankId, sevenDaysFromNow);

        List<Map<String, Object>> expired = new ArrayList<>();
        List<Map<String, Object>> critical = new ArrayList<>();
        List<Map<String, Object>> warning = new ArrayList<>();

        for (BloodInventory inv : expiring) {
            Map<String, Object> item = new HashMap<>();
            item.put("bloodType", inv.getBloodType());
            item.put("units", inv.getUnitsAvailable());
            item.put("expiryDate", inv.getExpiryDate());
            item.put("daysLeft", inv.getDaysLeft());
            item.put("status", inv.getExpiryStatus());

            switch (inv.getExpiryStatus()) {
                case "expired" -> expired.add(item);
                case "critical" -> critical.add(item);
                case "warning" -> warning.add(item);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("expired", expired);
        result.put("critical", critical);
        result.put("warning", warning);
        result.put("total", expiring.size());

        return result;
    }

    public List<Map<String, Object>> getLowStockAlerts() {
        return getLowStockAlerts(DEFAULT_LOW_STOCK_THRESHOLD);
    }

    public List<Map<String, Object>> getLowStockAlerts(int threshold) {
        List<BloodInventory> lowStock = inventoryRepository.findLowStock(threshold);

        return lowStock.stream()
                .map(inv -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("bloodBankId", inv.getBloodBank().getId());
                    alert.put("bloodBankName", inv.getBloodBank().getName());
                    alert.put("bloodType", inv.getBloodType());
                    alert.put("units", inv.getUnitsAvailable());
                    alert.put("threshold", threshold);
                    return alert;
                })
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getBloodTypeStats() {
        List<Object[]> stats = inventoryRepository.getTotalUnitsByBloodType();

        return stats.stream()
                .map(row -> {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("type", row[0]);
                    stat.put("units", ((Number) row[1]).intValue());
                    return stat;
                })
                .collect(Collectors.toList());
    }

    public Long getTotalUnits() {
        return inventoryRepository.getTotalUnitsAvailable();
    }

    private BloodInventoryDto convertToDto(BloodInventory inventory) {
        return BloodInventoryDto.builder()
                .id(inventory.getId())
                .bloodType(inventory.getBloodType())
                .unitsAvailable(inventory.getUnitsAvailable())
                .collectionDate(inventory.getCollectionDate())
                .expiryDate(inventory.getExpiryDate())
                .daysLeft(inventory.getDaysLeft())
                .expiryStatus(inventory.getExpiryStatus())
                .lastUpdated(inventory.getLastUpdated())
                .bloodBankId(inventory.getBloodBank().getId())
                .bloodBankName(inventory.getBloodBank().getName())
                .build();
    }
}
