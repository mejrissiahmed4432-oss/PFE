export interface PartRequestItem {
  partName: string;
  category: string;
  type: string;
  specification: string;
  quantity: number;
  equipmentId?: string;
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
