package com.github.boisvertmathieu.aichatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackRequest {
    
    @NotBlank(message = "conversationId est requis")
    private String conversationId;
    
    @NotNull(message = "useful est requis")
    private Boolean useful;
    
    private String correctedResponse;
} 