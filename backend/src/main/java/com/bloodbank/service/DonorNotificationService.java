package com.bloodbank.service;

import com.bloodbank.entity.Donor;
import com.bloodbank.repository.DonorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class DonorNotificationService {

    private static final Logger log = LoggerFactory.getLogger(DonorNotificationService.class);
    private static final int DONATION_GAP_DAYS = 90;

    private final DonorRepository donorRepository;
    private final WebClient webClient;
    private final String whatsappServiceUrl;

    public DonorNotificationService(
            DonorRepository donorRepository,
            WebClient.Builder webClientBuilder,
            @Value("${whatsapp.service.url}") String whatsappServiceUrl) {
        this.donorRepository = donorRepository;
        this.webClient = webClientBuilder.build();
        this.whatsappServiceUrl = whatsappServiceUrl;
    }

    /**
     * Scheduled job: Runs every day at 9:00 AM to check for donors who became
     * eligible
     * Cron: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void sendDailyEligibilityReminders() {
        log.info("Starting daily eligibility reminder job...");

        // Find donors whose last donation was exactly 90 days ago
        LocalDate eligibilityDate = LocalDate.now().minusDays(DONATION_GAP_DAYS);
        List<Donor> eligibleDonors = donorRepository.findDonorsEligibleOn(eligibilityDate);

        log.info("Found {} donors who became eligible today", eligibleDonors.size());

        for (Donor donor : eligibleDonors) {
            try {
                sendEligibilityReminder(donor.getPhone(), donor.getName());
                log.info("Sent eligibility reminder to: {}", donor.getName());
            } catch (Exception e) {
                log.error("Failed to send reminder to donor {}: {}", donor.getId(), e.getMessage());
            }
        }

        log.info("Completed daily eligibility reminder job");
    }

    /**
     * Send eligibility reminder to a specific donor
     */
    public void sendEligibilityReminder(String phone, String donorName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("donorName", donorName);

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-eligibility-reminder")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("Eligibility reminder sent to {}", donorName),
                            error -> log.warn("Failed to send eligibility reminder: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Error sending eligibility reminder: {}", e.getMessage());
        }
    }

    /**
     * Send blood shortage alert to all eligible donors of a specific blood type in
     * a city
     */
    public void sendBloodShortageAlert(String bloodType, String city, String bloodBankName) {
        log.info("Sending blood shortage alert for {} in {}", bloodType, city);

        // Find eligible donors matching the blood type and city
        LocalDate eligibleDate = LocalDate.now().minusDays(DONATION_GAP_DAYS);
        List<Donor> eligibleDonors = donorRepository.findAvailableDonors(bloodType, city, eligibleDate);

        log.info("Found {} eligible donors for {} in {}", eligibleDonors.size(), bloodType, city);

        for (Donor donor : eligibleDonors) {
            try {
                sendBloodShortageAlertToOne(
                        donor.getPhone(),
                        donor.getName(),
                        bloodType,
                        city,
                        bloodBankName);
            } catch (Exception e) {
                log.error("Failed to send shortage alert to donor {}: {}", donor.getId(), e.getMessage());
            }
        }
    }

    /**
     * Send blood shortage alert to a single donor
     */
    private void sendBloodShortageAlertToOne(String phone, String donorName,
            String bloodType, String city, String bloodBankName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phone);
            payload.put("donorName", donorName);
            payload.put("bloodType", bloodType);
            payload.put("city", city);
            payload.put("bloodBankName", bloodBankName);

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-blood-shortage-alert")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.debug("Shortage alert sent to {}", donorName),
                            error -> log.warn("Failed to send shortage alert: {}", error.getMessage()));
        } catch (Exception e) {
            log.error("Error sending shortage alert: {}", e.getMessage());
        }
    }

    /**
     * Send alerts for all blood types that are critically low (less than threshold)
     */
    public void sendCriticalShortageAlerts(String city, String bloodBankName,
            Map<String, Integer> inventoryByType, int criticalThreshold) {

        for (Map.Entry<String, Integer> entry : inventoryByType.entrySet()) {
            String bloodType = entry.getKey();
            int units = entry.getValue();

            if (units < criticalThreshold) {
                log.warn("Critical shortage: {} has only {} units", bloodType, units);
                sendBloodShortageAlert(bloodType, city, bloodBankName);
            }
        }
    }

    /**
     * Manual trigger to test the eligibility reminder
     * Can be called from an admin endpoint
     */
    public Map<String, Object> triggerManualEligibilityCheck() {
        Map<String, Object> result = new HashMap<>();

        LocalDate eligibilityDate = LocalDate.now().minusDays(DONATION_GAP_DAYS);
        List<Donor> eligibleDonors = donorRepository.findDonorsEligibleOn(eligibilityDate);

        result.put("success", true);
        result.put("eligibleCount", eligibleDonors.size());
        result.put("message", "Found " + eligibleDonors.size() + " donors eligible today");

        for (Donor donor : eligibleDonors) {
            sendEligibilityReminder(donor.getPhone(), donor.getName());
        }

        return result;
    }
}
