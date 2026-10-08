package com.service;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkServiceTest {

    @Test
    void splitsTextIntoAppropriateChunks() {
        TextChunkService textChunkService = new TextChunkService();
        String text = """
                Spring Boot is an open-source Java-based framework used to create a microservice.
                It is developed by Pivotal Team and is used to build stand-alone and production ready spring applications.
                Spring AI provides abstractions for developing AI applications with models, vector stores, and embeddings.
                Retrieval-Augmented Generation (RAG) combines search with language models to produce accurate answers
                based on external documentation.
                """;

        List<Document> chunks = textChunkService.splitText(text);
        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.get(0).getText().contains("Spring Boot"));
    }
}
