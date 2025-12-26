package com.bloodbank.repository;

import com.bloodbank.entity.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, Long> {

    /**
     * Find blood bank by phone number (for login) - only finds banks with passwords
     * set
     */
    Optional<BloodBank> findFirstByPhoneAndPasswordHashIsNotNull(String phone);

    /**
     * Find blood bank by phone containing digits (flexible match) - only finds
     * banks with passwords set
     */
    Optional<BloodBank> findFirstByPhoneContainingAndPasswordHashIsNotNull(String phone);

    /**
     * Find first blood bank by phone containing digits (for password reset)
     */
    Optional<BloodBank> findFirstByPhoneContaining(String phone);

    /**
     * Check if phone number already exists
     */
    boolean existsByPhone(String phone);

    /**
     * Find all blood banks in a specific city
     */
    List<BloodBank> findByCity(String city);

    /**
     * Find all open blood banks
     */
    List<BloodBank> findByIsOpenTrue();

    /**
     * Count blood banks with accounts (has password)
     */
    @Query("SELECT COUNT(b) FROM BloodBank b WHERE b.passwordHash IS NOT NULL")
    long countWithAccounts();

    /**
     * Find blood banks that have a specific blood type available
     */
    @Query("SELECT b FROM BloodBank b " +
            "WHERE EXISTS (SELECT 1 FROM BloodInventory bi " +
            "WHERE bi.bloodBank.id = b.id AND bi.bloodType = :bloodType AND bi.unitsAvailable > 0)")
    List<BloodBank> findByBloodTypeAvailable(@Param("bloodType") String bloodType);
}
