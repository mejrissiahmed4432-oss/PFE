export interface CategoryType {
  name: string;
  requiresQrCode: boolean;
  specificationFields?: string[];
}

export interface EquipmentCategory {
  id?: string;
  name?: string;
  icon?: string;
  types?: CategoryType[];
}
