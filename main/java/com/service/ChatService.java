package com.service;

import org.springframework.ai.chat.client.ChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.entity.Conversation;
import com.entity.Message;
import com.entity.Role;
import com.repository.ConversationRepository;
import java.util.List;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            ConversationRepository conversationRepository) {
        this.chatClient = chatClientBuilder.build();
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public String generateResponse(
            String message,
            String conversationId) {
        // 1. Find existing conversation or create new
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        final String activeId = conversationId;
        Conversation conversation = conversationRepository
                .findByConversationId(activeId)
                .orElseGet(() -> conversationRepository.save(new Conversation(activeId)));

        // 2. Save user message
        Message userMessage = new Message(Role.USER, message);
        conversation.addMessage(userMessage);
        conversationRepository.save(conversation);

        // 3. Get previous conversation messages
        List<Message> messages = conversation.getMessages();

        // 4. Build context
        StringBuilder context = new StringBuilder();
        for (Message msg : messages) {
            if (msg == userMessage) {
                continue;
            }
            context.append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        // 5. Send context + current question to Gemini
        String prompt = """
        You are a helpful AI assistant.
        Here is the conversation history:
        %s
        Continue the conversation naturally.
        Current user message:
        %s
        """.formatted(context, message);

        try {
            String response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (response == null || response.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Gemini returned an empty response."
                );
            }

            // 6. Save AI response only after Gemini succeeds
            Message assistantMessage = new Message(Role.ASSISTANT, response);
            conversation.addMessage(assistantMessage);
            conversationRepository.save(conversation);
            return response;
        } catch (RuntimeException e) {
            if (e instanceof ResponseStatusException statusException) {
                throw statusException;
            }
            logger.error("Gemini chat request failed.", e);
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to get a response from Gemini."
            );
        }
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        return conversationRepository.findByConversationId(conversationId)
                .map(Conversation::getMessages)
                .orElse(List.of());
    }

    @Transactional
    public void clearConversation(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            conversationRepository.findByConversationId(conversationId)
                    .ifPresent(conversationRepository::delete);
        }
    }
}
