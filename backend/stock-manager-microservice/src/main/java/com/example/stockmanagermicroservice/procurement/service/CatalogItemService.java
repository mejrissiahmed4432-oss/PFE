package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.CatalogItem;
import com.example.stockmanagermicroservice.procurement.repository.CatalogItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CatalogItemService {

    private final CatalogItemRepository repository;

    public CatalogItemService(CatalogItemRepository repository) {
        this.repository = repository;
    }

    public List<CatalogItem> getAllItems() {
        return repository.findAll();
    }

    public List<CatalogItem> searchItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllItems();
        }
        return repository.searchByNameOrCategory(query.trim());
    }

    public Optional<CatalogItem> getItemById(String id) {
        return repository.findById(id);
    }

    public CatalogItem saveItem(CatalogItem item) {
        return repository.save(item);
    }

    public void deleteItem(String id) {
        repository.deleteById(id);
    }
}
