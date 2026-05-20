import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, STATUS_META, RequestStatus } from '../procurement.models';

@Component({
  selector: 'app-equipment-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-history.component.html',
  styleUrls: ['./equipment-history.component.css']
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
    if (status === 'PENDING_IT_APPROVAL') colorClass = 'bg-amber-50 text-amber-600 border-amber-100 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20';
    if (status === 'RECEIVED') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    if (status === 'SENT_TO_SUPPLIERS') colorClass = 'bg-blue-50 text-blue-600 border-blue-100 dark:bg-blue-500/10 dark:text-blue-400 dark:border-blue-500/20';
    if (status === 'RESPONDED') colorClass = 'bg-cyan-50 text-cyan-600 border-cyan-100 dark:bg-cyan-500/10 dark:text-cyan-400 dark:border-cyan-500/20';
    if (status === 'ORDER_CONFIRMED') colorClass = 'bg-emerald-50 text-emerald-600 border-emerald-100 dark:bg-emerald-500/10 dark:text-emerald-400 dark:border-emerald-500/20';
    return { ...meta, colorClass };
  }

  formatDate(d?: string): string { return d ? new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' }) : ''; }
}
