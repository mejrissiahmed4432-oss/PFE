package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.model.LicensePool;
import com.example.stockmanagermicroservice.model.Software;
import com.example.stockmanagermicroservice.repository.LicensePoolRepository;
import com.example.stockmanagermicroservice.repository.SoftwareRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class LicenseAlertScheduler {

    @Autowired
    private LicensePoolRepository licensePoolRepository;

    @Autowired
    private SoftwareRepository softwareRepository;

    @Autowired
    private AlertService alertService;

    @Scheduled(cron = "0 0 * * * *")
    public void generateLicenseAlerts() {
        LocalDate today = LocalDate.now();
        List<LicensePool> pools = licensePoolRepository.findAll();

        for (LicensePool pool : pools) {
            String expiredKey = "LICENSE_POOL_EXPIRED_" + pool.getId();

            if (pool.getExpirationDate() != null && pool.getExpirationDate().isBefore(today)) {
                String softwareName = getSoftwareName(pool.getSoftwareId());
                alertService.triggerSystemAlert(
                        expiredKey,
                        "LICENSE_POOL_EXPIRED",
                        "HIGH",
                        "ROLE",
                        "IT_MANAGER",
                        "License Pool Expired: " + softwareName,
                        "A license pool for " + softwareName + " expired on " + pool.getExpirationDate() + ".");
            } else {
                alertService.resolveSystemAlert(expiredKey);
            }
        }
    }

    private String getSoftwareName(String softwareId) {
        if (softwareId == null || softwareId.trim().isEmpty()) {
            return "Unknown software";
        }

        return softwareRepository.findById(softwareId)
                .map(Software::getName)
                .orElse("Unknown software");
    }
}
