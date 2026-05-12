import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProcurementService } from '../procurement.service';
import { PurchaseOrder, SupplierResponse } from '../procurement.models';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-purchase-order-view',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-6">
      <!-- Order Creation Overlay (if preparing an order) -->
      <div *ngIf="preparingOrder" class="bg-white border-2 border-indigo-200 rounded-2xl p-8 shadow-xl animate-fade-in-up">
        <h2 class="text-xl font-bold text-slate-800 mb-4 flex items-center gap-3">
          <span class="text-2xl">🛒</span> Confirm Purchase Order
        </h2>
        <div class="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
          <div class="space-y-2">
            <p class="text-sm font-bold text-slate-400 uppercase tracking-wider">Supplier</p>
            <p class="text-lg font-bold text-indigo-600">{{ preparingOrder.supplierName }}</p>
          </div>
          <div class="space-y-2">
            <p class="text-sm font-bold text-slate-400 uppercase tracking-wider">Total Amount</p>
            <p class="text-lg font-bold text-slate-800">{{ preparingOrder.totalPrice }} {{ preparingOrder.currency || 'TND' }}</p>
          </div>
        </div>
        <div class="flex gap-4">
          <button (click)="confirmOrder()" class="px-6 py-3 bg-indigo-600 text-white font-bold rounded-xl hover:bg-indigo-700 transition-all">
            Confirm & Finalize Order
          </button>
          <button (click)="preparingOrder = null" class="px-6 py-3 bg-slate-100 text-slate-600 font-bold rounded-xl hover:bg-slate-200 transition-all">
            Cancel
          </button>
        </div>
      </div>

      <!-- History of Orders -->
      <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div class="p-6 border-b border-slate-100 flex justify-between items-center">
          <h3 class="font-bold text-slate-800 m-0">Purchase Order History</h3>
          <span class="text-xs text-slate-400">{{ orders.length }} Confirmed Orders</span>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-left">
            <tbody class="divide-y divide-slate-100">
              <ng-container *ngFor="let order of orders">
                <tr class="hover:bg-slate-50 transition-colors group">
                  <td class="px-6 py-4">
                    <button (click)="toggleExpand(order.id!)" class="p-1 text-slate-400 hover:text-indigo-600 transition-transform" [class.rotate-90]="isExpanded(order.id!)">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M9 18l6-6-6-6"/></svg>
                    </button>
                  </td>
                  <td class="px-6 py-4 font-mono text-xs text-slate-500">PO-{{ order.id?.substring(0,8)?.toUpperCase() }}</td>
                  <td class="px-6 py-4 font-bold text-slate-700 text-sm">{{ order.supplierName }}</td>
                  <td class="px-6 py-4 text-sm">{{ order.totalPrice }} {{ order.currency }}</td>
                  <td class="px-6 py-4 text-xs text-slate-500">
                    <div>Conf: {{ formatDate(order.createdAt) }}</div>
                    <div class="text-[10px] text-emerald-600 font-bold" *ngIf="order.deliveryDays">Est. Delivery: {{ getDeliveryDate(order.createdAt, order.deliveryDays) }}</div>
                  </td>
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-2">
                      <span class="px-2.5 py-1 rounded-full text-[10px] font-bold uppercase bg-emerald-100 text-emerald-700">Confirmed</span>
                      <a *ngIf="order.selectedResponseId" [href]="viewInvoiceUrl(order.selectedResponseId)" target="_blank" 
                         class="p-1.5 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all" title="View Invoice">
                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                      </a>
                    </div>
                  </td>
                </tr>
                <!-- Expandable Details Row -->
                <tr *ngIf="isExpanded(order.id!)" class="bg-slate-50/50">
                  <td colspan="6" class="px-12 py-6">
                    <div class="bg-white rounded-xl border border-slate-100 p-4 shadow-sm">
                      <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">Requested Items</p>
                      <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div *ngFor="let item of order.items" class="flex justify-between items-center p-3 bg-slate-50 rounded-lg border border-slate-100">
                          <div>
                            <div class="text-sm font-bold text-slate-700">{{ item.name }}</div>
                            <div class="text-[10px] text-slate-500 mb-1">{{ item.description }}</div>
                            <!-- Specifications -->
                            <div *ngIf="item.selectedSpecs && getKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-1 mt-1">
                              <span *ngFor="let key of getKeys(item.selectedSpecs)" class="px-1.5 py-0.5 bg-slate-50 text-slate-500 text-[9px] font-semibold rounded border border-slate-200">
                                {{ key }}: {{ item.selectedSpecs[key] }}
                              </span>
                            </div>
                          </div>
                          <div class="text-xs font-black text-indigo-600 bg-indigo-50 px-2 py-1 rounded">x{{ item.quantity }}</div>
                        </div>
                      </div>
                      <div *ngIf="!order.items || order.items.length === 0" class="text-sm text-slate-400 italic">No item details available for this order.</div>
                    </div>
                  </td>
                </tr>
              </ng-container>
              <tr *ngIf="orders.length === 0">
                <td colspan="6" class="px-6 py-12 text-center text-slate-400 italic">No purchase orders found.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `
})
export class PurchaseOrderViewComponent implements OnInit {
  orders: PurchaseOrder[] = [];
  preparingOrder: SupplierResponse | null = null;
  expandedOrders = new Set<string>();
  getKeys = Object.keys;

  constructor(
    private procService: ProcurementService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.procService.getAllOrders().subscribe(data => this.orders = data);
  }

  toggleExpand(id: string): void {
    if (this.expandedOrders.has(id)) {
      this.expandedOrders.delete(id);
    } else {
      this.expandedOrders.add(id);
    }
  }

  isExpanded(id: string): boolean {
    return this.expandedOrders.has(id);
  }

  getDeliveryDate(createdAt: any, days: number): string {
    if (!createdAt || !days) return 'N/A';
    const date = new Date(createdAt);
    date.setDate(date.getDate() + days);
    return date.toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
  }

  prepareOrder(response: SupplierResponse): void {
    this.preparingOrder = response;
  }

  confirmOrder(): void {
    if (!this.preparingOrder) return;
    
    this.procService.createOrder({
      requestId: this.preparingOrder.requestId!, 
      rfqId: this.preparingOrder.rfqId!, 
      responseId: this.preparingOrder.id!
    }).subscribe({
      next: () => {
        this.preparingOrder = null;
        this.load();
        this.toastService.success('Purchase Order confirmed! Supplier notified via email.');
      },
      error: () => this.toastService.error('Failed to create order.')
    });
  }

  viewInvoiceUrl(responseId: string): string {
    return this.procService.getViewUrl(responseId);
  }

  formatDate(d?: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
  }
}
