// ─── Procurement Domain Models ────────────────────────────────────────────────

export interface EquipmentRequestItem {
  name: string;
  quantity: number;
  description?: string;
  catalogItemId?: string;
  selectedSpecs?: Record<string, string>;
}

export interface CatalogItem {
  id?: string;
  name: string;
  category: string;
  description?: string;
  defaultSpecs?: string;
  relatedTags?: string[];
  defaultSuppliers?: string[];
}

export interface ParsedItem {
  name: string;
  quantity: number;
  inferredCategory?: string;
  detectedSpecs?: Record<string, string>;
}

export interface EquipmentSpecification {
  id?: string;
  catalogItemId: string;
  name: string;
  possibleValues: string[];
}

export interface EquipmentParsingResponse {
  items: ParsedItem[];
  success: boolean;
  error?: string;
}

export type RequestStatus =
  | 'PENDING_IT_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'SENT_TO_SUPPLIERS'
  | 'RESPONDED'
  | 'ORDER_CONFIRMED'
  | 'RECEIVED';

export type SupplierResponseStatus =
  | 'PENDING'
  | 'APPROVED_SUPPLIER'
  | 'REJECTED_SUPPLIER';

export interface EquipmentRequest {
  id?: string;
  createdByUserId?: string;
  createdByName?: string;
  createdByRole?: string;
  items: EquipmentRequestItem[];
  notes?: string;
  status?: RequestStatus;
  rejectionReason?: string;
  supplierName?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface RFQ {
  id?: string;
  requestId: string;
  supplierIds: string[];
  supplierEmails: string[];
  pdfFilePath?: string;
  sentAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface SupplierResponse {
  id?: string;
  rfqId: string;
  requestId: string;
  supplierId: string;
  supplierName: string;
  pdfFilePath?: string;
  originalFileName?: string;
  totalPrice?: number;
  deliveryDays?: number;
  notes?: string;
  currency?: string;
  status?: SupplierResponseStatus;
  createdAt?: string;
  updatedAt?: string;
}

export interface PurchaseOrder {
  id?: string;
  requestId: string;
  rfqId: string;
  selectedResponseId: string;
  supplierId: string;
  supplierName: string;
  totalPrice?: number;
  currency?: string;
  deliveryDays?: number;
  status?: RequestStatus;
  items?: EquipmentRequestItem[];
  createdAt?: string;
  updatedAt?: string;
  receivedAt?: string;
  receiptNotes?: string;
  supplierRating?: number;
}

// ─── UI Helpers ───────────────────────────────────────────────────────────────

export interface StatusBadge {
  label: string;
  colorClass: string;
  icon: string;
}

export const STATUS_META: Record<RequestStatus, StatusBadge> = {
  PENDING_IT_APPROVAL: { label: 'Pending Approval',   colorClass: 'badge-warning',  icon: '⏳' },
  APPROVED:            { label: 'Approved',            colorClass: 'badge-success',  icon: '✅' },
  REJECTED:            { label: 'Rejected',            colorClass: 'badge-danger',   icon: '❌' },
  SENT_TO_SUPPLIERS:   { label: 'Sent to Suppliers',  colorClass: 'badge-info',     icon: '📤' },
  RESPONDED:           { label: 'Responses Received', colorClass: 'badge-purple',   icon: '📩' },
  ORDER_CONFIRMED:     { label: 'Order Confirmed',    colorClass: 'badge-emerald',  icon: '🛒' },
  RECEIVED:            { label: 'Items Received',     colorClass: 'badge-blue',     icon: '📦' },
};

export const RESPONSE_STATUS_META: Record<SupplierResponseStatus, StatusBadge> = {
  PENDING:           { label: 'Pending Review',    colorClass: 'badge-warning', icon: '⏳' },
  APPROVED_SUPPLIER: { label: 'Selected Supplier', colorClass: 'badge-success', icon: '🏆' },
  REJECTED_SUPPLIER: { label: 'Not Selected',      colorClass: 'badge-danger',  icon: '❌' },
};
