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
//        log.info("Creating ParagraphSentenceSplitter with maxSentencesPerChunk: ${maxSentencesPerChunk}, minSentencesPerChunk: ${minSentencesPerChunk}, keepSeparator: ${keepSeparator}")
        this.sentenceDetector = sentenceDetector
        this.maxSentencesPerChunk = maxSentencesPerChunk
        this.minSentencesPerChunk = minSentencesPerChunk
        this.keepSeparator = keepSeparator
        // Pattern to split by one or more blank lines
        this.paragraphPattern = Pattern.compile("\\n\\s*\\n+")
    }

    @Override
    List<Document> transform(List<Document> documents) {
        log.debug("\t\tTransforming {} documents (paragraph/sentence splitting)",  documents.size())
        int docNo = 0
        List<Document> transformedDocuments = []
        documents.each {Document document ->
            transformedDocuments << document
            docNo++
            def docId = document.getMetadata().get("docId") ?: document.getId()
            log.debug("\t\t$docNo:$docId) metadata:(${document.getMetadata()}) --  doc content size:${document.getText()?.size()}")
            log.debug("\t\t$docNo:$docId) Original document: ${document}")

            // SPLIT DOCUMENT
            List<Document> splitDocuments = splitDocument(document)
            log.debug("\t\t$docNo:$docId) Split into ${splitDocuments.size()} documents")
            transformedDocuments.addAll( splitDocuments)
        }

        return transformedDocuments
    }


    /**
     * Split a document into paragraphs and sentences using blank lines and OpenNLP
     *
     * @param document
     * @return
     */
    private List<Document> splitDocument(Document document) {
        log.debug("Splitting document: ${document.getId()}")
        String text = document.getText()
        Map<String, Object> metadata = document.getMetadata()
        String docId =  metadata.get("docId") ?: document.getId()
        if (text == null || text.trim().isEmpty()) {
            log.warn("Document content is empty, returning original document")
            return [document]
        }

        log.debug("\t\t$docId) Splitting document into paragraphs and sentences: ${document.getId()}")
        List<Document> splitParagraphsAndSentences = []

        // First split into paragraphs
        String[] paragraphs = paragraphPattern.split(text)
        log.debug("\t\t$docId) Split into ${paragraphs.length} paragraphs")

        int paragraphIndex = 0
        for (String paragraph : paragraphs) {
            Document paraDoc = null
            if (paragraph.trim()) {
                paraDoc = new Document(paragraph.trim())
                paraDoc.metadata.putAll(document.getMetadata())
                paraDoc.metadata.put('type', 'paragraph')
                paraDoc.metadata.put('paragraph_index', paragraphIndex)
                log.debug("\t\t$docId) Paragraph ${paragraphIndex + 1}: ${paraDoc}")
                splitParagraphsAndSentences << paraDoc
            } else {
                log.info("\t\t$docId)  Found empty paragraph($paragraphIndex): skipping")
                continue
            }

            // Then split paragraphs into sentences
            String[] sentences = sentenceDetector.sentDetect(paragraph)
            log.debug("\t\t$docId)  Paragraph ${paragraphIndex + 1} split into ${sentences.length} sentences")

            // Create a document for each chunk
            int sentenceIndex = 0
            for (String sentence : sentences) {
                sentenceIndex++
                String trimmedSentence = sentence.trim()
                if (trimmedSentence) {
                    // Create a new document with the chunk text
                    Document sentDoc = new Document(sentence)
                    sentDoc.metadata.putAll(paraDoc.metadata)

                    // Add metadata about the chunk
                    sentDoc.metadata.put("paragraph_index", paragraphIndex)
                    sentDoc.metadata.put("sentence_index", sentenceIndex)
                    sentDoc.metadata.put("type", 'sentence')
                    log.debug("\t\t\t\t$docId:$sentenceIndex) Sentence: $sentDoc")

                    splitParagraphsAndSentences.add(sentDoc)
                } else {
                    log.info("\t\t\t\t$docId:$sentenceIndex) Skipping empty sentence chunk")
                    continue
                }

            }

            paragraphIndex++
        }

        log.debug("Created ${splitParagraphsAndSentences.size()} document chunks")
        return splitParagraphsAndSentences
    }


    /**
     * Split (paragraph) chunk into sentences using OpenNLP
     *
     * @param sentences
     * @return
     */
/*
    private List<List<String>> chunkSentences(String content) {
        List<List<String>> sentences = []

        for (String sentence : sentences) {
            if (sentence.trim()) {
                sentences.add([sentence])
            } else {
                log.info("\t\tblank sentence: $sentence")
                continue
            }

        }

        // Add the last chunk if it's not empty
        if (!currentChunk.isEmpty()) {
            sentences.add(currentChunk)
        }

        return sentences
    }
*/

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
