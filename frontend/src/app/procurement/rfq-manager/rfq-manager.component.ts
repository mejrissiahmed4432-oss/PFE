import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ProcurementService } from '../procurement.service';
import { SupplierService } from '../../supplier/supplier.service';
import { EquipmentRequest, RFQ } from '../procurement.models';
import { Supplier } from '../../supplier/supplier.model';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-rfq-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './rfq-manager.component.html',
  styleUrls: ['./rfq-manager.component.css']
})
export class RfqManagerComponent implements OnInit {
  approvedRequests: EquipmentRequest[] = [];
  selectedRequest: EquipmentRequest | null = null;
  selectedItemIndices: number[] = [];

  suppliers: Supplier[] = [];
  selectedSupplierIds: string[] = [];

  // Failed RFQs
  failedRfqs: RFQ[] = [];
  selectedFailedRfq: RFQ | null = null;
  selectedFailedSupplierIds: string[] = [];
  viewMode: 'pending' | 'failed' = 'pending';

  isProcessing = false;
  objectKeys = Object.keys;

  constructor(
    private procService: ProcurementService,
    private supplierService: SupplierService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    forkJoin({
      requests: this.procService.getAllRequests(),
      suppliers: this.supplierService.getAllSuppliers(),
      rfqs: this.procService.getAllRFQs()
    }).subscribe(({ requests, suppliers, rfqs }) => {
      this.failedRfqs = rfqs.filter(r => r.status === 'FAILED');
      
      const failedRequestIds = new Set(this.failedRfqs.map(r => r.requestId));
      
      this.approvedRequests = requests.filter(r => 
        r.status === 'APPROVED' && !failedRequestIds.has(r.id!)
      );
      
      this.suppliers = suppliers || [];
    });
  }

  selectRequest(req: EquipmentRequest): void {
    this.selectedRequest = req;
    this.selectedItemIndices = req.items ? req.items.map((_, i) => i) : [];
  }

  toggleItemSelection(index: number): void {
    const pos = this.selectedItemIndices.indexOf(index);
    if (pos >= 0) {
      this.selectedItemIndices.splice(pos, 1);
    } else {
      this.selectedItemIndices.push(index);
    }
  }

  toggleSelectAllItems(): void {
    if (!this.selectedRequest || !this.selectedRequest.items) return;
    if (this.selectedItemIndices.length === this.selectedRequest.items.length) {
      this.selectedItemIndices = [];
    } else {
      this.selectedItemIndices = this.selectedRequest.items.map((_, i) => i);
    }
  }

  toggleSupplierSelection(supplierId: string): void {
    const pos = this.selectedSupplierIds.indexOf(supplierId);
    if (pos >= 0) {
      this.selectedSupplierIds.splice(pos, 1);
    } else {
      this.selectedSupplierIds.push(supplierId);
    }
  }

  sendRFQ(): void {
    if (!this.selectedRequest || this.selectedSupplierIds.length === 0 || this.selectedItemIndices.length === 0) return;

    const selectedSuppliers = this.suppliers.filter(s => this.selectedSupplierIds.includes(s.id!));
    const emails = selectedSuppliers.map(s => s.email).filter(e => !!e);

    if (emails.length === 0) {
      alert('Selected suppliers do not have valid email addresses.');
      return;
    }

    this.isProcessing = true;

    this.procService.createRFQ({
      requestId: this.selectedRequest.id!,
      supplierIds: this.selectedSupplierIds,
      supplierEmails: emails,
      selectedItemIndices: this.selectedItemIndices
    }).subscribe({
      next: (createdRfq) => {
        this.isProcessing = false;
        this.selectedRequest = null;
        this.selectedItemIndices = [];
        this.selectedSupplierIds = [];
        this.load();
        
        if (createdRfq && createdRfq.status === 'FAILED') {
          this.toastService.error('Some or all emails failed to send. Check Failed RFQs tab.');
        } else {
          this.toastService.success('RFQ sent successfully to ' + emails.length + ' supplier(s)!');
        }
      },
      error: () => {
        this.isProcessing = false;
        this.toastService.error('Failed to send RFQ. Please check backend connection.');
      }
    });
  }

  // ─── Failed RFQs Logic ───

  selectFailedRfq(rfq: RFQ): void {
    this.selectedFailedRfq = rfq;
    // Default select only the failed ones
    if (rfq.deliveryStatuses) {
      this.selectedFailedSupplierIds = rfq.deliveryStatuses
        .filter(s => s.status === 'FAILED')
        .map(s => s.supplierId);
    } else {
      this.selectedFailedSupplierIds = [];
    }
  }

  toggleFailedSupplierSelection(supplierId: string): void {
    const pos = this.selectedFailedSupplierIds.indexOf(supplierId);
    if (pos >= 0) {
      this.selectedFailedSupplierIds.splice(pos, 1);
    } else {
      this.selectedFailedSupplierIds.push(supplierId);
    }
  }

  resendFailedRfq(): void {
    if (!this.selectedFailedRfq || this.selectedFailedSupplierIds.length === 0) return;

    this.isProcessing = true;
    this.procService.resendRfq(this.selectedFailedRfq.id!, this.selectedFailedSupplierIds).subscribe({
      next: (updatedRfq) => {
        this.isProcessing = false;
        
        if (updatedRfq.status === 'SENT') {
          this.toastService.success('All emails sent successfully! RFQ is now marked as SENT.');
          this.selectedFailedRfq = null;
          this.selectedFailedSupplierIds = [];
          this.viewMode = 'pending';
        } else {
          this.toastService.error('Some emails still failed to send. Try again later.');
          // Update local state
          const idx = this.failedRfqs.findIndex(r => r.id === updatedRfq.id);
          if (idx !== -1) {
            this.failedRfqs[idx] = updatedRfq;
            if (this.selectedFailedRfq?.id === updatedRfq.id) {
              this.selectFailedRfq(updatedRfq); // re-select failed ones
            }
          }
        }
        this.load(); // Refresh lists
      },
      error: () => {
        this.isProcessing = false;
        this.toastService.error('An error occurred during resend. Please try again.');
      }
    });
  }

  getItemList(req: EquipmentRequest): string {
    if (!req.items || req.items.length === 0) return 'No items';
    return req.items.map(i => i.name).join(', ');
  }

  formatDate(d?: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
  }
}
