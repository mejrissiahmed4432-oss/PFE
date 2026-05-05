package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.Supplier;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
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
public class SupplierService {

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MongoTemplate mongoTemplate;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public Optional<Supplier> getSupplierById(String id) {
        return supplierRepository.findById(id);
    }

    public Supplier createSupplier(Supplier supplier) {
        supplier.setCreatedAt(LocalDateTime.now());
        supplier.setUpdatedAt(LocalDateTime.now());
        Supplier saved = supplierRepository.save(supplier);
        notificationService.createNotification("New Supplier Added: " + saved.getCompanyName(),
                "Supplier " + saved.getCompanyName() + " has been registered",
                "SUCCESS", "SUPPLIER", saved.getId(), null, "STOCK_MANAGER");
        return saved;
    }

    public Supplier updateSupplier(String id, Supplier supplierDetails) {
        return supplierRepository.findById(id).map(supplier -> {
            String oldCompanyName = supplier.getCompanyName();
            String newCompanyName = supplierDetails.getCompanyName();

            supplier.setCompanyName(newCompanyName);
            supplier.setRating(supplierDetails.getRating());
            supplier.setAddress(supplierDetails.getAddress());
            supplier.setPhoneNumber(supplierDetails.getPhoneNumber());
            supplier.setEmail(supplierDetails.getEmail());
            supplier.setWebsite(supplierDetails.getWebsite());
            supplier.setCategory(supplierDetails.getCategory());
            supplier.setContactPerson(supplierDetails.getContactPerson());
            supplier.setNote(supplierDetails.getNote());
            supplier.setUpdatedAt(LocalDateTime.now());
            
            Supplier updatedSupplier = supplierRepository.save(supplier);

            // Generate notification for supplier update
            notificationService.createNotification("Supplier Updated: " + updatedSupplier.getCompanyName(),
                    "Supplier " + updatedSupplier.getCompanyName() + " details have been modified",
                    "INFO", "SUPPLIER", updatedSupplier.getId(), null, "STOCK_MANAGER");

            // Cascade update to Equipment if companyName changed — using atomic update (no file data loaded)
            if (oldCompanyName != null && !oldCompanyName.equals(newCompanyName)) {
                Query q = new Query(Criteria.where("supplierId").is(id));
                Update u = new Update().set("supplier", newCompanyName);
                mongoTemplate.updateMulti(q, u, Equipment.class);
            }

            return updatedSupplier;
        }).orElseThrow(() -> new RuntimeException("Supplier not found with id " + id));
    }

    public void deleteSupplier(String id) {
        if (equipmentRepository.existsBySupplierId(id)) {
            throw new IllegalStateException("Cannot delete supplier: Equipment is currently associated with it.");
        }

        supplierRepository.findById(id).ifPresent(supplier -> {
            notificationService.createNotification("Supplier Deleted: " + supplier.getCompanyName(),
                    "Supplier " + supplier.getCompanyName() + " has been removed",
                    "ERROR", "SUPPLIER", id, null, "STOCK_MANAGER");
        });
        supplierRepository.deleteById(id);
    }
}
