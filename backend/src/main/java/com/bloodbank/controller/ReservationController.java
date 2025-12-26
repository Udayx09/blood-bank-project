package com.bloodbank.controller;

import com.bloodbank.dto.CreateReservationRequest;
import com.bloodbank.dto.ReservationDto;
import com.bloodbank.entity.Reservation;
import com.bloodbank.service.ReservationService;
import com.bloodbank.service.WhatsAppService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final WhatsAppService whatsAppService;

    public ReservationController(ReservationService reservationService, WhatsAppService whatsAppService) {
        this.reservationService = reservationService;
        this.whatsAppService = whatsAppService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createReservation(
            @Valid @RequestBody CreateReservationRequest request) {
        try {
            ReservationDto reservation = reservationService.createReservation(request);

            Map<String, Object> whatsappStatus = whatsAppService.getStatus();
            boolean isReady = Boolean.TRUE.equals(whatsappStatus.get("isReady"));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Reservation created successfully");
            response.put("whatsappNotification", isReady ? "sent" : "not_sent");
            response.put("data", reservation);

            return ResponseEntity.status(201).body(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllReservations() {
        List<ReservationDto> reservations = reservationService.getAllReservations();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("count", reservations.size());
        response.put("data", reservations);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getReservationById(@PathVariable Long id) {
        return reservationService.getReservationById(id)
                .map(reservation -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("data", reservation);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Reservation not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Map<String, Object>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        if (status == null || !Reservation.isValidStatus(status)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Invalid status. Must be one of: pending, confirmed, completed, cancelled");
            return ResponseEntity.badRequest().body(error);
        }

        return reservationService.updateStatus(id, status)
                .map(reservation -> {
                    Map<String, Object> whatsappStatus = whatsAppService.getStatus();
                    boolean isReady = Boolean.TRUE.equals(whatsappStatus.get("isReady"));

                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Reservation status updated to " + status);
                    response.put("whatsappNotification", isReady ? "sent" : "not_sent");
                    response.put("data", reservation);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Reservation not found");
                    return ResponseEntity.status(404).body(error);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> cancelReservation(@PathVariable Long id) {
        return reservationService.cancelReservation(id)
                .map(reservation -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Reservation cancelled");
                    response.put("data", reservation);
                    return ResponseEntity.ok(response);
                })
                .orElseGet(() -> {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("error", "Reservation not found");
                    return ResponseEntity.status(404).body(error);
                });
    }
}
