import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProcurementService } from '../procurement.service';
import { PurchaseOrder, SupplierResponse } from '../procurement.models';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-purchase-order-view',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './purchase-order-view.component.html',
  styleUrls: ['./purchase-order-view.component.css']
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
