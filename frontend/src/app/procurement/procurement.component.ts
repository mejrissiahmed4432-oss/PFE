import { Component, OnInit, Input, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EquipmentRequestFormComponent } from './equipment-request-form/equipment-request-form.component';
import { EquipmentRequestListComponent } from './equipment-request-list/equipment-request-list.component';
import { ItApprovalDashboardComponent } from './it-approval-dashboard/it-approval-dashboard.component';
import { RfqManagerComponent } from './rfq-manager/rfq-manager.component';
import { SupplierResponseListComponent } from './supplier-response-list/supplier-response-list.component';
import { PurchaseOrderViewComponent } from './purchase-order-view/purchase-order-view.component';
import { ProcurementDashboardComponent } from './procurement-dashboard/procurement-dashboard.component';
import { EquipmentRequest, SupplierResponse } from './procurement.models';

type ProcTab = 'dashboard' | 'new-request' | 'my-requests' | 'approvals' | 'rfq' | 'responses' | 'orders';

@Component({
  selector: 'app-procurement',
  standalone: true,
  imports: [
    CommonModule,
    EquipmentRequestFormComponent,
    EquipmentRequestListComponent,
    ItApprovalDashboardComponent,
    RfqManagerComponent,
    SupplierResponseListComponent,
    PurchaseOrderViewComponent,
    ProcurementDashboardComponent,
  ],
  templateUrl: './procurement.component.html',
  styleUrls: ['./procurement.component.css']
})
export class ProcurementComponent implements OnInit {
  @Input() userRole: string = '';
  @Input() userId: string = '';
  @Input() userName: string = '';

  @ViewChild(PurchaseOrderViewComponent) orderView?: PurchaseOrderViewComponent;

  activeTab: ProcTab = 'dashboard';

  get isStockManager(): boolean { return this.userRole === 'STOCK_MANAGER'; }
  get isItManager(): boolean    { return this.userRole === 'IT_MANAGER'; }

  get tabs(): { id: ProcTab; label: string; icon: string; roles: string[] }[] {
    return [
      { id: 'dashboard' as ProcTab,    label: 'Overview',      icon: '📊', roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'new-request' as ProcTab,  label: 'New Request',   icon: '➕', roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'my-requests' as ProcTab,  label: 'My Requests',   icon: '📋', roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'approvals' as ProcTab,    label: 'Approvals',     icon: '✅', roles: ['IT_MANAGER'] },
      { id: 'rfq' as ProcTab,          label: 'Send RFQ',      icon: '📤', roles: ['IT_MANAGER'] },
      { id: 'responses' as ProcTab,    label: 'Quotations',    icon: '📩', roles: ['IT_MANAGER'] },
    ].filter(t => t.roles.includes(this.userRole));
  }

  ngOnInit(): void {}

  onSendToRFQ(req: EquipmentRequest): void {
    this.activeTab = 'rfq';
  }

  onCreateOrder(response: SupplierResponse): void {
    this.activeTab = 'orders';
    setTimeout(() => this.orderView?.prepareOrder(response), 100);
  }
}
