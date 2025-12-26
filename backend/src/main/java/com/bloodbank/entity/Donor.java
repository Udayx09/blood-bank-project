package com.bloodbank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "donors")
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phone;

    @Column(name = "blood_type", nullable = false)
    private String bloodType;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Integer weight;

    @Column(name = "last_donation_date")
    private LocalDate lastDonationDate;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "is_available_for_contact")
    private Boolean isAvailableForContact = true; // Opt-out toggle

    @Column(name = "password")
    private String password; // BCrypt hashed password (null for existing donors until they set one)

    @Column(name = "profile_photo")
    private String profilePhoto; // URL or path to profile photo

    @Column(name = "reset_otp")
    private String resetOtp; // Temporary OTP for password reset

    @Column(name = "reset_otp_expiry")
    private LocalDateTime resetOtpExpiry; // Expiry time for reset OTP

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Donor() {
    }

    public Donor(String name, String phone, String bloodType, LocalDate dateOfBirth,
            String city, Integer weight) {
        this.name = name;
        this.phone = phone;
        this.bloodType = bloodType;
        this.dateOfBirth = dateOfBirth;
        this.city = city;
        this.weight = weight;
        this.isVerified = false;
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

    // Calculate if donor is eligible (90+ days since last donation or never
    // donated)
    public boolean isEligible() {
        if (lastDonationDate == null) {
            return true; // First-time donor
        }
        long daysSinceLastDonation = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
        return daysSinceLastDonation >= 90;
    }

    // Get days until eligible (0 if already eligible)
    public long getDaysUntilEligible() {
        if (lastDonationDate == null) {
            return 0;
        }
        long daysSinceLastDonation = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
        if (daysSinceLastDonation >= 90) {
            return 0;
        }
        return 90 - daysSinceLastDonation;
    }

    // Calculate age from date of birth
    public int getAge() {
        if (dateOfBirth == null) {
            return 0;
        }
        return (int) ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now());
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

    public Boolean getIsAvailableForContact() {
        return isAvailableForContact;
    }

    public void setIsAvailableForContact(Boolean isAvailableForContact) {
        this.isAvailableForContact = isAvailableForContact;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public String getResetOtp() {
        return resetOtp;
    }

    public void setResetOtp(String resetOtp) {
        this.resetOtp = resetOtp;
    }

    public LocalDateTime getResetOtpExpiry() {
        return resetOtpExpiry;
    }

    public void setResetOtpExpiry(LocalDateTime resetOtpExpiry) {
        this.resetOtpExpiry = resetOtpExpiry;
    }
}
