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


  


  constructor(private auditService: AuditService) { }

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    console.log('[AuditDashboard] Chargement des logs depuis /api/employees/audit/logs ...');
    this.auditService.getLogs().subscribe({
      next: (data) => {
        console.log('[AuditDashboard] Logs recus :', data);
        console.log('[AuditDashboard] Nombre de logs :', data.length);
        this.logs = data;
        this.filteredLogs = data;
      },
      error: (err) => {
        console.error('[AuditDashboard] Erreur lors du chargement des logs :', err);
        console.error('[AuditDashboard] Status HTTP :', err.status);
        console.error('[AuditDashboard] URL appelée :', err.url);
      }
    });
  }

  filterLogs(): void {
    if (!this.searchTerm) {
      this.filteredLogs = this.logs;
    } else {
      const term = this.searchTerm.toLowerCase();
      this.filteredLogs = this.logs.filter(log => 
        log.action.toLowerCase().includes(term) || 
        log.userName.toLowerCase().includes(term) ||
        log.userRole.toLowerCase().includes(term) ||
        (log.userId && log.userId.includes(term)) ||
        (log.blockchainLogId && log.blockchainLogId.toString() === term)
      );
    }
  }


}
