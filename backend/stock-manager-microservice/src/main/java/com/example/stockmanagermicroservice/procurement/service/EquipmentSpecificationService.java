package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.EquipmentSpecification;
import com.example.stockmanagermicroservice.procurement.repository.EquipmentSpecificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipmentSpecificationService {

    private final EquipmentSpecificationRepository repository;

    public EquipmentSpecificationService(EquipmentSpecificationRepository repository) {
        this.repository = repository;
    }

    public List<EquipmentSpecification> getSpecsForCatalogItem(String catalogItemId) {
        return repository.findByCatalogItemId(catalogItemId);
    }

    public EquipmentSpecification saveSpecification(EquipmentSpecification spec) {
        return repository.save(spec);
    }
}
