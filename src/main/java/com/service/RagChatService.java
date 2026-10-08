package com.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.entity.Conversation;
import com.entity.Message;
import com.entity.Role;
import com.repository.ConversationRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final Optional<ConversationRepository> conversationRepository;

    // RAG similarity search is active by default across indexed documents
    private volatile boolean ragEnabled = true;

    public RagChatService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            @Autowired(required = false) ConversationRepository conversationRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.conversationRepository = Optional.ofNullable(conversationRepository);
    }

    public void enableRag() {
        this.ragEnabled = true;
    }

    public boolean isRagEnabled() {
        return this.ragEnabled;
    }

    public String askQuestion(String message) {
        return askQuestion(message, null);
    }

    @Transactional
    public String askQuestion(String message, String conversationId) {
        Conversation conversation = null;
        if (conversationId != null && !conversationId.isBlank() && conversationRepository.isPresent()) {
            final String activeId = conversationId;
            conversation = conversationRepository.get()
                    .findByConversationId(activeId)
                    .orElseGet(() -> conversationRepository.get().save(new Conversation(activeId)));
            Message userMessage = new Message(Role.USER, message);
            conversation.addMessage(userMessage);
            conversationRepository.get().save(conversation);
        }

        String answer;

        // ===================================================
        // NORMAL CHAT (No PDF uploaded)
        // ===================================================
        if (!ragEnabled) {
            try {
                answer = chatClient.prompt()
                        .user(message)
                        .call()
                        .content();
            } catch (Exception e) {
                answer = "AI assistant response unavailable: " + e.getMessage();
            }
        } else {
            // ===================================================
            // RAG CHAT (PDF has been uploaded)
            // ===================================================
            List<Document> documents;
            try {
                documents = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(message)
                                .topK(3)
                                .build()
                );
            } catch (Exception e) {
                documents = List.of();
            }

            // NO RELEVANT PDF INFORMATION
            if (documents == null || documents.isEmpty()) {
                try {
                    answer = chatClient.prompt()
                            .user("""
                            Answer the user's question normally.
                            There was no relevant information found in the uploaded document.
                            USER QUESTION:
                            %s
                            """.formatted(message))
                            .call()
                            .content();
                } catch (Exception e) {
                    answer = "AI assistant response unavailable: " + e.getMessage();
                }
            } else {
                // COMBINE RETRIEVED PDF CHUNKS
                String context = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n\n---\n\n"));

                // RAG PROMPT
                String prompt = """
                You are an AI assistant with access to an uploaded PDF.
                If the user's question is related to the uploaded PDF, answer using the document context.
                If the question is unrelated to the PDF, answer normally.
                Do not invent information from the document.
                DOCUMENT CONTEXT:
                %s
                USER QUESTION:
                %s
                """.formatted(context, message);

                // GEMINI FINAL ANSWER
                try {
                    answer = chatClient.prompt()
                            .user(prompt)
                            .call()
                            .content();
                } catch (Exception e) {
                    answer = "AI assistant response unavailable: " + e.getMessage();
                }
            }
        }

        if (conversation != null && answer != null && !answer.isBlank() && conversationRepository.isPresent()) {
            Message assistantMessage = new Message(Role.ASSISTANT, answer);
            conversation.addMessage(assistantMessage);
            conversationRepository.get().save(conversation);
        }

        return answer;
    }
}
