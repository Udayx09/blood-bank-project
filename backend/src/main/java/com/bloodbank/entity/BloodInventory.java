package com.bloodbank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "blood_inventory", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "blood_bank_id", "blood_type" })
})
public class BloodInventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    private BloodBank bloodBank;

    @Column(name = "blood_type", nullable = false)
    private String bloodType;

    @Column(name = "units_available", nullable = false)
    private Integer unitsAvailable = 0;

    @Column(name = "collection_date")
    private LocalDate collectionDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    public BloodInventory() {
    }

    public BloodInventory(Long id, BloodBank bloodBank, String bloodType, Integer unitsAvailable,
            LocalDate collectionDate, LocalDate expiryDate, LocalDateTime lastUpdated) {
        this.id = id;
        this.bloodBank = bloodBank;
        this.bloodType = bloodType;
        this.unitsAvailable = unitsAvailable;
        this.collectionDate = collectionDate;
        this.expiryDate = expiryDate;
        this.lastUpdated = lastUpdated;
    }

    @PreUpdate
    @PrePersist
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Calculated fields
    public Long getDaysLeft() {
        if (expiryDate == null)
            return null;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public String getExpiryStatus() {
        Long daysLeft = getDaysLeft();
        if (daysLeft == null)
            return "unknown";
        if (daysLeft < 0)
            return "expired";
        if (daysLeft <= 3)
            return "critical";
        if (daysLeft <= 7)
            return "warning";
        return "good";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BloodBank getBloodBank() {
        return bloodBank;
    }

    public void setBloodBank(BloodBank bloodBank) {
        this.bloodBank = bloodBank;
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

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    // Builder pattern
    public static BloodInventoryBuilder builder() {
        return new BloodInventoryBuilder();
    }

    public static class BloodInventoryBuilder {
        private Long id;
        private BloodBank bloodBank;
        private String bloodType;
        private Integer unitsAvailable;
        private LocalDate collectionDate;
        private LocalDate expiryDate;
        private LocalDateTime lastUpdated;

        public BloodInventoryBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BloodInventoryBuilder bloodBank(BloodBank bloodBank) {
            this.bloodBank = bloodBank;
            return this;
        }

        public BloodInventoryBuilder bloodType(String bloodType) {
            this.bloodType = bloodType;
            return this;
        }

        public BloodInventoryBuilder unitsAvailable(Integer unitsAvailable) {
            this.unitsAvailable = unitsAvailable;
            return this;
        }

        public BloodInventoryBuilder collectionDate(LocalDate collectionDate) {
            this.collectionDate = collectionDate;
            return this;
        }

        public BloodInventoryBuilder expiryDate(LocalDate expiryDate) {
            this.expiryDate = expiryDate;
            return this;
        }

        public BloodInventoryBuilder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }

        public BloodInventory build() {
            return new BloodInventory(id, bloodBank, bloodType, unitsAvailable, collectionDate, expiryDate,
                    lastUpdated);
        }
    }
}
