package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.entity.Conversation;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByConversationId(String conversationId);
}
