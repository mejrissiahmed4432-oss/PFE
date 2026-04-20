package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.CategoryType;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EquipmentService {

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private AlertService alertService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ShelfService shelfService;

    @Autowired
    private EquipmentCategoryRepository equipmentCategoryRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAllExcludingFiles();
    }

    public List<Equipment> getEquipmentByShelfId(String shelfId) {
        return equipmentRepository.findByShelfIdExcludingFiles(shelfId);
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
        return equipmentRepository.findByIdExcludingFiles(id);
    }

    /** Fetches the full Equipment object INCLUDING file data. Use only for file download endpoints. */
    public Optional<Equipment> getEquipmentFiles(String id) {
        return equipmentRepository.findById(id);
    }

    public Equipment createEquipment(Equipment equipment) {
        // If no serial number is provided, check if an identical item already exists to merge quantity
        if (equipment.getSerialNumber() == null || equipment.getSerialNumber().trim().isEmpty()) {
            Query query = new Query();
            query.addCriteria(Criteria.where("equipmentName").is(equipment.getEquipmentName()));
            query.addCriteria(Criteria.where("brand").is(equipment.getBrand()));
            query.addCriteria(Criteria.where("type").is(equipment.getType()));
            query.addCriteria(Criteria.where("category").is(equipment.getCategory()));
            if (equipment.getSpecification() != null) {
                query.addCriteria(Criteria.where("specification").is(equipment.getSpecification()));
            }
            // Also ensure it has no serial number
            query.addCriteria(new Criteria().orOperator(
                Criteria.where("serialNumber").is(null),
                Criteria.where("serialNumber").is("")
            ));

            Equipment existing = mongoTemplate.findOne(query, Equipment.class);
            if (existing != null) {
                System.out.println("Found existing matching equipment (ID: " + existing.getId() + "). Merging quantity.");
                int oldQte = existing.getQte() != null ? existing.getQte() : 0;
                int newAddQte = equipment.getQte() != null ? equipment.getQte() : 1;
                existing.setQte(oldQte + newAddQte);
                existing.setUpdatedAt(LocalDateTime.now());
                
                // Update status if it was out of stock
                if (existing.getQte() > 0 && ("Out of stock".equalsIgnoreCase(existing.getStatus()) || existing.getStatus() == null)) {
                    existing.setStatus("Available");
                    // If it was in OUT_OF_STOCK shelf, move it back to the requested shelf if provided
                    if ("OUT_OF_STOCK".equals(existing.getShelfId()) && equipment.getShelfId() != null && !equipment.getShelfId().isEmpty()) {
                        existing.setShelfId(equipment.getShelfId());
                    }
                }
                
                Equipment saved = equipmentRepository.save(existing);
                
                // Update shelf quantity for the existing item
                if (saved.getShelfId() != null && !saved.getShelfId().isEmpty() && !"OUT_OF_STOCK".equals(saved.getShelfId())) {
                    atomicUpdateShelfQuantity(saved.getShelfId(), newAddQte);
                }
                
                return saved;
            }
        }

        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());

        // Respect the "Requires QR Code" setting from categories, unless overridden by
        // frontend
        if (equipment.getQrCode() != null && "NONE".equals(equipment.getQrCode())) {
            // Frontend explicitly chose NO QR CODE.
            equipment.setQrCode(null);
        } else if (equipment.getQrCode() != null && !equipment.getQrCode().isEmpty()) {
            // User explicitly requested QR code generation on the form, keep the generated
            // code
        } else if (typeRequiresQrCode(equipment.getCategory(), equipment.getType())) {
            // Type requires a QR code, generate one if missing
            if (equipment.getQrCode() == null || equipment.getQrCode().isEmpty()) {
                equipment.setQrCode("QR-" + System.currentTimeMillis());
            }
        } else {
            // Type doesn't require it and user didn't request it, ensure it's null
            equipment.setQrCode(null);
        }

        if (equipment.getQte() != null && equipment.getQte() > 0) {
            if (equipment.getStatus() == null || equipment.getStatus().isEmpty() || "Out of stock".equalsIgnoreCase(equipment.getStatus())) {
                equipment.setStatus("Available");
            }
        } else if (equipment.getQte() != null && equipment.getQte() == 0) {
            equipment.setStatus("Out of stock");
        }
        
        if (equipment.getStatus() == null || equipment.getStatus().isEmpty()) {
            equipment.setStatus("Available");
        }
        Equipment saved = equipmentRepository.save(equipment);

        // Generate notification for new equipment
        String creator = (saved.getCreatedBy() != null && !saved.getCreatedBy().isEmpty()) ? saved.getCreatedBy()
                : "System";
        notificationService.createNotification("New " + saved.getType() + " Added",
                "New " + saved.getType() + " " + saved.getBrand() + " " + saved.getModel() + " added by " + creator,
                "SUCCESS", "EQUIPMENT", saved.getId(), null, "STOCK_MANAGER");

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
            String oldName = equipment.getEquipmentName();
            String oldShelfId = equipment.getShelfId();
            Integer oldQte = equipment.getQte() != null ? equipment.getQte() : 1;

            String status = equipmentDetails.getStatus();
            String newShelfId = equipmentDetails.getShelfId();
            Integer newQte = equipmentDetails.getQte() != null ? equipmentDetails.getQte() : oldQte;

            // Automatic Redirection Logic
            if (newQte != null && newQte > 0 && "Out of stock".equalsIgnoreCase(status)) {
                status = "Available";
            } else if (newQte != null && newQte == 0) {
                status = "Out of stock";
            }

            if ("Maintenance".equals(status)) {
                newShelfId = "MAINTENANCE_AREA";
            } else if ("Broken".equals(status)) {
                newShelfId = "SCRAP_YARD";
            } else if ("Out of Stock".equalsIgnoreCase(status)) {
                newShelfId = "OUT_OF_STOCK";
            }

            if (newShelfId != null && !newShelfId.equals(oldShelfId)) {
                equipment.setLocationChangeAt(LocalDateTime.now());
                equipment.setLocationChanged(true);

                // Notification for location change
                notificationService.createNotification("Location Changed: " + equipment.getEquipmentName(),
                        "Equipment moved from shelf " + (oldShelfId != null ? oldShelfId : "N/A") + " to " + newShelfId,
                        "INFO", "EQUIPMENT", equipment.getId(), null, "STOCK_MANAGER");
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

            // File Documents (Conditional update to avoid wiping un-fetched data)
            if (equipmentDetails.getInvoiceFileName() != null) {
                if ("DELETE".equals(equipmentDetails.getInvoiceFileName())) {
                    equipment.setInvoiceFileName(null);
                    equipment.setInvoiceFileData(null);
                } else {
                    equipment.setInvoiceFileName(equipmentDetails.getInvoiceFileName());
                    // Keep existing if new data wasn't provided but name is there
                    if (equipmentDetails.getInvoiceFileData() != null) {
                        equipment.setInvoiceFileData(equipmentDetails.getInvoiceFileData());
                    }
                }
            }

            if (equipmentDetails.getWarrantyFileName() != null) {
                if ("DELETE".equals(equipmentDetails.getWarrantyFileName())) {
                    equipment.setWarrantyFileName(null);
                    equipment.setWarrantyFileData(null);
                } else {
                    equipment.setWarrantyFileName(equipmentDetails.getWarrantyFileName());
                    if (equipmentDetails.getWarrantyFileData() != null) {
                        equipment.setWarrantyFileData(equipmentDetails.getWarrantyFileData());
                    }
                }
            }

            // Device Specifications
            equipment.setCpu(equipmentDetails.getCpu());
            equipment.setRam(equipmentDetails.getRam());
            equipment.setStorage(equipmentDetails.getStorage());
            equipment.setGraphicsCard(equipmentDetails.getGraphicsCard());
            equipment.setOperatingSystem(equipmentDetails.getOperatingSystem());
            equipment.setSpecification(equipmentDetails.getSpecification());

            // Determine if we need to clear QR code explicitly
            boolean clearQrCode = false;
            if (equipmentDetails.getQrCode() != null) {
                if ("NONE".equals(equipmentDetails.getQrCode()) || equipmentDetails.getQrCode().isEmpty()) {
                    // Explicit request to remove QR code
                    equipment.setQrCode(null);
                    clearQrCode = true;
                } else {
                    equipment.setQrCode(equipmentDetails.getQrCode());
                }
            } else {
                // qrCode not specified in update — keep existing
            }

            equipment.setUpdatedAt(LocalDateTime.now());
            Equipment updated = equipmentRepository.save(equipment);

            // Sync alerts if name changed (Deprecated - historical alerts act as point-in-time snapshot)

            // If QR code was explicitly removed, force $unset in MongoDB to guarantee field
            // removal
            if (clearQrCode) {
                Query query = new Query(Criteria.where("_id").is(id));
                Update unsetUpdate = new Update().unset("qrCode");
                mongoTemplate.updateFirst(query, unsetUpdate, Equipment.class);
            }

            // Generate notification for equipment update
            String updater = (updated.getCreatedBy() != null && !updated.getCreatedBy().isEmpty())
                    ? updated.getCreatedBy()
                    : "System";
            notificationService.createNotification("Equipment Updated: " + updated.getEquipmentName(),
                    updated.getBrand() + " " + updated.getModel() + " was updated by " + updater,
                    "INFO", "EQUIPMENT", updated.getId(), null, "STOCK_MANAGER");

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
                    "INFO", "EQUIPMENT", id, null, "STOCK_MANAGER");
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
                "INFO", "EQUIPMENT", null, null, "STOCK_MANAGER");

        return saved;
    }

    private boolean typeRequiresQrCode(String categoryName, String typeName) {
        if (categoryName == null || typeName == null)
            return true;
        return equipmentCategoryRepository.findByNameIgnoreCase(categoryName.trim())
                .map(cat -> cat.getTypes().stream()
                        .filter(t -> t.getName().equalsIgnoreCase(typeName.trim()))
                        .map(CategoryType::isRequiresQrCode)
                        .findFirst()
                        .orElse(true)) // Fallback if type not found in cat
                .orElse(true); // Fallback if cat not found
    }

    public void consumeParts(List<com.example.stockmanagermicroservice.controller.EquipmentController.PartConsumeRequest> requests) {
        for (com.example.stockmanagermicroservice.controller.EquipmentController.PartConsumeRequest req : requests) {
            System.out.println("Processing consume request: name=" + req.name + ", type=" + req.type + ", id=" + req.equipmentId + ", qty=" + req.qty);
            Query query = new Query();
            if (req.equipmentId != null && !req.equipmentId.isEmpty()) {
                query.addCriteria(Criteria.where("id").is(req.equipmentId));
            } else {
                if (req.name != null && !req.name.isEmpty()) {
                    query.addCriteria(Criteria.where("equipmentName").regex("^" + req.name + "$", "i"));
                }
                if (req.brand != null && !req.brand.isEmpty()) {
                    query.addCriteria(Criteria.where("brand").regex("^" + req.brand + "$", "i"));
                }
                if (req.type != null && !req.type.isEmpty()) {
                    query.addCriteria(Criteria.where("type").regex("^" + req.type + "$", "i"));
                }
                if (req.specification != null && !req.specification.isEmpty()) {
                    query.addCriteria(Criteria.where("specification").is(req.specification));
                }
            }
            
            List<Equipment> matches = mongoTemplate.find(query, Equipment.class);
            System.out.println("Found " + matches.size() + " potential matches for consumption.");

            int remainingToConsume = req.qty;
            for (Equipment eq : matches) {
                if (remainingToConsume <= 0) break;
                
                int current = eq.getQte() != null ? eq.getQte() : 1;
                if (current <= 0) continue; // Skip out of stock items
                
                int deduct = Math.min(current, remainingToConsume);
                int newQte = current - deduct;
                remainingToConsume -= deduct;
                
                System.out.println("Updating equipment " + eq.getId() + " (" + eq.getEquipmentName() + "): " + current + " -> " + newQte);
                
                eq.setQte(newQte);
                if (newQte <= 0) {
                    eq.setQte(0);
                    eq.setStatus("Out of stock");
                }
                equipmentRepository.save(eq);
                
                if (eq.getShelfId() != null && !eq.getShelfId().isEmpty() && 
                    !"MAINTENANCE_AREA".equals(eq.getShelfId()) && 
                    !"SCRAP_YARD".equals(eq.getShelfId()) && 
                    !"OUT_OF_STOCK".equals(eq.getShelfId())) {
                     atomicUpdateShelfQuantity(eq.getShelfId(), -deduct);
                }
            }
            if (remainingToConsume > 0) {
                System.out.println("Warning: Could only consume " + (req.qty - remainingToConsume) + " out of " + req.qty + " requested items.");
            }
        }
    }
}
