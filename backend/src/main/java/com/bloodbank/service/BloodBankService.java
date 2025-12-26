package com.bloodbank.service;

import com.bloodbank.dto.BloodBankDto;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.entity.BloodInventory;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.repository.BloodInventoryRepository;
import com.bloodbank.repository.BloodUnitRepository;
import com.bloodbank.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BloodBankService {

    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final ReservationRepository reservationRepository;
    private final BloodUnitRepository bloodUnitRepository;

    public BloodBankService(BloodBankRepository bloodBankRepository,
            BloodInventoryRepository inventoryRepository,
            ReservationRepository reservationRepository,
            BloodUnitRepository bloodUnitRepository) {
        this.bloodBankRepository = bloodBankRepository;
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.bloodUnitRepository = bloodUnitRepository;
    }

    public List<BloodBankDto> getAllBloodBanks() {
        List<BloodBank> banks = bloodBankRepository.findAll();
        return banks.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Optional<BloodBankDto> getBloodBankById(Long id) {
        return bloodBankRepository.findById(id)
                .map(this::convertToDto);
    }

    public List<BloodBankDto> searchByBloodType(String bloodType) {
        List<BloodBank> banks = bloodBankRepository.findByBloodTypeAvailable(bloodType);
        return banks.stream()
                .map(bank -> {
                    BloodBankDto dto = convertToDto(bank);
                    inventoryRepository.findByBloodBankIdAndBloodType(bank.getId(), bloodType)
                            .ifPresent(inv -> dto.setUnitsAvailable(inv.getUnitsAvailable()));
                    dto.setDistance(String.format("%.1f km", Math.random() * 10 + 1));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<String> getBloodTypes() {
        return Arrays.asList("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");
    }

    @Transactional
    public BloodBank createBloodBank(BloodBank bloodBank) {
        if (bloodBank.getRating() == null) {
            bloodBank.setRating(new BigDecimal("4.5"));
        }
        if (bloodBank.getIsOpen() == null) {
            bloodBank.setIsOpen(true);
        }
        return bloodBankRepository.save(bloodBank);
    }

    @Transactional
    public Optional<BloodBank> updateBloodBank(Long id, BloodBank updates) {
        return bloodBankRepository.findById(id)
                .map(bank -> {
                    if (updates.getName() != null)
                        bank.setName(updates.getName());
                    if (updates.getAddress() != null)
                        bank.setAddress(updates.getAddress());
                    if (updates.getCity() != null)
                        bank.setCity(updates.getCity());
                    if (updates.getPhone() != null)
                        bank.setPhone(updates.getPhone());
                    if (updates.getEmail() != null)
                        bank.setEmail(updates.getEmail());
                    if (updates.getRating() != null)
                        bank.setRating(updates.getRating());
                    if (updates.getIsOpen() != null)
                        bank.setIsOpen(updates.getIsOpen());
                    if (updates.getLatitude() != null)
                        bank.setLatitude(updates.getLatitude());
                    if (updates.getLongitude() != null)
                        bank.setLongitude(updates.getLongitude());
                    return bloodBankRepository.save(bank);
                });
    }

    @Transactional
    public Optional<BloodBank> toggleStatus(Long id) {
        return bloodBankRepository.findById(id)
                .map(bank -> {
                    bank.setIsOpen(!bank.getIsOpen());
                    return bloodBankRepository.save(bank);
                });
    }

    @Transactional
    public boolean deleteBloodBank(Long id) {
        if (bloodBankRepository.existsById(id)) {
            // Delete all related data first
            reservationRepository.deleteByBloodBankId(id);
            bloodUnitRepository.deleteByBloodBankId(id);
            inventoryRepository.deleteByBloodBankId(id);
            // Clear password hash to remove bank account if any
            bloodBankRepository.findById(id).ifPresent(bank -> {
                bank.setPasswordHash(null);
                bloodBankRepository.save(bank);
            });
            bloodBankRepository.deleteById(id);
            return true;
        }
        return false;
    }

    private BloodBankDto convertToDto(BloodBank bank) {
        List<BloodInventory> inventories = inventoryRepository
                .findByBloodBankIdOrderByBloodType(bank.getId());

        List<String> bloodTypes = inventories.stream()
                .map(BloodInventory::getBloodType)
                .collect(Collectors.toList());

        Map<String, Integer> availableUnits = inventories.stream()
                .collect(Collectors.toMap(
                        BloodInventory::getBloodType,
                        BloodInventory::getUnitsAvailable,
                        (a, b) -> a));

        return BloodBankDto.builder()
                .id(bank.getId())
                .name(bank.getName())
                .address(bank.getAddress())
                .city(bank.getCity())
                .phone(bank.getPhone())
                .email(bank.getEmail())
                .rating(bank.getRating())
                .isOpen(bank.getIsOpen())
                .hasAccount(bank.getPasswordHash() != null)
                .bloodTypes(bloodTypes)
                .availableUnits(availableUnits)
                .location(BloodBankDto.LocationDto.builder()
                        .lat(bank.getLatitude())
                        .lng(bank.getLongitude())
                        .build())
                .build();
    }
}
