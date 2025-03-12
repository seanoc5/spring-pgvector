package com.oconeco.spring_pgvector.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptionsBuilder
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.stereotype.Service

@Service
class EmbeddingService {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class)

    private final EmbeddingModel embeddingModel

    EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel
    }

    /**
     * Embeds a query string into a vector using the default model settings
     * @param query The text to embed
     * @return The resulting embedding vector
     */
    float[] embedQuery(String query) {
        logger.info("Embedding query: ${query}")
        var embeddings = embeddingModel.embed(query)
        logger.debug("Embedding(${embeddings.size()}): ${embeddings}")
        return embeddings
    }

    /**
     * Embeds a query string into a vector using custom embedding options
     * @param query The text to embed
     * @return The resulting embedding vector with specific dimensions
     */
    float[] embedQueryWithOptions(String query) {
        logger.info("Embedding with generic options: ${query}")
        var embeddings = embeddingModel.call(new EmbeddingRequest(List.of(query), 
                EmbeddingOptionsBuilder.builder()
                        .withDimensions(384)
                        .build()))
                .getResult().getOutput()
        return embeddings
    }
}
