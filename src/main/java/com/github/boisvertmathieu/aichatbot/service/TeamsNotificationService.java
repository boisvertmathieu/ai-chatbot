package com.github.boisvertmathieu.aichatbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TeamsNotificationService {
    
    private final RestTemplate restTemplate;
    
    @Value("${teams.webhook.test-channel}")
    private String testChannelWebhook;
    
    @Value("${teams.webhook.production-channel}")
    private String productionChannelWebhook;
    
    @Value("${teams.mode:test}")
    private String teamsMode;
    
    public TeamsNotificationService() {
        this.restTemplate = new RestTemplate();
    }
    
    public void sendResponse(String conversationId, String response) {
        try {
            String webhook = "test".equals(teamsMode) ? testChannelWebhook : productionChannelWebhook;
            String channelType = "test".equals(teamsMode) ? "Test" : "Production";
            
            Map<String, Object> messageCard = createAdaptiveCard(conversationId, response, channelType);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(messageCard, headers);
            
            ResponseEntity<String> result = restTemplate.postForEntity(webhook, entity, String.class);
            
            if (result.getStatusCode().is2xxSuccessful()) {
                log.info("Message envoyé avec succès vers Teams ({}) pour conversationId: {}", 
                         channelType, conversationId);
            } else {
                log.error("Erreur lors de l'envoi vers Teams: {}", result.getStatusCode());
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification Teams: {}", e.getMessage(), e);
        }
    }
    
    private Map<String, Object> createAdaptiveCard(String conversationId, String response, String channelType) {
        Map<String, Object> card = new HashMap<>();
        card.put("@type", "MessageCard");
        card.put("@context", "http://schema.org/extensions");
        card.put("themeColor", "test".equals(teamsMode) ? "FF6D00" : "0078D4");
        card.put("summary", "Réponse du Chatbot AI");
        
        Map<String, Object> section = new HashMap<>();
        section.put("activityTitle", String.format("🤖 Chatbot AI - Canal %s", channelType));
        section.put("activitySubtitle", String.format("Conversation ID: %s", conversationId));
        section.put("text", response);
        section.put("markdown", true);
        
        card.put("sections", new Object[]{section});
        
        // Ajout d'actions pour le feedback si en mode test
        if ("test".equals(teamsMode)) {
            Map<String, Object> actions = new HashMap<>();
            actions.put("@type", "ActionCard");
            actions.put("name", "Feedback");
            
            Map<String, Object> input1 = new HashMap<>();
            input1.put("@type", "MultichoiceInput");
            input1.put("id", "feedback");
            input1.put("title", "Cette réponse était-elle utile ?");
            input1.put("isMultiSelect", false);
            
            Map<String, Object> choice1 = new HashMap<>();
            choice1.put("display", "👍 Utile");
            choice1.put("value", "useful");
            
            Map<String, Object> choice2 = new HashMap<>();
            choice2.put("display", "👎 Non utile");
            choice2.put("value", "not_useful");
            
            input1.put("choices", new Object[]{choice1, choice2});
            
            Map<String, Object> input2 = new HashMap<>();
            input2.put("@type", "TextInput");
            input2.put("id", "correction");
            input2.put("title", "Réponse corrigée (optionnel)");
            input2.put("isMultiline", true);
            
            actions.put("inputs", new Object[]{input1, input2});
            
            Map<String, Object> action = new HashMap<>();
            action.put("@type", "HttpPOST");
            action.put("name", "Envoyer Feedback");
            action.put("target", "{{webhook_base_url}}/api/feedback");
            action.put("body", "{ \"conversationId\": \"" + conversationId + "\", \"useful\": \"{{feedback.value}}\", \"correctedResponse\": \"{{correction.value}}\" }");
            
            actions.put("actions", new Object[]{action});
            card.put("potentialAction", new Object[]{actions});
        }
        
        return card;
    }
    
    public void sendErrorNotification(String conversationId, String errorMessage) {
        try {
            String webhook = testChannelWebhook; // Les erreurs vont toujours sur le canal de test
            
            Map<String, Object> messageCard = createErrorCard(conversationId, errorMessage);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(messageCard, headers);
            
            ResponseEntity<String> result = restTemplate.postForEntity(webhook, entity, String.class);
            
            if (result.getStatusCode().is2xxSuccessful()) {
                log.info("Notification d'erreur envoyée vers Teams pour conversationId: {}", conversationId);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi de la notification d'erreur Teams: {}", e.getMessage(), e);
        }
    }
    
    private Map<String, Object> createErrorCard(String conversationId, String errorMessage) {
        Map<String, Object> card = new HashMap<>();
        card.put("@type", "MessageCard");
        card.put("@context", "http://schema.org/extensions");
        card.put("themeColor", "FF0000");
        card.put("summary", "Erreur du Chatbot AI");
        
        Map<String, Object> section = new HashMap<>();
        section.put("activityTitle", "⚠️ Erreur Chatbot AI");
        section.put("activitySubtitle", String.format("Conversation ID: %s", conversationId));
        section.put("text", String.format("Une erreur s'est produite: %s", errorMessage));
        
        card.put("sections", new Object[]{section});
        
        return card;
    }
} 