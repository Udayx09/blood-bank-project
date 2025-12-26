package com.bloodbank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateReservationRequest {

    @NotBlank(message = "Patient name is required")
    private String patientName;

    @NotBlank(message = "WhatsApp number is required")
    private String whatsappNumber;

    @NotBlank(message = "Blood type is required")
    private String bloodType;

    @NotNull(message = "Blood bank ID is required")
    private Long bloodBankId;

    @NotNull(message = "Units needed is required")
    @Positive(message = "Units needed must be positive")
    private Integer unitsNeeded;

    private String urgencyLevel;
    private String additionalNotes;

    @NotBlank(message = "Prescription is required")
    private String prescriptionPath; // Path to uploaded doctor prescription

    @NotBlank(message = "Referring doctor name is required")
    private String referringDoctor; // Doctor who referred/prescribed

    public CreateReservationRequest() {
    }

    public CreateReservationRequest(String patientName, String whatsappNumber, String bloodType, Long bloodBankId,
            Integer unitsNeeded, String urgencyLevel, String additionalNotes) {
        this.patientName = patientName;
        this.whatsappNumber = whatsappNumber;
        this.bloodType = bloodType;
        this.bloodBankId = bloodBankId;
        this.unitsNeeded = unitsNeeded;
        this.urgencyLevel = urgencyLevel;
        this.additionalNotes = additionalNotes;
    }

    // Getters and Setters
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

    public Long getBloodBankId() {
        return bloodBankId;
    }

    public void setBloodBankId(Long bloodBankId) {
        this.bloodBankId = bloodBankId;
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
}
