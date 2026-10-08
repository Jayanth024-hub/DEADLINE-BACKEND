package com.controller;

import com.dto.ChatRequest;
import com.service.PdfService;
import com.service.RagChatService;
import com.service.TextChunkService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final PdfService pdfService;
    private final TextChunkService textChunkService;
    private final VectorStore vectorStore;
    private final RagChatService ragChatService;
    private final java.util.concurrent.atomic.AtomicBoolean academicDocsIngested = new java.util.concurrent.atomic.AtomicBoolean(false);

    public RagController(
            PdfService pdfService,
            TextChunkService textChunkService,
            VectorStore vectorStore,
            RagChatService ragChatService) {
        this.pdfService = pdfService;
        this.textChunkService = textChunkService;
        this.vectorStore = vectorStore;
        this.ragChatService = ragChatService;
    }

    // =========================================================
    // PDF UPLOAD + RAG INGESTION
    // =========================================================
    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(
            @RequestParam("file") MultipartFile file) {
        // 1. Check file presence
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please select a PDF file.");
        }
        // 2. Bound file size (max 10MB) to prevent heap exhaustion
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body("File size exceeds 10MB limit. Please upload a smaller document.");
        }
        // 3. Check MIME type
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only PDF files are allowed.");
        }

        try {
            // 4. Extract PDF text safely with streaming and page bounds
            String text = pdfService.extractText(file);
            System.out.println("Extracted characters: " + text.length());

            // 5. Split text into bounded chunks
            List<Document> chunks = textChunkService.splitText(text);
            System.out.println("Total chunks: " + chunks.size());

            // 6. Generate embeddings + store in VectorStore in batches of 20 to bound heap RAM
            int batchSize = 20;
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<Document> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
                vectorStore.add(batch);
            }

            // 7. Enable RAG only after successful storage
            ragChatService.enableRag();
            System.out.println("Successfully stored " + chunks.size() + " chunks in VectorStore.");

            // 8. Response
            return ResponseEntity.ok(
                    "PDF processed successfully. Extracted characters: "
                            + text.length()
                            + ", Total chunks: "
                            + chunks.size()
                            + ", Stored in VectorStore (" + vectorStore.getClass().getSimpleName() + ") successfully."
            );
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to read PDF.");
        } catch (Exception e) {
            System.out.println("========== VECTOR STORE ERROR ==========");
            e.printStackTrace();
            System.out.println("Message: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("Cause: " + e.getCause().getMessage());
                e.getCause().printStackTrace();
            }
            return ResponseEntity.internalServerError()
                    .body("PDF processed, but failed to store embeddings in VectorStore: " + e.getMessage());
        }
    }

    // =========================================================
    // RAG CHAT
    // =========================================================
    @PostMapping("/chat")
    public ResponseEntity<String> chat(
            @RequestBody ChatRequest request) {
        // 1. Validate message
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Message cannot be empty.");
        }

        try {
            // Gemini directly OR Pinecone RAG -> Gemini (with MySQL memory)
            String answer = ragChatService.askQuestion(request.getMessage(), request.getConversationId());
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("Failed to process your message: " + e.getMessage());
        }
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chatGet(
            @RequestParam("message") String message,
            @RequestParam(value = "conversationId", required = false) String conversationId) {
        String answer = ragChatService.askQuestion(message, conversationId);
        return ResponseEntity.ok(answer);
    }

    @PostMapping("/ingest-academic-docs")
    public ResponseEntity<?> ingestAcademicDocs() {
        if (academicDocsIngested.get()) {
            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Academic handbook is already ingested in VectorStore. Duplicate ingestion skipped to preserve memory.",
                    "duplicateSkipped", true,
                    "vectorStoreType", vectorStore.getClass().getSimpleName()
            ));
        }

        try {
            org.springframework.core.io.ClassPathResource resource = 
                    new org.springframework.core.io.ClassPathResource("knowledge/deadlineiq-academic-handbook.txt");
            if (!resource.exists()) {
                return ResponseEntity.badRequest().body(java.util.Map.of(
                        "success", false,
                        "error", "Academic handbook resource file not found."
                ));
            }

            String content = new String(resource.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            List<Document> chunks = textChunkService.splitText(content);
            
            // Ingest in controlled batches of 20
            int batchSize = 20;
            for (int i = 0; i < chunks.size(); i += batchSize) {
                List<Document> batch = chunks.subList(i, Math.min(i + batchSize, chunks.size()));
                vectorStore.add(batch);
            }

            academicDocsIngested.set(true);
            ragChatService.enableRag();

            return ResponseEntity.ok(java.util.Map.of(
                    "success", true,
                    "message", "Successfully ingested DeadlineIQ Academic Handbook into VectorStore.",
                    "totalChunks", chunks.size(),
                    "vectorStoreType", vectorStore.getClass().getSimpleName()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(java.util.Map.of(
                    "success", false,
                    "error", "Ingestion failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Object>> status() {
        boolean isPinecone = vectorStore.getClass().getSimpleName().contains("Pinecone");
        return ResponseEntity.ok(java.util.Map.of(
                "ragEnabled", ragChatService.isRagEnabled(),
                "vectorStoreType", vectorStore.getClass().getSimpleName(),
                "isPinecone", isPinecone,
                "status", "online"
        ));
    }
}
