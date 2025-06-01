package com.github.boisvertmathieu.aichatbot.service;

import com.github.boisvertmathieu.aichatbot.entity.Conversation;
import com.github.boisvertmathieu.aichatbot.entity.KnowledgeDocument;
import com.github.boisvertmathieu.aichatbot.repository.ConversationRepository;
import com.github.boisvertmathieu.aichatbot.repository.KnowledgeDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIndexingService {
    
    private final ConversationRepository conversationRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AzureOpenAiEmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    
    /**
     * Tâche planifiée pour indexer les nouvelles réponses corrigées dans la base de connaissances
     * S'exécute tous les jours à 2h du matin
     */
    @Scheduled(cron = "0 0 2 * * *")
    @SchedulerLock(name = "indexCorrectedResponses", 
                   lockAtMostFor = "PT30M", 
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void indexCorrectedResponses() {
        log.info("Début de l'indexation des réponses corrigées");
        
        try {
            // Récupération des conversations avec des réponses corrigées
            List<Conversation> conversationsWithCorrections = conversationRepository.findConversationsWithCorrectFeedback();
            
            int indexedCount = 0;
            
            for (Conversation conversation : conversationsWithCorrections) {
                try {
                    // Vérifier si cette réponse corrigée n'est pas déjà indexée
                    String documentId = "corrected_" + conversation.getConversationId();
                    
                    if (knowledgeDocumentRepository.findByDocumentId(documentId).isEmpty()) {
                        indexCorrectedResponse(conversation);
                        indexedCount++;
                        
                        log.debug("Réponse corrigée indexée pour conversationId: {}", 
                                 conversation.getConversationId());
                    }
                    
                } catch (Exception e) {
                    log.error("Erreur lors de l'indexation de la conversation {}: {}", 
                             conversation.getConversationId(), e.getMessage(), e);
                }
            }
            
            log.info("Indexation terminée. {} nouvelles réponses corrigées indexées", indexedCount);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'indexation des réponses corrigées: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Tâche planifiée pour synchroniser les documents non indexés avec Azure AI Search
     * S'exécute toutes les heures
     */
    @Scheduled(fixedRate = 3600000) // 1 heure
    @SchedulerLock(name = "syncDocumentsToSearch", 
                   lockAtMostFor = "PT15M", 
                   lockAtLeastFor = "PT1M")
    @Transactional
    public void syncDocumentsToSearch() {
        log.debug("Début de la synchronisation des documents vers Azure AI Search");
        
        try {
            List<KnowledgeDocument> documentsToSync = knowledgeDocumentRepository.findDocumentsNeedingIndexing();
            
            int syncedCount = 0;
            
            for (KnowledgeDocument document : documentsToSync) {
                try {
                    syncDocumentToVectorStore(document);
                    
                    document.setIndexedInSearch(true);
                    document.setUpdatedTimestamp(LocalDateTime.now());
                    knowledgeDocumentRepository.save(document);
                    
                    syncedCount++;
                    
                } catch (Exception e) {
                    log.error("Erreur lors de la synchronisation du document {}: {}", 
                             document.getDocumentId(), e.getMessage(), e);
                }
            }
            
            if (syncedCount > 0) {
                log.info("Synchronisation terminée. {} documents synchronisés avec Azure AI Search", syncedCount);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la synchronisation des documents: {}", e.getMessage(), e);
        }
    }
    
    private void indexCorrectedResponse(Conversation conversation) throws Exception {
        // Créer un nouveau document de connaissance
        String documentId = "corrected_" + conversation.getConversationId();
        String content = String.format("Question: %s\nRéponse: %s", 
                                      conversation.getQuestion(), 
                                      conversation.getCorrectedResponse());
        
        String embeddingHash = generateEmbeddingHash(content);
        
        KnowledgeDocument document = KnowledgeDocument.builder()
            .documentId(documentId)
            .title("Réponse corrigée - " + conversation.getConversationId())
            .content(content)
            .source("feedback_correction")
            .tags("qa,correction,feedback")
            .embeddingHash(embeddingHash)
            .indexedInSearch(false)
            .createdTimestamp(LocalDateTime.now())
            .updatedTimestamp(LocalDateTime.now())
            .build();
            
        knowledgeDocumentRepository.save(document);
        
        // Synchroniser immédiatement avec le vector store
        syncDocumentToVectorStore(document);
        
        document.setIndexedInSearch(true);
        knowledgeDocumentRepository.save(document);
    }
    
    private void syncDocumentToVectorStore(KnowledgeDocument document) throws Exception {
        // Créer le document Spring AI
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("id", document.getDocumentId());
        metadata.put("title", document.getTitle());
        metadata.put("source", document.getSource());
        metadata.put("tags", document.getTags());
        metadata.put("created", document.getCreatedTimestamp().toString());
        
        Document springAiDocument = new Document(document.getContent(), metadata);
        
        // Ajouter au vector store (avec génération automatique d'embeddings)
        vectorStore.add(List.of(springAiDocument));
        
        log.debug("Document {} synchronisé avec Azure AI Search", document.getDocumentId());
    }
    
    private String generateEmbeddingHash(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes("UTF-8"));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (Exception e) {
            log.warn("Erreur lors de la génération du hash d'embedding: {}", e.getMessage());
            return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Méthode pour ajouter manuellement un document à la base de connaissances
     */
    @Transactional
    public void addKnowledgeDocument(String title, String content, String source, String tags) {
        try {
            String documentId = UUID.randomUUID().toString();
            String embeddingHash = generateEmbeddingHash(content);
            
            KnowledgeDocument document = KnowledgeDocument.builder()
                .documentId(documentId)
                .title(title)
                .content(content)
                .source(source)
                .tags(tags)
                .embeddingHash(embeddingHash)
                .indexedInSearch(false)
                .createdTimestamp(LocalDateTime.now())
                .updatedTimestamp(LocalDateTime.now())
                .build();
                
            knowledgeDocumentRepository.save(document);
            
            // Synchroniser immédiatement
            syncDocumentToVectorStore(document);
            
            document.setIndexedInSearch(true);
            knowledgeDocumentRepository.save(document);
            
            log.info("Document de connaissance ajouté et indexé: {}", title);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout du document de connaissance: {}", e.getMessage(), e);
            throw new RuntimeException("Impossible d'ajouter le document à la base de connaissances", e);
        }
    }
} 