package com.github.boisvertmathieu.aichatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    
    @NotBlank(message = "conversationId est requis")
    private String conversationId;
    
    @NotBlank(message = "userId est requis")
    private String userId;
    
    @NotBlank(message = "text est requis")
    private String text;
} 