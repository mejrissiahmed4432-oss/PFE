import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PartRequestService } from '../part-request.service';
import { PartRequest, PartRequestItem } from '../part-request.model';
import { AuthService } from '../../auth.service';
import { EquipmentService } from '../../equipment/equipment.service';

@Component({
  selector: 'app-request-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './request-manager.component.html',
  styleUrl: './request-manager.component.css'
})
export class RequestManagerComponent implements OnInit {
  user: any;
  allRequests: PartRequest[] = [];
  filteredRequests: PartRequest[] = [];

  activeTab: 'PENDING' | 'PROCESSED' = 'PENDING';
  searchQuery: string = '';
  expandedRequestId: string | null = null;
  processingId: string | null = null;

  // Pick List State
  availableStockByItem: Map<string, any[]> = new Map();
  pickedEquipmentByItem: Map<string, any[]> = new Map();
  itemCheckboxes: Map<string, boolean> = new Map(); // Track checked state
  showPickerForItem: string | null = null;
  isLoadingStock: boolean = false;

  // Custom Alert Modal State
  alertConfig: {
    show: boolean;
    title: string;
    message: string;
    type: 'warning' | 'error' | 'success';
  } = {
      show: false,
      title: '',
      message: '',
      type: 'warning'
    };

  constructor(
    private partRequestService: PartRequestService,
    private authService: AuthService,
    private equipmentService: EquipmentService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      if (this.user) {
        this.loadAllRequests();
      }
    });
  }

  loadAllRequests(): void {
    this.partRequestService.getAllRequests().subscribe(requests => {
      this.allRequests = requests.sort((a, b) =>
        new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      );
      this.filterRequests();
    });
  }

  setTab(tab: 'PENDING' | 'PROCESSED'): void {
    this.activeTab = tab;
    this.expandedRequestId = null;
    this.filterRequests();
  }

  filterRequests(): void {
    let filtered = this.allRequests;

    // Tab Filter
    if (this.activeTab === 'PENDING') {
      filtered = filtered.filter(r => r.status === 'PENDING');
    } else {
      filtered = filtered.filter(r => r.status !== 'PENDING');
    }

    // Search Filter
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      filtered = filtered.filter(r =>
        (r.requesterName || '').toLowerCase().includes(q) ||
        (r.description || '').toLowerCase().includes(q) ||
        (r.id || '').toLowerCase().includes(q) ||
        (r.items || []).some(item => (item.partName || '').toLowerCase().includes(q))
      );
    }

    this.filteredRequests = filtered;
  }

  toggleExpand(requestId: string | undefined): void {
    if (!requestId) return;
    this.expandedRequestId = this.expandedRequestId === requestId ? null : requestId;
  }

  updateStatus(requestId: string | undefined, status: 'APPROVED' | 'REJECTED', event: Event): void {
    event.stopPropagation(); // prevent expanding the row when clicking action buttons
    if (!requestId) return;

    const requestToApprove = this.allRequests.find(r => r.id === requestId);

    this.processingId = requestId;
    this.partRequestService.updateStatus(requestId, status).subscribe({
      next: (updated) => {
        if (status === 'APPROVED' && requestToApprove && requestToApprove.items) {
          const consumed = requestToApprove.items.map(item => ({
            name: item.partName,
            brand: item.brand,
            type: item.type,
            specification: item.specification,
            qty: item.quantity,
            equipmentId: item.equipmentId
          }));
          this.equipmentService.consumeParts(consumed).subscribe({
            next: () => console.log('Successfully decremented global stock for approved parts.'),
            error: (err) => console.error('Failed to decrement global stock for approved parts.', err)
          });
        }

        // Update the local list
        const index = this.allRequests.findIndex(r => r.id === requestId);
        if (index !== -1) {
          this.allRequests[index] = updated;
        }
        this.filterRequests();
        this.processingId = null;
      },
      error: (err) => {
        console.error('Error updating status', err);
        this.processingId = null;
      }
    });
  }

  // \u2500\u2500 Pick List Logic \u2500\u2500

  getItemKey(request: PartRequest, index: number): string {
    return (request.id || '') + '-' + index;
  }

  togglePicker(request: PartRequest, item: any, index: number): void {
    const key = this.getItemKey(request, index);
    if (this.showPickerForItem === key) {
      this.showPickerForItem = null;
      return;
    }

    this.showPickerForItem = key;
    this.isLoadingStock = true;

    // Load equipment matching the type/category
    this.equipmentService.getAllEquipment().subscribe(all => {
      const matching = all.filter(e =>
        e.type === item.type &&
        e.category === item.category &&
        (e.status === 'Available' || e.status === 'In stock')
      );
      this.availableStockByItem.set(key, matching);
      this.isLoadingStock = false;
    });
  }

  isPicked(request: PartRequest, itemIndex: number, eqId: string): boolean {
    const key = this.getItemKey(request, itemIndex);
    const picked = this.pickedEquipmentByItem.get(key) || [];
    return picked.some(e => e.id === eqId);
  }

  pickEquipment(request: PartRequest, itemIndex: number, equipment: any): void {
    const key = this.getItemKey(request, itemIndex);
    let picked = this.pickedEquipmentByItem.get(key) || [];

    const requestItem = request.items[itemIndex];
    if (picked.length >= requestItem.quantity) {
      if (requestItem.quantity === 1) {
        // Auto-replace for single item requests instead of showing an error
        picked = [];
      } else {
        this.showAlert(
          'Quantity Exceeded',
          `You requested ${requestItem.quantity} units of "${requestItem.partName}", and you have already matched this amount from stock. Please remove an existing selection first.`,
          'error'
        );
        return;
      }
    }

    if (!picked.some(e => e.id === equipment.id)) {
      picked.push(equipment);
      this.pickedEquipmentByItem.set(key, [...picked]);

      // NEW: Persist the match to the backend immediately so the technician sees it
      const updatedItems = [...request.items];
      updatedItems[itemIndex] = {
        ...updatedItems[itemIndex],
        matchedEquipmentName: equipment.equipmentName || equipment.name || updatedItems[itemIndex].partName,
        matchedSpecification: equipment.specification || updatedItems[itemIndex].specification,
        matchedSerialNumber: equipment.serialNumber,
        brand: equipment.brand || updatedItems[itemIndex].brand,
        equipmentId: equipment.id // Store the ID to signify it's no longer "pending match"
      };

      if (request.id) {
        this.partRequestService.updateRequest(request.id, { items: updatedItems }).subscribe({
          next: () => {
            request.items = updatedItems; // Update local reference
            console.log('Match persisted to backend for technician visibility.');
            // Automatically check for approval when a match is made
            this.toggleItemSelection(request, itemIndex, true);
          },
          error: (err) => console.error('Failed to persist match', err)
        });
      }
    }
  }

  removePickedEquipment(request: PartRequest, itemIndex: number, eqId: string): void {
    const key = this.getItemKey(request, itemIndex);
    let picked = this.pickedEquipmentByItem.get(key) || [];
    picked = picked.filter(e => e.id !== eqId);
    this.pickedEquipmentByItem.set(key, picked);

    // NEW: Clear the match from the backend immediately
    const updatedItems = [...request.items];
    updatedItems[itemIndex] = {
      ...updatedItems[itemIndex],
      matchedEquipmentName: undefined,
      matchedSpecification: undefined,
      matchedSerialNumber: undefined,
      equipmentId: undefined
    };

    if (request.id) {
      this.partRequestService.updateRequest(request.id, { items: updatedItems }).subscribe({
        next: () => {
          request.items = updatedItems; // Update local reference
          console.log('Match cleared from backend.');
          // Uncheck if no more matches exist
          if (picked.length === 0) {
            this.toggleItemSelection(request, itemIndex, false);
          }
        },
        error: (err) => console.error('Failed to clear match', err)
      });
    }
  }

  getPickedCount(request: PartRequest, itemIndex: number): number {
    const key = this.getItemKey(request, itemIndex);
    return (this.pickedEquipmentByItem.get(key) || []).length;
  }

  getPickedList(request: PartRequest, itemIndex: number): any[] {
    const key = this.getItemKey(request, itemIndex);
    return this.pickedEquipmentByItem.get(key) || [];
  }

  toggleItemSelection(request: PartRequest, index: number, forceState?: boolean): void {
    const item = request.items[index];
    if (this.isItemAlreadyProcessed(item)) return; // Cannot toggle already processed items

    const key = this.getItemKey(request, index);
    const current = this.itemCheckboxes.get(key) || false;
    this.itemCheckboxes.set(key, forceState !== undefined ? forceState : !current);
  }

  isTechnicianStockSelection(item: PartRequestItem): boolean {
    return !!item.equipmentId && !item.matchedEquipmentName;
  }

  isItemChecked(request: PartRequest, index: number): boolean {
    const key = this.getItemKey(request, index);
    return this.itemCheckboxes.get(key) || false;
  }

  isAllChecked(request: PartRequest): boolean {
    return (request.items || []).every((item, idx) =>
      this.isItemAlreadyProcessed(item) || this.isItemChecked(request, idx)
    );
  }

  canCompleteRequest(request: PartRequest): boolean {
    // A request can be completed if at least one UNPROCESSED item is checked
    const hasNewSelections = request.items.some((item, idx) => {
      if (this.isItemAlreadyProcessed(item)) return false;
      return this.isItemChecked(request, idx);
    });

    if (hasNewSelections) return true;

    // OR if everything is already processed, we can finalize the APPROVED status
    return (request.items || []).every(item => this.isItemAlreadyProcessed(item));
  }

  /**
   * Returns true when an item has already been allocated in a partial approval.
   */
  isItemAlreadyProcessed(item: any): boolean {
    return item.processed === true;
  }

  /**
   * Returns a warning message if any checked item is a custom item with no
   * picks selected, meaning the manager cannot allocate it yet.
   */
  getApprovalWarnings(request: PartRequest): string[] {
    const warnings: string[] = [];
    request.items.forEach((item, idx) => {
      if (!this.isItemChecked(request, idx)) return;
      if (this.isItemAlreadyProcessed(item)) return;
      // Custom item (no original equipmentId) but no stock picks selected
      if (!item.equipmentId && this.getPickedCount(request, idx) === 0) {
        warnings.push(`"${item.partName}" is checked but has no stock match selected.`);
      }
    });
    return warnings;
  }

  completeApproval(request: PartRequest): void {
    if (!request.id) return;

    // ── Pre-approval validation ──────────────────────────────────────────────
    // Warn if any checked item is a custom part with no stock match selected yet.
    const warnings = this.getApprovalWarnings(request);
    if (warnings.length > 0) {
      this.showAlert(
        'Incomplete Allocation',
        `The following items are checked but have no stock match selected:\n\n` +
        warnings.map(w => `• ${w}`).join('\n') +
        `\n\nPlease match them or uncheck them before approving.`,
        'warning'
      );
      return;
    }
    // ────────────────────────────────────────────────────────────────────────

    this.processingId = request.id;

    // Collect all accepted items
    const allAccepted: any[] = [];
    let allChecked = true;

    // We'll also update the request's items with matched IDs to clear 'Custom' label
    const updatedItems = [...(request.items || [])];

    request.items.forEach((item, idx) => {
      // If already processed, we don't need to do anything but it counts as 'checked' for full approval logic
      if (this.isItemAlreadyProcessed(item)) {
        return;
      }

      const checked = this.isItemChecked(request, idx);
      if (!checked) {
        allChecked = false;
        return;
      }

      if (item.equipmentId) {
        // Already in stock, mark as processed
        updatedItems[idx] = { ...item, processed: true };

        allAccepted.push({
          name: item.partName,
          brand: item.brand,
          type: item.type,
          specification: item.specification,
          qty: item.quantity,
          equipmentId: item.equipmentId
        });
      } else {
        const picked = this.getPickedList(request, idx);
        if (picked.length > 0) {
          // Update the matched item info and mark as processed
          updatedItems[idx] = {
            ...updatedItems[idx],
            equipmentId: picked[0].id,
            brand: picked[0].brand || updatedItems[idx].brand,
            matchedEquipmentName: picked[0].equipmentName || picked[0].name || updatedItems[idx].partName,
            matchedSpecification: picked[0].specification || updatedItems[idx].specification,
            processed: true
          };

          picked.forEach(p => {
            allAccepted.push({
              name: item.partName,
              brand: p.brand,
              type: item.type,
              specification: p.specification || item.specification,
              qty: 1,
              equipmentId: p.id
            });
          });
        }
      }
    });

    if (allAccepted.length === 0) {
      // If everything is already processed and all are checked, we just need to finalize the status
      if (allChecked) {
        this.partRequestService.updateStatus(request.id!, 'APPROVED').subscribe({
          next: () => this.finalizeUI(request),
          error: (err) => {
            console.error('Error finalising status', err);
            this.processingId = null;
          }
        });
      } else {
        this.processingId = null;
        this.showAlert('No Selection', 'Please select at least one item to process.', 'warning');
      }
      return;
    }

    // ── Optimized Approval Flow ──────────────────────────────────────────
    // 1. First, perform the actual stock allocation
    this.equipmentService.allocateParts(request.requesterId, request.requesterName, allAccepted).subscribe({
      next: () => {
        // 2. If allocation succeeds, persist the 'processed' flags to the request
        this.partRequestService.updateRequest(request.id!, { items: updatedItems }).subscribe({
          next: () => {
            // 3. Finalize status if everything is now done
            if (allChecked) {
              this.partRequestService.updateStatus(request.id!, 'APPROVED').subscribe({
                next: () => this.finalizeUI(request),
                error: (err) => {
                  console.error('Error finalising status', err);
                  this.processingId = null;
                }
              });
            } else {
              // Partial approval success
              this.showAlert(
                'Allocation Successful',
                'The selected items have been allocated to the technician. The request remains active for the remaining parts.',
                'success'
              );
              this.finalizeUI(request);
            }
          },
          error: (err) => {
            console.error('Failed to update request items', err);
            this.processingId = null;
            this.showAlert('Partial Success', 'Parts were allocated in stock, but request records failed to update. Please refresh.', 'warning');
          }
        });
      },
      error: (err) => {
        this.processingId = null;
      }
    });
  }

  resetProcessedItem(request: PartRequest, index: number): void {
    if (!request.id) return;

    const updatedItems = [...request.items];
    updatedItems[index] = { ...updatedItems[index], processed: false };

    this.partRequestService.updateRequest(request.id, { items: updatedItems }).subscribe({
      next: () => {
        request.items = updatedItems;
        console.log('Item reset to unprocessed state.');
        // After reset, we need to refresh the selection state
        this.toggleItemSelection(request, index, false);
      },
      error: (err) => console.error('Failed to reset item', err)
    });
  }

  private finalizeUI(request: PartRequest): void {
    this.loadAllRequests();
    this.processingId = null;
    this.expandedRequestId = null;
    // Clear pick state for this request
    request.items.forEach((_, idx) => {
      const key = this.getItemKey(request, idx);
      this.pickedEquipmentByItem.delete(key);
      this.itemCheckboxes.delete(key);
    });
  }

  getPriorityColor(priority?: string): string {
    switch (priority) {
      case 'High': return '#ef4444'; // Red
      case 'Medium': return '#f59e0b'; // Orange
      case 'Low': return '#3b82f6'; // Blue
      default: return '#64748b'; // Gray
    }
  }

  getStatusBadgeClass(status?: string): string {
    switch (status) {
      case 'APPROVED': return 'badge-approved';
      case 'REJECTED': return 'badge-rejected';
      case 'CANCELLED': return 'badge-cancelled';
      case 'PENDING': return 'badge-pending';
      default: return 'badge-default';
    }
  }

  showAlert(title: string, message: string, type: 'warning' | 'error' | 'success' = 'warning'): void {
    this.alertConfig = { show: true, title, message, type };
  }

  closeAlert(): void {
    this.alertConfig.show = false;
  }
}
