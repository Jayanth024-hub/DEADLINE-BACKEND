package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.entity.Conversation;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationId(String conversationId);
    List<Conversation> findByUserIdOrderByUpdatedAtDesc(Long userId);
    List<Conversation> findByUserEmailOrderByUpdatedAtDesc(String userEmail);
    Optional<Conversation> findByConversationIdAndUserId(String conversationId, Long userId);
}
