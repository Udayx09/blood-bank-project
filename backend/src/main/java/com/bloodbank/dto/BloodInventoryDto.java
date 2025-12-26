package com.bloodbank.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BloodInventoryDto {

    private Long id;
    private String bloodType;
    private Integer unitsAvailable;
    private LocalDate collectionDate;
    private LocalDate expiryDate;
    private Long daysLeft;
    private String expiryStatus;
    private LocalDateTime lastUpdated;
    private Long bloodBankId;
    private String bloodBankName;

    public BloodInventoryDto() {
    }

    public BloodInventoryDto(Long id, String bloodType, Integer unitsAvailable, LocalDate collectionDate,
            LocalDate expiryDate, Long daysLeft, String expiryStatus, LocalDateTime lastUpdated,
            Long bloodBankId, String bloodBankName) {
        this.id = id;
        this.bloodType = bloodType;
        this.unitsAvailable = unitsAvailable;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.daysLeft = daysLeft;
        this.expiryStatus = expiryStatus;
        this.lastUpdated = lastUpdated;
        this.bloodBankId = bloodBankId;
        this.bloodBankName = bloodBankName;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Integer getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(Integer unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Long getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Long daysLeft) {
        this.daysLeft = daysLeft;
    }

    public String getExpiryStatus() {
        return expiryStatus;
    }

    public void setExpiryStatus(String expiryStatus) {
        this.expiryStatus = expiryStatus;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Long getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(Long bloodBankId) {
        this.bloodBankId = bloodBankId;
    }

    public String getBloodBankName() {
        return bloodBankName;
    }

    public void setBloodBankName(String bloodBankName) {
        this.bloodBankName = bloodBankName;
    }

    // Builder
    public static BloodInventoryDtoBuilder builder() {
        return new BloodInventoryDtoBuilder();
    }

    public static class BloodInventoryDtoBuilder {
        private Long id;
        private String bloodType;
        private Integer unitsAvailable;
        private LocalDate collectionDate;
        private LocalDate expiryDate;
        private Long daysLeft;
        private String expiryStatus;
        private LocalDateTime lastUpdated;
        private Long bloodBankId;
        private String bloodBankName;

        public BloodInventoryDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BloodInventoryDtoBuilder bloodType(String bloodType) {
            this.bloodType = bloodType;
            return this;
        }

        public BloodInventoryDtoBuilder unitsAvailable(Integer unitsAvailable) {
            this.unitsAvailable = unitsAvailable;
            return this;
        }

        public BloodInventoryDtoBuilder collectionDate(LocalDate collectionDate) {
            this.collectionDate = collectionDate;
            return this;
        }

        public BloodInventoryDtoBuilder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public BloodInventoryDtoBuilder daysLeft(Long daysLeft) {
            this.daysLeft = daysLeft;
            return this;
        }

        public BloodInventoryDtoBuilder expiryStatus(String expiryStatus) {
            this.expiryStatus = expiryStatus;
            return this;
        }

        public BloodInventoryDtoBuilder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public BloodInventoryDtoBuilder bloodBankId(Long bloodBankId) {
            this.bloodBankId = bloodBankId;
            return this;
        }

        public BloodInventoryDtoBuilder bloodBankName(String bloodBankName) {
            this.bloodBankName = bloodBankName;
            return this;
        }

        public BloodInventoryDto build() {
            return new BloodInventoryDto(id, bloodType, unitsAvailable, collectionDate, expiryDate,
                    daysLeft, expiryStatus, lastUpdated, bloodBankId, bloodBankName);
        }
    }
}
