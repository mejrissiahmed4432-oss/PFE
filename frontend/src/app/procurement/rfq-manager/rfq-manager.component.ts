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
    <div class="bg-white dark:bg-slate-900 backdrop-blur-md border border-slate-200 dark:border-white/5 rounded-[2.5rem] p-10 shadow-sm dark:shadow-2xl transition-colors duration-300">
      <div class="flex items-center gap-6 mb-10 pb-8 border-b border-slate-100 dark:border-white/5">
        <div class="w-16 h-16 rounded-2xl bg-indigo-50 dark:bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center border border-indigo-100 dark:border-indigo-500/20 shadow-inner">
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
        </div>
        <div>
          <h2 class="text-2xl font-black text-slate-900 dark:text-white tracking-tighter m-0">Direct Email RFQ</h2>
          <p class="text-sm text-slate-500 mt-1 font-medium italic">Suppliers will receive the request via email. No login required for them.</p>
        </div>
      </div>

      <div class="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <!-- Approved Requests waiting for RFQ -->
        <div class="flex flex-col gap-6">
          <h3 class="text-[10px] font-black text-slate-400 dark:text-slate-500 uppercase tracking-[0.3em] mb-2">Requests Buffer Pool</h3>
          <div class="flex flex-col gap-4">
            <div *ngFor="let req of approvedRequests" 
                 class="p-6 border rounded-[2rem] transition-all cursor-pointer group relative overflow-hidden"
                 [ngClass]="selectedRequest?.id === req.id ? 'border-indigo-600 bg-indigo-50/30 dark:bg-indigo-600/5' : 'border-slate-100 dark:border-white/5 bg-slate-50/30 dark:bg-black/20 hover:border-indigo-300 dark:hover:border-indigo-500/30'"
                 (click)="selectRequest(req)">
              <div class="flex justify-between items-center mb-4">
                <span class="px-3 py-1 bg-indigo-100 dark:bg-indigo-600/10 text-indigo-600 dark:text-indigo-400 rounded-lg text-[10px] font-black uppercase tracking-widest">{{ req.items.length }} UNITS</span>
                <span class="text-[10px] text-slate-400 dark:text-slate-600 font-bold uppercase tracking-tighter">{{ formatDate(req.createdAt) }}</span>
              </div>
              <div class="text-sm font-black text-slate-800 dark:text-slate-200 line-clamp-2 mb-4 leading-relaxed group-hover:text-indigo-600 dark:group-hover:text-white transition-colors">
                {{ getItemList(req) }}
              </div>
              <div class="flex items-center gap-2">
                <div class="w-1 h-1 bg-indigo-500 rounded-full animate-pulse"></div>
                <p class="text-[9px] font-black text-indigo-500 dark:text-indigo-400 uppercase tracking-[0.2em]">{{ req.createdByName }}</p>
              </div>
            </div>
            <div *ngIf="approvedRequests.length === 0" class="flex flex-col items-center justify-center p-20 border-2 border-dashed border-slate-200 dark:border-white/5 rounded-[2.5rem] bg-slate-50/50 dark:bg-black/10">
              <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1" class="mb-4 opacity-20 text-slate-400" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
              <p class="text-[10px] font-black uppercase tracking-widest text-slate-400 dark:text-slate-600 italic">No approved requests waiting for RFQ.</p>
            </div>
          </div>
        </div>

        <!-- RFQ Form -->
        <div *ngIf="selectedRequest" class="flex flex-col gap-8 p-10 bg-slate-50 dark:bg-black/40 rounded-[3rem] border border-slate-200 dark:border-white/5 shadow-inner animate-in slide-in-from-right-6 duration-500">
          <div>
            <div class="flex justify-between items-center mb-6">
              <h4 class="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em]">Payload Optimization</h4>
              <button (click)="toggleSelectAllItems()"
                      class="px-4 py-2 bg-white dark:bg-white/5 border border-slate-200 dark:border-white/10 rounded-xl text-[9px] font-black text-indigo-600 dark:text-indigo-400 uppercase tracking-widest hover:bg-indigo-600 hover:text-white transition-all shadow-sm">
                {{ selectedItemIndices.length === selectedRequest.items.length ? 'Deselect All' : 'Select All Items' }}
              </button>
            </div>
            <div class="flex flex-col gap-4">
              <div *ngFor="let item of selectedRequest.items; let i = index" 
                  class="bg-white dark:bg-black/60 border rounded-2xl p-5 flex items-start gap-5 transition-all relative overflow-hidden"
                  [ngClass]="selectedItemIndices.includes(i) ? 'border-indigo-400 dark:border-indigo-500/30 bg-indigo-50/10' : 'border-slate-100 dark:border-white/5 opacity-50'">
                
                <div class="pt-1">
                  <input type="checkbox" 
                         [checked]="selectedItemIndices.includes(i)" 
                         (change)="toggleItemSelection(i)"
                         class="w-5 h-5 text-indigo-600 rounded-lg border-slate-300 dark:border-white/10 focus:ring-indigo-500 cursor-pointer bg-slate-50 dark:bg-black/40">
                </div>
                
                <div class="flex-1 cursor-pointer" (click)="toggleItemSelection(i)">
                  <div class="font-black text-slate-900 dark:text-white mb-2 leading-tight">
                    {{ item.name }} <span class="text-indigo-600 dark:text-indigo-400 ml-2">×{{ item.quantity }}</span>
                  </div>
                  <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-2">
                    <span *ngFor="let key of objectKeys(item.selectedSpecs)" class="px-2 py-1 bg-slate-100 dark:bg-white/5 text-slate-500 dark:text-slate-400 text-[9px] rounded-md border border-slate-200 dark:border-white/5 font-black uppercase tracking-tighter">
                      {{ key }}: {{ item.selectedSpecs[key] }}
                    </span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-4">
            <h4 class="text-[10px] font-black text-slate-500 uppercase tracking-[0.3em]">Vendor Distribution</h4>
            <div *ngIf="suppliers.length === 0" class="text-xs text-slate-500 italic p-8 bg-white dark:bg-black/60 rounded-[2rem] border border-slate-200 dark:border-white/5 text-center shadow-inner">
              No suppliers found. Please add suppliers in the Supplier Management section.
            </div>
            <div *ngIf="suppliers.length > 0" class="flex flex-col gap-3 max-h-72 overflow-y-auto pr-3 custom-scrollbar">
              <label *ngFor="let supplier of suppliers" 
                     class="flex items-center gap-5 p-5 bg-white dark:bg-black/60 border rounded-2xl cursor-pointer transition-all hover:border-indigo-400 group"
                     [ngClass]="selectedSupplierIds.includes(supplier.id!) ? 'border-indigo-600 dark:border-indigo-500/50 bg-indigo-50/10' : 'border-slate-100 dark:border-white/5'">
                <input type="checkbox" 
                       [checked]="selectedSupplierIds.includes(supplier.id!)"
                       (change)="toggleSupplierSelection(supplier.id!)"
                       class="w-5 h-5 text-indigo-600 rounded-lg border-slate-300 dark:border-white/10 focus:ring-indigo-500 cursor-pointer bg-slate-50 dark:bg-black/40">
                <div class="flex-1">
                  <div class="font-black text-slate-900 dark:text-white text-sm tracking-tight group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">{{ supplier.companyName }}</div>
                  <div class="text-[10px] text-slate-500 font-bold mt-1 opacity-70">{{ supplier.email }} | {{ supplier.category }}</div>
                </div>
              </label>
            </div>
            <p class="text-[10px] text-slate-400 dark:text-slate-600 font-bold italic mt-2 opacity-60">Selected suppliers will receive an official PDF request to their saved email address.</p>
          </div>

          <button (click)="sendRFQ()" 
                  [disabled]="selectedSupplierIds.length === 0 || isProcessing || selectedItemIndices.length === 0"
                  class="w-full py-5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-30 text-white font-black text-[10px] uppercase tracking-[0.3em] rounded-2xl shadow-xl shadow-indigo-600/20 transition-all flex items-center justify-center gap-4 active:scale-95 group">
            <span *ngIf="isProcessing" class="w-5 h-5 border-3 border-white/30 border-t-white rounded-full animate-spin"></span>
            <svg *ngIf="!isProcessing" class="group-hover:translate-x-1 group-hover:-translate-y-1 transition-transform" xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24"><path d="m22 2-7 20-4-9-9-4Z"/><path d="M22 2 11 13"/></svg>
            {{ isProcessing ? 'Sending RFQs...' : 'Commit & Disseminate RFQ' }}
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
