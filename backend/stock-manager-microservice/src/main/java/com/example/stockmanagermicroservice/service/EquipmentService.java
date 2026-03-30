package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.Shelf;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.ShelfRepository;
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
    private AlertService alertService;

    @Autowired
    private ShelfRepository shelfRepository;

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
    }

    public List<Equipment> getEquipmentByShelfId(String shelfId) {
        return equipmentRepository.findByShelfId(shelfId);
    }

    private void updateShelfStatus(Shelf shelf) {
        if (shelf.getCurrentQte() == 0) {
            shelf.setStatus("EMPTY");
        } else if (shelf.getCurrentQte() >= shelf.getMaxQte()) {
            shelf.setStatus("FULL");
        } else {
            shelf.setStatus("NORMAL");
        }
    }

    public Optional<Equipment> getEquipmentById(String id) {
        return equipmentRepository.findById(id);
    }

    public Equipment createEquipment(Equipment equipment) {
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());
        // Generate a simple pseudo QR code string if none provided
        if (equipment.getQrCode() == null || equipment.getQrCode().isEmpty()) {
            equipment.setQrCode("QR-" + System.currentTimeMillis());
        }
        Equipment saved = equipmentRepository.save(equipment);
        
        // Generate alert for new equipment
        alertService.createAlert("New Equipment Added", 
                                "A new " + saved.getType() + " (" + saved.getEquipmentName() + ") has been registered in shelf " + saved.getShelfId(),
                                "SUCCESS", "INVENTORY", saved.getId());
        
        // Update shelf quantity
        if (saved.getShelfId() != null && !saved.getShelfId().isEmpty()) {
            shelfRepository.findById(saved.getShelfId()).ifPresent(shelf -> {
                int qte = saved.getQte() != null ? saved.getQte() : 1;
                shelf.setCurrentQte(shelf.getCurrentQte() + qte);
                updateShelfStatus(shelf);
                shelfRepository.save(shelf);
            });
        }
        
        return saved;
    }

    public Equipment updateEquipment(String id, Equipment equipmentDetails) {
        return equipmentRepository.findById(id).map(equipment -> {
            String oldShelfId = equipment.getShelfId();
            Integer oldQte = equipment.getQte() != null ? equipment.getQte() : 1;
            String newShelfId = equipmentDetails.getShelfId();
            Integer newQte = equipmentDetails.getQte() != null ? equipmentDetails.getQte() : 1;

            if (newShelfId != null && !newShelfId.equals(oldShelfId)) {
                equipment.setLocationChangeAt(LocalDateTime.now());
                equipment.setLocationChanged(true);
            }
            equipment.setEquipmentName(equipmentDetails.getEquipmentName());
            equipment.setBrand(equipmentDetails.getBrand());
            equipment.setModel(equipmentDetails.getModel());
            equipment.setSerialNumber(equipmentDetails.getSerialNumber());
            equipment.setCategory(equipmentDetails.getCategory());
            equipment.setSupplier(equipmentDetails.getSupplier());
            equipment.setType(equipmentDetails.getType());
            equipment.setQte(equipmentDetails.getQte());
            equipment.setShelfId(equipmentDetails.getShelfId());
            
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
            
            if (equipmentDetails.getQrCode() != null && !equipmentDetails.getQrCode().isEmpty()) {
                equipment.setQrCode(equipmentDetails.getQrCode());
            }

            equipment.setUpdatedAt(LocalDateTime.now());
            Equipment updated = equipmentRepository.save(equipment);

            boolean shelfChanged = (oldShelfId == null && newShelfId != null && !newShelfId.isEmpty()) ||
                                   (oldShelfId != null && oldShelfId.isEmpty() && newShelfId != null && !newShelfId.isEmpty()) ||
                                   (oldShelfId != null && !oldShelfId.isEmpty() && newShelfId == null) ||
                                   (oldShelfId != null && !oldShelfId.isEmpty() && newShelfId != null && !oldShelfId.equals(newShelfId));

            if (shelfChanged) {
                if (oldShelfId != null && !oldShelfId.isEmpty()) {
                    shelfRepository.findById(oldShelfId).ifPresent(shelf -> {
                        shelf.setCurrentQte(Math.max(0, shelf.getCurrentQte() - oldQte));
                        updateShelfStatus(shelf);
                        shelfRepository.save(shelf);
                    });
                }
                if (newShelfId != null && !newShelfId.isEmpty()) {
                    shelfRepository.findById(newShelfId).ifPresent(shelf -> {
                        shelf.setCurrentQte(shelf.getCurrentQte() + newQte);
                        updateShelfStatus(shelf);
                        shelfRepository.save(shelf);
                    });
                }
            } else if (oldShelfId != null && !oldShelfId.isEmpty() && !oldQte.equals(newQte)) {
                shelfRepository.findById(oldShelfId).ifPresent(shelf -> {
                    int diff = newQte - oldQte;
                    shelf.setCurrentQte(Math.max(0, shelf.getCurrentQte() + diff));
                    updateShelfStatus(shelf);
                    shelfRepository.save(shelf);
                });
            }

            return updated;
        }).orElseThrow(() -> new RuntimeException("Equipment not found with id " + id));
    }

    public void deleteEquipment(String id) {
        equipmentRepository.findById(id).ifPresent(equipment -> {
            if (equipment.getShelfId() != null && !equipment.getShelfId().isEmpty()) {
                shelfRepository.findById(equipment.getShelfId()).ifPresent(shelf -> {
                    int qte = equipment.getQte() != null ? equipment.getQte() : 1;
                    shelf.setCurrentQte(Math.max(0, shelf.getCurrentQte() - qte));
                    updateShelfStatus(shelf);
                    shelfRepository.save(shelf);
                });
            }
        });
        equipmentRepository.deleteById(id);
    }
}
