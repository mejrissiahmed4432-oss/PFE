package com.example.stockmanagermicroservice.repository;

import com.example.stockmanagermicroservice.model.AssignmentStatus;
import com.example.stockmanagermicroservice.model.SoftwareAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SoftwareAssignmentRepository extends MongoRepository<SoftwareAssignment, String> {
    List<SoftwareAssignment> findByLicensePoolId(String licensePoolId);
    List<SoftwareAssignment> findBySoftwareId(String softwareId);
    List<SoftwareAssignment> findByAssignedTargetId(String assignedTargetId);
    List<SoftwareAssignment> findByLicensePoolIdAndStatus(String licensePoolId, AssignmentStatus status);
}
