package com.oconeco.spring_pgvector.service

import groovy.util.logging.Slf4j
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Slf4j
@Service
class EmbeddingService {
//    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class)

    private final EmbeddingModel embeddingModel
    private final VectorStore vectorStore

    EmbeddingService(EmbeddingModel embeddingModel, VectorStore vectorStore) {
        this.embeddingModel = embeddingModel
        this.vectorStore = vectorStore
        log.info "default constructor: EmbeddingService initialized as well as vectorStore"
    }

    /**
     * Embeds a query string into a vector using the default model settings
     * @param query The text to embed
     * @return The resulting embedding vector
     */
    float[] embedQuery(String query) {
        log.info("Embedding query: ${query}")
        var embeddings = embeddingModel.embed(query)
        // todo - save embeddings to vector store

        log.debug("Embedding(${embeddings.size()}): ${embeddings}")
        return embeddings
    }

    List<Document> embedDocuments(List<Document> documents) {
        log.info("Embedding documents (${documents.size()}): ${documents}")
        // int defaultChunkSize, int minChunkSizeChars, int minChunkLengthToEmbed, int maxNumChunks, boolean keepSeparator
        var textSplitter = new TokenTextSplitter(50, 10, 5, 5, true);
        var transformedDocuments = textSplitter.apply(documents);
        transformedDocuments.each { doc ->
            log.info("Doc: ${doc}")
        }
        vectorStore.add(transformedDocuments)
        return transformedDocuments
    }


}
