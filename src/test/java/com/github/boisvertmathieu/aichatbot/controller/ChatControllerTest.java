package com.github.boisvertmathieu.aichatbot.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.boisvertmathieu.aichatbot.dto.ChatRequest;
import com.github.boisvertmathieu.aichatbot.dto.FeedbackRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureTestMockMvc
@ActiveProfiles("test")
class ChatControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    void healthEndpointShouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("AI Chatbot"));
    }
    
    @Test
    void chatEndpointShouldAcceptValidRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .conversationId("test-conv-123")
                .userId("test-user-456")
                .text("Comment configurer Spring Boot ?")
                .build();
        
        // Note: Ce test échouera sans les vraies clés Azure configurées
        // En environnement de test, on s'attend à une réponse d'erreur appropriée
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("test-conv-123"));
    }
    
    @Test
    void chatEndpointShouldRejectInvalidRequest() throws Exception {
        ChatRequest request = ChatRequest.builder()
                .conversationId("") // conversationId vide
                .userId("test-user")
                .text("Test question")
                .build();
        
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
    
    @Test
    void feedbackEndpointShouldAcceptValidRequest() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .conversationId("test-conv-123")
                .useful(true)
                .correctedResponse("Réponse corrigée")
                .build();
        
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").exists());
    }
    
    @Test
    void feedbackEndpointShouldRejectInvalidRequest() throws Exception {
        FeedbackRequest request = FeedbackRequest.builder()
                .conversationId("") // conversationId vide
                .useful(null) // useful null
                .build();
        
        mockMvc.perform(post("/api/feedback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
} 