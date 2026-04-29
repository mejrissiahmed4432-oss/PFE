export interface OperatingSystem {
  id?: string;
  resourceId?: string;
  name?: string;
  version?: string;
  edition?: string;
  architecture?: string;
  licenseType?: string;
  licenseKey?: string;
  totalLicenses?: number;
  usedLicenses?: number;
  isoPath?: string;
  size?: string;
  requiredRam?: number;
  requiredStorage?: number;
  status?: string;
}

export interface InstallOSRequest {
  osId: string;
  equipmentId: string;
  installedBy: string;
}

export interface EquipmentSoftware {
  id?: string;
  equipmentId?: string;
  osId?: string;
  installedBy?: string;
  installedAt?: string;
  status?: string;
}
