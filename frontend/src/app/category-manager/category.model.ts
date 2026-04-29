export interface CategoryType {
  name: string;
  requiresQrCode: boolean;
  nature?: 'Asset' | 'Consumable';
  specificationFields?: string[];
}

export interface EquipmentCategory {
  id?: string;
  name?: string;
  icon?: string;
  types?: CategoryType[];
}
