import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, STATUS_META } from '../procurement.models';

@Component({
  selector: 'app-procurement-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './procurement-dashboard.component.html',
  styleUrls: ['./procurement-dashboard.component.css']
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
