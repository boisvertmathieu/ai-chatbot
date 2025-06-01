package com.github.boisvertmathieu.aichatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    
    private String conversationId;
    private String response;
    private List<String> retrievedDocumentIds;
    private Integer tokensUsed;
    private LocalDateTime timestamp;
    private boolean success;
    private String errorMessage;
} 