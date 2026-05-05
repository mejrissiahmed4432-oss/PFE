package com.example.stockmanagermicroservice.controller;

import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.service.ShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shelves")
public class ShelfController {

    @Autowired
    private ShelfService shelfService;

    @GetMapping
    public List<Shelf> getAllShelves() {
        return shelfService.getAllShelves();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Shelf> getShelfById(@PathVariable String id) {
        return shelfService.getShelfById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/type/{equipmentType}")
    public List<Shelf> getShelvesByType(@PathVariable String equipmentType) {
        return shelfService.getShelvesByEquipmentType(equipmentType);
    }

    @PostMapping
    public Shelf createShelf(@RequestBody Shelf shelf) {
        return shelfService.createShelf(shelf);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Shelf> updateShelf(@PathVariable String id, @RequestBody Shelf shelf) {
        try {
            return ResponseEntity.ok(shelfService.updateShelf(id, shelf));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShelf(@PathVariable String id) {
        shelfService.deleteShelf(id);
        return ResponseEntity.noContent().build();
    }
}
