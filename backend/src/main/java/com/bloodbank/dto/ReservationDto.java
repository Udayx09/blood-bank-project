package com.bloodbank.dto;

import java.time.LocalDateTime;

public class ReservationDto {

    private Long id;
    private String patientName;
    private String whatsappNumber;
    private String bloodType;
    private Integer unitsNeeded;
    private String urgencyLevel;
    private String additionalNotes;
    private Long bloodBankId;
    private String bloodBankName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;
    private String prescriptionPath;
    private String referringDoctor;

    public ReservationDto() {
    }

    public ReservationDto(Long id, String patientName, String whatsappNumber, String bloodType, Integer unitsNeeded,
            String urgencyLevel, String additionalNotes, Long bloodBankId, String bloodBankName,
            String status, LocalDateTime createdAt, LocalDateTime expiresAt, LocalDateTime updatedAt) {
        this.id = id;
        this.patientName = patientName;
        this.whatsappNumber = whatsappNumber;
        this.bloodType = bloodType;
        this.unitsNeeded = unitsNeeded;
        this.urgencyLevel = urgencyLevel;
        this.additionalNotes = additionalNotes;
        this.bloodBankId = bloodBankId;
        this.bloodBankName = bloodBankName;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.updatedAt = updatedAt;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
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

    // Builder
    public static ReservationDtoBuilder builder() {
        return new ReservationDtoBuilder();
    }

    public static class ReservationDtoBuilder {
        private Long id;
        private String patientName;
        private String whatsappNumber;
        private String bloodType;
        private Integer unitsNeeded;
        private String urgencyLevel;
        private String additionalNotes;
        private Long bloodBankId;
        private String bloodBankName;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private LocalDateTime updatedAt;

        public ReservationDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReservationDtoBuilder patientName(String patientName) {
            this.patientName = patientName;
            return this;
        }

        public ReservationDtoBuilder whatsappNumber(String whatsappNumber) {
            this.whatsappNumber = whatsappNumber;
            return this;
        }

        public ReservationDtoBuilder bloodType(String bloodType) {
            this.bloodType = bloodType;
            return this;
        }

        public ReservationDtoBuilder unitsNeeded(Integer unitsNeeded) {
            this.unitsNeeded = unitsNeeded;
            return this;
        }

        public ReservationDtoBuilder urgencyLevel(String urgencyLevel) {
            this.urgencyLevel = urgencyLevel;
            return this;
        }

        public ReservationDtoBuilder additionalNotes(String additionalNotes) {
            this.additionalNotes = additionalNotes;
            return this;
        }

        public ReservationDtoBuilder bloodBankId(Long bloodBankId) {
            this.bloodBankId = bloodBankId;
            return this;
        }

        public ReservationDtoBuilder bloodBankName(String bloodBankName) {
            this.bloodBankName = bloodBankName;
            return this;
        }

        public ReservationDtoBuilder status(String status) {
            this.status = status;
            return this;
        }

        public ReservationDtoBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public ReservationDtoBuilder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public ReservationDtoBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public ReservationDto build() {
            return new ReservationDto(id, patientName, whatsappNumber, bloodType, unitsNeeded, urgencyLevel,
                    additionalNotes, bloodBankId, bloodBankName, status, createdAt, expiresAt, updatedAt);
        }
    }
}
