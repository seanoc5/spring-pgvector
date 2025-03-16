package com.oconeco.spring_pgvector.transformer

import groovy.util.logging.Slf4j
import opennlp.tools.sentdetect.SentenceDetector
import org.springframework.ai.document.Document
import org.springframework.ai.document.DocumentTransformer
import org.springframework.ai.transformer.splitter.TextSplitter

import java.util.function.Function
import java.util.regex.Pattern

/**
 * A document transformer that splits text into paragraphs (by blank lines)
 * and then further splits those paragraphs into sentences using OpenNLP.
 */
@Slf4j
class ParagraphSentenceSplitter implements DocumentTransformer {

    private final SentenceDetector sentenceDetector
    private final int maxSentencesPerChunk
    private final int minSentencesPerChunk
    private final boolean keepSeparator
    private final Pattern paragraphPattern

    /**
     * Creates a new ParagraphSentenceSplitter.
     *
     * @param sentenceDetector The OpenNLP sentence detector to use for sentence splitting
     * @param maxSentencesPerChunk Maximum number of sentences to include in a chunk
     * @param minSentencesPerChunk Minimum number of sentences to include in a chunk
     * @param keepSeparator Whether to keep the paragraph separator in the output
     */
    ParagraphSentenceSplitter(SentenceDetector sentenceDetector,
                             int maxSentencesPerChunk = 5,
                             int minSentencesPerChunk = 1,
                             boolean keepSeparator = false) {
        log.info("Creating ParagraphSentenceSplitter with maxSentencesPerChunk: ${maxSentencesPerChunk}, minSentencesPerChunk: ${minSentencesPerChunk}, keepSeparator: ${keepSeparator}")
        this.sentenceDetector = sentenceDetector
        this.maxSentencesPerChunk = maxSentencesPerChunk
        this.minSentencesPerChunk = minSentencesPerChunk
        this.keepSeparator = keepSeparator
        // Pattern to split by one or more blank lines
        this.paragraphPattern = Pattern.compile("\\n\\s*\\n+")
    }

    @Override
    List<Document> transform(List<Document> documents) {
        log.info("Transforming ${documents.size()} documents (paragraph/sentence splitting)")
        int docNo = 0
        List<Document> transformedDocuments = []
        documents.each {Document document ->
            transformedDocuments << document
            docNo++
            def docId = document.getMetadata().get("docId") ?: document.getId()
            log.info("\t\t$docNo:$docId) Original document: ${document}")
            List<Document> splitDocuments = splitDocument(document)
            log.info("\t\t$docNo:$docId) Split into ${splitDocuments.size()} documents")
            transformedDocuments.addAll(splitDocuments)
        }

        return transformedDocuments
    }

    private List<Document> splitDocument(Document document) {
        log.debug("Splitting document: ${document.getId()}")
        String text = document.getText()
        def metadata = document.getMetadata()
        String docId =  metadata.get("docId") ?: document.getId()
        if (text == null || text.trim().isEmpty()) {
            log.warn("Document content is empty, returning original document")
            return [document]
        }

        log.debug("\t\t$docId) Splitting document into paragraphs and sentences: ${document.getId()}")
        List<Document> result = []

        // First split into paragraphs
        String[] paragraphs = paragraphPattern.split(text)
        log.info("\t\t$docId) Split into ${paragraphs.length} paragraphs")

        int index = 0
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                log.info("\t\t$docId)  Found empty paragraph($index): skipping")
                continue
            }

            // Then split paragraphs into sentences
            String[] sentences = sentenceDetector.sentDetect(paragraph)
            log.info("\t\t$docId)  Paragraph ${index + 1} split into ${sentences.length} sentences")

            // Group sentences into chunks based on maxSentencesPerChunk
            List<List<String>> sentenceChunks = chunkSentences(sentences)

            // Create a document for each chunk
            int sentenceIndex = 0
            for (List<String> chunk : sentenceChunks) {
                sentenceIndex++
                String chunkText = String.join(" ", chunk)
                if (chunkText.trim().isEmpty()) {
                    log.info("\t\t\t\t$docId:$sentenceIndex) Skipping empty sentence chunk")
                    continue
                }

                // Create a new document with the chunk text
                Document newDoc = new Document(chunkText)

                // Add metadata about the chunk
                newDoc.getMetadata().put("paragraph_index", index)
                newDoc.getMetadata().put("sentence_count", chunk.size())

                result.add(newDoc)
            }

            index++
        }

        log.debug("Created ${result.size()} document chunks")
        return result
    }

    private List<List<String>> chunkSentences(String[] sentences) {
        List<List<String>> chunks = []
        List<String> currentChunk = []

        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue
            }

            // If adding this sentence would exceed maxSentencesPerChunk and we have at least minSentencesPerChunk,
            // start a new chunk
            if (currentChunk.size() >= maxSentencesPerChunk && currentChunk.size() >= minSentencesPerChunk) {
                chunks.add(currentChunk)
                currentChunk = []
            }

            currentChunk.add(sentence)
        }

        // Add the last chunk if it's not empty
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk)
        }

        return chunks
    }

    @Override
    List<Document> apply(List<Document> documents) {
        return null
    }

    @Override
    def <V> Function<V, List<Document>> compose(Function<? super V, ? extends List<Document>> before) {
        return super.compose(before)
    }

    @Override
    def <V> Function<List<Document>, V> andThen(Function<? super List<Document>, ? extends V> after) {
        return super.andThen(after)
    }
}
