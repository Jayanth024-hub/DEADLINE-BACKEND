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
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000", "*"})
public class RagController {

    private final PdfService pdfService;
    private final TextChunkService textChunkService;
    private final VectorStore vectorStore;
    private final RagChatService ragChatService;

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
        // 1. Check file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body("Please select a PDF file.");
        }
        // 2. Check PDF
        if (!"application/pdf".equals(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only PDF files are allowed.");
        }

        try {
            // 3. Extract PDF text
            String text = pdfService.extractText(file);
            System.out.println("Extracted characters: " + text.length());

            // 4. Split text into chunks
            List<Document> chunks = textChunkService.splitText(text);
            System.out.println("Total chunks: " + chunks.size());

            // 5. Generate embeddings + store in VectorStore (Pinecone / Local)
            vectorStore.add(chunks);

            // 6. Enable RAG only after successful storage
            ragChatService.enableRag();
            System.out.println("Successfully stored " + chunks.size() + " chunks in VectorStore.");

            // 7. Response
            return ResponseEntity.ok(
                    "PDF processed successfully. Extracted characters: "
                            + text.length()
                            + ", Total chunks: "
                            + chunks.size()
                            + ", Stored in Pinecone successfully."
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

    @GetMapping("/status")
    public ResponseEntity<java.util.Map<String, Object>> status() {
        return ResponseEntity.ok(java.util.Map.of(
                "ragEnabled", ragChatService.isRagEnabled(),
                "status", "online"
        ));
    }
}
