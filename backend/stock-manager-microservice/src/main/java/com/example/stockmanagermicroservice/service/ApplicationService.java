package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.ApplicationDTO;
import com.example.stockmanagermicroservice.dto.InstallApplicationRequest;
import com.example.stockmanagermicroservice.model.*;
import com.example.stockmanagermicroservice.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApplicationService {

    @Autowired
    private ResourceRepository resourceRepository;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Autowired
    private EquipmentApplicationRepository equipmentApplicationRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private AlertService alertService;

    public List<ApplicationDTO> getAllApplications() {
        List<Application> apps = applicationRepository.findAll();
        return apps.stream()
                .map(app -> {
                    Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
                    return mapToDTO(app, res);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    public ApplicationDTO getApplicationById(String id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
        return mapToDTO(app, res);
    }

    public ApplicationDTO createApplication(ApplicationDTO dto) {
        // 1. Create Resource
        Resource resource = new Resource();
        resource.setName(dto.getName());
        resource.setType("SOFTWARE");
        resource.setCategory("APPLICATION");
        resource.setCreatedAt(LocalDateTime.now());
        resource.setUpdatedAt(LocalDateTime.now());
        resource = resourceRepository.save(resource);

        // 2. Create Application
        Application app = new Application();
        app.setResourceId(resource.getId());
        app.setVersion(dto.getVersion());
        app.setVendor(dto.getVendor());
        app.setCategory(dto.getCategory());
        app.setInstallerPath(dto.getInstallerPath());
        app.setDownloadLink(dto.getDownloadLink());
        app.setSilentInstallCommand(dto.getSilentInstallCommand());
        app.setLicenseType(dto.getLicenseType());
        app.setLicenseKey(dto.getLicenseKey());
        app.setTotalLicenses(dto.getTotalLicenses());
        app.setUsedLicenses(0);
        app.setRequiredRam(dto.getRequiredRam());
        app.setRequiredStorage(dto.getRequiredStorage());
        app.setSupportedOs(dto.getSupportedOs());
        app.setStatus("Active");

        app = applicationRepository.save(app);
        return mapToDTO(app, resource);
    }

    public ApplicationDTO updateApplication(String id, ApplicationDTO dto) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        app.setVersion(dto.getVersion());
        app.setVendor(dto.getVendor());
        app.setCategory(dto.getCategory());
        app.setInstallerPath(dto.getInstallerPath());
        app.setDownloadLink(dto.getDownloadLink());
        app.setSilentInstallCommand(dto.getSilentInstallCommand());
        app.setLicenseType(dto.getLicenseType());
        app.setLicenseKey(dto.getLicenseKey());
        app.setTotalLicenses(dto.getTotalLicenses());
        app.setRequiredRam(dto.getRequiredRam());
        app.setRequiredStorage(dto.getRequiredStorage());
        app.setSupportedOs(dto.getSupportedOs());
        app.setStatus(dto.getStatus());
        
        app = applicationRepository.save(app);

        Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
        if (res != null) {
            res.setName(dto.getName());
            res.setUpdatedAt(LocalDateTime.now());
            resourceRepository.save(res);
        }
        
        return mapToDTO(app, res);
    }

    public void deleteApplication(String id) {
        Application app = applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        
        // Check if any equipment has it installed
        List<EquipmentApplication> installations = equipmentApplicationRepository.findByApplicationId(id);
        if (!installations.isEmpty()) {
            throw new RuntimeException("Cannot delete application: It is currently installed on " + installations.size() + " equipment(s)");
        }

        applicationRepository.deleteById(id);
        if (app.getResourceId() != null) {
            resourceRepository.deleteById(app.getResourceId());
        }
    }

    public EquipmentApplication installApplication(InstallApplicationRequest request) {
        Application app = applicationRepository.findById(request.getApplicationId())
                .orElseThrow(() -> new RuntimeException("Application not found"));

        if ("Deprecated".equalsIgnoreCase(app.getStatus())) {
            throw new RuntimeException("Application cannot be installed (status issue: Deprecated)");
        }

        if (!"Free".equalsIgnoreCase(app.getLicenseType())) {
            if (app.getUsedLicenses() >= app.getTotalLicenses()) {
                throw new RuntimeException("Application cannot be installed (license issue: No available licenses)");
            }
        }

        EquipmentApplication ea = new EquipmentApplication();
        ea.setEquipmentId(request.getEquipmentId());
        ea.setApplicationId(app.getId());
        ea.setInstalledBy(request.getInstalledBy());
        ea.setInstalledAt(LocalDateTime.now());
        ea.setStatus("installed");
        
        ea = equipmentApplicationRepository.save(ea);

        // Increment used licenses
        if (!"Free".equalsIgnoreCase(app.getLicenseType())) {
            app.setUsedLicenses(app.getUsedLicenses() + 1);
            applicationRepository.save(app);

            // Alert if depleted
            if (app.getUsedLicenses() >= app.getTotalLicenses()) {
                Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
                String appName = res != null ? res.getName() : "Unknown Application";
                alertService.triggerSystemAlert(
                    "APP_LICENSE_DEPLETED_" + app.getId(),
                    "LICENSE_DEPLETED",
                    "HIGH",
                    "APPLICATION",
                    app.getId(),
                    "Licenses Depleted: " + appName,
                    "All licenses for " + appName + " (Version: " + app.getVersion() + ") have been used."
                );
            }
        }

        // Update equipment lifecycle
        Equipment equipment = equipmentRepository.findById(request.getEquipmentId()).orElse(null);
        if (equipment != null) {
            Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
            String appName = res != null ? res.getName() : "Unknown Application";
            
            if (equipment.getLifecycle() == null) {
                equipment.setLifecycle(new ArrayList<>());
            }
            
            LifecycleEntry entry = new LifecycleEntry(
                "App Installed",
                LocalDateTime.now(),
                "Application installed: " + appName + " v" + app.getVersion(),
                request.getInstalledBy() != null ? request.getInstalledBy() : "System"
            );
            equipment.getLifecycle().add(entry);
            equipmentRepository.save(equipment);
        }

        return ea;
    }

    public void uninstallApplication(String installationId) {
        EquipmentApplication ea = equipmentApplicationRepository.findById(installationId)
                .orElseThrow(() -> new RuntimeException("Installation record not found"));

        if ("removed".equalsIgnoreCase(ea.getStatus())) {
            throw new RuntimeException("Application is already removed");
        }

        ea.setStatus("removed");
        equipmentApplicationRepository.save(ea);

        Application app = applicationRepository.findById(ea.getApplicationId()).orElse(null);
        if (app != null && !"Free".equalsIgnoreCase(app.getLicenseType())) {
            if (app.getUsedLicenses() > 0) {
                app.setUsedLicenses(app.getUsedLicenses() - 1);
                applicationRepository.save(app);
                
                if (app.getUsedLicenses() < app.getTotalLicenses()) {
                    alertService.resolveSystemAlert("APP_LICENSE_DEPLETED_" + app.getId());
                }
            }
        }
        
        // Update equipment lifecycle
        Equipment equipment = equipmentRepository.findById(ea.getEquipmentId()).orElse(null);
        if (equipment != null) {
            Resource res = resourceRepository.findById(app.getResourceId()).orElse(null);
            String appName = res != null ? res.getName() : "Unknown Application";
            
            if (equipment.getLifecycle() == null) {
                equipment.setLifecycle(new ArrayList<>());
            }
            
            LifecycleEntry entry = new LifecycleEntry(
                "App Uninstalled",
                LocalDateTime.now(),
                "Application uninstalled: " + appName,
                "System"
            );
            equipment.getLifecycle().add(entry);
            equipmentRepository.save(equipment);
        }
    }

    private ApplicationDTO mapToDTO(Application app, Resource res) {
        if (app == null) return null;
        ApplicationDTO dto = new ApplicationDTO();
        dto.setId(app.getId());
        dto.setResourceId(app.getResourceId());
        if (res != null) {
            dto.setName(res.getName());
        }
        dto.setVersion(app.getVersion());
        dto.setVendor(app.getVendor());
        dto.setCategory(app.getCategory());
        dto.setInstallerPath(app.getInstallerPath());
        dto.setDownloadLink(app.getDownloadLink());
        dto.setSilentInstallCommand(app.getSilentInstallCommand());
        dto.setLicenseType(app.getLicenseType());
        dto.setLicenseKey(app.getLicenseKey());
        dto.setTotalLicenses(app.getTotalLicenses());
        dto.setUsedLicenses(app.getUsedLicenses());
        dto.setRequiredRam(app.getRequiredRam());
        dto.setRequiredStorage(app.getRequiredStorage());
        dto.setSupportedOs(app.getSupportedOs());
        dto.setStatus(app.getStatus());
        return dto;
    }
}
