package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.SoftwareDTO;
import com.example.stockmanagermicroservice.model.LicensePool;
import com.example.stockmanagermicroservice.model.Software;
import com.example.stockmanagermicroservice.repository.LicensePoolRepository;
import com.example.stockmanagermicroservice.repository.SoftwareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SoftwareService {

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private LicensePoolRepository licensePoolRepository;

    public List<SoftwareDTO> getAllSoftware() {
        return softwareRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public SoftwareDTO getSoftwareById(String id) {
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Software not found"));
        return mapToDTO(software);
    }

    public SoftwareDTO createSoftware(SoftwareDTO dto) {
        Software software = new Software();
        software.setName(dto.getName());
        software.setType(dto.getType());
        software.setVendor(dto.getVendor());
        software.setVersion(dto.getVersion());
        software.setWebsite(dto.getWebsite());
        software.setStatus(dto.getStatus() != null ? dto.getStatus() : "Active");
        software.setCreatedAt(LocalDateTime.now());
        software.setUpdatedAt(LocalDateTime.now());
        
        Software saved = softwareRepository.save(software);
        return mapToDTO(saved);
    }

    public SoftwareDTO updateSoftware(String id, SoftwareDTO dto) {
        Software software = softwareRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Software not found"));
        
        software.setName(dto.getName());
        software.setType(dto.getType());
        software.setVendor(dto.getVendor());
        software.setVersion(dto.getVersion());
        software.setWebsite(dto.getWebsite());
        software.setStatus(dto.getStatus());
        software.setUpdatedAt(LocalDateTime.now());

        Software updated = softwareRepository.save(software);
        return mapToDTO(updated);
    }

    public void deleteSoftware(String id) {
        softwareRepository.deleteById(id);
    }

    private SoftwareDTO mapToDTO(Software software) {
        SoftwareDTO dto = new SoftwareDTO();
        dto.setId(software.getId());
        dto.setName(software.getName());
        dto.setType(software.getType());
        dto.setVendor(software.getVendor());
        dto.setVersion(software.getVersion());
        dto.setWebsite(software.getWebsite());
        dto.setStatus(software.getStatus());
        
        // Aggregate seats from pools
        List<LicensePool> pools = licensePoolRepository.findBySoftwareId(software.getId());
        int total = pools.stream().mapToInt(p -> p.getTotalSeats() != null ? p.getTotalSeats() : 0).sum();
        int available = pools.stream().mapToInt(p -> p.getAvailableSeats() != null ? p.getAvailableSeats() : 0).sum();
        
        dto.setTotalSeats(total);
        dto.setAvailableSeats(available);
        dto.setCreatedAt(software.getCreatedAt());
        
        return dto;
    }
}
