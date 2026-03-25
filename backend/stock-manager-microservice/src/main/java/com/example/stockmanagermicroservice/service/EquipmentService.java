package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
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
    private AlertService alertService;

    public List<Equipment> getAllEquipment() {
        return equipmentRepository.findAll();
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
                                "A new " + saved.getCategory() + " (" + saved.getEquipmentName() + ") has been registered in " + saved.getLocation(),
                                "SUCCESS", "INVENTORY", saved.getId());
        
        return saved;
    }

    public Equipment updateEquipment(String id, Equipment equipmentDetails) {
        return equipmentRepository.findById(id).map(equipment -> {
            equipment.setEquipmentName(equipmentDetails.getEquipmentName());
            equipment.setBrand(equipmentDetails.getBrand());
            equipment.setModel(equipmentDetails.getModel());
            equipment.setSerialNumber(equipmentDetails.getSerialNumber());
            equipment.setCategory(equipmentDetails.getCategory());
            equipment.setSupplier(equipmentDetails.getSupplier());
            
            if (equipmentDetails.getLocation() != null && !equipmentDetails.getLocation().equals(equipment.getLocation())) {
                equipment.setLocation(equipmentDetails.getLocation());
                equipment.setLocationChangeAt(LocalDateTime.now());
                equipment.setLocationChanged(true);
            }
            
            equipment.setPurchaseDate(equipmentDetails.getPurchaseDate());
            equipment.setWarrantyExpiration(equipmentDetails.getWarrantyExpiration());
            equipment.setPurchasePrice(equipmentDetails.getPurchasePrice());
            equipment.setIcon(equipmentDetails.getIcon());
            equipment.setNote(equipmentDetails.getNote());
            equipment.setDepartment(equipmentDetails.getDepartment());
            equipment.setCreatedBy(equipmentDetails.getCreatedBy());
            
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
            // Preserve QR code
            return equipmentRepository.save(equipment);
        }).orElseThrow(() -> new RuntimeException("Equipment not found with id " + id));
    }

    public void deleteEquipment(String id) {
        equipmentRepository.deleteById(id);
    }
}
