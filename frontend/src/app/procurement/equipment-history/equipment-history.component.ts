import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, STATUS_META, RequestStatus } from '../procurement.models';

@Component({
  selector: 'app-equipment-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col gap-8 pb-12 animate-in fade-in duration-700">
      
      <!-- HEADER -->
      <div class="bg-white dark:bg-slate-900 p-8 rounded-[2.5rem] border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-2xl flex flex-col md:flex-row justify-between items-center gap-8 relative overflow-hidden">
        <div class="absolute top-0 left-0 w-full h-full bg-gradient-to-r from-slate-500/5 to-transparent pointer-events-none"></div>
        <div class="flex items-center gap-6 relative z-10">
          <div class="w-16 h-16 rounded-[1.5rem] bg-slate-500/10 text-slate-600 dark:text-slate-400 flex items-center justify-center border border-slate-500/20 shadow-inner">
            <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 8v4l3 3m6-3a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"/></svg>
          </div>
          <div>
            <h2 class="text-3xl font-black text-slate-900 dark:text-white tracking-tighter">Deployment Archive</h2>
            <p class="text-[10px] text-slate-500 font-black uppercase tracking-[0.2em] mt-1 italic">Audit Log & Historical Data</p>
          </div>
        </div>

        <div class="flex-1 max-w-md w-full relative z-10">
          <input type="text" 
                 [(ngModel)]="searchQuery" 
                 placeholder="Search archive..."
                 class="w-full pl-14 pr-6 py-4 bg-slate-50 dark:bg-black/40 border border-slate-200 dark:border-white/10 rounded-2xl text-sm text-slate-900 dark:text-white focus:ring-2 focus:ring-slate-500 outline-none transition-all shadow-inner placeholder:text-slate-400 dark:placeholder:text-slate-600">
          <svg class="absolute left-5 top-1/2 -translate-y-1/2 text-slate-400 dark:text-slate-500" xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
        </div>
      </div>

      <!-- TABLE VIEW -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/5 rounded-[2.5rem] overflow-hidden shadow-sm dark:shadow-2xl">
        <div class="overflow-x-auto custom-scrollbar">
          <table class="w-full text-left border-collapse">
            <thead>
              <tr class="bg-slate-50 dark:bg-white/2">
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Protocol ID</th>
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Involved User</th>
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Hardware Payload</th>
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Status</th>
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Decision Logic</th>
                <th class="px-8 py-6 text-[10px] font-black text-slate-500 uppercase tracking-widest">Logged At</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100 dark:divide-white/5">
              <tr *ngFor="let req of filteredRequests" class="hover:bg-slate-50/50 dark:hover:bg-white/1 transition-colors group">
                <td class="px-8 py-6">
                  <span class="font-mono text-xs font-black text-slate-400 dark:text-slate-500 group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                    #{{ (req.id || '').substring(0,8).toUpperCase() }}
                  </span>
                </td>
                <td class="px-8 py-6">
                  <div class="flex flex-col">
                    <span class="text-sm font-black text-slate-900 dark:text-slate-200">{{ req.createdByName }}</span>
                    <span class="text-[9px] font-black text-slate-500 uppercase tracking-widest opacity-60">{{ req.createdByRole || 'Operator' }}</span>
                  </div>
                </td>
                <td class="px-8 py-6">
                  <div class="flex flex-col gap-1">
                    <div *ngFor="let item of req.items.slice(0, 2)" class="flex items-center gap-2">
                      <span class="text-[10px] font-black text-indigo-600 dark:text-indigo-400">×{{ item.quantity }}</span>
                      <span class="text-xs font-bold text-slate-700 dark:text-slate-300 truncate max-w-[150px]">{{ item.name }}</span>
                    </div>
                    <span *ngIf="req.items.length > 2" class="text-[9px] font-black text-slate-400 uppercase tracking-widest">+{{ req.items.length - 2 }} more</span>
                  </div>
                </td>
                <td class="px-8 py-6">
                  <span class="px-4 py-1.5 rounded-full text-[9px] font-black uppercase tracking-widest border" [ngClass]="getStatusMeta(req.status).colorClass">
                    {{ getStatusMeta(req.status).label }}
                  </span>
                </td>
                <td class="px-8 py-6 max-w-xs">
                  <p class="text-xs text-slate-500 dark:text-slate-400 italic line-clamp-2 leading-relaxed">
                    {{ req.rejectionReason || req.notes || 'No log provided.' }}
                  </p>
                </td>
                <td class="px-8 py-6">
                  <span class="text-[10px] font-black text-slate-400 dark:text-slate-600 uppercase tracking-tighter">
                    {{ formatDate(req.createdAt) }}
                  </span>
                </td>
              </tr>
              <tr *ngIf="filteredRequests.length === 0">
                <td colspan="6" class="px-8 py-20 text-center">
                  <div class="flex flex-col items-center gap-4 opacity-20">
                    <svg xmlns="http://www.w3.org/2000/svg" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1" viewBox="0 0 24 24"><path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"/><polyline points="13 2 13 9 20 9"/></svg>
                    <span class="text-xs font-black uppercase tracking-[0.3em]">No Historical Fragments Found</span>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .line-clamp-2 {
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
  `]
})
export class EquipmentHistoryComponent implements OnInit {
  @Input() userId: string = '';
  @Input() userRole: string = '';

  requests: EquipmentRequest[] = [];
  loading = true;
  searchQuery = '';

  constructor(private procService: ProcurementService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    // For history, we usually show everything the user is entitled to see
    // IT_MANAGER sees everything, others see only theirs? 
    // Or Stock Manager also sees everything in history?
    // User said "rejected and approved", let's follow the general permission model.
    
    const request$ = this.userRole === 'IT_MANAGER' || this.userRole === 'STOCK_MANAGER'
      ? this.procService.getAllRequests()
      : this.procService.getRequestsByUser(this.userId);

    request$.subscribe({
      next: (data) => {
        this.requests = data.filter(r => 
          r.status === 'REJECTED' || r.status === 'APPROVED' || r.status === 'RECEIVED'
        );
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  get filteredRequests(): EquipmentRequest[] {
    if (!this.searchQuery) return this.requests;
    const q = this.searchQuery.toLowerCase();
    return this.requests.filter(r => 
      r.id?.toLowerCase().includes(q) ||
      r.createdByName?.toLowerCase().includes(q) ||
      r.items.some(i => i.name.toLowerCase().includes(q))
    );
  }

  getStatusMeta(status?: RequestStatus) { 
    const meta = STATUS_META[status || 'PENDING_IT_APPROVAL'];
    let colorClass = 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10';
    if (status === 'APPROVED') colorClass = 'bg-emerald-50 text-emerald-600 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20';
    if (status === 'REJECTED') colorClass = 'bg-rose-50 text-rose-600 border-rose-100 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20';
    if (status === 'RECEIVED') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    return { ...meta, colorClass };
  }

  formatDate(d?: string): string { return d ? new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }) : ''; }
}
