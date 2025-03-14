package com.oconeco.spring_pgvector.controller

import com.oconeco.spring_pgvector.service.EmbeddingService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@RequestMapping("/embedding")
@Controller
class EmbeddingController {
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingController.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    EmbeddingController(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        logger.info "default constructor: EmbeddingController initialized as well as vectorStore"
    }

    @GetMapping(["", "/", '/index', '/list'])
    String list(Model model,
                @PageableDefault(size = 10, sort = "id") Pageable pageable,
                @RequestParam(name = "search", required = false) String searchQuery) {
        logger.info("Listing embeddings with pagination: page=${pageable.pageNumber}, size=${pageable.pageSize}, search=${searchQuery}")

        // Retrieve documents from the vector store
        List<Document> documents

        if (searchQuery && !searchQuery.trim().isEmpty()) {
            // If search query provided, use it to find similar documents
            documents = vectorStore.similaritySearch(SearchRequest.builder().query(searchQuery).topK(100).build())
            logger.info("Search query: '${searchQuery}' returned ${documents.size()} results")
        } else {
            // Default query if no search term provided
            documents = vectorStore.similaritySearch(SearchRequest.builder().query("pets").topK(100).build())
        }

        // Calculate start and end indices for the current page
        int start = pageable.pageNumber * pageable.pageSize
        int end = Math.min(start + pageable.pageSize, documents.size())

        // Create a sublist for the current page
        def pageContent = start < documents.size() ? documents.subList(start, end) : []

        // Create a Page object
        Page<Document> page = new PageImpl<>(pageContent, pageable, documents.size())

        model.addAttribute('page', page)
        model.addAttribute('currentPage', pageable.pageNumber)
        model.addAttribute('totalPages', page.totalPages)
        model.addAttribute('searchQuery', searchQuery ?: "")

        // Check if this is an HTMX request
        if (isHtmxRequest()) {
            return "embedding/fragments/document-table :: documentTable"
        }

        return "embedding/list"
    }

    private boolean isHtmxRequest() {
        def request = org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes().request
        return request.getHeader("HX-Request") != null
    }

    @GetMapping(["/embed", "/demo"])
    String embed(@RequestParam(name="content", required = true) String content,
                 @RequestParam(name="source", required = true) String source,        //, defaultValue = "testing"
                 @RequestParam(name="docId", required = true) String docId,
                 Model model) {
        logger.info("============== Embed text: "+  content);
        Document doc = new Document(content);
//        doc.setI
        List<Document> docs = [doc]
        def md = doc.getMetadata()
        md.put("source", source);
        md.put("docId", docId);

        def chunks = embeddingService.embedDocuments(docs)

//        var embeddings = embeddingService.embedQuery(content);
        logger.debug("Doc: ${doc} (split into: ${chunks.size()} chunks)");

        model.addAttribute('embeddedDocuments',docs)
        return "embedding/demo"
    }

    @GetMapping("/search")
    String search(@RequestParam(name="query", defaultValue = "What is a test query") String query, Model model) {
        logger.info("============== Embed query: "+  query);
        var embeddings = embeddingService.embedQuery(query);
        var results = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build())
        results.each { result ->
            logger.info("Result: ${result}")
        }
        model.addAttribute('results',results)
        logger.info("Found ${results.size()} results")
        return "embedding/results"
    }

    @GetMapping("/create")
    String create() {
        return "embedding/create"
    }

    @PostMapping("/create")
    String create(@RequestParam(name="content", required = true) String content,
                 @RequestParam(name="source", required = true) String source,        //, defaultValue = "testing"
                 @RequestParam(name="docId", required = true) String docId,
                 Model model) {
        logger.info("============== Embed text: "+  content);
        Document doc = new Document(content);
//        doc.setI
        List<Document> docs = [doc]
        def md = doc.getMetadata()
        md.put("source", source);
        md.put("docId", docId);

        def chunks = embeddingService.embedDocuments(docs)

//        var embeddings = embeddingService.embedQuery(content);
        logger.debug("Doc: ${doc} (split into: ${chunks.size()} chunks)");

        model.addAttribute('embeddedDocuments',docs)
        return "embedding/demo"
    }
}
