package com.oconeco.spring_pgvector.service

import groovy.util.logging.Slf4j
import opennlp.tools.sentdetect.SentenceDetector
import org.apache.solr.client.solrj.SolrClient
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
    private SolrClient solrClient

    EmbeddingService(EmbeddingModel embeddingModel, VectorStore vectorStore, SentenceDetector sentenceDetector, SolrClient solrClient) {
        this.embeddingModel = embeddingModel
        this.vectorStore = vectorStore
        this.sentenceDetector = sentenceDetector
        this.solrClient = solrClient
        log.debug "default constructor: EmbeddingService initialized as well as vectorStore: " +
                "\n\t\tEmbeddingModel:${embeddingModel.toString()}, " +
                "\n\t\tVectorStore:$vectorStore, " +
                "\n\t\tSolrClient:$solrClient, " +
                "\n\t\tSentenceDetector:$sentenceDetector"
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

    /**
     * Accept a list of documents (or just one in list form),
     * split them by paragraph (naively split by blank line)
     * split the paragraphs by sentence (OpenNLP)
     *
     * Add all those resulting chunk/docs to the embedding service
     *
     * NOTE: For comparison, here's the old TokenTextSplitter
     * var textSplitter = new TokenTextSplitter(50, 20, 5, 1000, true);
     *
     * @param documents
     * @return
     */
    List<Document> embedDocuments(List<Document> documents) {
        log.info("Embedding documents (${documents.size()})")
        List<Document> docsToEmbed = new ArrayList<>()
        if (documents) {
            // Use our custom ParagraphSentenceSplitter instead of TokenTextSplitter
            var textSplitter = new ParagraphSentenceSplitter(sentenceDetector, 5, 1, true);
            // Then add chunks for each document
            documents.each { Document doc ->
                List<Document> docChunks = textSplitter.transform([doc]);
                log.info("\t\tDoc chunks: (${docChunks.size()})")
                docChunks.each { chunk ->
                    log.info("\t\t transformed/split Doc id:(${chunk.id}) -- metadata:(${chunk.getMetadata()}) --  content size:(${chunk.getText()?.size()}) ")
                    log.debug("\t\t transformed/split Doc id:(${chunk.id})")
                    docsToEmbed.add(chunk)
                }
            }
            vectorStore.add(docsToEmbed)
            log.info("\t\tAdded ${docsToEmbed.size()} documents to embedding service")
        } else {
            log.warn "Got empty list of documents to embed, skipping..."
        }

        return docsToEmbed
    }
}
