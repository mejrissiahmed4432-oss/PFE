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
  
  // Selection
  selectedIds = new Set<string>();
  
  currentUser: any = null;
  private socketSub?: Subscription;

  // Stats
  urgentCount = 0;
  upcomingCount = 0;
  overdueCount = 0;
  unreadCount = 0;
  activityData: number[] = []; // Real activity data for the chart

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
    const userId = this.currentUser?.id;
    const role = this.currentUser?.role;
    
    this.alertService.getAlerts(userId, role).subscribe({
      next: (data) => {
        this.alerts = data;
        this.calculateStats();
        this.calculateActivity();
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
    this.unreadCount = this.alerts.filter(a => !a.read).length;

    // Generate categories dynamically
    const catMap = new Map<string, number>();
    this.alerts.forEach(a => {
      catMap.set(a.category, (catMap.get(a.category) || 0) + 1);
    });
    this.categories = Array.from(catMap.entries()).map(([name, count]) => ({name, count}));
  }

  calculateActivity(): void {
    // Generate activity data for the last 12 periods (e.g., hours or days)
    // We'll count alerts in 2-hour windows for the last 24 hours
    const now = new Date();
    const data = new Array(12).fill(0);
    
    this.alerts.forEach(a => {
      const date = new Date(a.createdAt);
      const diffHours = Math.floor((now.getTime() - date.getTime()) / (1000 * 60 * 60));
      if (diffHours < 24) {
        const index = 11 - Math.floor(diffHours / 2); // 0-11 index
        if (index >= 0) data[index]++;
      }
    });

    // Normalize for height (max value = 100% height)
    const max = Math.max(...data, 1);
    this.activityData = data.map(v => (v / max) * 100);
  }

  filterDate: string = '';
  filterType: string = 'All Types';
  filterTimeRange: string = 'all'; // 'today', 'yesterday', 'week', 'all'

  filterAlerts(): void {
    let temp = this.alerts;
    
    // Tab filter (Status-based)
    if (this.activeTab !== 'All') {
      const tab = this.activeTab.toLowerCase();
      if (tab === 'requests') {
        temp = temp.filter(a => a.category?.toLowerCase().includes('request') || a.title?.toLowerCase().includes('request'));
      } else if (tab === 'low') {
        temp = temp.filter(a => a.message?.toLowerCase().includes('low') || a.title?.toLowerCase().includes('low') || a.category?.toLowerCase().includes('low'));
      } else if (tab === 'full') {
        temp = temp.filter(a => a.message?.toLowerCase().includes('full') || a.title?.toLowerCase().includes('full') || a.category?.toLowerCase().includes('full'));
      } else if (tab === 'unread') {
        temp = temp.filter(a => !a.read);
      } else if (tab === 'critical') {
        temp = temp.filter(a => {
          const theme = this.getAlertTheme(a);
          return theme === 'urgent' || theme === 'overdue';
        });
      } else {
        temp = temp.filter(a => a.category === this.activeTab);
      }
    }
    
    // Time Range Filter
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const lastWeek = new Date(today);
    lastWeek.setDate(lastWeek.getDate() - 7);

    if (this.filterTimeRange !== 'all') {
      temp = temp.filter(a => {
        const d = new Date(a.createdAt);
        if (this.filterTimeRange === 'today') return d >= today;
        if (this.filterTimeRange === 'yesterday') return d >= yesterday && d < today;
        if (this.filterTimeRange === 'week') return d >= lastWeek;
        return true;
      });
    }

    // Specific Date Filter
    if (this.filterDate) {
      temp = temp.filter(a => {
        if (!a.createdAt) return false;
        const alertDateStr = new Date(a.createdAt).toISOString().split('T')[0];
        return alertDateStr === this.filterDate;
      });
    }

    // Search filter (Logs/Content)
    if (this.searchQuery.trim() !== '') {
      const q = this.searchQuery.toLowerCase();
      temp = temp.filter(a => 
        a.title.toLowerCase().includes(q) || 
        a.message.toLowerCase().includes(q) ||
        a.category.toLowerCase().includes(q) ||
        (a.type && a.type.toLowerCase().includes(q))
      );
    }

    // Type filter (Severity-based)
    if (this.filterType !== 'All Types') {
      temp = temp.filter(a => {
        const theme = this.getAlertTheme(a);
        if (this.filterType === 'Urgent' && theme === 'urgent') return true;
        if (this.filterType === 'Upcoming' && theme === 'upcoming') return true;
        if (this.filterType === 'Overdue' && theme === 'overdue') return true;
        return false;
      });
    }
    
    this.filteredAlerts = temp;
    this.selectedIds.clear();
  }

  setTab(tab: string): void {
    this.activeTab = tab;
    this.filterAlerts();
  }

  setTimeRange(range: string): void {
    this.filterTimeRange = range;
    this.filterAlerts();
  }

  // Selection Logic
  toggleSelectAll(event: any): void {
    if (event.target.checked) {
      this.filteredAlerts.forEach(a => this.selectedIds.add(a.id));
    } else {
      this.selectedIds.clear();
    }
  }

  toggleSelect(id: string): void {
    if (this.selectedIds.has(id)) {
      this.selectedIds.delete(id);
    } else {
      this.selectedIds.add(id);
    }
  }

  isAllSelected(): boolean {
    return this.filteredAlerts.length > 0 && this.selectedIds.size === this.filteredAlerts.length;
  }

  // Grouping Logic
  getGroupedAlerts() {
    const groups: { title: string, alerts: Alert[] }[] = [];
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const todayAlerts = this.filteredAlerts.filter(a => new Date(a.createdAt) >= today);
    const yesterdayAlerts = this.filteredAlerts.filter(a => {
      const date = new Date(a.createdAt);
      return date >= yesterday && date < today;
    });
    const olderAlerts = this.filteredAlerts.filter(a => new Date(a.createdAt) < yesterday);

    if (todayAlerts.length > 0) groups.push({ title: 'Today', alerts: todayAlerts });
    if (yesterdayAlerts.length > 0) groups.push({ title: 'Yesterday', alerts: yesterdayAlerts });
    if (olderAlerts.length > 0) groups.push({ title: 'Older', alerts: olderAlerts });

    return groups;
  }

  markAsRead(alert: Alert): void {
    if (alert.read) return;
    this.alertService.markAsRead(alert.id).subscribe(() => {
      alert.read = true;
      this.calculateStats();
    });
  }

  markAllAsReadBulk(): void {
    const userId = this.currentUser?.id;
    const role = this.currentUser?.role;
    
    this.alertService.markAllAsRead(userId, role).subscribe(() => {
      this.alerts.forEach(a => a.read = true);
      this.calculateStats();
    });
  }

  deleteAlert(id: string): void {
    this.alerts = this.alerts.filter(a => a.id !== id);
    this.filterAlerts();
    this.calculateStats();
    this.alertService.deleteAlert(id).subscribe();
  }

  selectAllFiltered(): void {
    this.filteredAlerts.forEach(a => {
      if (a.id) this.selectedIds.add(a.id);
    });
  }

  deleteAllFiltered(): void {
    if (this.filteredAlerts.length === 0) return;
    this.filteredAlerts.forEach(a => {
      if (a.id) this.selectedIds.add(a.id);
    });
    this.deleteSelected();
  }

  deleteSelected(): void {
    const idsToDelete = Array.from(this.selectedIds);
    this.alerts = this.alerts.filter(a => !this.selectedIds.has(a.id));
    this.filterAlerts();
    this.calculateStats();
    // In a real app, call service.deleteAlerts(idsToDelete)
    idsToDelete.forEach(id => this.alertService.deleteAlert(id).subscribe());
  }

  markSelectedAsRead(): void {
    const idsToMark = Array.from(this.selectedIds);
    this.alerts.forEach(a => {
      if (this.selectedIds.has(a.id)) a.read = true;
    });
    this.calculateStats();
    // In a real app, call service.markAsReadBulk(idsToMark)
    idsToMark.forEach(id => this.alertService.markAsRead(id).subscribe());
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
