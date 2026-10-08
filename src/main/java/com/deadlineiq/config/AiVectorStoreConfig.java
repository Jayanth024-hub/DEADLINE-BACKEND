package com.deadlineiq.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback VectorStore configuration for DeadlineIQ.
 * When PINECONE_API_KEY is configured, Spring AI's PineconeVectorStoreAutoConfiguration
 * automatically instantiates the PineconeVectorStore bean.
 * When PINECONE_API_KEY is missing or empty, this fallback provides an in-memory VectorStore.
 */
@Configuration
public class AiVectorStoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(AiVectorStoreConfig.class);

    @Bean
    @ConditionalOnProperty(name = "spring.ai.vectorstore.pinecone.api-key", havingValue = "", matchIfMissing = true)
    public VectorStore fallbackVectorStore(EmbeddingModel embeddingModel) {
        logger.info("Pinecone API key is not configured; initializing local in-memory fallback VectorStore.");
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}


