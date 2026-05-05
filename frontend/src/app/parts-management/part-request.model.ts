export interface PartRequestItem {
  partName: string;
  category: string;
  type: string;
  brand?: string;
  specification: string;
  quantity: number;
  equipmentId?: string;
  // Set by stock manager when matching a custom part to a real inventory item
  matchedEquipmentName?: string;
  matchedSpecification?: string;
  matchedSerialNumber?: string;
  processed?: boolean;
  returned?: boolean;
}

export interface PartRequest {
  id?: string;
  items: PartRequestItem[];
  priority: 'Low' | 'Medium' | 'High';
  description: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  requesterId: string;
  requesterName: string;
  createdAt?: string;

  // UI State Properties (for inline editing)
  isEditing?: boolean;
}
