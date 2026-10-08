package com.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
import java.util.Optional;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);
    private static final int MAX_RAG_CONTEXT_CHARS = 3000;
    private static final int MAX_USER_INPUT_CHARS = 1000;

    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    private final ConversationRepository conversationRepository;
    private final com.repository.MessageRepository messageRepository;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            VectorStore vectorStore,
            ConversationRepository conversationRepository,
            com.repository.MessageRepository messageRepository) {
        this.chatClient = chatClientBuilder.build();
        this.vectorStore = vectorStore;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    public VectorStore getVectorStore() {
        return this.vectorStore;
    }

    @Transactional
    public String generateResponse(String message, String conversationId) {
        return generateResponse(message, conversationId, null, null);
    }

    @Transactional
    public String generateResponse(String message, String conversationId, Long userId, String userEmail) {
        // Safe input truncation to prevent prompt bloating
        if (message != null && message.length() > MAX_USER_INPUT_CHARS) {
            message = message.substring(0, MAX_USER_INPUT_CHARS);
        }

        // 1. Find existing conversation or create new
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = java.util.UUID.randomUUID().toString();
        }
        final String activeId = conversationId;
        
        Optional<Conversation> existingOpt = conversationRepository.findByConversationId(activeId);
        Conversation conversation;
        if (existingOpt.isPresent()) {
            conversation = existingOpt.get();
            // User isolation check: if conversation belongs to another user, prevent access
            if (conversation.getUserId() != null && userId != null && !conversation.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Conversation belongs to another user.");
            }
            if (conversation.getUserId() == null && userId != null) {
                conversation.setUserId(userId);
                conversation.setUserEmail(userEmail);
            }
        } else {
            conversation = new Conversation(activeId, userId, userEmail);
            conversation = conversationRepository.save(conversation);
        }

        // 2. Save user message to MySQL directly to avoid unbounded collection loading
        Message userMessage = new Message(Role.USER, message);
        userMessage.setConversation(conversation);
        messageRepository.save(userMessage);

        // 3. Retrieve relevant DeadlineIQ document chunks via Pinecone / VectorStore similarity search (topK bounded)
        List<Document> retrievedDocs = List.of();
        try {
            logger.info("Executing vector similarity search in {} for query: '{}'",
                    vectorStore.getClass().getSimpleName(), message);
            retrievedDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(3)
                            .build()
            );
            logger.info("Retrieved {} relevant document chunk(s) from VectorStore.",
                    retrievedDocs != null ? retrievedDocs.size() : 0);
        } catch (Exception e) {
            logger.warn("Vector similarity search encountered an issue: {}. Proceeding with conversational context.", e.getMessage());
        }

        // 4. Construct strictly bounded RAG Context (max 3000 chars)
        StringBuilder ragContext = new StringBuilder();
        if (retrievedDocs != null && !retrievedDocs.isEmpty()) {
            for (Document doc : retrievedDocs) {
                String text = doc.getText();
                if (text != null && !text.isBlank()) {
                    if (ragContext.length() + text.length() > MAX_RAG_CONTEXT_CHARS) {
                        int remaining = Math.max(0, MAX_RAG_CONTEXT_CHARS - ragContext.length());
                        if (remaining > 50) {
                            ragContext.append(text.substring(0, remaining).trim()).append("...\n\n---\n\n");
                        }
                        break;
                    } else {
                        ragContext.append(text.trim()).append("\n\n---\n\n");
                    }
                }
            }
        }

        // 5. Retrieve only the bounded recent history (last 15 messages) from MySQL directly
        List<Message> recentMessages = messageRepository.findTop15ByConversationOrderByIdDesc(conversation);
        java.util.Collections.reverse(recentMessages);
        StringBuilder historyContext = new StringBuilder();
        for (Message msg : recentMessages) {
            if (msg.getId() != null && msg.getId().equals(userMessage.getId())) {
                continue;
            }
            historyContext.append(msg.getRole())
                    .append(": ")
                    .append(msg.getContent())
                    .append("\n");
        }

        // 6. Build RAG-grounded prompt for Gemini
        String prompt;
        if (ragContext.length() > 0) {
            prompt = """
            You are DeadlineIQ AI Copilot, the university's academic deadline, exam schedule, and career drive advisor.
            Use the following verified knowledge base context retrieved from the vector store to answer the user's question accurately.
            Prioritize the specific deadlines, dates, course codes, requirements, and recommendations found in the context.
            If the context does not contain the answer, answer helpfully and clearly using your general academic knowledge.

            ==================================================
            VERIFIED DEADLINEIQ KNOWLEDGE CONTEXT (RAG):
            ==================================================
            %s

            ==================================================
            PREVIOUS CONVERSATION HISTORY:
            ==================================================
            %s

            ==================================================
            STUDENT QUESTION:
            ==================================================
            %s
            """.formatted(ragContext.toString(), historyContext.toString(), message);
        } else {
            prompt = """
            You are DeadlineIQ AI Copilot, the university's academic deadline, exam schedule, and career drive advisor.
            Assist the student or faculty member with academic planning, course timelines, and preparation tips.

            ==================================================
            PREVIOUS CONVERSATION HISTORY:
            ==================================================
            %s

            ==================================================
            STUDENT QUESTION:
            ==================================================
            %s
            """.formatted(historyContext.toString(), message);
        }

        // 7. Send context + question to Gemini
        try {
            String response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            if (response == null || response.isBlank()) {
                response = "I have noted your request. Your deadline records are tracked and on schedule!";
            }

            // 8. Save AI response to MySQL directly without triggering lazy collection loading
            Message assistantMessage = new Message(Role.ASSISTANT, response);
            assistantMessage.setConversation(conversation);
            messageRepository.save(assistantMessage);
            conversation.setUpdatedAt(java.time.LocalDateTime.now());
            conversationRepository.save(conversation);
            return response;
        } catch (Exception e) {
            logger.error("Gemini model request failed: {}", e.getMessage(), e);
            String fallback;
            if (e.getMessage() != null && (e.getMessage().contains("API key") || e.getMessage().contains("UNAUTHENTICATED"))) {
                fallback = "DeadlineIQ AI Copilot: Gemini API key is unauthenticated or invalid. Please check your GEMINI_API_KEY in backend/demo/.env.";
            } else {
                fallback = "DeadlineIQ AI Assistant: I received your question: \"" + message + "\". Please ensure your upcoming academic milestones and project submissions are verified in the dashboard!";
            }
            Message assistantMessage = new Message(Role.ASSISTANT, fallback);
            assistantMessage.setConversation(conversation);
            messageRepository.save(assistantMessage);
            conversation.setUpdatedAt(java.time.LocalDateTime.now());
            conversationRepository.save(conversation);
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(Long userId) {
        if (userId == null) return List.of();
        return conversationRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(String conversationId) {
        return getConversationMessages(conversationId, null);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(String conversationId, Long userId) {
        if (conversationId == null || conversationId.isBlank()) {
            return List.of();
        }
        Optional<Conversation> convOpt = conversationRepository.findByConversationId(conversationId);
        if (convOpt.isEmpty()) return List.of();
        Conversation conv = convOpt.get();
        if (conv.getUserId() != null && userId != null && !conv.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: Conversation belongs to another user.");
        }
        List<Message> list = messageRepository.findTop15ByConversationOrderByIdDesc(conv);
        java.util.Collections.reverse(list);
        return list;
    }

    @Transactional
    public void clearConversation(String conversationId) {
        clearConversation(conversationId, null);
    }

    @Transactional
    public void clearConversation(String conversationId, Long userId) {
        if (conversationId != null && !conversationId.isBlank()) {
            Optional<Conversation> convOpt = conversationRepository.findByConversationId(conversationId);
            if (convOpt.isPresent()) {
                Conversation conv = convOpt.get();
                if (conv.getUserId() != null && userId != null && !conv.getUserId().equals(userId)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
                }
                conversationRepository.delete(conv);
            }
        }
    }
}
