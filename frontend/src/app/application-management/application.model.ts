export interface Application {
    id?: string;
    resourceId?: string;
    name: string;
    version: string;
    vendor: string;
    category: 'Office' | 'Browser' | 'Security' | 'Utility' | 'Other';
    
    installerPath?: string;
    downloadLink?: string;
    silentInstallCommand?: string;
    
    licenseType: 'Free' | 'Paid' | 'Subscription';
    licenseKey?: string;
    totalLicenses?: number;
    usedLicenses?: number;
    
    requiredRam?: number;
    requiredStorage?: number;
    supportedOs?: string;
    
    status: 'Active' | 'Deprecated';
    icon?: string;
}

export interface EquipmentApplication {
    id?: string;
    equipmentId: string;
    applicationId: string;
    installedBy: string;
    installedAt: string;
    status: 'installed' | 'removed';
}

export interface InstallApplicationRequest {
    applicationId: string;
    equipmentId: string;
    installedBy: string;
}
