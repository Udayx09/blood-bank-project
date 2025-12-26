package com.bloodbank.repository;

import com.bloodbank.entity.Donor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRepository extends JpaRepository<Donor, Long> {

        /**
         * Find donor by phone number
         */
        Optional<Donor> findByPhone(String phone);

        /**
         * Check if phone number already registered
         */
        boolean existsByPhone(String phone);

        /**
         * Find all donors by blood type in a specific city
         */
        List<Donor> findByBloodTypeAndCity(String bloodType, String city);

        /**
         * Find all verified donors in a city
         */
        List<Donor> findByCityAndIsVerifiedTrue(String city);

        /**
         * Find eligible donors (90+ days since last donation or never donated)
         * for a specific blood type and city
         */
        @Query("SELECT d FROM Donor d WHERE d.bloodType = :bloodType " +
                        "AND d.city = :city " +
                        "AND d.isVerified = true " +
                        "AND (d.lastDonationDate IS NULL OR d.lastDonationDate <= :eligibleDate)")
        List<Donor> findAvailableDonors(
                        @Param("bloodType") String bloodType,
                        @Param("city") String city,
                        @Param("eligibleDate") LocalDate eligibleDate);

        /**
         * Find all eligible donors in a city (regardless of blood type)
         */
        @Query("SELECT d FROM Donor d WHERE d.city = :city " +
                        "AND d.isVerified = true " +
                        "AND (d.lastDonationDate IS NULL OR d.lastDonationDate <= :eligibleDate)")
        List<Donor> findAllAvailableDonorsInCity(
                        @Param("city") String city,
                        @Param("eligibleDate") LocalDate eligibleDate);

        /**
         * Find donors who became eligible on a specific date (for notifications)
         * These are donors whose last donation was exactly 90 days ago
         */
        @Query("SELECT d FROM Donor d WHERE d.lastDonationDate = :donationDate " +
                        "AND d.isVerified = true")
        List<Donor> findDonorsEligibleOn(@Param("donationDate") LocalDate donationDate);

        /**
         * Count total verified donors
         */
        long countByIsVerifiedTrue();

        /**
         * Count donors by blood type
         */
        long countByBloodTypeAndIsVerifiedTrue(String bloodType);

        /**
         * Search for donors available for contact (for bank portal donor search)
         * Excludes: opted-out donors, donors contacted recently, ineligible donors
         */
        @Query("SELECT d FROM Donor d WHERE d.bloodType = :bloodType " +
                        "AND LOWER(d.city) = LOWER(:city) " +
                        "AND (d.isAvailableForContact IS NULL OR d.isAvailableForContact = true) " +
                        "AND (d.lastDonationDate IS NULL OR d.lastDonationDate <= :eligibleDate) " +
                        "AND d.id NOT IN :excludeIds " +
                        "ORDER BY d.name")
        List<Donor> searchAvailableDonorsExcluding(
                        @Param("bloodType") String bloodType,
                        @Param("city") String city,
                        @Param("eligibleDate") LocalDate eligibleDate,
                        @Param("excludeIds") List<Long> excludeIds);

        /**
         * Search all blood types available for contact in a city
         */
        @Query("SELECT d FROM Donor d WHERE LOWER(d.city) = LOWER(:city) " +
                        "AND (d.isAvailableForContact IS NULL OR d.isAvailableForContact = true) " +
                        "AND (d.lastDonationDate IS NULL OR d.lastDonationDate <= :eligibleDate) " +
                        "AND d.id NOT IN :excludeIds " +
                        "ORDER BY d.name")
        List<Donor> searchAllAvailableDonorsExcluding(
                        @Param("city") String city,
                        @Param("eligibleDate") LocalDate eligibleDate,
                        @Param("excludeIds") List<Long> excludeIds);

        /**
         * Count donors registered between two dates (for analytics)
         */
        @Query("SELECT COUNT(d) FROM Donor d WHERE d.createdAt BETWEEN :start AND :end")
        long countByCreatedAtBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
