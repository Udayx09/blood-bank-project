package com.bloodbank.dto;

import java.time.LocalDate;

public class DonorDto {

    private Long id;
    private String name;
    private String phone;
    private String bloodType;
    private LocalDate dateOfBirth;
    private String city;
    private Integer weight;
    private LocalDate lastDonationDate;
    private Boolean isVerified;
    private Boolean isAvailableForContact;

    // Computed fields
    private Boolean eligible;
    private Long daysUntilEligible;
    private Integer age;
    private String maskedPhone; // For bank portal (privacy)

    public DonorDto() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public LocalDate getLastDonationDate() {
        return lastDonationDate;
    }

    public void setLastDonationDate(LocalDate lastDonationDate) {
        this.lastDonationDate = lastDonationDate;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getEligible() {
        return eligible;
    }

    public void setEligible(Boolean eligible) {
        this.eligible = eligible;
    }

    public Long getDaysUntilEligible() {
        return daysUntilEligible;
    }

    public void setDaysUntilEligible(Long daysUntilEligible) {
        this.daysUntilEligible = daysUntilEligible;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getIsAvailableForContact() {
        return isAvailableForContact;
    }

    public void setIsAvailableForContact(Boolean isAvailableForContact) {
        this.isAvailableForContact = isAvailableForContact;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }
}
