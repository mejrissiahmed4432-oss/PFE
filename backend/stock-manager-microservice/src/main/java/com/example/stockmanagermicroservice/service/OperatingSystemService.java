package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.InstallOSRequest;
import com.example.stockmanagermicroservice.dto.OperatingSystemDTO;
import com.example.stockmanagermicroservice.model.EquipmentSoftware;
import com.example.stockmanagermicroservice.model.OperatingSystem;
import com.example.stockmanagermicroservice.model.Resource;
import com.example.stockmanagermicroservice.repository.EquipmentSoftwareRepository;
import com.example.stockmanagermicroservice.repository.OperatingSystemRepository;
import com.example.stockmanagermicroservice.repository.ResourceRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.model.LifecycleEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OperatingSystemService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private OperatingSystemRepository osRepository;

    @Autowired
    private EquipmentSoftwareRepository softwareRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private AlertService alertService;

    public List<OperatingSystemDTO> getAllOperatingSystems() {
        List<OperatingSystem> osList = osRepository.findAll();
        List<OperatingSystemDTO> dtoList = new ArrayList<>();

        for (OperatingSystem os : osList) {
            Resource res = resourceRepository.findById(os.getResourceId()).orElse(null);
            if (res != null) {
                dtoList.add(mapToDTO(os, res));
            }
        }
        return dtoList;
    }

    public OperatingSystemDTO addOperatingSystem(OperatingSystemDTO dto) {
        // 1. Create Resource
        Resource resource = new Resource();
        resource.setName(dto.getName());
        resource.setType("SOFTWARE");
        resource.setCategory("OS");
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resource = resourceRepository.save(resource);

        // 2. Create OS
        OperatingSystem os = new OperatingSystem();
        os.setResourceId(resource.getId());
        os.setVersion(dto.getVersion());
        os.setEdition(dto.getEdition());
        os.setArchitecture(dto.getArchitecture());
        os.setLicenseType(dto.getLicenseType());
        os.setLicenseKey(dto.getLicenseKey());
        os.setTotalLicenses(dto.getTotalLicenses());
        os.setUsedLicenses(0);
        os.setIsoPath(dto.getIsoPath());
        os.setSize(dto.getSize());
        os.setRequiredRam(dto.getRequiredRam());
        os.setRequiredStorage(dto.getRequiredStorage());
        os.setStatus("Active");

        os = osRepository.save(os);
        return mapToDTO(os, resource);
    }

    public OperatingSystemDTO updateOperatingSystem(String id, OperatingSystemDTO dto) {
        Optional<OperatingSystem> osOpt = osRepository.findById(id);
        if (osOpt.isPresent()) {
            OperatingSystem os = osOpt.get();
            os.setVersion(dto.getVersion());
            os.setEdition(dto.getEdition());
            os.setArchitecture(dto.getArchitecture());
            os.setLicenseType(dto.getLicenseType());
            os.setLicenseKey(dto.getLicenseKey());
            os.setTotalLicenses(dto.getTotalLicenses());
            os.setIsoPath(dto.getIsoPath());
            os.setSize(dto.getSize());
            os.setRequiredRam(dto.getRequiredRam());
            os.setRequiredStorage(dto.getRequiredStorage());
            os.setStatus(dto.getStatus());
            
            os = osRepository.save(os);
            
            Optional<Resource> resOpt = resourceRepository.findById(os.getResourceId());
            if (resOpt.isPresent()) {
                Resource res = resOpt.get();
                res.setName(dto.getName());
                res.setUpdatedAt(LocalDateTime.now());
                resourceRepository.save(res);
                return mapToDTO(os, res);
            }
        }
        throw new RuntimeException("OS not found");
    }

    public EquipmentSoftware installOS(InstallOSRequest request) {
        OperatingSystem os = osRepository.findById(request.getOsId())
                .orElseThrow(() -> new RuntimeException("OS not found"));

        if ("Deprecated".equalsIgnoreCase(os.getStatus())) {
            throw new RuntimeException("Cannot install a deprecated Operating System");
        }

        if (!"Free".equalsIgnoreCase(os.getLicenseType())) {
            if (os.getUsedLicenses() >= os.getTotalLicenses()) {
                throw new RuntimeException("No licenses available");
            }
        }

        EquipmentSoftware es = new EquipmentSoftware();
        es.setEquipmentId(request.getEquipmentId());
        es.setOsId(os.getId());
        es.setInstalledBy(request.getInstalledBy());
        es.setInstalledAt(LocalDateTime.now());
        es.setStatus("installed");
        
        es = softwareRepository.save(es);

        // Increment used licenses
        if (!"Free".equalsIgnoreCase(os.getLicenseType())) {
            os.setUsedLicenses(os.getUsedLicenses() + 1);
            osRepository.save(os);

            // Alert logic
            if (os.getUsedLicenses() >= os.getTotalLicenses()) {
                Resource res = resourceRepository.findById(os.getResourceId()).orElse(null);
                String osName = res != null ? res.getName() : "Unknown OS";
                
                alertService.triggerSystemAlert(
                    "OS_LICENSE_DEPLETED_" + os.getId(),
                    "LICENSE_DEPLETED",
                    "HIGH",
                    "ROLE",
                    "IT_MANAGER",
                    "Licenses Depleted: " + osName,
                    "All licenses for " + osName + " have been used. No further installations are possible."
                );
            }
        }

        // Update equipment specifications + add lifecycle entry
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElse(null);
        if (equipment != null) {
            Resource res = resourceRepository.findById(os.getResourceId()).orElse(null);
            String osName = res != null ? res.getName() : "Unknown OS";
            if (equipment.getSpecifications() == null) {
                equipment.setSpecifications(new java.util.HashMap<>());
            }
            String fullOsName = osName + " " + os.getVersion();
            equipment.getSpecifications().put("Operating System", fullOsName);
            equipment.getSpecifications().put("OS Installed At", LocalDateTime.now().toString());

            // Add lifecycle entry
            if (equipment.getLifecycle() == null) {
                equipment.setLifecycle(new ArrayList<>());
            }
            String actorId = request.getInstalledBy() != null ? request.getInstalledBy() : "System";
            LifecycleEntry entry = new LifecycleEntry(
                "OS Installed",
                LocalDateTime.now(),
                "Operating System installed: " + fullOsName,
                actorId
            );
            equipment.getLifecycle().add(entry);

            equipmentRepository.save(equipment);
        }

        return es;
    }

    public void uninstallOS(String softwareId) {
        EquipmentSoftware es = softwareRepository.findById(softwareId)
                .orElseThrow(() -> new RuntimeException("Installation record not found"));

        if ("removed".equalsIgnoreCase(es.getStatus())) {
            throw new RuntimeException("OS is already uninstalled");
        }

        es.setStatus("removed");
        softwareRepository.save(es);

        OperatingSystem os = osRepository.findById(es.getOsId()).orElse(null);
        if (os != null && !"Free".equalsIgnoreCase(os.getLicenseType())) {
            if (os.getUsedLicenses() > 0) {
                os.setUsedLicenses(os.getUsedLicenses() - 1);
                osRepository.save(os);
                
                // If it drops below total, we could resolve the alert
                if (os.getUsedLicenses() < os.getTotalLicenses()) {
                    alertService.resolveSystemAlert("OS_LICENSE_DEPLETED_" + os.getId());
                }
            }
        }
    }

    public void deleteOperatingSystem(String id) {
        OperatingSystem os = osRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("OS not found"));

        // Check if any equipment has it installed
        List<EquipmentSoftware> installations = softwareRepository.findByOsId(id);
        if (installations.stream().anyMatch(es -> "installed".equalsIgnoreCase(es.getStatus()))) {
            throw new RuntimeException("Cannot delete OS: It is currently installed on one or more equipment(s)");
        }

        osRepository.deleteById(id);
        if (os.getResourceId() != null) {
            resourceRepository.deleteById(os.getResourceId());
        }
    }

    private OperatingSystemDTO mapToDTO(OperatingSystem os, Resource res) {
        OperatingSystemDTO dto = new OperatingSystemDTO();
        dto.setId(os.getId());
        dto.setResourceId(os.getResourceId());
        dto.setName(res.getName());
        dto.setVersion(os.getVersion());
        dto.setEdition(os.getEdition());
        dto.setArchitecture(os.getArchitecture());
        dto.setLicenseType(os.getLicenseType());
        dto.setLicenseKey(os.getLicenseKey());
        dto.setTotalLicenses(os.getTotalLicenses());
        dto.setUsedLicenses(os.getUsedLicenses());
        dto.setIsoPath(os.getIsoPath());
        dto.setSize(os.getSize());
        dto.setRequiredRam(os.getRequiredRam());
        dto.setRequiredStorage(os.getRequiredStorage());
        dto.setStatus(os.getStatus());
        return dto;
    }
}
