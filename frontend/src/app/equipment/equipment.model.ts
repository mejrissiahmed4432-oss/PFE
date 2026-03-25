export interface Equipment {
  id?: string;
  icon?: string;
  createdAt?: string;
  updatedAt?: string;
  purchaseDate?: string;
  purchasePrice?: number;
  brand?: string;
  name?: string; // equipmentName from backend map, but we'll use equipmentName to match backend DTO
  equipmentName?: string;
  model?: string;
  serialNumber?: string;
  category?: string;
  supplier?: string;
  supplierId?: string;
  warrantyExpiration?: string;
  location?: string;
  note?: string;
  locationChangeAt?: string;
  locationChanged?: boolean;
  qrCode?: string;
  department?: string;
  createdBy?: string;
  // Device Specifications
  cpu?: string;
  ram?: string;
  storage?: string;
  graphicsCard?: string;
  operatingSystem?: string;
}

export interface EquipmentCategory {
  id?: string;
  name: string;
  icon: string;
}
