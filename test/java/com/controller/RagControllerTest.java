package com.controller;

import com.dto.ChatRequest;
import com.service.PdfService;
import com.service.RagChatService;
import com.service.TextChunkService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagControllerTest {

    @Test
    void rejectsEmptyOrNonPdfFile() {
        PdfService pdfService = mock(PdfService.class);
        TextChunkService textChunkService = mock(TextChunkService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RagChatService ragChatService = mock(RagChatService.class);

        RagController controller = new RagController(pdfService, textChunkService, vectorStore, ragChatService);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);
        ResponseEntity<String> res1 = controller.uploadDocument(emptyFile);
        assertEquals(400, res1.getStatusCode().value());
        assertTrue(res1.getBody().contains("Please select a PDF file."));

        MockMultipartFile txtFile = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        ResponseEntity<String> res2 = controller.uploadDocument(txtFile);
        assertEquals(400, res2.getStatusCode().value());
        assertTrue(res2.getBody().contains("Only PDF files are allowed."));
    }

    @Test
    void processesPdfAndEnablesRag() throws Exception {
        PdfService pdfService = mock(PdfService.class);
        TextChunkService textChunkService = mock(TextChunkService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RagChatService ragChatService = mock(RagChatService.class);

        when(pdfService.extractText(any())).thenReturn("Spring Boot and Spring AI guide");
        when(textChunkService.splitText(anyString())).thenReturn(List.of(new Document("chunk 1"), new Document("chunk 2")));

        RagController controller = new RagController(pdfService, textChunkService, vectorStore, ragChatService);

        MockMultipartFile pdfFile = new MockMultipartFile("file", "guide.pdf", "application/pdf", "dummy pdf content".getBytes());
        ResponseEntity<String> res = controller.uploadDocument(pdfFile);

        assertEquals(200, res.getStatusCode().value());
        assertTrue(res.getBody().contains("Total chunks: 2"));
        verify(vectorStore).add(any());
        verify(ragChatService).enableRag();
    }

    @Test
    void chatsWithRagService() {
        PdfService pdfService = mock(PdfService.class);
        TextChunkService textChunkService = mock(TextChunkService.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RagChatService ragChatService = mock(RagChatService.class);

        when(ragChatService.askQuestion("What is Spring Boot?", "cid-123")).thenReturn("Spring Boot is a framework.");

        RagController controller = new RagController(pdfService, textChunkService, vectorStore, ragChatService);

        ChatRequest req = new ChatRequest("What is Spring Boot?", "cid-123");
        ResponseEntity<String> res = controller.chat(req);

        assertEquals(200, res.getStatusCode().value());
        assertEquals("Spring Boot is a framework.", res.getBody());
    }
}
