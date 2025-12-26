package com.bloodbank.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "blood_banks")
public class BloodBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String address;

    private String city;

    private String phone;

    private String email;

    @Column(precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "is_open")
    private Boolean isOpen;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public BloodBank() {
    }

    public BloodBank(Long id, String name, String address, String city, String phone, String email,
            BigDecimal rating, Boolean isOpen, BigDecimal latitude, BigDecimal longitude,
            String passwordHash, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.rating = rating;
        this.isOpen = isOpen;
        this.latitude = latitude;
        this.longitude = longitude;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Boolean getIsOpen() {
        return isOpen;
    }

    public void setIsOpen(Boolean isOpen) {
        this.isOpen = isOpen;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    // Builder pattern
    public static BloodBankBuilder builder() {
        return new BloodBankBuilder();
    }

    public static class BloodBankBuilder {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String phone;
        private String email;
        private BigDecimal rating;
        private Boolean isOpen;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private String passwordHash;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public BloodBankBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BloodBankBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BloodBankBuilder address(String address) {
            this.address = address;
            return this;
        }

        public BloodBankBuilder city(String city) {
            this.city = city;
            return this;
        }

        public BloodBankBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public BloodBankBuilder email(String email) {
            this.email = email;
            return this;
        }

        public BloodBankBuilder rating(BigDecimal rating) {
            this.rating = rating;
            return this;
        }

        public BloodBankBuilder isOpen(Boolean isOpen) {
            this.isOpen = isOpen;
            return this;
        }

        public BloodBankBuilder latitude(BigDecimal latitude) {
            this.latitude = latitude;
            return this;
        }

        public BloodBankBuilder longitude(BigDecimal longitude) {
            this.longitude = longitude;
            return this;
        }

        public BloodBankBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public BloodBankBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public BloodBankBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public BloodBank build() {
            return new BloodBank(id, name, address, city, phone, email, rating, isOpen,
                    latitude, longitude, passwordHash, createdAt, updatedAt);
        }
    }
}
