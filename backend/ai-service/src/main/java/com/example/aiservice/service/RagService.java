package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.TechnicianClient;
import com.example.aiservice.model.VectorDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Service — Retrieval-Augmented Generation pipeline.
 *
 * 1. Retrieves semantically similar documents from the vector store
 * 2. Formats retrieved context as a clean text block for the LLM prompt
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final VectorStoreService vectorStore;

    public RagService(VectorStoreService vectorStore) {
        this.vectorStore = vectorStore;
    }

    /**
     * Retrieve the top-K most relevant documents for the query and role.
     * Returns formatted context text, or a "no data" message if nothing found.
     */
    public String retrieveContext(String query, String role) {
        List<VectorDocument> docs = vectorStore.findSimilar(query, role);

        if (docs.isEmpty()) {
            log.warn("No relevant documents found for query: '{}'", query);
            return "No relevant stock data found in the knowledge base.";
        }

        log.debug("Retrieved {} relevant documents for RAG", docs.size());

        String context = docs.stream()
                .map(VectorDocument::getContent)
                .collect(Collectors.joining("\n---\n"));

        return "RETRIEVED KNOWLEDGE BASE ENTRIES:\n" + context;
    }

    /**
     * Retrieve raw document objects (for debugging or structured responses).
     */
    public List<VectorDocument> retrieveDocuments(String query, String role) {
        return vectorStore.findSimilar(query, role);
    }
}
