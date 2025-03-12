package com.oconeco.spring_pgvector.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Controller
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam
import com.oconeco.spring_pgvector.service.EmbeddingService;

@RequestMapping("/embedding")
@Controller
class EmbeddingController {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    EmbeddingController(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @GetMapping(["/","/embed", "/demo"])
    String embed(@RequestParam(name="content", defaultValue = "This is a test sentence to embed!") String content, Model model) {
        logger.info("============== Embed text: "+  content);
        Document doc = new Document(content);
        List<Document> docs = [doc]
        def md = doc.getMetadata()
        md.put("source", "test");
        vectorStore.add([doc])
//        var embeddings = embeddingService.embedQuery(content);
        logger.info("Doc: ${doc} (split into: ${docs.size()} chunks)");

        model.addAttribute('embeddedDocuments',docs)
        return "embedding/demo"
    }

    @GetMapping("/generic-options")
    String embedGenericOptions(@RequestParam(name="query", defaultValue = "This is a test query to embed") String query) {
        logger.info("============== Embed generic-options: "+  query);
        var embeddings = embeddingService.embedQueryWithOptions(query);
        return "Size of the embedding vector: " + embeddings.length;
    }
}
