package com.bloodbank.repository;

import com.bloodbank.entity.DonorOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface DonorOtpRepository extends JpaRepository<DonorOtp, Long> {

        /**
         * Find the latest valid OTP for a phone number
         */
        Optional<DonorOtp> findFirstByPhoneAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                        String phone, LocalDateTime now);

        /**
         * Mark all OTPs for a phone as used (cleanup)
         */
        @Modifying
        @Query("UPDATE DonorOtp o SET o.isUsed = true WHERE o.phone = :phone")
        void markAllAsUsed(@Param("phone") String phone);

        /**
         * Delete expired OTPs (cleanup job)
         */
        @Modifying
        @Query("DELETE FROM DonorOtp o WHERE o.expiresAt < :now")
        void deleteExpiredOtps(@Param("now") LocalDateTime now);
}
