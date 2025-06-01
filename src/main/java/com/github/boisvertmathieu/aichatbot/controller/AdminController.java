package com.github.boisvertmathieu.aichatbot.controller;

import com.github.boisvertmathieu.aichatbot.repository.ConversationRepository;
import com.github.boisvertmathieu.aichatbot.repository.KnowledgeDocumentRepository;
import com.github.boisvertmathieu.aichatbot.service.KnowledgeIndexingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {
    
    private final ConversationRepository conversationRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeIndexingService knowledgeIndexingService;
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Statistiques des conversations
            Long totalConversations = conversationRepository.count();
            Long positiveFeedback = conversationRepository.countPositiveFeedback();
            Long negativeFeedback = conversationRepository.countNegativeFeedback();
            
            // Statistiques des documents
            Long totalDocuments = knowledgeDocumentRepository.count();
            Long indexedDocuments = knowledgeDocumentRepository.countIndexedDocuments();
            
            stats.put("conversations", Map.of(
                "total", totalConversations,
                "positiveFeedback", positiveFeedback,
                "negativeFeedback", negativeFeedback,
                "satisfactionRate", calculateSatisfactionRate(positiveFeedback, negativeFeedback)
            ));
            
            stats.put("knowledgeBase", Map.of(
                "totalDocuments", totalDocuments,
                "indexedDocuments", indexedDocuments,
                "indexingProgress", calculateIndexingProgress(indexedDocuments, totalDocuments)
            ));
            
            stats.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Erreur lors de la récupération des statistiques: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Erreur lors de la récupération des statistiques"));
        }
    }
    
    @PostMapping("/knowledge")
    public ResponseEntity<Map<String, Object>> addKnowledgeDocument(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(defaultValue = "manual") String source,
            @RequestParam(defaultValue = "manual") String tags) {
        
        try {
            knowledgeIndexingService.addKnowledgeDocument(title, content, source, tags);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document ajouté avec succès à la base de connaissances");
            response.put("title", title);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du document: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de l'ajout du document: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @PostMapping("/index/trigger")
    public ResponseEntity<Map<String, Object>> triggerIndexing() {
        try {
            // Déclencher manuellement l'indexation
            knowledgeIndexingService.indexCorrectedResponses();
            knowledgeIndexingService.syncDocumentsToSearch();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Indexation déclenchée avec succès");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Erreur lors du déclenchement de l'indexation: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Erreur lors de l'indexation: " + e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // Vérifier la connectivité à la base de données
            Long conversationCount = conversationRepository.count();
            Long documentCount = knowledgeDocumentRepository.count();
            
            health.put("status", "UP");
            health.put("database", "Connected");
            health.put("conversationTable", conversationCount != null ? "OK" : "ERROR");
            health.put("knowledgeTable", documentCount != null ? "OK" : "ERROR");
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(health);
            
        } catch (Exception e) {
            log.error("Erreur lors du health check: {}", e.getMessage(), e);
            
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(health);
        }
    }
    
    private double calculateSatisfactionRate(Long positive, Long negative) {
        if (positive == null) positive = 0L;
        if (negative == null) negative = 0L;
        
        long total = positive + negative;
        if (total == 0) return 0.0;
        
        return (double) positive / total * 100.0;
    }
    
    private double calculateIndexingProgress(Long indexed, Long total) {
        if (indexed == null) indexed = 0L;
        if (total == null) total = 0L;
        
        if (total == 0) return 100.0;
        
        return (double) indexed / total * 100.0;
    }
} 