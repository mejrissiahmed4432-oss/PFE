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
import { ProcurementService } from './procurement.service';
import { EquipmentHistoryComponent } from './equipment-history/equipment-history.component';

type ProcTab = 'dashboard' | 'new-request' | 'my-requests' | 'approvals' | 'rfq' | 'responses' | 'orders' | 'history';

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
    EquipmentHistoryComponent,
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
  pendingApprovalsCount = 0;
  pendingRfqCount = 0;
  pendingQuotationsCount = 0;

  get isStockManager(): boolean { return this.userRole === 'STOCK_MANAGER'; }
  get isItManager(): boolean    { return this.userRole === 'IT_MANAGER'; }

  constructor(private procService: ProcurementService) {}

  ngOnInit(): void {
    this.loadPendingCount();
    // Refresh count whenever a request is created/updated
    this.procService.requestCreated$.subscribe(() => this.loadPendingCount());
  }

  loadPendingCount(): void {
    if (this.isItManager) {
      this.procService.getAllRequests().subscribe(reqs => {
        this.pendingApprovalsCount = reqs.filter(r => r.status === 'PENDING_IT_APPROVAL').length;
        this.pendingRfqCount = reqs.filter(r => r.status === 'APPROVED').length;
        this.pendingQuotationsCount = reqs.filter(r => r.status === 'RESPONDED').length;
      });
    }
  }

  get tabs(): { id: ProcTab; label: string; icon: string; roles: string[]; count?: number }[] {
    return [
      { id: 'dashboard' as ProcTab,    label: 'Overview',      icon: 'chart',    roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'new-request' as ProcTab,  label: 'New Request',   icon: 'plus',     roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'my-requests' as ProcTab,  label: 'Requests',      icon: 'list',     roles: ['STOCK_MANAGER', 'IT_MANAGER'] },
      { id: 'approvals' as ProcTab,    label: 'Approvals',     icon: 'check',    roles: ['IT_MANAGER'], count: this.pendingApprovalsCount },
      { id: 'rfq' as ProcTab,          label: 'Send RFQ',      icon: 'send',     roles: ['IT_MANAGER'], count: this.pendingRfqCount },
      { id: 'responses' as ProcTab,    label: 'Quotations',    icon: 'mail',     roles: ['IT_MANAGER'], count: this.pendingQuotationsCount },
    ].filter(t => t.roles.includes(this.userRole));
  }

  onSendToRFQ(req: EquipmentRequest): void {
    this.activeTab = 'rfq';
  }

  onCreateOrder(response: SupplierResponse): void {
    this.activeTab = 'orders';
    setTimeout(() => this.orderView?.prepareOrder(response), 100);
  }
}
