export interface CategoryType {
  name: string;
  requiresQrCode: boolean;
}

export interface EquipmentCategory {
  id?: string;
  name?: string;
  icon?: string;
  types?: CategoryType[];
}
