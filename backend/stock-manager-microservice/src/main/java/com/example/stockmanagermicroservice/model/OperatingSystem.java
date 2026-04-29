package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "operating_systems")
public class OperatingSystem {
    @Id
    private String id;
    
    private String resourceId;
    
    private String version;
    private String edition;
    private String architecture; // x64, x86
    
    private String licenseType; // OEM, Retail, Volume, Free
    private String licenseKey;
    private Integer totalLicenses;
    private Integer usedLicenses = 0;
    
    private String isoPath;
    private String size;
    private Integer requiredRam;
    private Integer requiredStorage;
    
    private String status; // Active, Deprecated

    public OperatingSystem() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getEdition() { return edition; }
    public void setEdition(String edition) { this.edition = edition; }

    public String getArchitecture() { return architecture; }
    public void setArchitecture(String architecture) { this.architecture = architecture; }

    public String getLicenseType() { return licenseType; }
    public void setLicenseType(String licenseType) { this.licenseType = licenseType; }

    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public Integer getTotalLicenses() { return totalLicenses; }
    public void setTotalLicenses(Integer totalLicenses) { this.totalLicenses = totalLicenses; }

    public Integer getUsedLicenses() { return usedLicenses; }
    public void setUsedLicenses(Integer usedLicenses) { this.usedLicenses = usedLicenses; }

    public String getIsoPath() { return isoPath; }
    public void setIsoPath(String isoPath) { this.isoPath = isoPath; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public Integer getRequiredRam() { return requiredRam; }
    public void setRequiredRam(Integer requiredRam) { this.requiredRam = requiredRam; }

    public Integer getRequiredStorage() { return requiredStorage; }
    public void setRequiredStorage(Integer requiredStorage) { this.requiredStorage = requiredStorage; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
