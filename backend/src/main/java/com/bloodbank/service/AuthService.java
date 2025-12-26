package com.bloodbank.service;

import com.bloodbank.dto.AuthRequest;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.repository.BloodBankRepository;
import com.bloodbank.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

        private static final Logger log = LoggerFactory.getLogger(AuthService.class);

        private final BloodBankRepository bloodBankRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtTokenProvider jwtTokenProvider;

        public AuthService(BloodBankRepository bloodBankRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider jwtTokenProvider) {
                this.bloodBankRepository = bloodBankRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtTokenProvider = jwtTokenProvider;
        }

        @Transactional
        public AuthRequest.AuthResponse register(String phone, String password, Long bloodBankId) {
                BloodBank bank = bloodBankRepository.findById(bloodBankId)
                                .orElse(null);

                if (bank == null) {
                        return AuthRequest.AuthResponse.builder()
                                        .success(false)
                                        .message("Blood bank not found")
                                        .build();
                }

                if (bank.getPasswordHash() != null) {
                        return AuthRequest.AuthResponse.builder()
                                        .success(false)
                                        .message("This blood bank already has an account")
                                        .build();
                }

                // Check if phone number is already used by another registered bank
                Optional<BloodBank> existingBank = bloodBankRepository.findFirstByPhoneAndPasswordHashIsNotNull(phone);
                if (existingBank.isPresent() && !existingBank.get().getId().equals(bloodBankId)) {
                        return AuthRequest.AuthResponse.builder()
                                        .success(false)
                                        .message("This phone number is already registered with another blood bank")
                                        .build();
                }

                String passwordHash = passwordEncoder.encode(password);
                bank.setPhone(phone);
                bank.setPasswordHash(passwordHash);
                bloodBankRepository.save(bank);

                String token = jwtTokenProvider.generateToken(bank.getId(), bank.getName(), bank.getEmail());

                log.info("Blood bank registered: {}", bank.getName());

                return AuthRequest.AuthResponse.builder()
                                .success(true)
                                .message("Account created successfully")
                                .token(token)
                                .bank(AuthRequest.AuthResponse.BankInfo.builder()
                                                .id(bank.getId())
                                                .name(bank.getName())
                                                .phone(bank.getPhone())
                                                .build())
                                .build();
        }

        public AuthRequest.AuthResponse login(String phone, String password) {
                // Normalize phone number - remove spaces, dashes, and handle country code
                // variations
                String normalizedPhone = phone.replaceAll("[\\s\\-()]", "");
                log.debug("Attempting login for phone: {} (normalized: {})", phone, normalizedPhone);

                // Try exact match first
                Optional<BloodBank> bankOpt = bloodBankRepository.findFirstByPhoneAndPasswordHashIsNotNull(phone);

                // If not found, try with normalized phone
                if (bankOpt.isEmpty()) {
                        bankOpt = bloodBankRepository.findFirstByPhoneAndPasswordHashIsNotNull(normalizedPhone);
                }

                // If still not found, try to find by phone containing the digits
                if (bankOpt.isEmpty()) {
                        log.debug("Exact match not found, trying flexible search...");
                        // Try without +91 prefix if present
                        String withoutPrefix = normalizedPhone.replaceFirst("^\\+?91", "");
                        bankOpt = bloodBankRepository.findFirstByPhoneContainingAndPasswordHashIsNotNull(withoutPrefix);
                }

                if (bankOpt.isEmpty()) {
                        log.warn("No registered bank found for phone: {} (tried exact and flexible match)", phone);
                        return AuthRequest.AuthResponse.builder()
                                        .success(false)
                                        .message("Invalid phone or password. Make sure your account is registered.")
                                        .build();
                }

                BloodBank bank = bankOpt.get();
                log.debug("Found bank: {} with ID: {}", bank.getName(), bank.getId());
                log.debug("Stored phone in DB: {}", bank.getPhone());
                log.debug("Stored password hash starts with: {}",
                                bank.getPasswordHash() != null
                                                ? bank.getPasswordHash().substring(0,
                                                                Math.min(10, bank.getPasswordHash().length()))
                                                : "null");

                boolean passwordMatches = passwordEncoder.matches(password, bank.getPasswordHash());
                log.debug("Password match result: {}", passwordMatches);

                if (!passwordMatches) {
                        log.warn("Password mismatch for bank: {} (ID: {})", bank.getName(), bank.getId());
                        return AuthRequest.AuthResponse.builder()
                                        .success(false)
                                        .message("Invalid phone or password")
                                        .build();
                }

                String token = jwtTokenProvider.generateToken(bank.getId(), bank.getName(), bank.getEmail());

                log.info("Blood bank logged in: {}", bank.getName());

                return AuthRequest.AuthResponse.builder()
                                .success(true)
                                .message("Login successful")
                                .token(token)
                                .bank(AuthRequest.AuthResponse.BankInfo.builder()
                                                .id(bank.getId())
                                                .name(bank.getName())
                                                .phone(bank.getPhone())
                                                .build())
                                .build();
        }

        public Optional<BloodBank> getBankFromToken(Long bankId) {
                return bloodBankRepository.findById(bankId);
        }

        // Temporary method to reset password for a specific bank by phone
        @Transactional
        public Map<String, Object> resetPassword(String phone, String newPassword) {
                Map<String, Object> result = new HashMap<>();

                // Find all banks with this phone
                Optional<BloodBank> bankOpt = bloodBankRepository
                                .findFirstByPhoneContainingAndPasswordHashIsNotNull(phone);

                if (bankOpt.isEmpty()) {
                        // Try to find any bank with this phone (even without password)
                        bankOpt = bloodBankRepository.findFirstByPhoneContaining(phone);
                }

                if (bankOpt.isEmpty()) {
                        result.put("success", false);
                        result.put("error", "No bank found with phone: " + phone);
                        return result;
                }

                BloodBank bank = bankOpt.get();
                String passwordHash = passwordEncoder.encode(newPassword);
                bank.setPasswordHash(passwordHash);
                bloodBankRepository.save(bank);

                log.info("Password reset for bank: {} (ID: {})", bank.getName(), bank.getId());

                result.put("success", true);
                result.put("message", "Password reset for: " + bank.getName());
                result.put("bankId", bank.getId());
                result.put("bankName", bank.getName());
                result.put("phone", bank.getPhone());
                return result;
        }
}
