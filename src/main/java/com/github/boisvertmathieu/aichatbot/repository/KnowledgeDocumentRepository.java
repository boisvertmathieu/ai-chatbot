package com.github.boisvertmathieu.aichatbot.repository;

import com.github.boisvertmathieu.aichatbot.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    
    Optional<KnowledgeDocument> findByDocumentId(String documentId);
    
    List<KnowledgeDocument> findByIndexedInSearchFalse();
    
    List<KnowledgeDocument> findByTagsContaining(String tag);
    
    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.embeddingHash IS NULL OR kd.indexedInSearch = false")
    List<KnowledgeDocument> findDocumentsNeedingIndexing();
    
    @Query("SELECT COUNT(kd) FROM KnowledgeDocument kd WHERE kd.indexedInSearch = true")
    Long countIndexedDocuments();
} 