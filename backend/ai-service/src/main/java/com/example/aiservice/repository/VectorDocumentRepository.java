package com.example.aiservice.repository;

import com.example.aiservice.model.VectorDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VectorDocumentRepository extends MongoRepository<VectorDocument, String> {

    /** Get all documents accessible by role (role-specific + shared "all" docs) */
    List<VectorDocument> findByRoleIn(List<String> roles);

    /** Get documents for a specific role and intent category */
    List<VectorDocument> findByRoleInAndIntentCategory(List<String> roles, String intentCategory);

    /** Delete all documents for a given role (used before reindexing) */
    void deleteByRole(String role);

    /** Delete all documents (full reindex) */
    void deleteAllByRoleIn(List<String> roles);

    /** Count documents per role */
    long countByRole(String role);
}
