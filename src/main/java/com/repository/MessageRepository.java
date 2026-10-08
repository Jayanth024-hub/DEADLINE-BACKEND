package com.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.entity.Conversation;
import com.entity.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findTop15ByConversationOrderByIdDesc(Conversation conversation);
}
