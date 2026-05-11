import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest } from '../procurement.models';

@Component({
  selector: 'app-procurement-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <!-- Total Requests -->
      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div class="flex justify-between items-start mb-4">
          <div class="w-12 h-12 rounded-xl bg-blue-50 text-blue-600 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
          </div>
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Total</span>
        </div>
        <h3 class="text-2xl font-bold text-slate-800">{{ requests.length }}</h3>
        <p class="text-sm text-slate-500 mt-1">Equipment Requests</p>
      </div>

      <!-- Pending IT -->
      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div class="flex justify-between items-start mb-4">
          <div class="w-12 h-12 rounded-xl bg-amber-50 text-amber-600 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/></svg>
          </div>
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Pending</span>
        </div>
        <h3 class="text-2xl font-bold text-slate-800">{{ getCount('PENDING_IT_APPROVAL') }}</h3>
        <p class="text-sm text-slate-500 mt-1">Awaiting IT Approval</p>
      </div>

      <!-- Approved -->
      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div class="flex justify-between items-start mb-4">
          <div class="w-12 h-12 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>
          </div>
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Approved</span>
        </div>
        <h3 class="text-2xl font-bold text-slate-800">{{ getCount('APPROVED') }}</h3>
        <p class="text-sm text-slate-500 mt-1">Ready for Procurement</p>
      </div>

      <!-- Orders -->
      <div class="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
        <div class="flex justify-between items-start mb-4">
          <div class="w-12 h-12 rounded-xl bg-indigo-50 text-indigo-600 flex items-center justify-center">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
          </div>
          <span class="text-xs font-bold text-slate-400 uppercase tracking-wider">Orders</span>
        </div>
        <h3 class="text-2xl font-bold text-slate-800">{{ getCount('ORDER_CONFIRMED') }}</h3>
        <p class="text-sm text-slate-500 mt-1">Confirmed Orders</p>
      </div>
    </div>

    <div class="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div class="p-6 border-b border-slate-100 flex justify-between items-center">
        <h3 class="font-bold text-slate-800">Recent Activity</h3>
      </div>
      <div class="divide-y divide-slate-50">
        <div *ngFor="let req of requests.slice(0, 5)" class="p-4 hover:bg-slate-50 transition-colors flex items-center justify-between">
          <div class="flex items-center gap-4">
            <div class="w-10 h-10 rounded-full bg-slate-100 flex items-center justify-center text-lg">
              {{ req.status === 'APPROVED' ? '✅' : req.status === 'REJECTED' ? '❌' : '⏳' }}
            </div>
            <div>
              <p class="text-sm font-semibold text-slate-800">
                {{ req.createdByName }} requested {{ getItemSummary(req) }}
              </p>
              <p class="text-xs text-slate-500">{{ formatDate(req.createdAt) }}</p>
            </div>
          </div>
          <span class="px-2 py-1 rounded text-[10px] font-bold uppercase" [ngClass]="getStatusColor(req.status)">
            {{ req.status?.replace('_', ' ') }}
          </span>
        </div>
      </div>
    </div>
  `
})
export class ProcurementDashboardComponent implements OnInit {
  requests: EquipmentRequest[] = [];

  constructor(private procService: ProcurementService) {}

  ngOnInit(): void {
    this.procService.getAllRequests().subscribe(data => this.requests = data);
  }

  getCount(status: string): number {
    return this.requests.filter(r => r.status === status).length;
  }

  getStatusColor(status?: string): string {
    switch(status) {
      case 'APPROVED': return 'bg-emerald-100 text-emerald-700';
      case 'REJECTED': return 'bg-red-100 text-red-700';
      case 'PENDING_IT_APPROVAL': return 'bg-amber-100 text-amber-700';
      default: return 'bg-slate-100 text-slate-700';
    }
  }

  getItemSummary(req: EquipmentRequest): string {
    if (!req.items || req.items.length === 0) return '0 items';
    const names = req.items.slice(0, 2).map(i => i.name).join(', ');
    return req.items.length > 2 ? `${names} and ${req.items.length - 2} more` : names;
  }

  formatDate(d?: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short' });
  }
}
