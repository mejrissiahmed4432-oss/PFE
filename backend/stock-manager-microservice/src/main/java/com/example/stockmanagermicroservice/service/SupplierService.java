package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.Supplier;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(String id) {
        return supplierRepository.findById(id);
    }

    public Supplier createSupplier(Supplier supplier) {
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        return supplierRepository.save(supplier);
    }

    public Supplier updateSupplier(String id, Supplier supplierDetails) {
        return supplierRepository.findById(id).map(supplier -> {
            String oldName = supplier.getName();
            String newName = supplierDetails.getName();

            supplier.setName(newName);
            supplier.setCompanyName(supplierDetails.getCompanyName());
            supplier.setAddress(supplierDetails.getAddress());
            supplier.setPhoneNumber(supplierDetails.getPhoneNumber());
            supplier.setEmail(supplierDetails.getEmail());
            supplier.setWebsite(supplierDetails.getWebsite());
            supplier.setCategory(supplierDetails.getCategory());
            supplier.setContactPerson(supplierDetails.getContactPerson());
            supplier.setNote(supplierDetails.getNote());
            supplier.setUpdatedAt(LocalDateTime.now());
            
            Supplier updatedSupplier = supplierRepository.save(supplier);

            // Cascade update to Equipment if name changed - using ID for safety
            if (oldName != null && !oldName.equals(newName)) {
                List<Equipment> relatedEquipments = equipmentRepository.findBySupplierId(id);
                if (!relatedEquipments.isEmpty()) {
                    for (Equipment eq : relatedEquipments) {
                        eq.setSupplier(newName);
                    }
                    equipmentRepository.saveAll(relatedEquipments);
                }
            }

            return updatedSupplier;
        }).orElseThrow(() -> new RuntimeException("Supplier not found with id " + id));
    }

    public void deleteSupplier(String id) {
        supplierRepository.deleteById(id);
    }
}
