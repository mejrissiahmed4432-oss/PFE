package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "applications")
public class Application {
    @Id
    private String id;
    
    private String resourceId; // FK to resources.id
    
    // BASIC INFO
    private String version;
    private String vendor;
    private String category; // Office, Browser, Security, Utility, Other
    
    // INSTALLATION
    private String installerPath;
    private String downloadLink;
    private String silentInstallCommand;
    
    // LICENSE
    private String licenseType; // Free, Paid, Subscription
    private String licenseKey;
    private Integer totalLicenses;
    private Integer usedLicenses = 0;
    
    // SYSTEM REQUIREMENTS
    private Integer requiredRam;
    private Integer requiredStorage;
    private String supportedOs;
    
    // STATUS
    private String status; // Active, Deprecated

    public Application() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getVendor() { return vendor; }
    public void setVendor(String vendor) { this.vendor = vendor; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getInstallerPath() { return installerPath; }
    public void setInstallerPath(String installerPath) { this.installerPath = installerPath; }

    public String getDownloadLink() { return downloadLink; }
    public void setDownloadLink(String downloadLink) { this.downloadLink = downloadLink; }

    public String getSilentInstallCommand() { return silentInstallCommand; }
    public void setSilentInstallCommand(String silentInstallCommand) { this.silentInstallCommand = silentInstallCommand; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public Integer getTotalLicenses() { return totalLicenses; }
    public void setTotalLicenses(Integer totalLicenses) { this.totalLicenses = totalLicenses; }

    public Integer getUsedLicenses() { return usedLicenses; }
    public void setUsedLicenses(Integer usedLicenses) { this.usedLicenses = usedLicenses; }

    public Integer getRequiredRam() { return requiredRam; }
    public void setRequiredRam(Integer requiredRam) { this.requiredRam = requiredRam; }

    public Integer getRequiredStorage() { return requiredStorage; }
    public void setRequiredStorage(Integer requiredStorage) { this.requiredStorage = requiredStorage; }

    public String getSupportedOs() { return supportedOs; }
    public void setSupportedOs(String supportedOs) { this.supportedOs = supportedOs; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
