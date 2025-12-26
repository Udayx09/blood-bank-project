package com.bloodbank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Individual blood unit with component type and expiry tracking
 */
@Entity
@Table(name = "blood_units")
public class BloodUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    private BloodBank bloodBank;

    @Column(name = "unit_number", nullable = false, unique = true)
    private String unitNumber;

    @Column(name = "blood_type", nullable = false)
    private String bloodType; // A+, A-, B+, B-, AB+, AB-, O+, O-

    @Enumerated(EnumType.STRING)
    @Column(name = "component", nullable = false)
    private BloodComponent component;

    @Column(name = "collection_date", nullable = false)
    private LocalDate collectionDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private UnitStatus status = UnitStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id")
    private Donor donor; // Optional: for traceability

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Blood Component Types with shelf life in days
    public enum BloodComponent {
        WHOLE_BLOOD("Whole Blood", 35),
        PRBC("Packed RBC (Non-SAGM)", 35),
        PRBC_SAGM("Packed RBC (SAGM)", 42),
        FFP("Fresh Frozen Plasma", 365),
        PLATELETS_RDP("Platelets (RDP)", 5),
        SDP("Single Donor Platelets", 5),
        CRYO("Cryoprecipitate", 365);

        private final String displayName;
        private final int shelfLifeDays;

        BloodComponent(String displayName, int shelfLifeDays) {
            this.displayName = displayName;
            this.shelfLifeDays = shelfLifeDays;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getShelfLifeDays() {
            return shelfLifeDays;
        }
    }

    // Unit Status
    public enum UnitStatus {
        AVAILABLE,
        RESERVED,
        USED,
        EXPIRED,
        DISCARDED
    }

    public BloodUnit() {
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Auto-calculate expiry date if not set
        if (expiryDate == null && collectionDate != null && component != null) {
            expiryDate = collectionDate.plusDays(component.getShelfLifeDays());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Calculated fields
    public long getDaysUntilExpiry() {
        if (expiryDate == null)
            return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public long getHoursUntilExpiry() {
        if (expiryDate == null)
            return 0;
        LocalDateTime expiryDateTime = expiryDate.atStartOfDay().plusDays(1); // End of expiry day
        long hours = ChronoUnit.HOURS.between(LocalDateTime.now(), expiryDateTime);
        return Math.max(0, hours);
    }

    public String getExpiryStatus() {
        long daysLeft = getDaysUntilExpiry();
        if (daysLeft < 0)
            return "expired";
        if (daysLeft <= 3)
            return "critical";
        if (daysLeft <= 7)
            return "warning";
        return "good";
    }

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
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

    public String getUnitNumber() {
        return unitNumber;
    }

    public void setUnitNumber(String unitNumber) {
        this.unitNumber = unitNumber;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public BloodComponent getComponent() {
        return component;
    }

    public void setComponent(BloodComponent component) {
        this.component = component;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
        // Auto-calculate expiry when collection date is set
        if (component != null) {
            this.expiryDate = collectionDate.plusDays(component.getShelfLifeDays());
        }
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public UnitStatus getStatus() {
        return status;
    }

    public void setStatus(UnitStatus status) {
        this.status = status;
    }

    public Donor getDonor() {
        return donor;
    }

    public void setDonor(Donor donor) {
        this.donor = donor;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
