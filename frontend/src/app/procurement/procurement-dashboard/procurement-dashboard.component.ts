import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, STATUS_META } from '../procurement.models';

@Component({
  selector: 'app-procurement-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="flex flex-col gap-10 animate-in fade-in slide-in-from-bottom-4 duration-700">
      
      <!-- KPI GRID -->
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        
        <!-- Total -->
        <div class="relative overflow-hidden bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-2xl group hover:border-indigo-500/50 transition-all duration-500">
          <div class="absolute -right-4 -top-4 w-24 h-24 bg-indigo-500/10 rounded-full blur-2xl group-hover:bg-indigo-500/20 transition-all"></div>
          <div class="relative z-10">
            <div class="w-12 h-12 rounded-2xl bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center mb-6 border border-indigo-500/20 shadow-inner group-hover:scale-110 transition-transform">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
            </div>
            <p class="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">Total Submissions</p>
            <h3 class="text-4xl font-black text-slate-900 dark:text-white tracking-tight">{{ requests.length }}</h3>
            <div class="mt-4 flex items-center gap-2">
              <span class="w-1.5 h-1.5 rounded-full bg-indigo-500 animate-pulse"></span>
              <p class="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">Live Database Sync</p>
            </div>
          </div>
        </div>

        <!-- Pending -->
        <div class="relative overflow-hidden bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-2xl group hover:border-amber-500/50 transition-all duration-500">
          <div class="absolute -right-4 -top-4 w-24 h-24 bg-amber-500/10 rounded-full blur-2xl group-hover:bg-amber-500/20 transition-all"></div>
          <div class="relative z-10">
            <div class="w-12 h-12 rounded-2xl bg-amber-500/10 text-amber-600 dark:text-amber-400 flex items-center justify-center mb-6 border border-amber-500/20 shadow-inner group-hover:scale-110 transition-transform">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
            </div>
            <p class="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">Awaiting Review</p>
            <h3 class="text-4xl font-black text-slate-900 dark:text-white tracking-tight">{{ getCount('PENDING_IT_APPROVAL') }}</h3>
            <div class="mt-4 flex items-center gap-2">
              <span class="w-1.5 h-1.5 rounded-full bg-amber-500 animate-pulse"></span>
              <p class="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">Critical Actions</p>
            </div>
          </div>
        </div>

        <!-- Approved -->
        <div class="relative overflow-hidden bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-2xl group hover:border-emerald-500/50 transition-all duration-500">
          <div class="absolute -right-4 -top-4 w-24 h-24 bg-emerald-500/10 rounded-full blur-2xl group-hover:bg-emerald-500/20 transition-all"></div>
          <div class="relative z-10">
            <div class="w-12 h-12 rounded-2xl bg-emerald-500/10 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mb-6 border border-emerald-500/20 shadow-inner group-hover:scale-110 transition-transform">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
            </div>
            <p class="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">Approved Supply</p>
            <h3 class="text-4xl font-black text-slate-900 dark:text-white tracking-tight">{{ getCount('APPROVED') }}</h3>
            <div class="mt-4 flex items-center gap-2">
              <span class="w-1.5 h-1.5 rounded-full bg-emerald-500"></span>
              <p class="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">Procurement Ready</p>
            </div>
          </div>
        </div>

        <!-- Inbound -->
        <div class="relative overflow-hidden bg-white dark:bg-slate-900 rounded-[2.5rem] p-8 border border-slate-200 dark:border-white/5 shadow-sm dark:shadow-2xl group hover:border-blue-500/50 transition-all duration-500">
          <div class="absolute -right-4 -top-4 w-24 h-24 bg-blue-500/10 rounded-full blur-2xl group-hover:bg-blue-500/20 transition-all"></div>
          <div class="relative z-10">
            <div class="w-12 h-12 rounded-2xl bg-blue-500/10 text-blue-600 dark:text-blue-400 flex items-center justify-center mb-6 border border-blue-500/20 shadow-inner group-hover:scale-110 transition-transform">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/></svg>
            </div>
            <p class="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-1">Items Inbound</p>
            <h3 class="text-4xl font-black text-slate-900 dark:text-white tracking-tight">{{ getCount('ORDER_CONFIRMED') + getCount('RECEIVED') }}</h3>
            <div class="mt-4 flex items-center gap-2">
              <span class="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
              <p class="text-[10px] text-slate-400 font-bold uppercase tracking-tighter">In Transit / Stocked</p>
            </div>
          </div>
        </div>
      </div>

      <!-- MAIN CONTENT -->
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-10">
        
        <!-- Timeline -->
        <div class="lg:col-span-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/5 rounded-[2.5rem] overflow-hidden shadow-sm dark:shadow-2xl">
          <div class="p-8 border-b border-slate-200 dark:border-white/5 flex justify-between items-center bg-slate-50/50 dark:bg-white/2 backdrop-blur-sm">
            <h3 class="text-lg font-black text-slate-900 dark:text-white tracking-tight">Recent Intelligence Log</h3>
            <div class="flex items-center gap-2">
              <div class="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></div>
              <span class="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em]">Real-time Updates</span>
            </div>
          </div>
          
          <div class="divide-y divide-slate-200 dark:divide-white/5 bg-white/50 dark:bg-slate-900/50">
            <div *ngFor="let req of requests.slice(0, 6)" 
                 class="p-6 hover:bg-slate-50 dark:hover:bg-white/5 transition-all flex items-center justify-between group cursor-pointer">
              <div class="flex items-center gap-5">
                <div class="w-12 h-12 rounded-2xl flex items-center justify-center text-xl shadow-inner border border-slate-200 dark:border-white/10 bg-slate-100 dark:bg-slate-800/50 group-hover:scale-110 transition-transform group-hover:border-indigo-500/30">
                  {{ req.status === 'RECEIVED' ? '📦' : req.status === 'APPROVED' ? '✅' : req.status === 'REJECTED' ? '❌' : '⏳' }}
                </div>
                <div>
                  <p class="text-sm font-bold text-slate-700 dark:text-slate-200 leading-snug">
                    <span class="text-indigo-600 dark:text-indigo-400">{{ req.createdByName }}</span>
                    requested {{ getItemSummary(req) }}
                  </p>
                  <p class="text-[10px] text-slate-500 font-bold uppercase tracking-widest mt-1">{{ formatDate(req.createdAt) }}</p>
                </div>
              </div>
              <div class="flex flex-col items-end gap-2">
                <span class="px-3 py-1 rounded-full text-[9px] font-black uppercase tracking-widest border shadow-sm dark:shadow-lg backdrop-blur-md" 
                      [ngClass]="getStatusMeta(req.status).colorClass">
                  {{ getStatusMeta(req.status).label }}
                </span>
              </div>
            </div>
          </div>
          
          <div *ngIf="requests.length === 0" class="p-24 text-center">
            <div class="w-16 h-16 rounded-full bg-slate-100 dark:bg-slate-800 flex items-center justify-center mx-auto mb-4 border border-slate-200 dark:border-white/5">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" class="text-slate-400" viewBox="0 0 24 24"><path d="M22 12h-4l-3 9L9 3l-3 9H2"/></svg>
            </div>
            <p class="font-black uppercase tracking-widest text-[10px] text-slate-400 dark:text-slate-600">No activity detected in local node</p>
          </div>
        </div>

        <!-- Metrics & Quick Info -->
        <div class="flex flex-col gap-6">
          <div class="bg-indigo-600 rounded-[2.5rem] p-8 text-white shadow-2xl shadow-indigo-600/20 relative overflow-hidden group">
            <div class="absolute -right-10 -bottom-10 w-40 h-40 bg-white/10 rounded-full blur-3xl group-hover:bg-white/20 transition-all"></div>
            <h3 class="text-xs font-black uppercase tracking-[0.2em] mb-8 pb-4 border-b border-white/10 opacity-70">Node Efficiency</h3>
            
            <div class="flex flex-col gap-8 relative z-10">
              <div>
                <div class="flex justify-between items-center mb-2">
                  <span class="text-[10px] font-black uppercase opacity-60">Approval Rate</span>
                  <span class="text-sm font-black">{{ approvalRate }}%</span>
                </div>
                <div class="h-1.5 w-full bg-white/10 rounded-full overflow-hidden border border-white/5">
                  <div class="h-full bg-white shadow-[0_0_10px_white] transition-all duration-1000" [style.width.%]="approvalRate"></div>
                </div>
              </div>

              <div>
                <div class="flex justify-between items-center mb-2">
                  <span class="text-[10px] font-black uppercase opacity-60">Fulfillment</span>
                  <span class="text-sm font-black">{{ fulfillmentRate }}%</span>
                </div>
                <div class="h-1.5 w-full bg-white/10 rounded-full overflow-hidden border border-white/5">
                  <div class="h-full bg-emerald-400 shadow-[0_0_10px_#10b981] transition-all duration-1000" [style.width.%]="fulfillmentRate"></div>
                </div>
              </div>
            </div>
          </div>

          <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-white/5 rounded-[2.5rem] p-8 shadow-sm dark:shadow-2xl">
            <div class="flex items-center gap-3 mb-6">
              <div class="w-8 h-8 rounded-lg bg-indigo-500/10 text-indigo-600 dark:text-indigo-400 flex items-center justify-center border border-indigo-500/20">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
              </div>
              <p class="text-[10px] font-black text-slate-900 dark:text-white uppercase tracking-widest">Protocol Guidance</p>
            </div>
            <p class="text-xs text-slate-600 dark:text-slate-400 leading-relaxed font-bold italic opacity-80">
              "{{ userRole === 'IT_MANAGER' 
                 ? 'High-performance clusters require proactive hardware rotation. Audit pending requests to minimize latency.'
                 : 'Detailed technical specs reduce review cycles by 42%. Use the AI assistant to refine request payloads.' }}"
            </p>
          </div>
        </div>

      </div>
    </div>
  `
})
export class ProcurementDashboardComponent implements OnInit {
  @Input() userId: string = '';
  @Input() userRole: string = '';
  
  requests: EquipmentRequest[] = [];

  constructor(private procService: ProcurementService) {}

  ngOnInit(): void {
    const request$ = this.userRole === 'IT_MANAGER' 
      ? this.procService.getAllRequests() 
      : this.procService.getRequestsByUser(this.userId);

    request$.subscribe(data => this.requests = data);
  }

  get approvalRate(): number {
    if (this.requests.length === 0) return 0;
    const approved = this.requests.filter(r => r.status !== 'PENDING_IT_APPROVAL' && r.status !== 'REJECTED').length;
    return Math.round((approved / this.requests.length) * 100);
  }

  get fulfillmentRate(): number {
    const approved = this.requests.filter(r => r.status !== 'PENDING_IT_APPROVAL' && r.status !== 'REJECTED').length;
    if (approved === 0) return 0;
    const fulfilled = this.requests.filter(r => r.status === 'RECEIVED').length;
    return Math.round((fulfilled / approved) * 100);
  }

  getCount(status: string): number {
    return this.requests.filter(r => r.status === status).length;
  }

  getStatusMeta(status?: string) {
    const s = status as keyof typeof STATUS_META;
    const meta = STATUS_META[s] || STATUS_META['PENDING_IT_APPROVAL'];
    
    // Theme-aware color classes
    let colorClass = 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10';
    if (status === 'APPROVED') colorClass = 'bg-emerald-50 text-emerald-600 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20';
    if (status === 'REJECTED') colorClass = 'bg-rose-50 text-rose-600 border-rose-100 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20';
    if (status === 'PENDING_IT_APPROVAL') colorClass = 'bg-amber-50 text-amber-600 border-amber-100 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20';
    if (status === 'RECEIVED') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    
    return { ...meta, colorClass };
  }

  getItemSummary(req: EquipmentRequest): string {
    if (!req.items || req.items.length === 0) return '0 items';
    const names = req.items.slice(0, 1).map(i => i.name).join('');
    return req.items.length > 1 ? `${names} + ${req.items.length - 1} more` : names;
  }

  formatDate(d?: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
  }
}
