package com.bloodbank.repository;

import com.bloodbank.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<Donation, Long> {

    /**
     * Find all donations by a donor (ordered by date descending)
     */
    List<Donation> findByDonorIdOrderByDonationDateDesc(Long donorId);

    /**
     * Find all donations at a blood bank
     */
    List<Donation> findByBloodBankIdOrderByDonationDateDesc(Long bloodBankId);

    /**
     * Count total donations by a donor
     */
    long countByDonorId(Long donorId);

    /**
     * Count total donations at a blood bank
     */
    long countByBloodBankId(Long bloodBankId);

    /**
     * Get donation history with blood bank name
     */
    @Query("SELECT d FROM Donation d " +
            "LEFT JOIN FETCH d.bloodBank " +
            "WHERE d.donor.id = :donorId " +
            "ORDER BY d.donationDate DESC")
    List<Donation> findDonationHistoryWithBloodBank(@Param("donorId") Long donorId);

    /**
     * Count donations on a specific date (for admin analytics)
     */
    long countByDonationDate(LocalDate donationDate);

    /**
     * Count donations at a blood bank on a specific date (for bank analytics)
     */
    long countByBloodBankIdAndDonationDate(Long bloodBankId, LocalDate donationDate);

    /**
     * Find pending donations (components not yet added)
     */
    List<Donation> findByBloodBankIdAndComponentsAddedFalseOrderByDonationDateDesc(Long bloodBankId);

    /**
     * Delete all donations for a specific blood bank
     */
    void deleteByBloodBankId(Long bloodBankId);
}
