package com.bloodbank.controller;

import com.bloodbank.service.WhatsAppService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = whatsAppService.getStatus();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isReady", Boolean.TRUE.equals(status.get("isReady")));
        response.put("hasQR", Boolean.TRUE.equals(status.get("hasQR")));

        if (status.containsKey("error")) {
            response.put("error", status.get("error"));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/qr")
    public ResponseEntity<Map<String, Object>> getQRCode() {
        String qrCode = whatsAppService.getQRCode();

        Map<String, Object> response = new HashMap<>();
        response.put("success", qrCode != null);

        if (qrCode != null) {
            response.put("qr", qrCode);
        } else {
            response.put("message", "No QR code available. Client may already be connected.");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendMessage(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String message = request.get("message");

        if (phoneNumber == null || message == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "phoneNumber and message are required");
            return ResponseEntity.badRequest().body(error);
        }

        Map<String, Object> status = whatsAppService.getStatus();
        boolean isReady = Boolean.TRUE.equals(status.get("isReady"));

        if (!isReady) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "WhatsApp is not connected");
            return ResponseEntity.status(503).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Message queued for sending");

        return ResponseEntity.ok(response);
    }
}
