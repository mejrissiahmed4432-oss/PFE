export interface PartRequest {
  id?: string;
  partName: string;
  category: string;
  type: string;
  specification: string;
  equipmentId?: string;
  quantity: number;
  priority: 'Low' | 'Medium' | 'High';
  description: string;
  status?: 'PENDING' | 'APPROVED' | 'REJECTED';
  requesterId: string;
  requesterName: string;
  createdAt?: string;
  
  // UI State Properties (for inline editing)
  isEditing?: boolean;
  editQuantity?: number;
  editPriority?: 'Low' | 'Medium' | 'High';
  editSpecification?: string;
}
