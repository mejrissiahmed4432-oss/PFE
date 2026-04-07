package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.ShelfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EquipmentCategoryService {

    @Autowired
    private EquipmentCategoryRepository repository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private ShelfRepository shelfRepository;

    @Autowired
    private NotificationService notificationService;

    public List<EquipmentCategory> getAllCategories() {
        return repository.findAll();
    }

    public Optional<EquipmentCategory> getCategoryById(String id) {
        return repository.findById(id);
    }

    public EquipmentCategory createOrUpdateCategory(EquipmentCategory category) {
        // Uniqueness check for name (Case-Insensitive)
        Optional<EquipmentCategory> existingNameOwner = repository.findByNameIgnoreCase(category.getName());
        if (existingNameOwner.isPresent()) {
            // If it's a new category OR a rename that conflicts with another category
            if (category.getId() == null || !existingNameOwner.get().getId().equals(category.getId())) {
                throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
            }
        }

        if (category.getId() != null) {
            Optional<EquipmentCategory> existingOpt = repository.findById(category.getId());
            if (existingOpt.isPresent()) {
                EquipmentCategory existing = existingOpt.get();
                // If name changed, update all associated equipment
                if (!existing.getName().equals(category.getName())) {
                    List<Equipment> equipmentList = equipmentRepository.findByCategory(existing.getName());
                    for (Equipment e : equipmentList) {
                        e.setCategory(category.getName());
                        equipmentRepository.save(e);
                    }
                }
            }
        }
        EquipmentCategory saved = repository.save(category);
        if (category.getId() == null) {
            notificationService.createNotification("New Category Created: " + saved.getName(),
                    "Equipment category " + saved.getName() + " has been added",
                    "SUCCESS", "CATEGORY", saved.getId());
        } else {
            notificationService.createNotification("Category Updated: " + saved.getName(),
                    "Equipment category " + saved.getName() + " has been modified",
                    "INFO", "CATEGORY", saved.getId());
        }
        return saved;
    }

    public EquipmentCategory addTypeToCategory(String categoryId, String type) {
        Optional<EquipmentCategory> categoryOpt = repository.findById(categoryId);
        if (categoryOpt.isPresent()) {
            EquipmentCategory category = categoryOpt.get();
            // Duplicate check (Case-Insensitive)
            boolean exists = category.getTypes().stream()
                    .anyMatch(t -> t.equalsIgnoreCase(type.trim()));
            
            if (exists) {
                throw new IllegalArgumentException("Type '" + type + "' already exists in category '" + category.getName() + "'.");
            }
            
            category.getTypes().add(type.trim());
            return repository.save(category);
        }
        throw new RuntimeException("Category not found with id: " + categoryId);
    }

    public EquipmentCategory removeTypeFromCategory(String categoryId, String type) {
        Optional<EquipmentCategory> categoryOpt = repository.findById(categoryId);
        if (categoryOpt.isPresent()) {
            EquipmentCategory category = categoryOpt.get();
            category.getTypes().remove(type);
            return repository.save(category);
        }
        throw new RuntimeException("Category not found with id: " + categoryId);
    }

    public void deleteCategory(String id) {
        Optional<EquipmentCategory> catOpt = repository.findById(id);
        if (catOpt.isPresent()) {
            EquipmentCategory category = catOpt.get();
            
            // Safety check 1: Equipment association
            if (equipmentRepository.existsByCategory(category.getName())) {
                throw new IllegalStateException("Cannot delete category: Equipment is still associated with it.");
            }
            
            // Safety check 2: Shelf association (check if any type in this category is used in a shelf)
            if (category.getTypes() != null && !category.getTypes().isEmpty()) {
                if (shelfRepository.existsByEquipmentTypeIn(category.getTypes())) {
                    throw new IllegalStateException("Cannot delete category: One or more of its equipment types are associated with shelving.");
                }
            }
            
            notificationService.createNotification("Category Deleted: " + category.getName(),
                    "Equipment category " + category.getName() + " has been removed",
                    "ERROR", "CATEGORY", id);
            repository.deleteById(id);
        } else {
            throw new RuntimeException("Category not found with id: " + id);
        }
    }
}
