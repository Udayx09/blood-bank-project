package com.bloodbank.controller;

import com.bloodbank.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        if (message == null || message.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Message is required");
            return ResponseEntity.badRequest().body(error);
        }

        String response = geminiService.chat(message);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("response", response);

        return ResponseEntity.ok(result);
    }
}
