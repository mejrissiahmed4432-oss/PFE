package com.example.aiservice.service;

import com.example.aiservice.model.VectorDocument;
import com.example.aiservice.repository.VectorDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB-backed vector store service.
 *
 * Stores embeddings as List<Double> in MongoDB.
 * Retrieval uses in-memory cosine similarity computation on loaded documents.
 *
 * This is a lightweight alternative to pgvector/Atlas that works with any MongoDB.
 */
@Service
public class VectorStoreService {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreService.class);

    private final VectorDocumentRepository repository;
    private final EmbeddingModel embeddingModel;

    @Value("${app.ai.vector-store.top-k:5}")
    private int defaultTopK;

    @Value("${app.ai.vector-store.similarity-threshold:0.60}")
    private double similarityThreshold;

    public VectorStoreService(VectorDocumentRepository repository,
                              @org.springframework.beans.factory.annotation.Autowired(required = false) EmbeddingModel embeddingModel) {
        this.repository = repository;
        this.embeddingModel = embeddingModel;
    }

    // ── Ingestion ─────────────────────────────────────────────────────────────

    /**
     * Embed a text chunk and persist it to MongoDB.
     */
    public void store(String content, String role, String intentCategory,
                      Map<String, Object> metadata) {
        if (embeddingModel == null) {
            log.warn("Embedding model is disabled. Skipping document storage.");
            return;
        }
        try {
            float[] raw = embeddingModel.embed(content);
            List<Double> embedding = toDoubleList(raw);

            VectorDocument doc = new VectorDocument(content, embedding, role, intentCategory, metadata);
            repository.save(doc);
        } catch (Exception e) {
            log.error("Failed to embed and store document: {}", e.getMessage());
        }
    }

    /**
     * Bulk store with progress logging.
     */
    public void storeAll(List<String> texts, String role, String intentCategory,
                         List<Map<String, Object>> metadataList) {
        if (embeddingModel == null) {
            log.warn("Embedding model is disabled. Skipping batch storage for role '{}'", role);
            return;
        }
        log.info("Embedding {} documents for role='{}' intent='{}'", texts.size(), role, intentCategory);
        List<VectorDocument> batch = new ArrayList<>();

        for (int i = 0; i < texts.size(); i++) {
            try {
                float[] raw = embeddingModel.embed(texts.get(i));
                Map<String, Object> meta = (metadataList != null && i < metadataList.size())
                        ? metadataList.get(i) : Map.of();
                batch.add(new VectorDocument(texts.get(i), toDoubleList(raw), role, intentCategory, meta));
            } catch (Exception e) {
                log.warn("Failed to embed doc #{}: {}", i, e.getMessage());
            }
        }

        if (!batch.isEmpty()) {
            repository.saveAll(batch);
            log.info("Stored {} vectors in MongoDB", batch.size());
        }
    }

    // ── Retrieval ─────────────────────────────────────────────────────────────

    /**
     * Semantic search: embed the query, compute cosine similarity, return top-K docs above threshold.
     */
    public List<VectorDocument> findSimilar(String query, String role) {
        return findSimilar(query, role, defaultTopK);
    }

    public List<VectorDocument> findSimilar(String query, String role, int topK) {
        if (embeddingModel == null) {
            log.warn("Embedding model is disabled. Cannot perform semantic search.");
            return Collections.emptyList();
        }
        try {
            float[] queryEmbedding = embeddingModel.embed(query);

            // Load only role-relevant docs
            List<VectorDocument> candidates = repository.findByRoleIn(
                    List.of(role.toLowerCase(), "all")
            );

            if (candidates.isEmpty()) {
                log.warn("No vector documents found for role '{}'", role);
                return Collections.emptyList();
            }

            // Score and rank
            return candidates.stream()
                    .map(doc -> new ScoredDoc(doc, cosineSimilarity(queryEmbedding, doc.getEmbedding())))
                    .filter(sd -> sd.score >= similarityThreshold)
                    .sorted(Comparator.comparingDouble(ScoredDoc::score).reversed())
                    .limit(topK)
                    .map(ScoredDoc::doc)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Vector search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ── Maintenance ───────────────────────────────────────────────────────────

    public void clearRole(String role) {
        repository.deleteByRole(role);
        log.info("Cleared all vector documents for role '{}'", role);
    }

    public void clearAll() {
        repository.deleteAll();
        log.info("Cleared all vector documents");
    }

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("stock_manager", repository.countByRole("stock_manager"));
        stats.put("technician", repository.countByRole("technician"));
        stats.put("all", repository.countByRole("all"));
        stats.put("total", repository.count());
        return stats;
    }

    // ── Math helpers ──────────────────────────────────────────────────────────

    private double cosineSimilarity(float[] a, List<Double> b) {
        if (a == null || b == null || a.length != b.size()) return 0.0;
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            double ai = a[i], bi = b.get(i);
            dot += ai * bi;
            normA += ai * ai;
            normB += bi * bi;
        }
        double denom = Math.sqrt(normA) * Math.sqrt(normB);
        return denom == 0 ? 0.0 : dot / denom;
    }

    private List<Double> toDoubleList(float[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (float f : arr) list.add((double) f);
        return list;
    }

    // ── Inner helpers ─────────────────────────────────────────────────────────

    private record ScoredDoc(VectorDocument doc, double score) {}
}
