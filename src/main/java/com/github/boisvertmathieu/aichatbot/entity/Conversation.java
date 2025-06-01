package com.github.boisvertmathieu.aichatbot.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "conversation_id", nullable = false, unique = true)
    private String conversationId;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @Column(name = "question", nullable = false, columnDefinition = "TEXT")
    private String question;
    
    @Column(name = "response", nullable = false, columnDefinition = "TEXT")
    private String response;
    
    @Column(name = "retrieved_document_ids", columnDefinition = "TEXT")
    private String retrievedDocumentIds;
    
    @Column(name = "tokens_used")
    private Integer tokensUsed;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "feedback_useful")
    private Boolean feedbackUseful;
    
    @Column(name = "corrected_response", columnDefinition = "TEXT")
    private String correctedResponse;
    
    @Column(name = "feedback_timestamp")
    private LocalDateTime feedbackTimestamp;
    
    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
} 