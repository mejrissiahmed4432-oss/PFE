package com.example.aiservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Persistent vector document stored in MongoDB.
 * Replaces SimpleVectorStore — embeddings survive service restarts.
 */
@Document(collection = "ai_vector_documents")
public class VectorDocument {

    @Id
    private String id;

    /** The human-readable text chunk this embedding represents */
    private String content;

    /** Embedding vector produced by nomic-embed-text */
    private List<Double> embedding;

    /**
     * Which role this document is relevant for.
     * Values: "stock_manager" | "technician" | "all"
     */
    @Indexed
    private String role;

    /** Which intent category this document supports */
    @Indexed
    private String intentCategory;

    /** Optional key-value metadata (e.g. equipmentId, shelfId, etc.) */
    private Map<String, Object> metadata;

    private LocalDateTime indexedAt;

    // ── Constructors ─────────────────────────────────────────────────────────
    public VectorDocument() {}

    public VectorDocument(String content, List<Double> embedding,
                          String role, String intentCategory,
                          Map<String, Object> metadata) {
        this.content = content;
        this.embedding = embedding;
        this.role = role;
        this.intentCategory = intentCategory;
        this.metadata = metadata;
        this.indexedAt = LocalDateTime.now();
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────
    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }

    public String getContent()          { return content; }
    public void setContent(String v)    { this.content = v; }

    public List<Double> getEmbedding()          { return embedding; }
    public void setEmbedding(List<Double> v)    { this.embedding = v; }

    public String getRole()             { return role; }
    public void setRole(String v)       { this.role = v; }

    public String getIntentCategory()           { return intentCategory; }
    public void setIntentCategory(String v)     { this.intentCategory = v; }

    public Map<String, Object> getMetadata()            { return metadata; }
    public void setMetadata(Map<String, Object> v)      { this.metadata = v; }

    public LocalDateTime getIndexedAt()             { return indexedAt; }
    public void setIndexedAt(LocalDateTime v)       { this.indexedAt = v; }
}
