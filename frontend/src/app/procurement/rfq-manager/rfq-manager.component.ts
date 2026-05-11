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
  template: `
    <div class="bg-white/70 backdrop-blur-md border border-slate-200 rounded-2xl p-6 shadow-sm">
      <div class="flex items-center gap-4 mb-6 pb-6 border-b border-slate-100">
        <div class="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
        </div>
        <div>
          <h2 class="text-lg font-bold text-slate-800 m-0">Direct Email RFQ</h2>
          <p class="text-sm text-slate-500 mt-1">Suppliers will receive the request via email. No login required for them.</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <!-- Approved Requests waiting for RFQ -->
        <div>
          <h3 class="text-sm font-bold text-slate-400 uppercase tracking-wider mb-4">Requests to Process</h3>
          <div class="flex flex-col gap-3">
            <div *ngFor="let req of approvedRequests" 
                 class="p-4 border rounded-xl transition-all cursor-pointer"
                 [ngClass]="selectedRequest?.id === req.id ? 'border-indigo-600 bg-indigo-50/30' : 'border-slate-200 hover:border-indigo-300'"
                 (click)="selectRequest(req)">
              <div class="flex justify-between mb-2">
                <span class="font-bold text-slate-700 text-sm">{{ req.items.length }} Items</span>
                <span class="text-xs text-slate-400">{{ formatDate(req.createdAt) }}</span>
              </div>
              <div class="text-[10px] text-slate-500 font-medium line-clamp-1 mb-2">
                {{ getItemList(req) }}
              </div>
              <p class="text-[10px] font-black text-indigo-500 uppercase tracking-tighter">{{ req.createdByName }}</p>
            </div>
            <div *ngIf="approvedRequests.length === 0" class="text-center p-8 text-slate-400 text-sm italic">
              No approved requests waiting for RFQ.
            </div>
          </div>
        </div>

        <!-- RFQ Form -->
        <div *ngIf="selectedRequest" class="flex flex-col gap-6 p-6 bg-slate-50 rounded-2xl border border-slate-200">
          <div>
            <div class="flex justify-between items-center mb-2">
              <h4 class="font-bold text-slate-800">Selected Request Details</h4>
              <label class="flex items-center gap-2 text-sm font-semibold text-indigo-600 cursor-pointer hover:text-indigo-800 transition-colors">
                <input type="checkbox" 
                       [checked]="selectedItemIndices.length === selectedRequest.items.length && selectedRequest.items.length > 0"
                       (change)="toggleSelectAllItems()"
                       class="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer">
                Select All Items
              </label>
            </div>
            <ul class="text-sm text-slate-600 list-none p-0 flex flex-col gap-3">
              <li *ngFor="let item of selectedRequest.items; let i = index" 
                  class="bg-white border rounded-xl p-3 flex items-start gap-3 transition-colors"
                  [ngClass]="selectedItemIndices.includes(i) ? 'border-indigo-300 bg-indigo-50/30' : 'border-slate-200'">
                
                <div class="pt-0.5">
                  <input type="checkbox" 
                         [checked]="selectedItemIndices.includes(i)" 
                         (change)="toggleItemSelection(i)"
                         class="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer">
                </div>
                
                <div class="flex-1 cursor-pointer" (click)="toggleItemSelection(i)">
                  <div class="font-bold text-slate-800 mb-1" [ngClass]="{'opacity-50 line-through': !selectedItemIndices.includes(i)}">
                    {{ item.name }} x {{ item.quantity }}
                  </div>
                  <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-1" [ngClass]="{'opacity-50': !selectedItemIndices.includes(i)}">
                    <span *ngFor="let key of objectKeys(item.selectedSpecs)" class="px-1.5 py-0.5 bg-slate-100 text-slate-600 text-[10px] rounded border border-slate-200">
                      {{ key }}: {{ item.selectedSpecs[key] }}
                    </span>
                  </div>
                </div>
              </li>
            </ul>
          </div>

          <div class="flex flex-col gap-3">
            <h4 class="font-bold text-slate-800">Select Suppliers</h4>
            <div *ngIf="suppliers.length === 0" class="text-sm text-slate-500 italic p-4 bg-white rounded-xl border border-slate-200 text-center">
              No suppliers found. Please add suppliers in the Supplier Management section.
            </div>
            <div *ngIf="suppliers.length > 0" class="flex flex-col gap-2 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
              <label *ngFor="let supplier of suppliers" 
                     class="flex items-center gap-3 p-3 bg-white border rounded-xl cursor-pointer transition-colors"
                     [ngClass]="selectedSupplierIds.includes(supplier.id!) ? 'border-indigo-400 bg-indigo-50/50' : 'border-slate-200 hover:border-indigo-300'">
                <input type="checkbox" 
                       [checked]="selectedSupplierIds.includes(supplier.id!)"
                       (change)="toggleSupplierSelection(supplier.id!)"
                       class="w-4 h-4 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500 cursor-pointer">
                <div class="flex-1">
                  <div class="font-bold text-slate-800 text-sm">{{ supplier.companyName }}</div>
                  <div class="text-xs text-slate-500">{{ supplier.email }} | {{ supplier.category }}</div>
                </div>
              </label>
            </div>
            <p class="text-[11px] text-slate-400 italic mt-1">Selected suppliers will receive an official PDF request to their saved email address.</p>
          </div>

          <button (click)="sendRFQ()" 
                  [disabled]="selectedSupplierIds.length === 0 || isProcessing || selectedItemIndices.length === 0"
                  class="w-full py-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-slate-300 text-white font-bold rounded-xl shadow-lg shadow-indigo-600/20 transition-all flex items-center justify-center gap-2">
            <span *ngIf="isProcessing" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
            {{ isProcessing ? 'Sending RFQs...' : 'Generate & Send RFQ PDF' }}
          </button>
        </div>
      </div>
    </div>
  `
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
    // Default to selecting all items when a request is selected
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
      this.selectedItemIndices = []; // Deselect all
    } else {
      this.selectedItemIndices = this.selectedRequest.items.map((_, i) => i); // Select all
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

    // Get emails for selected suppliers
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
        this.showSuccess('RFQ sent successfully to ' + emails.length + ' supplier(s)!');
      },
      error: () => {
        this.isProcessing = false;
        this.showError('Failed to send RFQ. Please check backend connection.');
      }
    });
  }

  private showSuccess(msg: string): void {
    this.toastService.success(msg);
  }

  private showError(msg: string): void {
    this.toastService.error(msg);
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
