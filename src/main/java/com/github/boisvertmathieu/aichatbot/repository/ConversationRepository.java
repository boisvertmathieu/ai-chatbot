package com.github.boisvertmathieu.aichatbot.repository;

import com.github.boisvertmathieu.aichatbot.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    Optional<Conversation> findByConversationId(String conversationId);
    
    List<Conversation> findByUserId(String userId);
    
    @Query("SELECT c FROM Conversation c WHERE c.feedbackUseful = true AND c.correctedResponse IS NOT NULL")
    List<Conversation> findConversationsWithCorrectFeedback();
    
    @Query("SELECT c FROM Conversation c WHERE c.timestamp BETWEEN :startDate AND :endDate")
    List<Conversation> findConversationsByDateRange(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.feedbackUseful = true")
    Long countPositiveFeedback();
    
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.feedbackUseful = false")
    Long countNegativeFeedback();
} 