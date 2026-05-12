package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.PurchaseOrder;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends MongoRepository<PurchaseOrder, String> {
    Optional<PurchaseOrder> findByRequestId(String requestId);
    List<PurchaseOrder> findAllByOrderByCreatedAtDesc();
}
