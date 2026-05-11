package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.CatalogItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CatalogItemRepository extends MongoRepository<CatalogItem, String> {
    
    @Query("{ '$or': [ { 'name': { '$regex': ?0, '$options': 'i' } }, { 'category': { '$regex': ?0, '$options': 'i' } } ] }")
    List<CatalogItem> searchByNameOrCategory(String query);
    
    List<CatalogItem> findByCategory(String category);
}
