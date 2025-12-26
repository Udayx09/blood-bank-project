package com.bloodbank.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;

@Entity
@Table(name = "reservations")
public class Reservation {

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_CONFIRMED = "confirmed";
    public static final String STATUS_COMPLETED = "completed";
    public static final String STATUS_CANCELLED = "cancelled";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_name", nullable = false)
    private String patientName;

    @Column(name = "whatsapp_number")
    private String whatsappNumber;

    @Column(name = "blood_type", nullable = false)
    private String bloodType;

    @Column(name = "units_needed", nullable = false)
    private Integer unitsNeeded = 1;

    @Column(name = "urgency_level")
    private String urgencyLevel = "normal";

    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes;

    @Column(name = "prescription_path")
    private String prescriptionPath; // Path to uploaded doctor prescription file

    @Column(name = "referring_doctor")
    private String referringDoctor; // Doctor who referred/prescribed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blood_bank_id", nullable = false)
    private BloodBank bloodBank;

    private String status = STATUS_PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Reservation() {
    }

    public Reservation(Long id, String patientName, String whatsappNumber, String bloodType, Integer unitsNeeded,
            String urgencyLevel, String additionalNotes, BloodBank bloodBank, String status,
            LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.patientName = patientName;
        this.whatsappNumber = whatsappNumber;
        this.bloodType = bloodType;
        this.unitsNeeded = unitsNeeded;
        this.urgencyLevel = urgencyLevel;
        this.additionalNotes = additionalNotes;
        this.bloodBank = bloodBank;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static boolean isValidStatus(String status) {
        return Arrays.asList(STATUS_PENDING, STATUS_CONFIRMED, STATUS_COMPLETED, STATUS_CANCELLED).contains(status);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getWhatsappNumber() {
        return whatsappNumber;
    }

    public void setWhatsappNumber(String whatsappNumber) {
        this.whatsappNumber = whatsappNumber;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public Integer getUnitsNeeded() {
        return unitsNeeded;
    }

    public void setUnitsNeeded(Integer unitsNeeded) {
        this.unitsNeeded = unitsNeeded;
    }

    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    public String getAdditionalNotes() {
        return additionalNotes;
    }

    public void setAdditionalNotes(String additionalNotes) {
        this.additionalNotes = additionalNotes;
    }

    public BloodBank getBloodBank() {
        return bloodBank;
    }

    public void setBloodBank(BloodBank bloodBank) {
        this.bloodBank = bloodBank;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    public String getPrescriptionPath() {
        return prescriptionPath;
    }

    public void setPrescriptionPath(String prescriptionPath) {
        this.prescriptionPath = prescriptionPath;
    }

    public String getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(String referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    // Builder pattern
    public static ReservationBuilder builder() {
        return new ReservationBuilder();
    }

    public static class ReservationBuilder {
        private Long id;
        private String patientName;
        private String whatsappNumber;
        private String bloodType;
        private Integer unitsNeeded;
        private String urgencyLevel;
        private String additionalNotes;
        private BloodBank bloodBank;
        private String status;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public ReservationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReservationBuilder patientName(String patientName) {
            this.patientName = patientName;
            return this;
        }

        public ReservationBuilder whatsappNumber(String whatsappNumber) {
            this.whatsappNumber = whatsappNumber;
            return this;
        }

        public ReservationBuilder bloodType(String bloodType) {
            this.bloodType = bloodType;
            return this;
        }

        public ReservationBuilder unitsNeeded(Integer unitsNeeded) {
            this.unitsNeeded = unitsNeeded;
            return this;
        }

        public ReservationBuilder urgencyLevel(String urgencyLevel) {
            this.urgencyLevel = urgencyLevel;
            return this;
        }

        public ReservationBuilder additionalNotes(String additionalNotes) {
            this.additionalNotes = additionalNotes;
            return this;
        }

        public ReservationBuilder bloodBank(BloodBank bloodBank) {
            this.bloodBank = bloodBank;
            return this;
        }

        public ReservationBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ReservationBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public ReservationBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ReservationBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Reservation build() {
            return new Reservation(id, patientName, whatsappNumber, bloodType, unitsNeeded, urgencyLevel,
                    additionalNotes, bloodBank, status, expiresAt, createdAt, updatedAt);
        }
    }
}
