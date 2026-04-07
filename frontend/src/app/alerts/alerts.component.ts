import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AlertService, Alert } from './alert.service';
import { SocketService } from '../messaging/socket.service';
import { AuthService } from '../auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alerts.component.html',
  styleUrl: './alerts.component.css'
})
export class AlertsComponent implements OnInit, OnDestroy {
  alerts: Alert[] = [];
  filteredAlerts: Alert[] = [];
  isLoading = true;
  
  searchQuery = '';
  activeTab = 'All';
  categories: {name: string, count: number}[] = [];
  
  currentUser: any = null;
  private socketSub?: Subscription;

  // Stats
  urgentCount = 0;
  upcomingCount = 0;
  overdueCount = 0;

  today: Date = new Date();

  constructor(
    private alertService: AlertService,
    private socketService: SocketService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.currentUser = this.authService.getCurrentUser();
    
    this.loadAlerts();

    this.socketSub = this.socketService.onAlertUpdate.subscribe(() => {
      this.loadAlerts();
    });
  }

  ngOnDestroy(): void {
    if (this.socketSub) this.socketSub.unsubscribe();
  }

  loadAlerts(): void {
    this.isLoading = true;
    this.alertService.getAlerts().subscribe({
      next: (data) => {
        this.alerts = data;
        
        this.calculateStats();
        this.filterAlerts();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading alerts:', err);
        this.isLoading = false;
      }
    });
  }

  calculateStats(): void {
    this.urgentCount = this.alerts.filter(a => this.getAlertTheme(a) === 'urgent').length;
    this.upcomingCount = this.alerts.filter(a => this.getAlertTheme(a) === 'upcoming').length;
    this.overdueCount = this.alerts.filter(a => this.getAlertTheme(a) === 'overdue').length;

    // Generate categories dynamically
    const catMap = new Map<string, number>();
    this.alerts.forEach(a => {
      catMap.set(a.category, (catMap.get(a.category) || 0) + 1);
    });
    this.categories = Array.from(catMap.entries()).map(([name, count]) => ({name, count}));
  }

  filterDate: string = '';
  filterType: string = 'All Types';

  filterAlerts(): void {
    let temp = this.alerts;
    
    // Tab filter
    if (this.activeTab !== 'All') {
      temp = temp.filter(a => a.category === this.activeTab);
    }
    
    // Search filter
    if (this.searchQuery.trim() !== '') {
      const q = this.searchQuery.toLowerCase();
      temp = temp.filter(a => 
        a.title.toLowerCase().includes(q) || 
        a.message.toLowerCase().includes(q) ||
        a.category.toLowerCase().includes(q)
      );
    }

    // Type filter
    if (this.filterType !== 'All Types') {
      temp = temp.filter(a => {
        const theme = this.getAlertTheme(a);
        if (this.filterType === 'Urgent' && theme === 'urgent') return true;
        if (this.filterType === 'Upcoming' && theme === 'upcoming') return true;
        if (this.filterType === 'Overdue' && theme === 'overdue') return true;
        return false;
      });
    }

    // Date filter
    if (this.filterDate) {
      temp = temp.filter(a => {
        if (!a.createdAt) return false;
        // Compare YYYY-MM-DD
        const alertDateStr = new Date(a.createdAt).toISOString().split('T')[0];
        return alertDateStr === this.filterDate;
      });
    }
    
    this.filteredAlerts = temp;
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.filterAlerts();
  }

  markAsRead(alert: Alert): void {
    if (alert.read) return;
    this.alertService.markAsRead(alert.id).subscribe(() => {
      alert.read = true;
    });
  }
  
  deleteAlert(id: string): void {
    this.alerts = this.alerts.filter(a => a.id !== id);
    this.filterAlerts();
    this.calculateStats();
    this.alertService.deleteAlert(id).subscribe();
  }

  getDaysText(createdAt: string): string {
    const diffTime = Math.abs(new Date().getTime() - new Date(createdAt).getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'Today';
    return `${diffDays}d ago`;
  }

  getAlertTheme(alert: Alert): 'overdue' | 'urgent' | 'upcoming' | 'info' | 'success' {
    if (alert.type === 'ERROR') {
      return alert.message.toLowerCase().includes('expired') ? 'overdue' : 'urgent';
    }
    if (alert.type === 'WARNING') return 'upcoming';
    if (alert.type === 'SUCCESS') return 'success';
    return 'info';
  }
  
  getAlertLabel(alert: Alert): string {
    const theme = this.getAlertTheme(alert);
    if (theme === 'overdue') return 'OVERDUE';
    if (theme === 'urgent') return 'URGENT';
    if (theme === 'upcoming') return 'UPCOMING';
    return alert.type;
  }
}
