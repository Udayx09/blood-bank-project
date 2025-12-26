package com.bloodbank.repository;

import com.bloodbank.entity.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {

        /**
         * Find all inventory for a specific blood bank
         */
        List<BloodInventory> findByBloodBankIdOrderByBloodType(Long bloodBankId);

        /**
         * Find all inventory for a specific blood bank (for analytics)
         */
        List<BloodInventory> findByBloodBankId(Long bloodBankId);

        /**
         * Find specific blood type inventory for a blood bank
         */
        Optional<BloodInventory> findByBloodBankIdAndBloodType(Long bloodBankId, String bloodType);

        /**
         * Get total units by blood type across all banks
         */
        @Query("SELECT bi.bloodType, SUM(bi.unitsAvailable) " +
                        "FROM BloodInventory bi GROUP BY bi.bloodType ORDER BY bi.bloodType")
        List<Object[]> getTotalUnitsByBloodType();

        /**
         * Get total units available
         */
        @Query("SELECT COALESCE(SUM(bi.unitsAvailable), 0) FROM BloodInventory bi")
        Long getTotalUnitsAvailable();

        /**
         * Find expiring blood (within specified days)
         */
        @Query("SELECT bi FROM BloodInventory bi " +
                        "WHERE bi.bloodBank.id = :bankId " +
                        "AND bi.expiryDate <= :expiryBefore " +
                        "AND bi.unitsAvailable > 0 " +
                        "ORDER BY bi.expiryDate ASC")
        List<BloodInventory> findExpiringBlood(
                        @Param("bankId") Long bankId,
                        @Param("expiryBefore") LocalDate expiryBefore);

        /**
         * Find all low stock inventory (below threshold)
         */
        @Query("SELECT bi FROM BloodInventory bi " +
                        "WHERE bi.unitsAvailable < :threshold AND bi.unitsAvailable > 0")
        List<BloodInventory> findLowStock(@Param("threshold") int threshold);

        /**
         * Deduct units from inventory
         */
        @Modifying
        @Query("UPDATE BloodInventory bi " +
                        "SET bi.unitsAvailable = CASE WHEN bi.unitsAvailable >= :units THEN bi.unitsAvailable - :units ELSE 0 END, "
                        +
                        "bi.lastUpdated = CURRENT_TIMESTAMP " +
                        "WHERE bi.bloodBank.id = :bankId AND bi.bloodType = :bloodType")
        int deductUnits(
                        @Param("bankId") Long bankId,
                        @Param("bloodType") String bloodType,
                        @Param("units") int units);

        /**
         * Delete all inventory for a blood bank (for cascade delete)
         */
        void deleteByBloodBankId(Long bloodBankId);
}
