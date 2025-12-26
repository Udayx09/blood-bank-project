package com.bloodbank.security;

/**
 * Principal class for authenticated Donor
 * Used in JWT authentication similar to BankPrincipal
 */
public class DonorPrincipal {
    private Long id;
    private String name;
    private String phone;
    private String bloodType;
    private String city;

    public DonorPrincipal() {
    }

    public DonorPrincipal(Long id, String name, String phone, String bloodType, String city) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.bloodType = bloodType;
        this.city = city;
    }

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

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
