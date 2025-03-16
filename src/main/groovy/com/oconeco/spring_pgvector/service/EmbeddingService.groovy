package com.oconeco.spring_pgvector.service

import groovy.util.logging.Slf4j
import opennlp.tools.sentdetect.SentenceDetector
import org.springframework.ai.document.Document
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.transformer.splitter.TokenTextSplitter
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceModel;
import com.oconeco.spring_pgvector.transformer.ParagraphSentenceSplitter

@Slf4j
@Service
class EmbeddingService {
//    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class)

    private final EmbeddingModel embeddingModel
    private final VectorStore vectorStore
    private SentenceDetector sentenceDetector;

    EmbeddingService(EmbeddingModel embeddingModel, VectorStore vectorStore, SentenceDetector sentenceDetector) {
        this.embeddingModel = embeddingModel
        this.vectorStore = vectorStore
        this.sentenceDetector = sentenceDetector
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
        log.info("Embedding documents (${documents.size()})")

        // Use our custom ParagraphSentenceSplitter instead of TokenTextSplitter
        // Parameters: sentenceDetector, maxSentencesPerChunk, minSentencesPerChunk, keepSeparator
        var textSplitter = new ParagraphSentenceSplitter(sentenceDetector, 5, 1, true);

        // For comparison, here's the old TokenTextSplitter
        // var textSplitter = new TokenTextSplitter(50, 20, 5, 1000, true);

        var transformedDocuments = textSplitter.transform(documents);
        transformedDocuments.each { doc ->
            log.info("\t\t transformed/split Doc: ${doc}")
        }
        transformedDocuments <<
        vectorStore.add(transformedDocuments)
        return transformedDocuments
    }


}
