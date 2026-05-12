import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, STATUS_META, RequestStatus } from '../procurement.models';

@Component({
  selector: 'app-it-approval-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col lg:flex-row gap-6">
      
      <!-- Main Content: Pending Approvals -->
      <div class="flex-1 bg-white/70 backdrop-blur-md border border-slate-200/60 rounded-2xl p-6 shadow-sm">
        <div class="flex items-center gap-4 mb-6 pb-6 border-b border-slate-100">
          <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-amber-50 text-amber-500 shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><path d="M12 18v-6"/><path d="M9 15h6"/></svg>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-800 m-0">Pending Approvals</h2>
            <p class="text-sm text-slate-500 mt-1">Review new equipment requests from Stock Managers.</p>
          </div>
        </div>

        <div *ngIf="isLoading" class="flex items-center justify-center p-12 text-slate-500 gap-3">
          <div class="w-6 h-6 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div>
          <span class="font-medium">Loading requests...</span>
        </div>

        <div *ngIf="!isLoading && pendingRequests.length === 0" class="flex flex-col items-center justify-center p-16 text-slate-400">
          <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.5" class="mb-4 opacity-50" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <p class="font-medium">All caught up! No pending requests.</p>
        </div>

        <div class="flex flex-col gap-4" *ngIf="!isLoading && pendingRequests.length > 0">
          <div *ngFor="let req of pendingRequests" class="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 p-5 bg-white border border-slate-200 rounded-2xl hover:border-indigo-300 hover:shadow-md transition-all">
            
            <div class="flex-1">
              <div class="flex items-center gap-3 mb-2">
                <span class="text-xs text-slate-400 font-medium flex items-center gap-1">
                  <svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  {{ formatDate(req.createdAt) }}
                </span>
              </div>
              
              <div class="text-sm font-medium text-slate-700 mb-3 flex items-center gap-2">
                <span class="w-6 h-6 rounded-full bg-slate-100 flex items-center justify-center text-xs">👤</span>
                {{ req.createdByName || 'Stock Manager' }}
              </div>
              
              <div class="flex flex-col gap-2 mb-3">
                <div *ngFor="let item of req.items" class="flex flex-col gap-1">
                  <div class="flex items-center gap-2">
                    <span class="px-2.5 py-1.5 bg-indigo-600 text-white rounded-lg text-xs font-bold shadow-sm">
                      {{ item.name }} × {{ item.quantity }}
                    </span>
                    <span *ngIf="item.description" class="text-[11px] text-slate-400 italic">{{ item.description }}</span>
                  </div>
                  <!-- Specifications -->
                  <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-1.5 mt-1">
                    <span *ngFor="let key of objectKeys(item.selectedSpecs)" class="px-2 py-1 bg-amber-50 text-amber-700 border border-amber-200 text-[10px] rounded-md font-bold uppercase tracking-tight">
                      {{ key }}: {{ item.selectedSpecs[key] }}
                    </span>
                  </div>
                </div>
              </div>
              
              <div *ngIf="req.notes" class="text-sm text-slate-500 italic bg-slate-50/50 p-3 rounded-xl border border-slate-100 border-l-4 border-l-slate-300">
                "{{ req.notes }}"
              </div>
            </div>

            <div class="w-full md:w-auto md:min-w-[180px]">
              <div *ngIf="processingId === req.id" class="flex items-center justify-center gap-2 text-indigo-600 font-medium p-3">
                <div class="w-4 h-4 border-2 border-indigo-200 border-t-indigo-600 rounded-full animate-spin"></div> Processing...
              </div>

              <div *ngIf="processingId !== req.id && rejectingId !== req.id" class="flex flex-row md:flex-col gap-2">
                <button class="flex-1 flex justify-center items-center gap-2 px-4 py-2.5 bg-emerald-500 hover:bg-emerald-600 text-white text-sm font-semibold rounded-xl shadow-sm transition-colors" (click)="approve(req.id!)">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg> Approve
                </button>
                <button class="flex-1 flex justify-center items-center gap-2 px-4 py-2.5 bg-white border border-red-200 text-red-600 hover:bg-red-50 text-sm font-semibold rounded-xl transition-colors" (click)="rejectingId = req.id; rejectReason = ''">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Reject
                </button>
              </div>

              <div *ngIf="rejectingId === req.id" class="flex flex-col gap-2 bg-red-50 p-3 rounded-xl border border-red-100">
                <textarea [(ngModel)]="rejectReason" class="w-full px-3 py-2 text-sm border border-red-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-red-500/20 resize-y" rows="2" placeholder="Reason for rejection..."></textarea>
                <div class="flex gap-2">
                  <button class="flex-1 px-3 py-1.5 bg-red-600 text-white text-xs font-semibold rounded-lg hover:bg-red-700 transition-colors" (click)="confirmReject(req.id!)" [disabled]="!rejectReason.trim()">Confirm Reject</button>
                  <button class="px-3 py-1.5 bg-white text-slate-500 border border-slate-200 text-xs font-semibold rounded-lg hover:bg-slate-50 transition-colors" (click)="rejectingId = null">Cancel</button>
                </div>
              </div>
            </div>

          </div>
        </div>
      </div>

      <!-- Sidebar: Processed & Send to RFQ -->
      <div class="w-full lg:w-80 flex flex-col gap-6">
        <div class="bg-white/70 backdrop-blur-md border border-slate-200/60 rounded-2xl p-5 shadow-sm">
          <h3 class="text-sm font-bold text-slate-800 uppercase tracking-wider mb-4 flex justify-between items-center">
            Recently Processed
            <span class="text-xs font-normal text-slate-400 normal-case">{{ processedRequests.length }} items</span>
          </h3>
          
          <div class="flex flex-col gap-3">
            <div *ngFor="let req of processedRequests.slice(0,5)" class="p-3 bg-slate-50 border border-slate-100 rounded-xl hover:bg-white hover:border-slate-200 transition-colors">
              <div class="flex justify-between items-center mb-2">
                <span class="px-2 py-0.5 rounded text-[10px] font-bold uppercase" [ngClass]="getStatusMeta(req.status).colorClass">
                  {{ getStatusMeta(req.status).label }}
                </span>
              </div>
              <div class="flex justify-between items-center mt-2">
                <span class="text-xs text-slate-500">{{ req.items.length }} item(s)</span>
                <button class="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 group" (click)="sendToRFQ.emit(req)" *ngIf="req.status === 'APPROVED'">
                  Send RFQ <span class="group-hover:translate-x-1 transition-transform">→</span>
                </button>
              </div>
            </div>
          </div>
          
          <div *ngIf="processedRequests.length === 0" class="text-center p-6 text-sm text-slate-400">
            No recently processed requests.
          </div>
        </div>
      </div>

    </div>
  `
})
export class ItApprovalDashboardComponent implements OnInit {
  @Output() sendToRFQ = new EventEmitter<EquipmentRequest>();
  pendingRequests: EquipmentRequest[] = [];
  processedRequests: EquipmentRequest[] = [];
  isLoading = true;
  processingId: string | undefined | null = null;
  rejectingId: string | undefined | null = null;
  rejectReason = '';
  objectKeys = Object.keys;

  constructor(private procService: ProcurementService) {}
  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    this.procService.getAllRequests().subscribe({
      next: (data) => {
        this.pendingRequests = data.filter(r => r.status === 'PENDING_IT_APPROVAL');
        this.processedRequests = data.filter(r => r.status !== 'PENDING_IT_APPROVAL');
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
    });
  }

  approve(id: string): void {
    this.processingId = id;
    this.procService.approveRequest(id).subscribe({ next: () => { this.processingId = null; this.load(); }, error: () => { this.processingId = null; } });
  }

  confirmReject(id: string): void {
    this.processingId = id;
    this.procService.rejectRequest(id, this.rejectReason).subscribe({ next: () => { this.processingId = null; this.rejectingId = null; this.rejectReason = ''; this.load(); }, error: () => { this.processingId = null; } });
  }

  getStatusMeta(status?: RequestStatus) { return STATUS_META[status || 'PENDING_IT_APPROVAL']; }
  formatDate(d?: string): string { if (!d) return ''; return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }); }
}
