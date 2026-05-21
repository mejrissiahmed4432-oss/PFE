package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.LicensePoolDTO;
import com.example.stockmanagermicroservice.model.LicensePool;
import com.example.stockmanagermicroservice.repository.LicensePoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LicensePoolService {

    @Autowired
    private LicensePoolRepository licensePoolRepository;

    // Simple mock encryption for demo purposes. In production, use standard AES-256.
    private String encryptKey(String rawKey) {
        if (rawKey == null) return null;
        return "ENC_" + new StringBuilder(rawKey).reverse().toString();
    }

    private String decryptKey(String encryptedKey) {
        if (encryptedKey == null || !encryptedKey.startsWith("ENC_")) return encryptedKey;
        return new StringBuilder(encryptedKey.substring(4)).reverse().toString();
    }

    public List<LicensePoolDTO> getPoolsBySoftwareId(String softwareId) {
        return licensePoolRepository.findBySoftwareId(softwareId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public LicensePoolDTO createLicensePool(LicensePoolDTO dto) {
        LicensePool pool = new LicensePool();
        pool.setSoftwareId(dto.getSoftwareId());
        pool.setLicenseModel(dto.getLicenseModel());
        pool.setActivationMethod(dto.getActivationMethod());
        pool.setTotalSeats(dto.getTotalSeats() != null ? dto.getTotalSeats() : 0);
        pool.setAvailableSeats(pool.getTotalSeats()); // Initially all available
        pool.setExpirationDate(dto.getExpirationDate());
        pool.setRenewalType(dto.getRenewalType());
        pool.setVendorSyncStatus("PENDING");

        // Encrypt raw keys if provided
        if (dto.getRawKeys() != null && !dto.getRawKeys().isEmpty()) {
            List<String> enc = dto.getRawKeys().stream().map(this::encryptKey).collect(Collectors.toList());
            pool.setEncryptedKeys(enc);
        } else {
            pool.setEncryptedKeys(new ArrayList<>());
        }

        pool.setCreatedAt(LocalDateTime.now());
        pool.setUpdatedAt(LocalDateTime.now());

        LicensePool saved = licensePoolRepository.save(pool);
        return mapToDTO(saved);
    }

    public List<String> revealKeys(String poolId, String password) {
        // In a real app, verify the password against the logged-in IT manager's hashed password
        // For this implementation, we simulate a master password 'admin123'
        if (!"admin123".equals(password)) {
            throw new RuntimeException("Invalid password. Access denied.");
        }

        LicensePool pool = licensePoolRepository.findById(poolId)
                .orElseThrow(() -> new RuntimeException("License Pool not found"));

        if (pool.getEncryptedKeys() == null) return new ArrayList<>();

        return pool.getEncryptedKeys().stream()
                .map(this::decryptKey)
                .collect(Collectors.toList());
    }

    // Maps to DTO but strips keys for security
    private LicensePoolDTO mapToDTO(LicensePool pool) {
        LicensePoolDTO dto = new LicensePoolDTO();
        dto.setId(pool.getId());
        dto.setSoftwareId(pool.getSoftwareId());
        dto.setLicenseModel(pool.getLicenseModel());
        dto.setActivationMethod(pool.getActivationMethod());
        dto.setTotalSeats(pool.getTotalSeats());
        dto.setAvailableSeats(pool.getAvailableSeats());
        dto.setExpirationDate(pool.getExpirationDate());
        dto.setRenewalType(pool.getRenewalType());
        dto.setVendorSyncStatus(pool.getVendorSyncStatus());
        // Deliberately NOT returning rawKeys or encryptedKeys in standard DTO
        return dto;
    }
}
