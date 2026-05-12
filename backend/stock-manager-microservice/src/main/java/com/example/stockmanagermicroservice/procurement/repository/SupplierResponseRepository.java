package com.example.stockmanagermicroservice.procurement.repository;

import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.model.SupplierResponseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierResponseRepository extends MongoRepository<SupplierResponse, String> {
    List<SupplierResponse> findByRfqId(String rfqId);
    List<SupplierResponse> findByRequestId(String requestId);
    List<SupplierResponse> findByStatus(SupplierResponseStatus status);
    List<SupplierResponse> findAllByOrderByCreatedAtDesc();
}
