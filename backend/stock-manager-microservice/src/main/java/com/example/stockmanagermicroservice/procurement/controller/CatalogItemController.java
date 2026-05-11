package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.CatalogItem;
import com.example.stockmanagermicroservice.procurement.service.CatalogItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement/catalog")
@CrossOrigin(origins = "*")
public class CatalogItemController {

    private final CatalogItemService service;

    public CatalogItemController(CatalogItemService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<CatalogItem>> getCatalog(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(service.searchItems(query));
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<CatalogItem>> autocomplete(@RequestParam String query) {
        // We can limit this to top 5 or just use searchItems
        List<CatalogItem> results = service.searchItems(query);
        return ResponseEntity.ok(results.size() > 5 ? results.subList(0, 5) : results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CatalogItem> getItemById(@PathVariable String id) {
        return service.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<CatalogItem> createItem(@RequestBody CatalogItem item) {
        return ResponseEntity.ok(service.saveItem(item));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        service.deleteItem(id);
        return ResponseEntity.ok().build();
    }
}
