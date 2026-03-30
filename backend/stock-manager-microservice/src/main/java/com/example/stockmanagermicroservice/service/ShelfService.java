package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.ShelfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShelfService {

    @Autowired
    private ShelfRepository shelfRepository;

    public List<Shelf> getAllShelves() {
        return shelfRepository.findAll();
    }

    public Optional<Shelf> getShelfById(String id) {
        return shelfRepository.findById(id);
    }

    public List<Shelf> getShelvesByEquipmentType(String equipmentType) {
        return shelfRepository.findByEquipmentType(equipmentType);
    }

    public Shelf createShelf(Shelf shelf) {
        updateShelfStatus(shelf);
        return shelfRepository.save(shelf);
    }

    public Shelf updateShelf(String id, Shelf shelfDetails) {
        return shelfRepository.findById(id).map(shelf -> {
            shelf.setNb(shelfDetails.getNb());
            shelf.setMaxQte(shelfDetails.getMaxQte());
            shelf.setCurrentQte(shelfDetails.getCurrentQte());
            shelf.setEquipmentType(shelfDetails.getEquipmentType());
            updateShelfStatus(shelf);
            return shelfRepository.save(shelf);
        }).orElseThrow(() -> new RuntimeException("Shelf not found with id: " + id));
    }

    public void deleteShelf(String id) {
        shelfRepository.deleteById(id);
    }

    public void updateShelfStatus(Shelf shelf) {
        if (shelf.getCurrentQte() == null) shelf.setCurrentQte(0);
        if (shelf.getMaxQte() == null) shelf.setMaxQte(0);
        
        if (shelf.getCurrentQte() == 0) {
            shelf.setStatus("EMPTY");
        } else if (shelf.getCurrentQte() >= shelf.getMaxQte()) {
            shelf.setStatus("FULL");
        } else {
            shelf.setStatus("NORMAL");
        }
    }
}
