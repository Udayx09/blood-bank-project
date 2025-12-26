package com.bloodbank.repository;

import com.bloodbank.entity.DonorRequest;
import com.bloodbank.entity.DonorRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DonorRequestRepository extends JpaRepository<DonorRequest, Long> {

        /**
         * Find existing pending request from a bank to a donor
         */
        Optional<DonorRequest> findByDonorIdAndBloodBankIdAndStatus(
                        Long donorId, Long bloodBankId, RequestStatus status);

        /**
         * Count requests sent by a bank today (for daily limit)
         */
        @Query("SELECT COUNT(r) FROM DonorRequest r " +
                        "WHERE r.bloodBank.id = :bankId " +
                        "AND r.requestedAt >= :since")
        long countByBloodBankIdSince(
                        @Param("bankId") Long bankId,
                        @Param("since") LocalDateTime since);

        /**
         * Check if donor was contacted by any bank within cooldown period
         */
        @Query("SELECT COUNT(r) > 0 FROM DonorRequest r " +
                        "WHERE r.donor.id = :donorId " +
                        "AND r.requestedAt >= :since")
        boolean existsByDonorIdSince(
                        @Param("donorId") Long donorId,
                        @Param("since") LocalDateTime since);

        /**
         * Get all requests for a specific blood bank
         */
        @Query("SELECT r FROM DonorRequest r " +
                        "WHERE r.bloodBank.id = :bankId " +
                        "ORDER BY r.requestedAt DESC")
        List<DonorRequest> findByBloodBankIdOrderByRequestedAtDesc(
                        @Param("bankId") Long bankId);

        /**
         * Get all requests for a specific donor
         */
        @Query("SELECT r FROM DonorRequest r " +
                        "WHERE r.donor.id = :donorId " +
                        "ORDER BY r.requestedAt DESC")
        List<DonorRequest> findByDonorIdOrderByRequestedAtDesc(
                        @Param("donorId") Long donorId);

        /**
         * Get donors recently contacted by a bank (for exclusion from search)
         */
        @Query("SELECT r.donor.id FROM DonorRequest r " +
                        "WHERE r.bloodBank.id = :bankId " +
                        "AND r.requestedAt >= :since")
        List<Long> findDonorIdsContactedByBankSince(
                        @Param("bankId") Long bankId,
                        @Param("since") LocalDateTime since);

        /**
         * Delete all donor requests for a specific blood bank
         */
        void deleteByBloodBankId(Long bloodBankId);
}
