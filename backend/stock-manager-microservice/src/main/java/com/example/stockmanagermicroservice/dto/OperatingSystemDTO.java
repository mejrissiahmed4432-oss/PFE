package com.example.stockmanagermicroservice.dto;

public class OperatingSystemDTO {
    private String id; // OS Id
    private String resourceId;
    private String name; // From Resource
    
    private String version;
    private String edition;
    private String architecture;
    
    private String licenseType;
    private String licenseKey;
    private Integer totalLicenses;
    private Integer usedLicenses;
    
    private String isoPath;
    private String size;
    private Integer requiredRam;
    private Integer requiredStorage;
    
    private String status;

    public OperatingSystemDTO() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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
