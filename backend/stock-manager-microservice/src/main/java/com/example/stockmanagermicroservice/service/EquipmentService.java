package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.CategoryType;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.model.LifecycleEntry;
import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
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

    /**
     * On startup, automatically remove any stale "Out of Stock" records that share
     * a serial number with an "Allocated" record. These were created by the old
     * allocateParts bug and should be cleaned up once.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartupCleanup() {
        int removedDuplicates = cleanupDuplicateOutOfStockRecords();
        if (removedDuplicates > 0) {
            System.out.println("[EquipmentService] Startup cleanup: removed " + removedDuplicates + " stale Out-of-Stock duplicate(s).");
        }
        
        // Wipe all parts for moetez as requested
        // int removedMoetez = cleanupTechnicianParts("moetez");
        // if (removedMoetez > 0) {
        //     System.out.println("[EquipmentService] Cleanup: removed " + removedMoetez + " parts allocated to 'moetez'.");
        // }

        // Recalculate all shelf quantities to ensure they are in sync with actual equipment documents
        syncAllShelfQuantities();
        
        // Repair legacy data: ensure installed/allocated parts have no shelfId
        repairLegacyInstalledParts();
    }

    private int cleanupTechnicianParts(String technicianId) {
        Query query = new Query(new Criteria().orOperator(
            Criteria.where("allocatedToTechnicianId").is(technicianId),
            Criteria.where("allocatedToTechnicianId").is("moetez@gmail.com"),
            Criteria.where("allocatedToTechnicianId").is("69d5170dfd668941de3716b3"),
            Criteria.where("allocatedToTechnicianName").regex("Moetez", "i")
        ));
        List<Equipment> toDelete = mongoTemplate.find(query, Equipment.class);
        int count = toDelete.size();
        for (Equipment eq : toDelete) {
            equipmentRepository.delete(eq);
        }
        return count;
    }

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

    /**
     * Fetches the full Equipment object INCLUDING file data. Use only for file
     * download endpoints.
     */
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
            if (equipment.getSpecifications() != null && !equipment.getSpecifications().isEmpty()) {
                equipment.getSpecifications().forEach((k, v) -> {
                    query.addCriteria(Criteria.where("specifications." + k).is(v));
                });
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
                
                handleStockAlerts(saved, oldQte, saved.getQte() != null ? saved.getQte() : 0);

                
                return saved;
            }
        }

        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());

        // Respect the "Requires QR Code" setting from categories, unless overridden by
        // frontend
        processEquipmentBeforeSave(equipment);
        Equipment saved = equipmentRepository.save(equipment);
        
        addLifecycleEntry(saved, "Created", "Equipment added to inventory", saved.getCreatedBy() != null ? saved.getCreatedBy() : "System");

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

        handleStockAlerts(saved, 0, saved.getQte() != null ? saved.getQte() : 1);


        return saved;
    }

    public List<Equipment> createBulkEquipment(List<Equipment> equipments) {
        if (equipments == null || equipments.isEmpty()) return java.util.Collections.emptyList();

        java.util.List<Equipment> savedEquipments = new java.util.ArrayList<>();
        String type = equipments.get(0).getType();
        String creator = equipments.get(0).getCreatedBy();
        if (creator == null || creator.isEmpty()) creator = "System";

        for (Equipment eq : equipments) {
            processEquipmentBeforeSave(eq);
            Equipment saved = equipmentRepository.save(eq);
            addLifecycleEntry(saved, "Created", "Equipment added to inventory (Bulk)", creator);
            
            // Update shelf quantity
            if (saved.getShelfId() != null && !saved.getShelfId().isEmpty()) {
                atomicUpdateShelfQuantity(saved.getShelfId(), saved.getQte() != null ? saved.getQte() : 1);
            }
            handleStockAlerts(saved, 0, saved.getQte() != null ? saved.getQte() : 1);
            savedEquipments.add(saved);
        }

        // Generate SINGLE notification for the whole batch
        String title = equipments.size() > 1 ? equipments.size() + " " + type + "s Added" : "New " + type + " Added";
        String message = equipments.size() > 1 
            ? equipments.size() + " new " + type + "s added to inventory by " + creator
            : "New " + type + " added to inventory by " + creator;

        notificationService.createNotification(title, message, "SUCCESS", "EQUIPMENT", 
            savedEquipments.get(0).getId(), null, "STOCK_MANAGER");

        return savedEquipments;
    }

    private void processEquipmentBeforeSave(Equipment equipment) {
        if (equipment.getQrCode() != null && "NONE".equals(equipment.getQrCode())) {
            // Frontend explicitly chose NO QR CODE.
            equipment.setQrCode(null);
        } else if (equipment.getQrCode() != null && !equipment.getQrCode().isEmpty()) {
            // User explicitly requested QR code generation on the form, keep the generated code
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
    }

    private void addLifecycleEntry(Equipment eq, String status, String description, String actor) {
        if (eq.getLifecycle() == null) {
            eq.setLifecycle(new java.util.ArrayList<>());
        }
        eq.getLifecycle().add(new LifecycleEntry(status, LocalDateTime.now(), description, actor));
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
        return equipmentRepository.findByIdExcludingFiles(id).map(equipment -> {
            String oldName = equipment.getEquipmentName();
            String oldShelfId = equipment.getShelfId();
            Integer oldQte = equipment.getQte() != null ? equipment.getQte() : 1;

            // Store original assignment state before mutation
            boolean wasAssigned = equipment.getAssignedToEquipmentId() != null || equipment.getAssignedToEquipmentName() != null;

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
            } else if ("Broken".equals(status) || "Unrepairable".equals(status)) {
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
            String oldStatus = equipment.getStatus();
            String newStatus = status;

            if (newStatus != null && !newStatus.equals(oldStatus)) {
                addLifecycleEntry(equipment, newStatus, "Status manually updated from " + (oldStatus != null ? oldStatus : "None") + " to " + newStatus, equipmentDetails.getCreatedBy() != null ? equipmentDetails.getCreatedBy() : "Stock Manager");
            }

            equipment.setStatus(status);

            equipment.setPurchaseDate(equipmentDetails.getPurchaseDate());
            equipment.setWarrantyExpiration(equipmentDetails.getWarrantyExpiration());
            equipment.setPurchasePrice(equipmentDetails.getPurchasePrice());
            equipment.setInvoiceRef(equipmentDetails.getInvoiceRef());
            equipment.setIcon(equipmentDetails.getIcon());
            equipment.setNote(equipmentDetails.getNote());
            equipment.setDepartment(equipmentDetails.getDepartment());
            equipment.setCreatedBy(equipmentDetails.getCreatedBy());

            // Part installation tracking — persist parent PC assignment
            equipment.setAssignedToEquipmentId(equipmentDetails.getAssignedToEquipmentId());
            equipment.setAssignedToEquipmentName(equipmentDetails.getAssignedToEquipmentName());

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
            equipment.setSpecifications(equipmentDetails.getSpecifications());

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
            }

            equipment.setUpdatedAt(LocalDateTime.now());
            
            // Build dynamic update for light fields
            org.bson.Document document = new org.bson.Document();
            mongoTemplate.getConverter().write(equipment, document);
            
            Update update = new Update();
            for (String key : document.keySet()) {
                if (!key.equals("_id") && !key.equals("invoiceFileData") && !key.equals("warrantyFileData") && !key.equals("_class")) {
                    update.set(key, document.get(key));
                }
            }
            
            if (clearQrCode) {
                update.unset("qrCode");
            }
            
            if (equipmentDetails.getInvoiceFileName() != null && "DELETE".equals(equipmentDetails.getInvoiceFileName())) {
                update.unset("invoiceFileName");
                update.unset("invoiceFileData");
            } else if (equipmentDetails.getInvoiceFileData() != null) {
                update.set("invoiceFileData", equipmentDetails.getInvoiceFileData());
            }
            
            if (equipmentDetails.getWarrantyFileName() != null && "DELETE".equals(equipmentDetails.getWarrantyFileName())) {
                update.unset("warrantyFileName");
                update.unset("warrantyFileData");
            } else if (equipmentDetails.getWarrantyFileData() != null) {
                update.set("warrantyFileData", equipmentDetails.getWarrantyFileData());
            }
            
            // Handle explicit clearing of part PC assignment fields (e.g. Unrepairable or manual status change)
            boolean clearAssignedToEquipment = wasAssigned
                    && equipmentDetails.getAssignedToEquipmentId() == null
                    && equipmentDetails.getAssignedToEquipmentName() == null;

            mongoTemplate.updateFirst(new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)), update, Equipment.class);

            // Explicitly unset PC assignment if cleared (unassigned or unrepairable part detached from PC)
            if (clearAssignedToEquipment) {
                Update clearUpdate = new Update().unset("assignedToEquipmentId").unset("assignedToEquipmentName");
                mongoTemplate.updateFirst(new org.springframework.data.mongodb.core.query.Query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(id)), clearUpdate, Equipment.class);
            }

            Equipment updated = equipment;

            // Sync alerts if name changed (Deprecated - historical alerts act as point-in-time snapshot)

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

            handleStockAlerts(updated, oldQte, newQte);

            return updated;
        }).orElseThrow(() -> new RuntimeException("Equipment not found with id " + id));
    }

    public void deleteEquipment(String id) {
        equipmentRepository.findByIdExcludingFiles(id).ifPresent(equipment -> {
            if (equipment.getShelfId() != null && !equipment.getShelfId().isEmpty()) {
                int qte = equipment.getQte() != null ? equipment.getQte() : 1;
                atomicUpdateShelfQuantity(equipment.getShelfId(), -qte);
            }
            notificationService.createNotification("Equipment Deleted: " + equipment.getEquipmentName(),
                    equipment.getBrand() + " " + equipment.getModel() + " has been removed from inventory",
                    "WARNING", "EQUIPMENT", id, null, "STOCK_MANAGER");
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
        notificationService.createNotification("Group Update Completed",
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

    public void consumeParts(String requesterId, List<com.example.stockmanagermicroservice.controller.EquipmentController.PartConsumeRequest> requests) {
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
                if (req.specifications != null && !req.specifications.isEmpty()) {
                    req.specifications.forEach((k, v) -> {
                        query.addCriteria(Criteria.where("specifications." + k).is(v));
                    });
                }
                if (requesterId != null && !requesterId.isEmpty()) {
                    query.addCriteria(Criteria.where("allocatedToTechnicianId").is(requesterId));
                    query.addCriteria(Criteria.where("status").is("Allocated"));
                }
            }
            
            List<Equipment> matches = mongoTemplate.find(query, Equipment.class);
            System.out.println("Found " + matches.size() + " potential matches for consumption.");

            // Fallback for legacy workflow where parts are consumed directly from Available
            if (matches.isEmpty() && requesterId != null && !requesterId.isEmpty()) {
                System.out.println("No allocated parts found, trying to consume from general stock");
                Query fallbackQuery = new Query();
                if (req.name != null && !req.name.isEmpty()) {
                    fallbackQuery.addCriteria(Criteria.where("equipmentName").regex("^" + req.name + "$", "i"));
                }
                if (req.specifications != null && !req.specifications.isEmpty()) {
                    req.specifications.forEach((k, v) -> {
                        fallbackQuery.addCriteria(Criteria.where("specifications." + k).is(v));
                    });
                }
                fallbackQuery.addCriteria(new Criteria().orOperator(
                    Criteria.where("status").is("Available"),
                    Criteria.where("status").is(null)
                ));
                matches = mongoTemplate.find(fallbackQuery, Equipment.class);
            }

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
                handleStockAlerts(eq, current, newQte);
                
                // Create a new record for the Assigned part
                Equipment assignedPart = cloneEquipment(eq);
                assignedPart.setQte(deduct);
                assignedPart.setStatus("Installed");
                assignedPart.setAssignedToEquipmentName(req.assignedToEquipmentName);
                assignedPart.setAssignedToEquipmentId(req.assignedToEquipmentId);
                assignedPart.setShelfId(""); // Clear shelf so the place returns empty
                
                // Copy lifecycle history to the assigned part and add "Installed" entry
                assignedPart.setLifecycle(new java.util.ArrayList<>(eq.getLifecycle() != null ? eq.getLifecycle() : java.util.Collections.emptyList()));
                addLifecycleEntry(assignedPart, "Installed", "Part installed in machine: " + req.assignedToEquipmentName, requesterId);
                
                equipmentRepository.save(assignedPart);
                
                if (eq.getShelfId() != null && !eq.getShelfId().isEmpty() && 
                    !"MAINTENANCE_AREA".equals(eq.getShelfId()) && 
                    !"SCRAP_YARD".equals(eq.getShelfId()) && 
                    !"OUT_OF_STOCK".equals(eq.getShelfId()) && 
                    !"Allocated".equals(eq.getStatus())) { // Only deduct from shelf if we are consuming from general stock
                     atomicUpdateShelfQuantity(eq.getShelfId(), -deduct);
                }
            }
            if (remainingToConsume > 0) {
                System.out.println("Warning: Could only consume " + (req.qty - remainingToConsume) + " out of " + req.qty + " requested items.");
            }
        }
    }

    public void allocateParts(String technicianId, String technicianName, List<com.example.stockmanagermicroservice.controller.EquipmentController.PartConsumeRequest> requests) {
        for (com.example.stockmanagermicroservice.controller.EquipmentController.PartConsumeRequest req : requests) {
            Query query = new Query();
            if (req.equipmentId != null && !req.equipmentId.isEmpty()) {
                query.addCriteria(Criteria.where("id").is(req.equipmentId));
            } else {
                if (req.name != null && !req.name.isEmpty()) {
                    query.addCriteria(Criteria.where("equipmentName").regex("^" + req.name + "$", "i"));
                }
                if (req.specifications != null && !req.specifications.isEmpty()) {
                    req.specifications.forEach((k, v) -> {
                        query.addCriteria(Criteria.where("specifications." + k).is(v));
                    });
                }
                // Only find available items
                query.addCriteria(new Criteria().orOperator(
                    Criteria.where("status").is("Available"),
                    Criteria.where("status").is(null)
                ));
            }
            List<Equipment> matches = mongoTemplate.find(query, Equipment.class);
            int remainingToAllocate = req.qty;
            for (Equipment eq : matches) {
                if (remainingToAllocate <= 0) break;

                int current = eq.getQte() != null ? eq.getQte() : 1;
                if (current <= 0) continue;

                int deduct = Math.min(current, remainingToAllocate);
                int newQte = current - deduct;
                remainingToAllocate -= deduct;

                if (eq.getShelfId() != null && !eq.getShelfId().isEmpty() &&
                    !"MAINTENANCE_AREA".equals(eq.getShelfId()) &&
                    !"SCRAP_YARD".equals(eq.getShelfId()) &&
                    !"OUT_OF_STOCK".equals(eq.getShelfId())) {
                    atomicUpdateShelfQuantity(eq.getShelfId(), -deduct);
                }

                // If there is leftover stock (partial deduction from a multi-unit record),
                // create a separate record ONLY for the remaining available quantity.
                if (newQte > 0) {
                    Equipment leftover = cloneEquipment(eq);
                    leftover.setQte(newQte);
                    leftover.setStatus("Available");
                    leftover.setAllocatedToTechnicianId(null);
                    leftover.setAllocatedToTechnicianName(null);
                    Equipment savedLeftover = equipmentRepository.save(leftover);
                    handleStockAlerts(savedLeftover, current, newQte);
                } else {
                    handleStockAlerts(eq, current, 0);
                }

                // Update the EXISTING record in-place to Allocated.
                // DO NOT clone — the old code was cloning and saving a new "Allocated" record
                // while setting the original to "Out of Stock", causing duplicate rows.
                eq.setQte(deduct);
                eq.setStatus("Allocated");
                eq.setAllocatedToTechnicianId(technicianId);
                eq.setAllocatedToTechnicianName(technicianName);
                eq.setShelfId(""); // Clear shelf so the place returns empty
                eq.setUpdatedAt(LocalDateTime.now());
                
                addLifecycleEntry(eq, "Allocated", "Part allocated to technician: " + technicianName, "Stock Manager");
                
                equipmentRepository.save(eq);
            }
        }
    }

    public void returnPartToStock(String equipmentId) {
        java.util.Optional<Equipment> optEq = equipmentRepository.findById(equipmentId);
        if (!optEq.isPresent()) {
            System.err.println("returnPartToStock: Equipment not found for id: " + equipmentId);
            throw new RuntimeException("Equipment not found: " + equipmentId);
        }
        Equipment eq = optEq.get();
        System.out.println("returnPartToStock: Found equipment [" + eq.getId() + "] status=" + eq.getStatus() + " name=" + eq.getEquipmentName());
        
        String oldStatus = eq.getStatus();
        
        if ("Allocated".equals(oldStatus) || "Installed".equals(oldStatus) || "Assigned".equals(oldStatus)) {
            List<Shelf> candidateShelves = shelfService.getShelvesByEquipmentType(eq.getType());
            Shelf targetShelf = null;
            int returnQty = eq.getQte() != null ? eq.getQte() : 1;
            for (Shelf s : candidateShelves) {
                int current = s.getCurrentQte() != null ? s.getCurrentQte() : 0;
                int max = s.getMaxQte() != null ? s.getMaxQte() : 0;
                if (current + returnQty <= max) {
                    targetShelf = s;
                    break;
                }
            }
            if (targetShelf == null) {
                throw new RuntimeException("No empty shelf with sufficient capacity found for equipment type: " + eq.getType());
            }
            eq.setShelfId(targetShelf.getId());
        }

        eq.setStatus("Available");
        eq.setAllocatedToTechnicianId(null);
        eq.setAllocatedToTechnicianName(null);
        eq.setUpdatedAt(LocalDateTime.now());

        if (eq.getAssignedToEquipmentId() != null) {
            String parentId = eq.getAssignedToEquipmentId();
            eq.setAssignedToEquipmentId(null);
            eq.setAssignedToEquipmentName(null);
            
            java.util.Optional<Equipment> optParent = equipmentRepository.findById(parentId);
            if (optParent.isPresent()) {
                Equipment parent = optParent.get();
                if (parent.getSpecifications() != null) {
                    String partType = (eq.getType() != null ? eq.getType() : "").toLowerCase().trim();
                    String specKey = null;
                    for (String key : parent.getSpecifications().keySet()) {
                        if (key.toLowerCase().trim().equals(partType)) {
                            specKey = key;
                            break;
                        }
                    }
                    if (specKey != null) {
                        parent.getSpecifications().remove(specKey);
                    }
                }
                addLifecycleEntry(parent, "Component Uninstalled", "Uninstalled component: " + eq.getEquipmentName() + " (S/N: " + (eq.getSerialNumber() != null ? eq.getSerialNumber() : "N/A") + ")", "System");
                equipmentRepository.save(parent);
            }
        }
        
        addLifecycleEntry(eq, "Returned to Stock", "Part returned to central stock", "Technician");
        
        equipmentRepository.save(eq);
        System.out.println("returnPartToStock: Status changed from [" + oldStatus + "] to [Available]");
        
        // Restore shelf quantity if not a virtual shelf
        if (("Allocated".equals(oldStatus) || "Installed".equals(oldStatus) || "Assigned".equals(oldStatus)) &&
            eq.getShelfId() != null && !eq.getShelfId().isEmpty() &&
            !"MAINTENANCE_AREA".equals(eq.getShelfId()) &&
            !"SCRAP_YARD".equals(eq.getShelfId()) &&
            !"OUT_OF_STOCK".equals(eq.getShelfId())) {
            atomicUpdateShelfQuantity(eq.getShelfId(), eq.getQte() != null ? eq.getQte() : 1);
        }
    }

    
    private Equipment cloneEquipment(Equipment eq) {
        Equipment clone = new Equipment();
        clone.setEquipmentName(eq.getEquipmentName());
        clone.setBrand(eq.getBrand());
        clone.setModel(eq.getModel());
        clone.setSerialNumber(eq.getSerialNumber());
        clone.setCategory(eq.getCategory());
        clone.setType(eq.getType());
        clone.setSupplier(eq.getSupplier());
        clone.setSupplierId(eq.getSupplierId());
        clone.setShelfId(eq.getShelfId());
        clone.setDepartment(eq.getDepartment());
        clone.setPurchaseDate(eq.getPurchaseDate());
        clone.setWarrantyExpiration(eq.getWarrantyExpiration());
        clone.setPurchasePrice(eq.getPurchasePrice());
        clone.setInvoiceRef(eq.getInvoiceRef());
        clone.setInvoiceFileName(eq.getInvoiceFileName());
        clone.setInvoiceFileData(eq.getInvoiceFileData());
        clone.setWarrantyFileName(eq.getWarrantyFileName());
        clone.setWarrantyFileData(eq.getWarrantyFileData());
        clone.setQrCode(eq.getQrCode());
        clone.setIcon(eq.getIcon());
        clone.setNote(eq.getNote());
        if (eq.getSpecifications() != null) {
            clone.setSpecifications(new java.util.HashMap<>(eq.getSpecifications()));
        }
        clone.setCreatedAt(LocalDateTime.now());
        clone.setUpdatedAt(LocalDateTime.now());
        clone.setCreatedBy(eq.getCreatedBy());
        return clone;
    }

    /**
     * Cleans up "Out of Stock" records that are duplicates of an "Allocated" record
     * with the same serial number. These were created by the old allocateParts bug that
     * set the original record to "Out of Stock" then saved a cloned "Allocated" record.
     *
     * @return the number of duplicate records removed
     */
    public int cleanupDuplicateOutOfStockRecords() {
        // 1. Find all Allocated records that have a non-empty serial number
        Query allocatedQuery = new Query();
        allocatedQuery.addCriteria(Criteria.where("status").is("Allocated"));
        allocatedQuery.addCriteria(Criteria.where("serialNumber").exists(true).ne("").ne(null));
        List<Equipment> allocated = mongoTemplate.find(allocatedQuery, Equipment.class);

        int removed = 0;
        for (Equipment alloc : allocated) {
            String serial = alloc.getSerialNumber();
            if (serial == null || serial.trim().isEmpty()) continue;

            // 2. Find any "Out of Stock" record with the same serial number but a different ID
            Query dupQuery = new Query();
            dupQuery.addCriteria(Criteria.where("serialNumber").is(serial.trim()));
            dupQuery.addCriteria(Criteria.where("status").is("Out of stock"));
            dupQuery.addCriteria(Criteria.where("_id").ne(alloc.getId()));

            List<Equipment> duplicates = mongoTemplate.find(dupQuery, Equipment.class);
            for (Equipment dup : duplicates) {
                equipmentRepository.deleteById(dup.getId());
                removed++;
            }
        }
        return removed;
    }

    /**
     * Finds any equipment marked as Installed, Assigned, or Allocated that still
     * has a shelfId, and clears it. This fixes legacy data issues.
     */
    public int repairLegacyInstalledParts() {
        Query query = new Query(Criteria.where("status").in("Installed", "Assigned", "Allocated")
                                     .and("shelfId").ne(""));
        Update update = new Update().set("shelfId", "");
        return (int) mongoTemplate.updateMulti(query, update, Equipment.class).getModifiedCount();
    }

    private void handleStockAlerts(Equipment eq, int oldQte, int newQte) {
        if (oldQte == newQte) return;

        int LOW_STOCK_THRESHOLD = 5; // Configurable threshold
        String outOfStockKey = "OUT_OF_STOCK_" + eq.getId();
        String lowStockKey = "LOW_STOCK_" + eq.getId();

        if (newQte == 0) {
            alertService.triggerSystemAlert(outOfStockKey, "OUT_OF_STOCK", "HIGH", "ROLE", "STOCK_MANAGER", "Out of Stock: " + eq.getEquipmentName(), eq.getEquipmentName() + " is now completely out of stock.");
            alertService.resolveSystemAlert(lowStockKey);
        } else if (newQte > 0 && newQte <= LOW_STOCK_THRESHOLD) {
            alertService.triggerSystemAlert(lowStockKey, "LOW_STOCK", "MEDIUM", "ROLE", "STOCK_MANAGER", "Low Stock: " + eq.getEquipmentName(), eq.getEquipmentName() + " stock is running low (" + newQte + " remaining).");
            if (oldQte == 0) alertService.resolveSystemAlert(outOfStockKey);
        } else if (newQte > LOW_STOCK_THRESHOLD) {
            alertService.resolveSystemAlert(outOfStockKey);
            alertService.resolveSystemAlert(lowStockKey);
        }
    }


    /**
     * Recalculates the current quantity for all shelves by summing up all 
     * 'Available' equipment records currently assigned to them.
     */
    public void syncAllShelfQuantities() {
        System.out.println("[EquipmentService] Starting shelf quantity synchronization...");
        List<Shelf> allShelves = shelfService.getAllShelves();
        int totalSynced = 0;

        for (Shelf shelf : allShelves) {
            String shelfId = shelf.getId();
            
            // Count all 'Available' items on this shelf
            // Note: We sum up the 'qte' field of each document
            Query query = new Query(Criteria.where("shelfId").is(shelfId).and("status").is("Available"));
            List<Equipment> itemsOnShelf = mongoTemplate.find(query, Equipment.class);
            
            int actualCount = 0;
            for (Equipment eq : itemsOnShelf) {
                actualCount += (eq.getQte() != null ? eq.getQte() : 1);
            }

            // Update the shelf if the count is different
            if (shelf.getCurrentQte() == null || shelf.getCurrentQte() != actualCount) {
                shelf.setCurrentQte(actualCount);
                shelfService.updateShelfStatus(shelf);
                
                // Save the corrected quantity atomically
                Query shelfQuery = new Query(Criteria.where("id").is(shelfId));
                Update shelfUpdate = new Update().set("currentQte", actualCount).set("status", shelf.getStatus());
                mongoTemplate.updateFirst(shelfQuery, shelfUpdate, Shelf.class);
                totalSynced++;
            }
        }
        System.out.println("[EquipmentService] Shelf sync complete. Updated " + totalSynced + " out-of-sync shelves.");
    }
}
