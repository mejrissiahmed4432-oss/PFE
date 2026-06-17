import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditService, AuditLog, BlockchainStatus } from './audit.service';
import { FormsModule } from '@angular/forms';
import { interval, Subscription } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import { of } from 'rxjs';

@Component({
  selector: 'app-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-dashboard.component.html',
  styleUrls: ['./audit-dashboard.component.css']
})
export class AuditDashboardComponent implements OnInit, OnDestroy {

  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  searchTerm: string = '';
  loading = true;
  selectedLog: AuditLog | null = null;
  activeFilter: string = 'ALL';
  dateFilter: string = '';
  blockchainOnline: boolean | null = null;
  private statusPolling?: Subscription;

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs();
    this.checkStatus();
    // Poll status every 15 seconds
    this.statusPolling = interval(15000).pipe(
      switchMap(() => this.auditService.getStatus().pipe(
        catchError(() => of({ online: false, network: 'Local Ganache' }))
      ))
    ).subscribe(status => {
      this.blockchainOnline = status.online;
    });
  }

  ngOnDestroy(): void {
    this.statusPolling?.unsubscribe();
  }

  checkStatus(): void {
    this.auditService.getStatus().pipe(
      catchError(() => of({ online: false, network: 'Local Ganache' }))
    ).subscribe(status => {
      this.blockchainOnline = status.online;
    });
  }

  loadLogs(): void {
    this.loading = true;
    this.auditService.getLogs().subscribe({
      next: (data) => {
        this.logs = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        console.error('[AuditDashboard] Error :', err);
        this.loading = false;
      }
    });
  }

  setFilter(filter: string) {
    this.activeFilter = filter;
    this.applyFilters();
  }

  applyFilters(): void {
    let result = this.logs;

    if (this.activeFilter !== 'ALL') {
      result = result.filter(log =>
        log.action?.toUpperCase().includes(this.activeFilter.toUpperCase()) ||
        log.userRole?.toUpperCase() === this.activeFilter.toUpperCase()
      );
    }

    if (this.searchTerm) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(log =>
        log.action?.toLowerCase().includes(term) ||
        log.userName?.toLowerCase().includes(term) ||
        log.userRole?.toLowerCase().includes(term) ||
        log.userId?.toLowerCase().includes(term) ||
        log.details?.toLowerCase().includes(term)
      );
    }

    if (this.dateFilter) {
      result = result.filter(log => {
        if (!log.timestamp) return false;
        const logDateStr = new Date(log.timestamp).toISOString().split('T')[0];
        return logDateStr === this.dateFilter;
      });
    }

    this.filteredLogs = result;
  }

  filterLogs(): void {
    this.applyFilters();
  }

  openDetail(log: AuditLog) {
    this.selectedLog = log;
  }

  closeDetail() {
    this.selectedLog = null;
  }

  getActionClass(action: string): string {
    const a = (action || '').toUpperCase();
    if (a.includes('DELETE') || a.includes('SUPPRIM')) return 'action-delete';
    if (a.includes('UPDATE') || a.includes('MODIFI') || a.includes('EDIT')) return 'action-update';
    if (a.includes('ADD') || a.includes('CREATE') || a.includes('AJOUT')) return 'action-create';
    if (a.includes('ASSIGN')) return 'action-assign';
    return 'action-default';
  }

  getActionIcon(action: string): string {
    const a = (action || '').toUpperCase();
    if (a.includes('DELETE') || a.includes('SUPPRIM')) return 'delete';
    if (a.includes('UPDATE') || a.includes('MODIFI') || a.includes('EDIT')) return 'update';
    if (a.includes('ADD') || a.includes('CREATE') || a.includes('AJOUT')) return 'create';
    if (a.includes('ASSIGN')) return 'assign';
    return 'default';
  }

  getRoleClass(role: string): string {
    switch ((role || '').toUpperCase()) {
      case 'STOCK_MANAGER': return 'role-stock';
      case 'IT_MANAGER':    return 'role-it';
      case 'ADMIN':         return 'role-admin';
      case 'TECHNICIAN':    return 'role-tech';
      default:              return 'role-default';
    }
  }

  formatDate(d: string): string {
    if (!d) return '—';
    const dt = new Date(d);
    return dt.toLocaleDateString('en-US', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' — ' + dt.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  get totalLogs() { return this.logs.length; }
  get deleteLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('DELETE') || (l.action||'').toUpperCase().includes('SUPPRIM')).length; }
  get updateLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('UPDATE') || (l.action||'').toUpperCase().includes('MODIFI')).length; }
  get createLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('ADD') || (l.action||'').toUpperCase().includes('CREATE')).length; }
}
