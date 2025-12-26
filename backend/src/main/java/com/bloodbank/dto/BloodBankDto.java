package com.bloodbank.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class BloodBankDto {

    private Long id;
    private String name;
    private String address;
    private String city;
    private String phone;
    private String email;
    private BigDecimal rating;
    private Boolean isOpen;
    private Boolean hasAccount;
    private List<String> bloodTypes;
    private Map<String, Integer> availableUnits;
    private Integer unitsAvailable;
    private LocationDto location;
    private String distance;

    public BloodBankDto() {
    }

    public BloodBankDto(Long id, String name, String address, String city, String phone, String email,
            BigDecimal rating, Boolean isOpen, Boolean hasAccount, List<String> bloodTypes,
            Map<String, Integer> availableUnits, Integer unitsAvailable, LocationDto location, String distance) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.rating = rating;
        this.isOpen = isOpen;
        this.hasAccount = hasAccount;
        this.bloodTypes = bloodTypes;
        this.availableUnits = availableUnits;
        this.unitsAvailable = unitsAvailable;
        this.location = location;
        this.distance = distance;
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

    public Boolean getHasAccount() {
        return hasAccount;
    }

    public void setHasAccount(Boolean hasAccount) {
        this.hasAccount = hasAccount;
    }

    public List<String> getBloodTypes() {
        return bloodTypes;
    }

    public void setBloodTypes(List<String> bloodTypes) {
        this.bloodTypes = bloodTypes;
    }

    public Map<String, Integer> getAvailableUnits() {
        return availableUnits;
    }

    public void setAvailableUnits(Map<String, Integer> availableUnits) {
        this.availableUnits = availableUnits;
    }

    public Integer getUnitsAvailable() {
        return unitsAvailable;
    }

    public void setUnitsAvailable(Integer unitsAvailable) {
        this.unitsAvailable = unitsAvailable;
    }

    public LocationDto getLocation() {
        return location;
    }

    public void setLocation(LocationDto location) {
        this.location = location;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    // Builder
    public static BloodBankDtoBuilder builder() {
        return new BloodBankDtoBuilder();
    }

    public static class BloodBankDtoBuilder {
        private Long id;
        private String name;
        private String address;
        private String city;
        private String phone;
        private String email;
        private BigDecimal rating;
        private Boolean isOpen;
        private Boolean hasAccount;
        private List<String> bloodTypes;
        private Map<String, Integer> availableUnits;
        private Integer unitsAvailable;
        private LocationDto location;
        private String distance;

        public BloodBankDtoBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public BloodBankDtoBuilder name(String name) {
            this.name = name;
            return this;
        }

        public BloodBankDtoBuilder address(String address) {
            this.address = address;
            return this;
        }

        public BloodBankDtoBuilder city(String city) {
            this.city = city;
            return this;
        }

        public BloodBankDtoBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public BloodBankDtoBuilder email(String email) {
            this.email = email;
            return this;
        }

        public BloodBankDtoBuilder rating(BigDecimal rating) {
            this.rating = rating;
            return this;
        }

        public BloodBankDtoBuilder isOpen(Boolean isOpen) {
            this.isOpen = isOpen;
            return this;
        }

        public BloodBankDtoBuilder hasAccount(Boolean hasAccount) {
            this.hasAccount = hasAccount;
            return this;
        }

        public BloodBankDtoBuilder bloodTypes(List<String> bloodTypes) {
            this.bloodTypes = bloodTypes;
            return this;
        }

        public BloodBankDtoBuilder availableUnits(Map<String, Integer> availableUnits) {
            this.availableUnits = availableUnits;
            return this;
        }

        public BloodBankDtoBuilder unitsAvailable(Integer unitsAvailable) {
            this.unitsAvailable = unitsAvailable;
            return this;
        }

        public BloodBankDtoBuilder location(LocationDto location) {
            this.location = location;
            return this;
        }

        public BloodBankDtoBuilder distance(String distance) {
            this.distance = distance;
            return this;
        }

        public BloodBankDto build() {
            return new BloodBankDto(id, name, address, city, phone, email, rating, isOpen, hasAccount,
                    bloodTypes, availableUnits, unitsAvailable, location, distance);
        }
    }

    // Nested LocationDto
    public static class LocationDto {
        private BigDecimal lat;
        private BigDecimal lng;

        public LocationDto() {
        }

        public LocationDto(BigDecimal lat, BigDecimal lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public BigDecimal getLat() {
            return lat;
        }

        public void setLat(BigDecimal lat) {
            this.lat = lat;
        }

        public BigDecimal getLng() {
            return lng;
        }

        public void setLng(BigDecimal lng) {
            this.lng = lng;
        }

        public static LocationDtoBuilder builder() {
            return new LocationDtoBuilder();
        }

        public static class LocationDtoBuilder {
            private BigDecimal lat;
            private BigDecimal lng;

            public LocationDtoBuilder lat(BigDecimal lat) {
                this.lat = lat;
                return this;
            }

            public LocationDtoBuilder lng(BigDecimal lng) {
                this.lng = lng;
                return this;
            }

            public LocationDto build() {
                return new LocationDto(lat, lng);
            }
        }
    }
}
