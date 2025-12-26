package com.bloodbank.repository;

import com.bloodbank.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        /**
         * Find all reservations ordered by creation date (newest first)
         */
        List<Reservation> findAllByOrderByCreatedAtDesc();

        /**
         * Find reservations for a specific blood bank
         */
        List<Reservation> findByBloodBankIdOrderByCreatedAtDesc(Long bloodBankId);

        /**
         * Find reservations by status
         */
        List<Reservation> findByStatus(String status);

        /**
         * Find top N recent reservations for a blood bank
         */
        List<Reservation> findTop5ByBloodBankIdOrderByCreatedAtDesc(Long bloodBankId);

        /**
         * Count reservations by status
         */
        long countByStatus(String status);

        /**
         * Count reservations by status for a specific blood bank
         */
        long countByBloodBankIdAndStatus(Long bloodBankId, String status);

        /**
         * Count all reservations for a specific blood bank
         */
        long countByBloodBankId(Long bloodBankId);

        /**
         * Get reservation statistics
         */
        @Query("SELECT " +
                        "COUNT(r), " +
                        "SUM(CASE WHEN r.status = 'pending' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'confirmed' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'completed' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'cancelled' THEN 1 ELSE 0 END) " +
                        "FROM Reservation r")
        Object[] getReservationStats();

        /**
         * Get reservation statistics for a specific blood bank
         */
        @Query("SELECT " +
                        "COUNT(r), " +
                        "SUM(CASE WHEN r.status = 'pending' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'confirmed' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'completed' THEN 1 ELSE 0 END), " +
                        "SUM(CASE WHEN r.status = 'cancelled' THEN 1 ELSE 0 END) " +
                        "FROM Reservation r WHERE r.bloodBank.id = :bankId")
        Object[] getReservationStatsByBankId(@Param("bankId") Long bankId);

        /**
         * Delete all reservations for a blood bank (for cascade delete)
         */
        void deleteByBloodBankId(Long bloodBankId);
}
