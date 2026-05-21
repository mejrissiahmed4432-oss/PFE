import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { ToastService } from '../../shared/toast.service';
import { EquipmentRequest, STATUS_META, RequestStatus, PurchaseOrder } from '../procurement.models';

@Component({
  selector: 'app-equipment-request-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-request-list.component.html',
  styleUrls: ['./equipment-request-list.component.css']
})
export class EquipmentRequestListComponent implements OnInit {
  @Input() userId: string = '';
  @Input() userRole: string = '';
  @Output() sendToRFQ = new EventEmitter<EquipmentRequest>();

  requests: EquipmentRequest[] = [];
  loading = true;
  filterStatus = 'PENDING';
  searchQuery = '';
  viewMode: 'list' | 'card' = 'list';
  objectKeys = Object.keys;

  showEditModal = false;
  editingRequest: EquipmentRequest | null = null;
  editingNotes = '';
  isSavingEdit = false;

  showReceiptModal = false;
  submittingReceipt = false;
  selectedOrderForReceipt: PurchaseOrder | null = null;
  receiptNotes = '';
  receiptRating = 5;
  checkedItems: boolean[] = [];

  constructor(
    private procService: ProcurementService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    if (this.userRole === 'STOCK_MANAGER' || this.userRole === 'IT_MANAGER') {
      this.procService.getAllRequests().subscribe({
        next: (data) => {
          if (this.userRole === 'STOCK_MANAGER') {
            this.requests = data.filter(r => 
              r.createdByUserId === this.userId || 
              r.status === 'ORDER_CONFIRMED' || 
              r.status === 'RECEIVED' ||
              r.status === 'REJECTED'
            );
          } else {
            this.requests = data;
          }
          this.loading = false;
        },
        error: () => { this.loading = false; }
      });
    } else {
      this.procService.getRequestsByUser(this.userId).subscribe({
        next: (data) => { this.requests = data; this.loading = false; },
        error: () => { this.loading = false; }
      });
    }
  }

  get filteredRequests(): EquipmentRequest[] {
    return this.requests.filter(r => {
      const status = r.status || 'PENDING_IT_APPROVAL';
      let matchesStatus = status === this.filterStatus;
      if (this.filterStatus === 'APPROVED') matchesStatus = ['APPROVED', 'SENT_TO_SUPPLIERS', 'RESPONDED'].includes(status);
      if (this.filterStatus === 'INBOUND') matchesStatus = status === 'ORDER_CONFIRMED';
      if (this.filterStatus === 'PENDING') matchesStatus = status === 'PENDING_IT_APPROVAL';
      if (this.filterStatus === 'RECEIVED') matchesStatus = status === 'RECEIVED';
      if (this.filterStatus === 'REJECTED') matchesStatus = status === 'REJECTED';
      const matchesSearch = !this.searchQuery || 
        r.items.some(i => i.name.toLowerCase().includes(this.searchQuery.toLowerCase())) ||
        (r.createdByName && r.createdByName.toLowerCase().includes(this.searchQuery.toLowerCase()));
      return matchesStatus && matchesSearch;
    });
  }

  get filterOptions() {
    return [
      { id: 'PENDING', label: 'Pending' },
      { id: 'APPROVED', label: 'Approved' },
      { id: 'INBOUND', label: 'Inbound' },
      { id: 'RECEIVED', label: 'Received' },
      { id: 'REJECTED', label: 'Rejected' }
    ];
  }

  canEdit(req: EquipmentRequest): boolean {
    const isLocked = ['SENT_TO_SUPPLIERS', 'RESPONDED', 'ORDER_CONFIRMED', 'RECEIVED'].includes(req.status || '');
    if (isLocked) return false;
    if (this.userRole === 'IT_MANAGER') return true;
    return req.createdByUserId === this.userId;
  }

  openEditModal(req: EquipmentRequest): void {
    this.editingRequest = JSON.parse(JSON.stringify(req));
    this.editingNotes = this.editingRequest?.notes || '';
    this.showEditModal = true;
  }

  updateItemQty(index: number, delta: number): void {
    if (!this.editingRequest?.items) return;
    const item = this.editingRequest.items[index];
    item.quantity = Math.max(1, item.quantity + delta);
  }

  removeItem(index: number): void {
    this.editingRequest?.items?.splice(index, 1);
  }

  saveEdit(): void {
    if (!this.editingRequest || !this.editingRequest.id) return;
    this.isSavingEdit = true;
    this.editingRequest.notes = this.editingNotes;
    this.procService.updateRequest(this.editingRequest.id, this.editingRequest, this.userId, this.userRole === 'IT_MANAGER')
      .subscribe({
        next: () => {
          this.isSavingEdit = false;
          this.showEditModal = false;
          this.toastService.success('Request synchronized successfully.');
          this.load();
        },
        error: () => {
          this.isSavingEdit = false;
          this.toastService.error('Failed to sync changes.');
        }
      });
  }

  openReceiptModal(req: EquipmentRequest): void {
    this.procService.getOrderByRequest(req.id!).subscribe({
      next: (po) => {
        this.selectedOrderForReceipt = po;
        this.receiptNotes = '';
        this.receiptRating = 5;
        this.checkedItems = new Array(po.items?.length || 0).fill(false);
        this.showReceiptModal = true;
      },
      error: () => this.toastService.error('Packet manifest not found.')
    });
  }

  get checkedCount(): number { return this.checkedItems.filter(v => v).length; }
  get allChecked(): boolean { return this.checkedItems.length > 0 && this.checkedItems.every(v => v); }
  getInvoiceUrl(): string { return this.selectedOrderForReceipt?.selectedResponseId ? this.procService.getViewUrl(this.selectedOrderForReceipt.selectedResponseId) : '#'; }

  submitReceipt(postToStock: boolean): void {
    if (!this.selectedOrderForReceipt) return;
    this.submittingReceipt = true;
    this.procService.confirmReceipt(this.selectedOrderForReceipt.id!, this.receiptNotes, this.receiptRating, postToStock)
      .subscribe({
        next: () => {
          this.submittingReceipt = false;
          this.showReceiptModal = false;
          this.toastService.success('Manifest committed to local inventory.');
          this.load();
        },
        error: () => { this.submittingReceipt = false; this.toastService.error('Commit failed.'); }
      });
  }

  getStatusMeta(status?: RequestStatus) { 
    const meta = STATUS_META[status || 'PENDING_IT_APPROVAL'];
    let colorClass = 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10';
    if (status === 'APPROVED') colorClass = 'bg-emerald-50 text-emerald-600 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20';
    if (status === 'REJECTED') colorClass = 'bg-rose-50 text-rose-600 border-rose-100 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20';
    if (status === 'PENDING_IT_APPROVAL') colorClass = 'bg-amber-50 text-amber-600 border-amber-100 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20';
    if (status === 'RECEIVED') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    if (status === 'SENT_TO_SUPPLIERS') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    if (status === 'RESPONDED') colorClass = 'bg-cyan-50 text-cyan-600 border-cyan-100 dark:bg-cyan-500/10 dark:text-cyan-400 dark:border-cyan-500/20';
    if (status === 'ORDER_CONFIRMED') colorClass = 'bg-emerald-50 text-emerald-600 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20';
    return { ...meta, colorClass };
  }
  
  formatDate(d?: string): string { return d ? new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }) : ''; }
}
