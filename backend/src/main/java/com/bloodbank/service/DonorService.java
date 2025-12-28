package com.bloodbank.service;

import com.bloodbank.dto.DonorDto;
import com.bloodbank.entity.Donation;
import com.bloodbank.entity.Donor;
import com.bloodbank.entity.DonorOtp;
import com.bloodbank.entity.DonorRequest;
import com.bloodbank.entity.BloodBank;
import com.bloodbank.repository.DonationRepository;
import com.bloodbank.repository.DonorRepository;
import com.bloodbank.repository.DonorOtpRepository;
import com.bloodbank.repository.DonorRequestRepository;
import com.bloodbank.repository.BloodBankRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DonorService {

    private static final Logger log = LoggerFactory.getLogger(DonorService.class);
    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int DONATION_GAP_DAYS = 90;
    private static final int DAILY_REQUEST_LIMIT = 10;
    private static final int COOLDOWN_DAYS = 7;

    private final DonorRepository donorRepository;
    private final DonorOtpRepository otpRepository;
    private final DonationRepository donationRepository;
    private final DonorRequestRepository donorRequestRepository;
    private final BloodBankRepository bloodBankRepository;
    private final WebClient webClient;
    private final String whatsappServiceUrl;
    private final PasswordEncoder passwordEncoder;

    public DonorService(
            DonorRepository donorRepository,
            DonorOtpRepository otpRepository,
            DonationRepository donationRepository,
            DonorRequestRepository donorRequestRepository,
            BloodBankRepository bloodBankRepository,
            WebClient.Builder webClientBuilder,
            @Value("${whatsapp.service.url}") String whatsappServiceUrl,
            PasswordEncoder passwordEncoder) {
        this.donorRepository = donorRepository;
        this.otpRepository = otpRepository;
        this.donationRepository = donationRepository;
        this.donorRequestRepository = donorRequestRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.webClient = webClientBuilder.build();
        this.whatsappServiceUrl = whatsappServiceUrl;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Generate and send OTP to donor's WhatsApp
     */
    @Transactional
    public Map<String, Object> sendOtp(String phone) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Normalize phone number
            String normalizedPhone = normalizePhone(phone);

            // Generate 6-digit OTP
            String otp = generateOtp();

            // Store OTP
            DonorOtp donorOtp = new DonorOtp(
                    normalizedPhone,
                    otp,
                    LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES));
            otpRepository.save(donorOtp);

            // Send OTP via WhatsApp
            sendOtpViaWhatsApp(normalizedPhone, otp);

            result.put("success", true);
            result.put("message", "OTP sent to " + maskPhone(normalizedPhone));
            result.put("expiresIn", OTP_EXPIRY_MINUTES + " minutes");

            log.info("OTP sent to phone: {}", maskPhone(normalizedPhone));

        } catch (Exception e) {
            log.error("Failed to send OTP: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Failed to send OTP. Please try again.");
        }

        return result;
    }

    /**
     * Verify OTP
     */
    @Transactional
    public Map<String, Object> verifyOtp(String phone, String otp) {
        Map<String, Object> result = new HashMap<>();
        String normalizedPhone = normalizePhone(phone);

        Optional<DonorOtp> validOtp = otpRepository.findFirstByPhoneAndIsUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(
                normalizedPhone, LocalDateTime.now());

        if (validOtp.isEmpty()) {
            result.put("success", false);
            result.put("error", "Invalid or expired OTP");
            return result;
        }

        DonorOtp donorOtp = validOtp.get();

        if (!donorOtp.getOtp().equals(otp)) {
            result.put("success", false);
            result.put("error", "Incorrect OTP");
            return result;
        }

        // Mark OTP as used
        donorOtp.setIsUsed(true);
        otpRepository.save(donorOtp);

        // Check if donor already exists
        Optional<Donor> existingDonor = donorRepository.findByPhone(normalizedPhone);

        result.put("success", true);
        result.put("verified", true);
        result.put("phone", normalizedPhone);
        result.put("isExistingDonor", existingDonor.isPresent());

        if (existingDonor.isPresent()) {
            result.put("donor", convertToDto(existingDonor.get()));
        }

        log.info("OTP verified for phone: {}", maskPhone(normalizedPhone));

        return result;
    }

    /**
     * Register new donor
     */
    @Transactional
    public Map<String, Object> registerDonor(DonorDto donorDto) {
        Map<String, Object> result = new HashMap<>();

        try {
            String normalizedPhone = normalizePhone(donorDto.getPhone());

            // Check if already registered
            if (donorRepository.existsByPhone(normalizedPhone)) {
                result.put("success", false);
                result.put("error", "Phone number already registered");
                return result;
            }

            // Create new donor
            Donor donor = new Donor();
            donor.setName(donorDto.getName());
            donor.setPhone(normalizedPhone);
            donor.setBloodType(donorDto.getBloodType());
            donor.setDateOfBirth(donorDto.getDateOfBirth());
            donor.setCity(donorDto.getCity());
            donor.setWeight(donorDto.getWeight());
            donor.setLastDonationDate(donorDto.getLastDonationDate());
            donor.setIsVerified(true); // Verified via OTP

            donor = donorRepository.save(donor);

            result.put("success", true);
            result.put("message", "Registration successful!");
            result.put("donor", convertToDto(donor));

            // Send welcome message via WhatsApp
            sendWelcomeMessage(normalizedPhone, donor.getName());

            log.info("New donor registered: {} ({})", donor.getName(), maskPhone(normalizedPhone));

        } catch (Exception e) {
            log.error("Failed to register donor: {}", e.getMessage());
            result.put("success", false);
            result.put("error", "Registration failed. Please try again.");
        }

        return result;
    }

    /**
     * Get donor profile by phone
     */
    public Optional<DonorDto> getDonorByPhone(String phone) {
        String normalizedPhone = normalizePhone(phone);
        return donorRepository.findByPhone(normalizedPhone)
                .map(this::convertToDto);
    }

    /**
     * Get donor profile by ID
     */
    public Optional<DonorDto> getDonorById(Long id) {
        return donorRepository.findById(id)
                .map(this::convertToDto);
    }

    /**
     * Update donor profile
     */
    @Transactional
    public Map<String, Object> updateDonor(Long id, DonorDto donorDto) {
        Map<String, Object> result = new HashMap<>();

        Optional<Donor> optionalDonor = donorRepository.findById(id);
        if (optionalDonor.isEmpty()) {
            result.put("success", false);
            result.put("error", "Donor not found");
            return result;
        }

        Donor donor = optionalDonor.get();
        donor.setName(donorDto.getName());
        donor.setBloodType(donorDto.getBloodType());
        donor.setCity(donorDto.getCity());
        donor.setWeight(donorDto.getWeight());

        donor = donorRepository.save(donor);

        result.put("success", true);
        result.put("donor", convertToDto(donor));

        return result;
    }

    /**
     * Record a donation
     */
    @Transactional
    public Map<String, Object> recordDonation(Long donorId, Long bloodBankId, LocalDate donationDate) {
        Map<String, Object> result = new HashMap<>();

        Optional<Donor> optionalDonor = donorRepository.findById(donorId);
        if (optionalDonor.isEmpty()) {
            result.put("success", false);
            result.put("error", "Donor not found");
            return result;
        }

        Donor donor = optionalDonor.get();
        BloodBank bloodBank = null;

        if (bloodBankId != null) {
            bloodBank = bloodBankRepository.findById(bloodBankId).orElse(null);
        }

        // Create donation record
        Donation donation = new Donation(donor, bloodBank, donationDate);
        donationRepository.save(donation);

        // Update donor's last donation date
        donor.setLastDonationDate(donationDate);
        donorRepository.save(donor);

        result.put("success", true);
        result.put("message", "Donation recorded successfully");
        result.put("nextEligibleDate", donationDate.plusDays(DONATION_GAP_DAYS));

        log.info("Donation recorded for donor: {}", donor.getName());

        return result;
    }

    /**
     * Get donor's donation history
     */
    public List<Map<String, Object>> getDonationHistory(Long donorId) {
        List<Donation> donations = donationRepository.findDonationHistoryWithBloodBank(donorId);
        List<Map<String, Object>> history = new ArrayList<>();

        for (Donation donation : donations) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", donation.getId());
            item.put("donationDate", donation.getDonationDate());
            item.put("bloodBankName", donation.getBloodBank() != null ? donation.getBloodBank().getName() : "Unknown");
            item.put("units", donation.getUnits());
            history.add(item);
        }

        return history;
    }

    /**
     * Find available donors for a blood type in a city
     */
    public List<DonorDto> findAvailableDonors(String bloodType, String city) {
        LocalDate eligibleDate = LocalDate.now().minusDays(DONATION_GAP_DAYS);
        List<Donor> donors = donorRepository.findAvailableDonors(bloodType, city, eligibleDate);
        return donors.stream().map(this::convertToDto).toList();
    }

    /**
     * Get donors who became eligible today (for notifications)
     */
    public List<Donor> getDonorsEligibleToday() {
        LocalDate donationDateFor90Days = LocalDate.now().minusDays(DONATION_GAP_DAYS);
        return donorRepository.findDonorsEligibleOn(donationDateFor90Days);
    }

    // Helper methods

    private String normalizePhone(String phone) {
        if (phone == null)
            return null;
        // Remove all non-digits
        String digits = phone.replaceAll("[^0-9]", "");
        // Add country code if missing
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        return digits;
    }

    private String generateOtp() {
        Random random = new Random();
        return String.format("%06d", random.nextInt(1000000));
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private void sendOtpViaWhatsApp(String phone, String otp) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("otp", otp);
            payload.put("type", "donor_otp");

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-donor-otp")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("OTP WhatsApp sent to {}", maskPhone(phone)),
                            error -> log.warn("Failed to send OTP via WhatsApp: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Error sending OTP via WhatsApp: {}", e.getMessage());
        }
    }

    private void sendWelcomeMessage(String phone, String name) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("donorName", name);
            payload.put("type", "donor_welcome");

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-donor-welcome")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("Welcome message sent to {}", name),
                            error -> log.warn("Failed to send welcome message: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Error sending welcome message: {}", e.getMessage());
        }
    }

    private DonorDto convertToDto(Donor donor) {
        DonorDto dto = new DonorDto();
        dto.setId(donor.getId());
        dto.setName(donor.getName());
        dto.setPhone(donor.getPhone());
        dto.setBloodType(donor.getBloodType());
        dto.setDateOfBirth(donor.getDateOfBirth());
        dto.setCity(donor.getCity());
        dto.setWeight(donor.getWeight());
        dto.setLastDonationDate(donor.getLastDonationDate());
        dto.setIsVerified(donor.getIsVerified());
        dto.setIsAvailableForContact(donor.getIsAvailableForContact());
        dto.setEligible(donor.isEligible());
        dto.setDaysUntilEligible(donor.getDaysUntilEligible());
        dto.setAge(donor.getAge());
        return dto;
    }

    // ==================== DONOR SEARCH FOR BANK PORTAL ====================

    /**
     * Search for donors available for contact (for bank portal)
     * Excludes: opted-out donors, recently contacted, ineligible
     */
    public List<DonorDto> searchDonorsForBank(Long bankId, String bloodType, String city) {
        // DEMO MODE: Use old date to show ALL donors regardless of eligibility
        LocalDate eligibleDate = LocalDate.now().minusYears(10);
        LocalDateTime cooldownDate = LocalDateTime.now().minusDays(COOLDOWN_DAYS);

        // Get IDs of donors contacted by this bank within cooldown period
        List<Long> recentlyContacted = donorRequestRepository.findDonorIdsContactedByBankSince(
                bankId, cooldownDate);

        // Ensure list is not empty for SQL IN clause
        if (recentlyContacted.isEmpty()) {
            recentlyContacted = List.of(-1L);
        }

        List<Donor> donors;
        if (bloodType != null && !bloodType.isEmpty() && !bloodType.equals("ALL")) {
            donors = donorRepository.searchAvailableDonorsExcluding(
                    bloodType, city, eligibleDate, recentlyContacted);
        } else {
            donors = donorRepository.searchAllAvailableDonorsExcluding(
                    city, eligibleDate, recentlyContacted);
        }

        return donors.stream().map(this::convertToDto).toList();
    }

    /**
     * Send donation request from bank to donor
     */
    @Transactional
    public Map<String, Object> sendDonationRequest(Long donorId, Long bankId) {
        Map<String, Object> result = new HashMap<>();

        // Check daily limit
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long todayCount = donorRequestRepository.countByBloodBankIdSince(bankId, today);
        if (todayCount >= DAILY_REQUEST_LIMIT) {
            result.put("success", false);
            result.put("error", "Daily request limit reached (" + DAILY_REQUEST_LIMIT + "/day)");
            return result;
        }

        // Check if donor is in cooldown
        LocalDateTime cooldownDate = LocalDateTime.now().minusDays(COOLDOWN_DAYS);
        boolean recentlyContacted = donorRequestRepository.existsByDonorIdSince(donorId, cooldownDate);
        if (recentlyContacted) {
            result.put("success", false);
            result.put("error", "This donor was contacted recently. Please wait " + COOLDOWN_DAYS + " days.");
            return result;
        }

        // Get donor and bank
        Optional<Donor> optDonor = donorRepository.findById(donorId);
        Optional<BloodBank> optBank = bloodBankRepository.findById(bankId);

        if (optDonor.isEmpty() || optBank.isEmpty()) {
            result.put("success", false);
            result.put("error", "Donor or Blood Bank not found");
            return result;
        }

        Donor donor = optDonor.get();
        BloodBank bank = optBank.get();

        // Check if donor is available for contact (NULL or true = available)
        Boolean isAvailable = donor.getIsAvailableForContact();
        if (Boolean.FALSE.equals(isAvailable)) {
            result.put("success", false);
            result.put("error", "Donor has opted out of direct contact");
            return result;
        }

        // Create request record
        DonorRequest request = new DonorRequest(donor, bank);
        donorRequestRepository.save(request);

        // Send WhatsApp message
        sendDonationRequestViaWhatsApp(donor.getPhone(), donor.getName(),
                bank.getName(), bank.getCity(), bank.getPhone(), bank.getAddress());

        result.put("success", true);
        result.put("message", "Donation request sent to " + donor.getName());
        result.put("requestId", request.getId());

        log.info("Donation request sent from {} to donor {}", bank.getName(), donor.getName());

        return result;
    }

    /**
     * Get bank's request history
     */
    public List<Map<String, Object>> getBankRequestHistory(Long bankId) {
        List<DonorRequest> requests = donorRequestRepository.findByBloodBankIdOrderByRequestedAtDesc(bankId);
        List<Map<String, Object>> history = new ArrayList<>();

        for (DonorRequest req : requests) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", req.getId());
            item.put("donorId", req.getDonor().getId());
            item.put("donorName", req.getDonor().getName());
            item.put("donorPhone", req.getDonor().getPhone());
            item.put("bloodType", req.getDonor().getBloodType());
            item.put("status", req.getStatus().name());
            item.put("requestedAt", req.getRequestedAt());
            item.put("respondedAt", req.getRespondedAt());
            history.add(item);
        }

        return history;
    }

    /**
     * Get incoming donation requests for a donor
     */
    public List<Map<String, Object>> getDonorRequests(Long donorId) {
        List<DonorRequest> requests = donorRequestRepository.findByDonorIdOrderByRequestedAtDesc(donorId);
        List<Map<String, Object>> result = new ArrayList<>();

        for (DonorRequest req : requests) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", req.getId());
            item.put("bankName", req.getBloodBank().getName());
            item.put("bankAddress", req.getBloodBank().getAddress());
            item.put("bankCity", req.getBloodBank().getCity());
            item.put("bankPhone", req.getBloodBank().getPhone());
            item.put("status", req.getStatus().name());
            item.put("requestedAt", req.getRequestedAt());
            item.put("respondedAt", req.getRespondedAt());
            item.put("isExpired", req.isExpired());
            result.add(item);
        }

        return result;
    }

    /**
     * Donor responds to a donation request (accept or decline)
     */
    @Transactional
    public Map<String, Object> respondToRequest(Long requestId, Long donorId, boolean accept) {
        Map<String, Object> result = new HashMap<>();

        Optional<DonorRequest> optRequest = donorRequestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            result.put("success", false);
            result.put("error", "Request not found");
            return result;
        }

        DonorRequest request = optRequest.get();

        // Verify this request belongs to this donor
        if (!request.getDonor().getId().equals(donorId)) {
            result.put("success", false);
            result.put("error", "Not authorized");
            return result;
        }

        // Check if already responded
        if (request.getStatus() != DonorRequest.RequestStatus.PENDING) {
            result.put("success", false);
            result.put("error", "Request already responded to");
            return result;
        }

        // Check if expired
        if (request.isExpired()) {
            request.setStatus(DonorRequest.RequestStatus.EXPIRED);
            donorRequestRepository.save(request);
            result.put("success", false);
            result.put("error", "Request has expired");
            return result;
        }

        // Update status
        if (accept) {
            request.accept();
        } else {
            request.decline();
        }
        donorRequestRepository.save(request);

        result.put("success", true);
        result.put("status", request.getStatus().name());
        result.put("message", accept ? "Request accepted! Please visit the blood bank." : "Request declined.");

        log.info("Donor {} {} request from {}",
                request.getDonor().getName(),
                accept ? "accepted" : "declined",
                request.getBloodBank().getName());

        return result;
    }

    /**
     * Mark a request as donated (called when bank records donation)
     */
    @Transactional
    public Map<String, Object> markRequestAsDonated(Long requestId) {
        Map<String, Object> result = new HashMap<>();

        Optional<DonorRequest> optRequest = donorRequestRepository.findById(requestId);
        if (optRequest.isEmpty()) {
            result.put("success", false);
            result.put("error", "Request not found");
            return result;
        }

        DonorRequest request = optRequest.get();
        request.setStatus(DonorRequest.RequestStatus.DONATED);
        request.setRespondedAt(LocalDateTime.now());
        donorRequestRepository.save(request);

        result.put("success", true);
        result.put("message", "Request marked as donated");

        // Send thank you message to donor
        sendThankYouMessage(request.getDonor().getPhone(), request.getDonor().getName(),
                request.getBloodBank().getName());

        return result;
    }

    /**
     * Send thank you message to donor after donation
     */
    private void sendThankYouMessage(String phone, String donorName, String bankName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("donorName", donorName);
            payload.put("bloodBankName", bankName);

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-thank-you")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("Thank you message sent to {}", donorName),
                            error -> log.warn("Failed to send thank you message: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Error sending thank you message: {}", e.getMessage());
        }
    }

    /**
     * Update donor's availability for contact (opt-in/opt-out)
     */
    @Transactional
    public Map<String, Object> updateContactAvailability(Long donorId, boolean available) {
        Map<String, Object> result = new HashMap<>();

        Optional<Donor> optDonor = donorRepository.findById(donorId);
        if (optDonor.isEmpty()) {
            result.put("success", false);
            result.put("error", "Donor not found");
            return result;
        }

        Donor donor = optDonor.get();
        donor.setIsAvailableForContact(available);
        donorRepository.save(donor);

        result.put("success", true);
        result.put("isAvailableForContact", available);
        result.put("message", available ? "You will now receive donation requests from blood banks"
                : "You have opted out of donation requests");

        return result;
    }

    /**
     * Send donation request via WhatsApp
     */
    private void sendDonationRequestViaWhatsApp(String phone, String donorName,
            String bankName, String city, String bankPhone, String bankAddress) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("donorName", donorName);
            payload.put("bloodBankName", bankName);
            payload.put("city", city);
            payload.put("bankPhone", bankPhone);
            payload.put("bankAddress", bankAddress);

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-donation-request")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("Donation request sent to {}", donorName),
                            error -> log.warn("Failed to send donation request: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Error sending donation request: {}", e.getMessage());
        }
    }

    // ==================== PASSWORD AUTHENTICATION ====================

    /**
     * Login donor with phone and password
     */
    public Map<String, Object> loginWithPassword(String phone, String password) {
        Map<String, Object> result = new HashMap<>();
        String normalizedPhone = normalizePhone(phone);

        Optional<Donor> optDonor = donorRepository.findByPhone(normalizedPhone);
        if (optDonor.isEmpty()) {
            result.put("success", false);
            result.put("error", "Account not found. Please register first.");
            return result;
        }

        Donor donor = optDonor.get();

        // Check if donor has a password set
        if (donor.getPassword() == null || donor.getPassword().isEmpty()) {
            result.put("success", false);
            result.put("error", "No password set. Please login with OTP first to set your password.");
            result.put("needsPassword", true);
            return result;
        }

        // Verify password
        if (!passwordEncoder.matches(password, donor.getPassword())) {
            result.put("success", false);
            result.put("error", "Incorrect password");
            return result;
        }

        result.put("success", true);
        result.put("donor", convertToDto(donor));
        result.put("message", "Login successful");

        log.info("Donor logged in with password: {}", maskPhone(normalizedPhone));

        return result;
    }

    /**
     * Set password for a donor (after OTP verification)
     */
    @Transactional
    public Map<String, Object> setPassword(Long donorId, String newPassword) {
        Map<String, Object> result = new HashMap<>();

        if (newPassword == null || newPassword.length() < 6) {
            result.put("success", false);
            result.put("error", "Password must be at least 6 characters");
            return result;
        }

        Optional<Donor> optDonor = donorRepository.findById(donorId);
        if (optDonor.isEmpty()) {
            result.put("success", false);
            result.put("error", "Donor not found");
            return result;
        }

        Donor donor = optDonor.get();
        donor.setPassword(passwordEncoder.encode(newPassword));
        donor.setIsVerified(true); // Mark as verified since they set password
        donorRepository.save(donor);

        result.put("success", true);
        result.put("message", "Password set successfully! You can now login with your password.");

        log.info("Password set for donor: {}", donor.getName());

        return result;
    }

    /**
     * Check if donor has password set
     */
    public boolean hasPasswordSet(Long donorId) {
        Optional<Donor> optDonor = donorRepository.findById(donorId);
        if (optDonor.isEmpty())
            return false;
        String password = optDonor.get().getPassword();
        return password != null && !password.isEmpty();
    }
}
