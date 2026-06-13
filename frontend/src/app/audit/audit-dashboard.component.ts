import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuditService, AuditLog } from './audit.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-audit-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './audit-dashboard.component.html',
  styleUrls: ['./audit-dashboard.component.css']
})
export class AuditDashboardComponent implements OnInit {

  logs: AuditLog[] = [];
  filteredLogs: AuditLog[] = [];
  searchTerm: string = '';
  loading = true;
  selectedLog: AuditLog | null = null;
  activeFilter: string = 'ALL';

  constructor(private auditService: AuditService) {}

  ngOnInit(): void {
    this.loadLogs();
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
        console.error('[AuditDashboard] Erreur :', err);
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
    return dt.toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' })
      + ' — ' + dt.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  }

  get totalLogs() { return this.logs.length; }
  get deleteLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('DELETE') || (l.action||'').toUpperCase().includes('SUPPRIM')).length; }
  get updateLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('UPDATE') || (l.action||'').toUpperCase().includes('MODIFI')).length; }
  get createLogs() { return this.logs.filter(l => (l.action||'').toUpperCase().includes('ADD') || (l.action||'').toUpperCase().includes('CREATE')).length; }
}
