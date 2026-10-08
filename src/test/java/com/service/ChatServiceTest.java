package com.service;

import com.entity.Conversation;
import com.entity.Message;
import com.repository.ConversationRepository;
import com.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Test
    void reusesStoredConversationContextOnSubsequentMessage() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("Nice to meet you, Ramswaroop.", "Your name is Ramswaroop.");
        clearInvocations(chatClient.prompt());

        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(Collections.emptyList());

        Map<String, Conversation> storedConversations = new HashMap<>();
        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        when(conversationRepository.findByConversationId(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(
                        storedConversations.get(invocation.getArgument(0))
                ));
        when(conversationRepository.save(any(Conversation.class)))
                .thenAnswer(invocation -> {
                    Conversation conversation = invocation.getArgument(0);
                    storedConversations.put(conversation.getConversationId(), conversation);
                    return conversation;
                });

        List<Message> storedMessages = new ArrayList<>();
        AtomicLong idSequence = new AtomicLong(1);
        MessageRepository messageRepository = mock(MessageRepository.class);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            try {
                Field idField = Message.class.getDeclaredField("id");
                idField.setAccessible(true);
                if (idField.get(msg) == null) {
                    idField.set(msg, idSequence.getAndIncrement());
                }
            } catch (Exception ignored) {
            }
            storedMessages.add(msg);
            return msg;
        });
        when(messageRepository.findTop15ByConversationOrderByIdDesc(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conv = invocation.getArgument(0);
            List<Message> matching = storedMessages.stream()
                    .filter(m -> m.getConversation() != null && conv.getConversationId().equals(m.getConversation().getConversationId()))
                    .sorted((m1, m2) -> Long.compare(m2.getId(), m1.getId()))
                    .limit(15)
                    .collect(Collectors.toList());
            return new ArrayList<>(matching);
        });

        ChatService chatService = new ChatService(chatClientBuilder, vectorStore, conversationRepository, messageRepository);

        assertEquals(
                "Nice to meet you, Ramswaroop.",
                chatService.generateResponse("My name is Ramswaroop", "test-conversation")
        );
        assertEquals(
                "Your name is Ramswaroop.",
                chatService.generateResponse("What is my name?", "test-conversation")
        );

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient.prompt(), times(2)).user(promptCaptor.capture());
        String secondPrompt = promptCaptor.getAllValues().get(1);
        assertAll(
                () -> assertTrue(
                        secondPrompt.contains("USER: My name is Ramswaroop"),
                        "Expected second prompt to contain previous USER message"
                ),
                () -> assertTrue(
                        secondPrompt.contains("ASSISTANT: Nice to meet you, Ramswaroop."),
                        "Expected second prompt to contain previous ASSISTANT message"
                ),
                () -> assertTrue(
                        secondPrompt.contains("STUDENT QUESTION:") && secondPrompt.contains("What is my name?"),
                        "Expected second prompt to contain STUDENT QUESTION"
                )
        );
    }

    @Test
    void includesRagContextWhenVectorStoreReturnsDocuments() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient.Builder chatClientBuilder = mock(ChatClient.Builder.class);
        when(chatClientBuilder.build()).thenReturn(chatClient);
        when(chatClient.prompt().user(anyString()).call().content())
                .thenReturn("The deadline for CSE-401 is November 15.");
        clearInvocations(chatClient.prompt());

        VectorStore vectorStore = mock(VectorStore.class);
        Document doc = new Document("The deadline for CSE-401 is November 15.");
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));

        ConversationRepository conversationRepository = mock(ConversationRepository.class);
        when(conversationRepository.findByConversationId(anyString())).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageRepository messageRepository = mock(MessageRepository.class);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findTop15ByConversationOrderByIdDesc(any(Conversation.class))).thenReturn(Collections.emptyList());

        ChatService chatService = new ChatService(chatClientBuilder, vectorStore, conversationRepository, messageRepository);

        String answer = chatService.generateResponse("When is CSE-401 deadline?", "rag-conversation");
        assertEquals("The deadline for CSE-401 is November 15.", answer);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient.prompt()).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();

        assertAll(
                () -> assertTrue(prompt.contains("VERIFIED DEADLINEIQ KNOWLEDGE CONTEXT (RAG):")),
                () -> assertTrue(prompt.contains("The deadline for CSE-401 is November 15.")),
                () -> assertTrue(prompt.contains("When is CSE-401 deadline?"))
        );
    }
}
