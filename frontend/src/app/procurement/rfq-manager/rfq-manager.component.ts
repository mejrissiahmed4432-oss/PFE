import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
    this.procService.getAllRequests().subscribe(data => {
      this.approvedRequests = data.filter(r => r.status === 'APPROVED');
    });
    this.supplierService.getAllSuppliers().subscribe(data => {
      this.suppliers = data || [];
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
      next: () => {
        this.isProcessing = false;
        this.selectedRequest = null;
        this.selectedItemIndices = [];
        this.selectedSupplierIds = [];
        this.load();
        this.toastService.success('RFQ sent successfully to ' + emails.length + ' supplier(s)!');
      },
      error: () => {
        this.isProcessing = false;
        this.toastService.error('Failed to send RFQ. Please check backend connection.');
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
