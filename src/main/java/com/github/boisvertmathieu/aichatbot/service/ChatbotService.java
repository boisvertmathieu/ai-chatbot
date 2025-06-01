package com.github.boisvertmathieu.aichatbot.service;

import com.github.boisvertmathieu.aichatbot.dto.ChatRequest;
import com.github.boisvertmathieu.aichatbot.dto.ChatResponse;
import com.github.boisvertmathieu.aichatbot.dto.FeedbackRequest;
import com.github.boisvertmathieu.aichatbot.entity.Conversation;
import com.github.boisvertmathieu.aichatbot.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {
    
    private final AzureOpenAiChatModel chatModel;
    private final AzureOpenAiEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;
    private final TeamsNotificationService teamsNotificationService;
    
    @Value("${chatbot.rag.max-results:5}")
    private int maxResults;
    
    @Value("${chatbot.rag.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Value("${chatbot.system-message}")
    private String systemMessage;
    
    @Transactional
    public ChatResponse processQuestion(ChatRequest request) {
        try {
            log.info("Traitement de la question pour conversationId: {}, userId: {}", 
                     request.getConversationId(), request.getUserId());
            
            // 1. Recherche de documents pertinents avec RAG
            List<Document> relevantDocuments = retrieveRelevantDocuments(request.getText());
            
            // 2. Construction du prompt avec contexte
            String contextualPrompt = buildContextualPrompt(request.getText(), relevantDocuments);
            
            // 3. Génération de la réponse avec Azure OpenAI
            org.springframework.ai.chat.model.ChatResponse aiResponse = generateResponse(contextualPrompt);
            String responseText = aiResponse.getResult().getOutput().getContent();
            
            // 4. Extraction des métadonnées
            List<String> documentIds = relevantDocuments.stream()
                .map(doc -> doc.getMetadata().get("id").toString())
                .collect(Collectors.toList());
            
            Integer tokensUsed = extractTokenUsage(aiResponse);
            
            // 5. Sauvegarde de la conversation
            Conversation conversation = saveConversation(request, responseText, documentIds, tokensUsed);
            
            // 6. Notification Teams (canal de test par défaut)
            teamsNotificationService.sendResponse(request.getConversationId(), responseText);
            
            return ChatResponse.builder()
                .conversationId(request.getConversationId())
                .response(responseText)
                .retrievedDocumentIds(documentIds)
                .tokensUsed(tokensUsed)
                .timestamp(LocalDateTime.now())
                .success(true)
                .build();
                
        } catch (Exception e) {
            log.error("Erreur lors du traitement de la question: {}", e.getMessage(), e);
            return ChatResponse.builder()
                .conversationId(request.getConversationId())
                .success(false)
                .errorMessage("Erreur lors du traitement de votre question: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        }
    }
    
    private List<Document> retrieveRelevantDocuments(String question) {
        log.debug("Recherche de documents pertinents pour la question: {}", question);
        
        SearchRequest searchRequest = SearchRequest.query(question)
            .withTopK(maxResults)
            .withSimilarityThreshold(similarityThreshold);
            
        return vectorStore.similaritySearch(searchRequest);
    }
    
    private String buildContextualPrompt(String question, List<Document> documents) {
        StringBuilder context = new StringBuilder();
        
        if (!documents.isEmpty()) {
            context.append("Contexte pertinent:\n");
            for (int i = 0; i < documents.size(); i++) {
                context.append(String.format("[Doc %d] %s\n", i + 1, documents.get(i).getContent()));
            }
            context.append("\n");
        }
        
        context.append("Question: ").append(question);
        
        return context.toString();
    }
    
    private org.springframework.ai.chat.model.ChatResponse generateResponse(String contextualPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemMessage),
            new UserMessage(contextualPrompt)
        );
        
        Prompt prompt = new Prompt(messages);
        return chatModel.call(prompt);
    }
    
    private Integer extractTokenUsage(org.springframework.ai.chat.model.ChatResponse response) {
        try {
            // Tentative d'extraction des tokens utilisés depuis les métadonnées
            if (response.getMetadata() != null && response.getMetadata().get("usage") != null) {
                return (Integer) response.getMetadata().get("usage");
            }
        } catch (Exception e) {
            log.warn("Impossible d'extraire l'usage des tokens: {}", e.getMessage());
        }
        return null;
    }
    
    private Conversation saveConversation(ChatRequest request, String response, 
                                          List<String> documentIds, Integer tokensUsed) {
        Conversation conversation = Conversation.builder()
            .conversationId(request.getConversationId())
            .userId(request.getUserId())
            .question(request.getText())
            .response(response)
            .retrievedDocumentIds(String.join(",", documentIds))
            .tokensUsed(tokensUsed)
            .timestamp(LocalDateTime.now())
            .build();
            
        return conversationRepository.save(conversation);
    }
    
    @Transactional
    public void processFeedback(FeedbackRequest feedbackRequest) {
        log.info("Traitement du feedback pour conversationId: {}", feedbackRequest.getConversationId());
        
        conversationRepository.findByConversationId(feedbackRequest.getConversationId())
            .ifPresentOrElse(
                conversation -> {
                    conversation.setFeedbackUseful(feedbackRequest.getUseful());
                    conversation.setCorrectedResponse(feedbackRequest.getCorrectedResponse());
                    conversation.setFeedbackTimestamp(LocalDateTime.now());
                    conversationRepository.save(conversation);
                    
                    log.info("Feedback enregistré pour conversationId: {}", 
                             feedbackRequest.getConversationId());
                },
                () -> {
                    log.warn("Conversation non trouvée pour conversationId: {}", 
                             feedbackRequest.getConversationId());
                    throw new IllegalArgumentException("Conversation non trouvée");
                }
            );
    }
} 