package com.oconeco.spring_pgvector.controller

import groovy.util.logging.Slf4j;
import org.springframework.ai.document.Document
import org.springframework.ai.model.Media
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam
import com.oconeco.spring_pgvector.service.EmbeddingService;

@Slf4j
@RequestMapping("/embedding")
@Controller
class EmbeddingController {
    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    EmbeddingController(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        log.info "default constructor: EmbeddingController initialized as well as vectorStore"
    }

    @GetMapping(["", "/", '/index', '/list'])
    String list(Model model,
                @PageableDefault(size = 10, sort = "id") Pageable pageable,
                @RequestParam(name = "search", required = false) String searchQuery) {
        log.info("Listing embeddings with pagination: page=${pageable.pageNumber}, size=${pageable.pageSize}, search=${searchQuery}")

        // Retrieve documents from the vector store
        List<Document> documents

        if (searchQuery && !searchQuery.trim().isEmpty()) {
            // If search query provided, use it to find similar documents
            documents = vectorStore.similaritySearch(SearchRequest.builder().query(searchQuery).topK(100).build())
            log.info("Search query: '${searchQuery}' returned ${documents.size()} results")
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

    @GetMapping(["/embed"])
    String embed(@RequestParam(name = "content", required = false) String content,
                 @RequestParam(name = "source", required = false) String source,        //, defaultValue = "testing"
                 @RequestParam(name = "docId", required = false) String docId,
                 @RequestParam(name = "media", required = false, defaultValue = 'text/plain') String media,
                 Model model) {
        log.info("============== Embed text: '${content}' source:$source, docId:$docId, media:$media");
        Document doc = new Document(content);
//        doc.setI
        List<Document> docs = [doc]
        def md = doc.getMetadata()
        md.put("source", source);
        md.put("docId", docId);

        def chunks = embeddingService.embedDocuments(docs)

//        var embeddings = embeddingService.embedQuery(content);
        log.debug("Doc: ${doc} (split into: ${chunks.size()} chunks)");

        model.addAttribute('embeddedDocuments', docs)
        return "embedding/success"
    }

    @PostMapping("/embed")
    String processEmbedding(@RequestParam(name = "content", required = true) String content,
                            @RequestParam(name = "source", required = true) String source,
                            @RequestParam(name = "docid", required = true) String docId,
//                            @RequestParam(name = "media", required = false, defaultValue = 'text/plain') String mediaParam,
                            Model model) {
        log.info("Processing embedding POST request with content: ${content}, source: ${source}, docId: ${docId ?: 'not provided'}")

        // Create a new document with the provided content
        Document doc = new Document(content, [docId:docId, source:source, type:'document'] )

        // Create a list with the document and embed it
        List<Document> docs = [doc]
        def chunks = embeddingService.embedDocuments(docs)

        log.info("\t\tDocument embedded successfully. Split into ${chunks.size()} chunks")

        // Add attributes to the model for display in the template
        model.addAttribute('document', doc)
        model.addAttribute('chunks', chunks)
        model.addAttribute('success', true)

        return "embedding/success"
    }

    @GetMapping("/search")
    String search(@RequestParam(name = "query", defaultValue = "What is a test query") String query, Model model) {
        log.info("============== Embed query: " + query);
        var embeddings = embeddingService.embedQuery(query);
        var results = vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build())
        results.each { result ->
            log.info("Result: ${result}")
        }
        model.addAttribute('results', results)
        log.info("Found ${results.size()} results")
        return "embedding/results"
    }

    @GetMapping("/create")
    String create() {
        log.info("Show create form...")
        return "embedding/create"
    }
}
