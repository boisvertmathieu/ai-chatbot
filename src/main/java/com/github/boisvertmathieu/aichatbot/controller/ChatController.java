package com.github.boisvertmathieu.aichatbot.controller;

import com.github.boisvertmathieu.aichatbot.dto.ChatRequest;
import com.github.boisvertmathieu.aichatbot.dto.ChatResponse;
import com.github.boisvertmathieu.aichatbot.dto.FeedbackRequest;
import com.github.boisvertmathieu.aichatbot.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ChatController {
    
    private final ChatbotService chatbotService;
    
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> processQuestion(@Valid @RequestBody ChatRequest request) {
        log.info("Réception d'une nouvelle question pour conversationId: {}", request.getConversationId());
        
        try {
            ChatResponse response = chatbotService.processQuestion(request);
            
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.internalServerError().body(response);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la question: {}", e.getMessage(), e);
            
            ChatResponse errorResponse = ChatResponse.builder()
                .conversationId(request.getConversationId())
                .success(false)
                .errorMessage("Erreur interne du serveur: " + e.getMessage())
                .build();
                
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    @PostMapping("/feedback")
    public ResponseEntity<Map<String, Object>> processFeedback(@Valid @RequestBody FeedbackRequest request) {
        log.info("Réception d'un feedback pour conversationId: {}", request.getConversationId());
        
        try {
            chatbotService.processFeedback(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Feedback enregistré avec succès");
            response.put("conversationId", request.getConversationId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Conversation non trouvée pour le feedback: {}", e.getMessage());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du traitement du feedback: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur interne du serveur");
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AI Chatbot");
        health.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(health);
    }
} 