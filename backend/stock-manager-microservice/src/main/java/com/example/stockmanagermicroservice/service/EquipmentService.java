package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.CategoryType;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;



    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ShelfService shelfService;

    @Autowired
    private EquipmentCategoryRepository equipmentCategoryRepository;

    @Autowired
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public List<Equipment> getEquipmentByShelfId(String shelfId) {
        return equipmentRepository.findByShelfId(shelfId);
    }

    public boolean isSerialNumberUnique(String serialNumber, String excludeId) {
        if (serialNumber == null || serialNumber.trim().isEmpty())
            return true;
        if (excludeId != null && !excludeId.isEmpty()) {
            return !equipmentRepository.existsBySerialNumberAndIdNot(serialNumber.trim(), excludeId);
        }
        return !equipmentRepository.existsBySerialNumber(serialNumber.trim());
    }



    public Optional<Equipment> getEquipmentById(String id) {
        return equipmentRepository.findById(id);
    }

    public Equipment createEquipment(Equipment equipment) {
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());
        
        // Respect the "Requires QR Code" setting from categories
        if (typeRequiresQrCode(equipment.getCategory(), equipment.getType())) {
            if (equipment.getQrCode() == null || equipment.getQrCode().isEmpty()) {
                equipment.setQrCode("QR-" + System.currentTimeMillis());
            }
        } else {
            equipment.setQrCode(null);
        }

        if (equipment.getStatus() == null || equipment.getStatus().isEmpty()) {
            equipment.setStatus("Available");
        }
        Equipment saved = equipmentRepository.save(equipment);

        // Generate notification for new equipment
        String creator = (saved.getCreatedBy() != null && !saved.getCreatedBy().isEmpty()) ? saved.getCreatedBy() : "System";
        notificationService.createNotification("New " + saved.getType() + " Added",
                "New " + saved.getType() + " " + saved.getBrand() + " " + saved.getModel() + " added by " + creator,
                "SUCCESS", "EQUIPMENT", saved.getId());

        // Update shelf quantity (Atomically)
        if (saved.getShelfId() != null && !saved.getShelfId().isEmpty()) {
            atomicUpdateShelfQuantity(saved.getShelfId(), saved.getQte() != null ? saved.getQte() : 1);
        }

        return saved;
    }

    private void atomicUpdateShelfQuantity(String shelfId, int delta) {
        if (shelfId == null || shelfId.isEmpty())
            return;

        org.springframework.data.mongodb.core.query.Query query = new org.springframework.data.mongodb.core.query.Query(
                org.springframework.data.mongodb.core.query.Criteria.where("id").is(shelfId));

        org.springframework.data.mongodb.core.query.Update update = new org.springframework.data.mongodb.core.query.Update()
                .inc("currentQte", delta);

        Shelf updatedShelf = mongoTemplate.findAndModify(
                query,
                update,
                new org.springframework.data.mongodb.core.FindAndModifyOptions().returnNew(true),
                Shelf.class);

        if (updatedShelf != null) {
            shelfService.updateShelfStatus(updatedShelf);
            // Update ONLY the status atomically to avoid overwriting currentQte
            mongoTemplate.updateFirst(
                    query,
                    new org.springframework.data.mongodb.core.query.Update().set("status", updatedShelf.getStatus()),
                    Shelf.class);
        }
    }

    public Equipment updateEquipment(String id, Equipment equipmentDetails) {
        return equipmentRepository.findById(id).map(equipment -> {
            String oldShelfId = equipment.getShelfId();
            Integer oldQte = equipment.getQte() != null ? equipment.getQte() : 1;

            String status = equipmentDetails.getStatus();
            String newShelfId = equipmentDetails.getShelfId();
            Integer newQte = equipmentDetails.getQte() != null ? equipmentDetails.getQte() : 1;

            // Automatic Redirection Logic
            if ("Maintenance".equals(status)) {
                newShelfId = "MAINTENANCE_AREA";
            } else if ("Broken".equals(status)) {
                newShelfId = "SCRAP_YARD";
            } else if ("Out of Stock".equals(status)) {
                newShelfId = "OUT_OF_STOCK";
            }

            if (newShelfId != null && !newShelfId.equals(oldShelfId)) {
                equipment.setLocationChangeAt(LocalDateTime.now());
                equipment.setLocationChanged(true);
                
                // Notification for location change
                notificationService.createNotification("Location Changed: " + equipment.getEquipmentName(),
                        "Equipment moved from shelf " + (oldShelfId != null ? oldShelfId : "N/A") + " to " + newShelfId,
                        "INFO", "EQUIPMENT", equipment.getId());
            }

            equipment.setEquipmentName(equipmentDetails.getEquipmentName());
            equipment.setBrand(equipmentDetails.getBrand());
            equipment.setModel(equipmentDetails.getModel());
            equipment.setSerialNumber(equipmentDetails.getSerialNumber());
            equipment.setCategory(equipmentDetails.getCategory());
            equipment.setSupplier(equipmentDetails.getSupplier());
            equipment.setType(equipmentDetails.getType());
            equipment.setQte(newQte);
            equipment.setShelfId(newShelfId);
            equipment.setStatus(status);

            equipment.setPurchaseDate(equipmentDetails.getPurchaseDate());
            equipment.setWarrantyExpiration(equipmentDetails.getWarrantyExpiration());
            equipment.setPurchasePrice(equipmentDetails.getPurchasePrice());
            equipment.setIcon(equipmentDetails.getIcon());
            equipment.setNote(equipmentDetails.getNote());
            equipment.setDepartment(equipmentDetails.getDepartment());
            equipment.setCreatedBy(equipmentDetails.getCreatedBy());

            // File Documents (Conditional Update)
            if (equipmentDetails.getInvoiceFileData() != null && !equipmentDetails.getInvoiceFileData().isEmpty()) {
                equipment.setInvoiceFileName(equipmentDetails.getInvoiceFileName());
                equipment.setInvoiceFileData(equipmentDetails.getInvoiceFileData());
            }
            if (equipmentDetails.getWarrantyFileData() != null && !equipmentDetails.getWarrantyFileData().isEmpty()) {
                equipment.setWarrantyFileName(equipmentDetails.getWarrantyFileName());
                equipment.setWarrantyFileData(equipmentDetails.getWarrantyFileData());
            }

            // Device Specifications
            equipment.setCpu(equipmentDetails.getCpu());
            equipment.setRam(equipmentDetails.getRam());
            equipment.setStorage(equipmentDetails.getStorage());
            equipment.setGraphicsCard(equipmentDetails.getGraphicsCard());
            equipment.setOperatingSystem(equipmentDetails.getOperatingSystem());
            equipment.setSpecification(equipmentDetails.getSpecification());

            if (typeRequiresQrCode(equipment.getCategory(), equipment.getType())) {
                if (equipment.getQrCode() == null || equipment.getQrCode().isEmpty()) {
                    equipment.setQrCode("QR-" + System.currentTimeMillis());
                } else if (equipmentDetails.getQrCode() != null && !equipmentDetails.getQrCode().isEmpty()) {
                    equipment.setQrCode(equipmentDetails.getQrCode());
                }
            } else {
                equipment.setQrCode(null);
            }

            equipment.setUpdatedAt(LocalDateTime.now());
            Equipment updated = equipmentRepository.save(equipment);

            // Generate notification for equipment update
            String updater = (updated.getCreatedBy() != null && !updated.getCreatedBy().isEmpty()) ? updated.getCreatedBy() : "System";
            notificationService.createNotification("Equipment Updated: " + updated.getEquipmentName(),
                    updated.getBrand() + " " + updated.getModel() + " was updated by " + updater,
                    "INFO", "EQUIPMENT", updated.getId());

            // Handle Shelf Capacity Changes using existing logic
            boolean isMarker = "MAINTENANCE_AREA".equals(newShelfId) ||
                    "SCRAP_YARD".equals(newShelfId) ||
                    "OUT_OF_STOCK".equals(newShelfId);

            boolean oldIsMarker = "MAINTENANCE_AREA".equals(oldShelfId) ||
                    "SCRAP_YARD".equals(oldShelfId) ||
                    "OUT_OF_STOCK".equals(oldShelfId);

            boolean shelfChanged = (oldShelfId == null && newShelfId != null && !newShelfId.isEmpty()) ||
                    (oldShelfId != null && oldShelfId.isEmpty() && newShelfId != null && !newShelfId.isEmpty()) ||
                    (oldShelfId != null && !oldShelfId.isEmpty() && newShelfId == null) ||
                    (oldShelfId != null && !oldShelfId.isEmpty() && newShelfId != null
                            && !oldShelfId.equals(newShelfId));

            if (shelfChanged) {
                if (oldShelfId != null && !oldShelfId.isEmpty() && !oldIsMarker) {
                    atomicUpdateShelfQuantity(oldShelfId, -oldQte);
                }
                if (newShelfId != null && !newShelfId.isEmpty() && !isMarker) {
                    atomicUpdateShelfQuantity(newShelfId, newQte);
                }
            } else if (oldShelfId != null && !oldShelfId.isEmpty() && !oldIsMarker && !oldQte.equals(newQte)) {
                int diff = newQte - oldQte;
                atomicUpdateShelfQuantity(oldShelfId, diff);
            }

            return updated;
        }).orElseThrow(() -> new RuntimeException("Equipment not found with id " + id));
    }

    public void deleteEquipment(String id) {
        equipmentRepository.findById(id).ifPresent(equipment -> {
            if (equipment.getShelfId() != null && !equipment.getShelfId().isEmpty()) {
                int qte = equipment.getQte() != null ? equipment.getQte() : 1;
                atomicUpdateShelfQuantity(equipment.getShelfId(), -qte);
            }
            // Generate notification for equipment deletion
            notificationService.createNotification("Equipment Deleted: " + equipment.getEquipmentName(),
                    equipment.getBrand() + " " + equipment.getModel() + " has been removed from inventory",
                    "ERROR", "EQUIPMENT", id);
        });
        equipmentRepository.deleteById(id);
    }

    public void deleteBulk(List<String> ids) {
        if (ids == null || ids.isEmpty())
            return;
        ids.forEach(this::deleteEquipment);
    }

    public List<Equipment> updateBulkBasicInfo(List<String> ids, String name, String brand, String model) {
        if (ids == null || ids.isEmpty())
            return List.of();
        List<Equipment> items = equipmentRepository.findAllById(ids);
        items.forEach(eq -> {
            if (name != null)
                eq.setEquipmentName(name);
            if (brand != null)
                eq.setBrand(brand);
            if (model != null)
                eq.setModel(model);
            eq.setUpdatedAt(LocalDateTime.now());
        });
        List<Equipment> saved = equipmentRepository.saveAll(items);
        
        // Notification for bulk update
        notificationService.createNotification("Bulk Update Completed",
                items.size() + " equipment items were updated successfully",
                "INFO", "EQUIPMENT", null);
                
        return saved;
    }
    private boolean typeRequiresQrCode(String categoryName, String typeName) {
        if (categoryName == null || typeName == null) return true;
        return equipmentCategoryRepository.findByNameIgnoreCase(categoryName.trim())
                .map(cat -> cat.getTypes().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(typeName.trim()))
                        .map(CategoryType::isRequiresQrCode)
                        .findFirst()
                        .orElse(true)) // Fallback if type not found in cat
                .orElse(true); // Fallback if cat not found
    }
}
