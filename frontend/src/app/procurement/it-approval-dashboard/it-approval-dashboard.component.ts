import { Component, OnInit, Output, EventEmitter, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { ToastService } from '../../shared/toast.service';
import { EquipmentRequest, STATUS_META, RequestStatus } from '../procurement.models';

@Component({
  selector: 'app-it-approval-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './it-approval-dashboard.component.html',
  styleUrls: ['./it-approval-dashboard.component.css']
})
export class ItApprovalDashboardComponent implements OnInit {
  @Input() userId: string = '';
  @Input() userRole: string = '';
  @Output() sendToRFQ = new EventEmitter<EquipmentRequest>();
  @Output() pendingCountChanged = new EventEmitter<number>();

  pendingRequests: EquipmentRequest[] = [];
  processedRequests: EquipmentRequest[] = [];
  isLoading = true;
  processingId: string | undefined | null = null;
  rejectingId: string | undefined | null = null;
  rejectReason = '';
  objectKeys = Object.keys;

  // Edit State
  showEditModal = false;
  editingRequest: EquipmentRequest | null = null;
  editingNotes = '';
  isSavingEdit = false;

  constructor(private procService: ProcurementService, private toastService: ToastService) {}
  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    this.procService.getAllRequests().subscribe({
      next: (data) => {
        this.pendingRequests = data.filter(r => r.status === 'PENDING_IT_APPROVAL');
        this.processedRequests = data.filter(r => r.status !== 'PENDING_IT_APPROVAL');
        this.pendingCountChanged.emit(this.pendingRequests.length);
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  approve(id: string): void {
    this.processingId = id;
    this.procService.approveRequest(id).subscribe({
      next: () => { this.processingId = null; this.toastService.success('Authorization protocol committed.'); this.load(); },
      error: () => { this.processingId = null; this.toastService.error('Authorization failure.'); }
    });
  }

  confirmReject(id: string): void {
    this.processingId = id;
    this.procService.rejectRequest(id, this.rejectReason).subscribe({
      next: () => { this.processingId = null; this.rejectingId = null; this.toastService.success('Denial protocol committed.'); this.load(); },
      error: () => { this.processingId = null; this.toastService.error('Denial failure.'); }
    });
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

  saveEdit(): void {
    if (!this.editingRequest || !this.editingRequest.id) return;
    this.isSavingEdit = true;
    this.editingRequest.notes = this.editingNotes;
    this.procService.updateRequest(this.editingRequest.id, this.editingRequest, this.userId, true)
      .subscribe({
        next: () => { this.isSavingEdit = false; this.showEditModal = false; this.toastService.success('Payload synchronized.'); this.load(); },
        error: () => { this.isSavingEdit = false; this.toastService.error('Sync failure.'); }
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
  formatDateShort(d?: string): string { return d ? new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short' }) : ''; }
}
