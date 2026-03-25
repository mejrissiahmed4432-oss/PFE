import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AlertService, Alert } from './alert.service';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './alerts.component.html',
  styleUrl: './alerts.component.css'
})
export class AlertsComponent implements OnInit {
  alerts: Alert[] = [];
  isLoading = true;

  constructor(private alertService: AlertService) { }

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.alertService.getAlerts().subscribe({
      next: (data) => {
        this.alerts = data;
        this.isLoading = false;
        // If no alerts, maybe generate some for demo?
        if (this.alerts.length === 0) {
           this.generateDemoAlerts();
        }
      },
      error: (err) => {
        console.error('Error loading alerts:', err);
        this.isLoading = false;
      }
    });
  }

  generateDemoAlerts(): void {
    this.alertService.generateTestAlerts().subscribe(() => {
      this.loadAlerts();
    });
  }

  markAsRead(alert: Alert): void {
    if (alert.read) return;
    this.alertService.markAsRead(alert.id).subscribe(() => {
      alert.read = true;
    });
  }

  getTypeClass(type: string): string {
    return type.toLowerCase();
  }

  getIcon(type: string): string {
    switch (type) {
      case 'ERROR': return 'critical';
      case 'WARNING': return 'warning';
      case 'SUCCESS': return 'check_circle';
      default: return 'info';
    }
  }
}
