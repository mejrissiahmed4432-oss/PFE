package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.SoftwareAssignmentDTO;
import com.example.stockmanagermicroservice.model.AssignmentStatus;
import com.example.stockmanagermicroservice.model.LicensePool;
import com.example.stockmanagermicroservice.model.SoftwareAssignment;
import com.example.stockmanagermicroservice.repository.LicensePoolRepository;
import com.example.stockmanagermicroservice.repository.SoftwareAssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssignmentService {

    @Autowired
    private SoftwareAssignmentRepository assignmentRepository;

    @Autowired
    private LicensePoolRepository licensePoolRepository;

    public List<SoftwareAssignmentDTO> getAssignmentsBySoftware(String softwareId) {
        return assignmentRepository.findBySoftwareId(softwareId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SoftwareAssignmentDTO assignLicense(SoftwareAssignmentDTO dto) {
        LicensePool pool = licensePoolRepository.findById(dto.getLicensePoolId())
                .orElseThrow(() -> new RuntimeException("License Pool not found"));

        if (pool.getAvailableSeats() <= 0) {
            throw new RuntimeException("No available seats in this license pool.");
        }

        // Deduct seat
        pool.setAvailableSeats(pool.getAvailableSeats() - 1);
        pool.setUpdatedAt(LocalDateTime.now());
        licensePoolRepository.save(pool);

        SoftwareAssignment assignment = new SoftwareAssignment();
        assignment.setLicensePoolId(pool.getId());
        assignment.setSoftwareId(pool.getSoftwareId());
        assignment.setAssignedToType(dto.getAssignedToType());
        assignment.setAssignedTargetId(dto.getAssignedTargetId());
        assignment.setAssignedTargetName(dto.getAssignedTargetName());
        assignment.setStatus(AssignmentStatus.ACTIVE);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setExpiresAt(dto.getExpiresAt());
        assignment.setAssignedBy(dto.getAssignedBy());
        assignment.setLicenseKeyUsed(dto.getLicenseKeyUsed());
        assignment.setCreatedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        SoftwareAssignment saved = assignmentRepository.save(assignment);
        return mapToDTO(saved);
    }

    public SoftwareAssignmentDTO revokeAssignment(String assignmentId) {
        SoftwareAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getStatus() == AssignmentStatus.REVOKED || assignment.getStatus() == AssignmentStatus.EXPIRED) {
            throw new RuntimeException("Assignment is already revoked or expired.");
        }

        LicensePool pool = licensePoolRepository.findById(assignment.getLicensePoolId())
                .orElseThrow(() -> new RuntimeException("License Pool not found"));

        // Return seat
        pool.setAvailableSeats(pool.getAvailableSeats() + 1);
        pool.setUpdatedAt(LocalDateTime.now());
        licensePoolRepository.save(pool);

        assignment.setStatus(AssignmentStatus.REVOKED);
        assignment.setRevokedAt(LocalDateTime.now());
        assignment.setUpdatedAt(LocalDateTime.now());

        SoftwareAssignment updated = assignmentRepository.save(assignment);
        return mapToDTO(updated);
    }

    private SoftwareAssignmentDTO mapToDTO(SoftwareAssignment assignment) {
        SoftwareAssignmentDTO dto = new SoftwareAssignmentDTO();
        dto.setId(assignment.getId());
        dto.setLicensePoolId(assignment.getLicensePoolId());
        dto.setSoftwareId(assignment.getSoftwareId());
        dto.setAssignedToType(assignment.getAssignedToType());
        dto.setAssignedTargetId(assignment.getAssignedTargetId());
        dto.setAssignedTargetName(assignment.getAssignedTargetName());
        dto.setStatus(assignment.getStatus());
        dto.setAssignedAt(assignment.getAssignedAt());
        dto.setExpiresAt(assignment.getExpiresAt());
        dto.setRevokedAt(assignment.getRevokedAt());
        dto.setAssignedBy(assignment.getAssignedBy());
        dto.setLicenseKeyUsed(assignment.getLicenseKeyUsed());
        return dto;
    }
}
