package com.bloodbank.service;

import com.bloodbank.dto.ReservationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    private final String whatsappServiceUrl;
    private final WebClient webClient;

    public WhatsAppService(
            @Value("${whatsapp.service.url}") String whatsappServiceUrl,
            WebClient.Builder webClientBuilder) {
        this.whatsappServiceUrl = whatsappServiceUrl;
        this.webClient = webClientBuilder
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .build();
    }

    public Map<String, Object> getStatus() {
        try {
            Map<String, Object> result = webClient
                    .get()
                    .uri(whatsappServiceUrl + "/api/whatsapp/status")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .onErrorReturn(new HashMap<>())
                    .block();
            return result != null ? result : new HashMap<>();
        } catch (Exception e) {
            log.warn("Could not get WhatsApp status: {}", e.getMessage());
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("isReady", false);
            fallback.put("hasQR", false);
            fallback.put("error", "WhatsApp microservice not available");
            return fallback;
        }
    }

    public void sendReservationConfirmation(ReservationDto reservation) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", reservation.getWhatsappNumber());
            payload.put("patientName", reservation.getPatientName());
            payload.put("bloodType", reservation.getBloodType());
            payload.put("unitsNeeded", reservation.getUnitsNeeded());
            payload.put("bloodBankName", reservation.getBloodBankName());
            payload.put("reservationId", reservation.getId());

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-confirmation")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("WhatsApp confirmation sent for reservation {}", reservation.getId()),
                            error -> log.warn("Failed to send WhatsApp confirmation: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Failed to send reservation confirmation: {}", e.getMessage());
        }
    }

    public void sendStatusUpdate(String phoneNumber, String patientName,
            String status, String bloodBankName) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phoneNumber);
            payload.put("patientName", patientName);
            payload.put("status", status);
            payload.put("bloodBankName", bloodBankName);

            webClient
                    .post()
                    .uri(whatsappServiceUrl + "/api/whatsapp/send-status-update")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .subscribe(
                            response -> log.info("WhatsApp status update sent: {} -> {}", patientName, status),
                            error -> log.warn("Failed to send WhatsApp status update: {}", error.getMessage()));
        } catch (Exception e) {
            log.warn("Failed to send status update: {}", e.getMessage());
        }
    }

    public String getQRCode() {
        try {
            Map<String, Object> response = webClient
                    .get()
                    .uri(whatsappServiceUrl + "/api/whatsapp/qr")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.containsKey("qr")) {
                return (String) response.get("qr");
            }
            return null;
        } catch (Exception e) {
            log.warn("Could not get QR code: {}", e.getMessage());
            return null;
        }
    }
}
