package com.service;

import com.entity.Conversation;
import com.repository.ConversationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceTest {

    @Test
    void reusesStoredConversationContextOnSubsequentMessage() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Nice to meet you, Ramswaroop.", "Your name is Ramswaroop.");
        clearInvocations(chatClient.prompt());

        Map<String, Conversation> storedConversations = new HashMap<>();
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        when(conversationRepository.findByConversationId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        storedConversations.get(invocation.getArgument(0))
                ));
        when(conversationRepository.save(org.mockito.ArgumentMatchers.any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);
                    storedConversations.put(conversation.getConversationId(), conversation);
                    return conversation;
                });

        ChatService chatService = new ChatService(chatClientBuilder, conversationRepository);

        assertEquals(
                "Nice to meet you, Ramswaroop.",
                chatService.generateResponse("My name is Ramswaroop", "test-conversation")
        );
        assertEquals(
                "Your name is Ramswaroop.",
                chatService.generateResponse("What is my name?", "test-conversation")
        );

        var promptCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(chatClient.prompt(), times(2)).user(promptCaptor.capture());
        String secondPrompt = promptCaptor.getAllValues().get(1);
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertTrue(
                        secondPrompt.contains("USER: My name is Ramswaroop")
                ),
                () -> org.junit.jupiter.api.Assertions.assertTrue(
                        secondPrompt.contains("ASSISTANT: Nice to meet you, Ramswaroop.")
                ),
                () -> org.junit.jupiter.api.Assertions.assertTrue(
                        secondPrompt.contains("Current user message:\nWhat is my name?")
                )
        );
    }
}
