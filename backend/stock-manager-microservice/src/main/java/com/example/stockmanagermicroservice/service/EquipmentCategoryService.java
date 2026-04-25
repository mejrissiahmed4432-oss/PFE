package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.CategoryType;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.ShelfRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private MongoTemplate mongoTemplate;

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
            if (category.getId() == null || !existingNameOwner.get().getId().equals(category.getId())) {
                throw new IllegalArgumentException("Category with name '" + category.getName() + "' already exists.");
            }
        }

        if (category.getId() != null) {
            Optional<EquipmentCategory> existingOpt = repository.findById(category.getId());
            if (existingOpt.isPresent()) {
                EquipmentCategory existing = existingOpt.get();
                if (!existing.getName().equals(category.getName())) {
                    // Atomic: only update category field, no file data loaded
                    Query q = new Query(Criteria.where("category").is(existing.getName()));
                    Update u = new Update().set("category", category.getName());
                    mongoTemplate.updateMulti(q, u, Equipment.class);
                }
            }
        }
        EquipmentCategory saved = repository.save(category);
        if (category.getId() == null) {
            notificationService.createNotification("New Category Created: " + saved.getName(),
                    "Equipment category " + saved.getName() + " has been added",
                    "SUCCESS", "CATEGORY", saved.getId(), null, "STOCK_MANAGER");
        } else {
            notificationService.createNotification("Category Updated: " + saved.getName(),
                    "Equipment category " + saved.getName() + " has been modified",
                    "INFO", "CATEGORY", saved.getId(), null, "STOCK_MANAGER");
        }
        return saved;
    }

    public EquipmentCategory addTypeToCategory(String categoryId, CategoryType newType) {
        Optional<EquipmentCategory> categoryOpt = repository.findById(categoryId);
        if (categoryOpt.isPresent()) {
            EquipmentCategory category = categoryOpt.get();
            // Duplicate check (Case-Insensitive)
            boolean exists = category.getTypes().stream()
                    .anyMatch(t -> t.getName().equalsIgnoreCase(newType.getName().trim()));

            if (exists) {
                throw new IllegalArgumentException("Type '" + newType.getName() + "' already exists in category '" + category.getName() + "'.");
            }

            newType.setName(newType.getName().trim());
            category.getTypes().add(newType);
            return repository.save(category);
        }
        throw new RuntimeException("Category not found with id: " + categoryId);
    }

    public EquipmentCategory updateTypeInCategory(String categoryId, String oldTypeName, CategoryType updatedType) {
        Optional<EquipmentCategory> categoryOpt = repository.findById(categoryId);
        if (!categoryOpt.isPresent()) {
            throw new RuntimeException("Category not found with id: " + categoryId);
        }

        EquipmentCategory category = categoryOpt.get();
        String newName = updatedType.getName().trim();

        // Find the existing type
        CategoryType existing = category.getTypes().stream()
                .filter(t -> t.getName().equalsIgnoreCase(oldTypeName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Type '" + oldTypeName + "' not found in category."));

        // Check new name is unique within this category (if it changed)
        boolean nameChanged = !existing.getName().equalsIgnoreCase(newName);
        if (nameChanged) {
            boolean nameConflict = category.getTypes().stream()
                    .anyMatch(t -> !t.getName().equalsIgnoreCase(oldTypeName) && t.getName().equalsIgnoreCase(newName));
            if (nameConflict) {
                throw new IllegalArgumentException("Type '" + newName + "' already exists in category '" + category.getName() + "'.");
            }
        }

        // Check if QR code setting can be toggled: cascade to equipment
        boolean qrChanged = existing.isRequiresQrCode() != updatedType.isRequiresQrCode();
        
        // Apply changes
        existing.setName(newName);
        existing.setRequiresQrCode(updatedType.isRequiresQrCode());
        existing.setSpecificationFields(updatedType.getSpecificationFields());

        // Process existing equipment based on changes
        if (qrChanged || nameChanged) {
            List<Equipment> equipmentList;
            if (nameChanged) {
                equipmentList = equipmentRepository.findByTypeIgnoreCaseExcludingFiles(oldTypeName);
            } else {
                equipmentList = equipmentRepository.findByTypeIgnoreCaseExcludingFiles(newName);
            }
            
            for (Equipment e : equipmentList) {
                Update eu = new Update();
                boolean needsUpdate = false;
                if (nameChanged) {
                    eu.set("type", newName);
                    needsUpdate = true;
                }
                if (qrChanged) {
                    if (updatedType.isRequiresQrCode()) {
                        if (e.getQrCode() == null || e.getQrCode().isEmpty()) {
                            eu.set("qrCode", "QR-" + System.currentTimeMillis() + "-" + e.getId());
                            needsUpdate = true;
                        }
                    } else {
                        eu.unset("qrCode");
                        needsUpdate = true;
                    }
                }
                if (needsUpdate) {
                    Query eq = new Query(Criteria.where("_id").is(e.getId()));
                    mongoTemplate.updateFirst(eq, eu, Equipment.class);
                }
            }

            if (nameChanged) {
                List<Shelf> shelfList = shelfRepository.findByEquipmentTypeIgnoreCase(oldTypeName);
                for (Shelf s : shelfList) {
                    s.setEquipmentType(newName);
                    if (s.getNb() != null && s.getNb().toLowerCase().startsWith(oldTypeName.toLowerCase() + "-")) {
                        String suffix = s.getNb().substring(oldTypeName.length() + 1);
                        s.setNb(newName.toLowerCase() + "-" + suffix);
                    }
                    shelfRepository.save(s);
                }
            }
        }

        return repository.save(category);
    }

    public EquipmentCategory removeTypeFromCategory(String categoryId, String typeName) {
        Optional<EquipmentCategory> categoryOpt = repository.findById(categoryId);
        if (categoryOpt.isPresent()) {
            EquipmentCategory category = categoryOpt.get();

            // Safety check: is equipment using this type?
            if (equipmentRepository.existsByTypeIgnoreCase(typeName.trim())) {
                throw new IllegalStateException("Cannot remove type '" + typeName + "': Equipment is currently associated with it.");
            }

            category.getTypes().removeIf(t -> t.getName().equalsIgnoreCase(typeName));
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

            // Safety check 2: Shelf association
            if (category.getTypes() != null && !category.getTypes().isEmpty()) {
                List<String> typeNames = category.getTypes().stream()
                        .map(CategoryType::getName)
                        .collect(Collectors.toList());
                if (shelfRepository.existsByEquipmentTypeIn(typeNames)) {
                    throw new IllegalStateException("Cannot delete category: One or more of its equipment types are associated with shelving.");
                }
            }

            notificationService.createNotification("Category Deleted: " + category.getName(),
                    "Equipment category " + category.getName() + " has been removed",
                    "ERROR", "CATEGORY", id, null, "STOCK_MANAGER");
            repository.deleteById(id);
        } else {
            throw new RuntimeException("Category not found with id: " + id);
        }
    }
}
