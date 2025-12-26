package com.bloodbank.repository;

import com.bloodbank.entity.BloodUnit;
import com.bloodbank.entity.BloodUnit.BloodComponent;
import com.bloodbank.entity.BloodUnit.UnitStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BloodUnitRepository extends JpaRepository<BloodUnit, Long> {

        // Find all units for a blood bank
        List<BloodUnit> findByBloodBankIdOrderByExpiryDateAsc(Long bloodBankId);

        // Find available units for a blood bank
        List<BloodUnit> findByBloodBankIdAndStatusOrderByExpiryDateAsc(Long bloodBankId, UnitStatus status);

        // Find by blood type and status
        List<BloodUnit> findByBloodBankIdAndBloodTypeAndStatusOrderByExpiryDateAsc(
                        Long bloodBankId, String bloodType, UnitStatus status);

        // Find by component type
        List<BloodUnit> findByBloodBankIdAndComponentAndStatusOrderByExpiryDateAsc(
                        Long bloodBankId, BloodComponent component, UnitStatus status);

        // Find units expiring before a date
        @Query("SELECT bu FROM BloodUnit bu WHERE bu.bloodBank.id = :bankId AND bu.status = :status AND bu.expiryDate <= :date ORDER BY bu.expiryDate ASC")
        List<BloodUnit> findExpiringBefore(@Param("bankId") Long bankId, @Param("status") UnitStatus status,
                        @Param("date") LocalDate date);

        // Find units expiring within N days
        default List<BloodUnit> findExpiringWithinDays(Long bankId, int days) {
                return findExpiringBefore(bankId, UnitStatus.AVAILABLE, LocalDate.now().plusDays(days));
        }

        // Find expired units
        @Query("SELECT bu FROM BloodUnit bu WHERE bu.bloodBank.id = :bankId AND bu.expiryDate < :today AND bu.status = 'AVAILABLE'")
        List<BloodUnit> findExpiredUnits(@Param("bankId") Long bankId, @Param("today") LocalDate today);

        // Count by status
        long countByBloodBankIdAndStatus(Long bloodBankId, UnitStatus status);

        // Count by blood type and status
        long countByBloodBankIdAndBloodTypeAndStatus(Long bloodBankId, String bloodType, UnitStatus status);

        // Count expiring within days
        @Query("SELECT COUNT(bu) FROM BloodUnit bu WHERE bu.bloodBank.id = :bankId AND bu.status = 'AVAILABLE' AND bu.expiryDate <= :date AND bu.expiryDate >= :today")
        long countExpiringBetween(@Param("bankId") Long bankId, @Param("today") LocalDate today,
                        @Param("date") LocalDate date);

        // Check if unit number exists
        boolean existsByUnitNumber(String unitNumber);

        // Find by unit number
        BloodUnit findByUnitNumber(String unitNumber);

        // Summary: count available units by blood type for a bank
        @Query("SELECT bu.bloodType, COUNT(bu) FROM BloodUnit bu WHERE bu.bloodBank.id = :bankId AND bu.status = 'AVAILABLE' GROUP BY bu.bloodType")
        List<Object[]> countAvailableByBloodType(@Param("bankId") Long bankId);

        // Summary: count available units by component for a bank
        @Query("SELECT bu.component, COUNT(bu) FROM BloodUnit bu WHERE bu.bloodBank.id = :bankId AND bu.status = 'AVAILABLE' GROUP BY bu.component")
        List<Object[]> countAvailableByComponent(@Param("bankId") Long bankId);

        // Count total units for a bank (for sequential numbering)
        long countByBloodBankId(Long bloodBankId);

        // Delete all units for a blood bank
        void deleteByBloodBankId(Long bloodBankId);
}
