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
  activeCount = 0;
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
    this.activeCount = this.alerts.filter(a => (a.status || 'ACTIVE') === 'ACTIVE').length;

    // Generate categories dynamically
    const catMap = new Map<string, number>();
    this.alerts.forEach(a => {
      const category = this.getAlertCategory(a);
      catMap.set(category, (catMap.get(category) || 0) + 1);
    });
    this.categories = Array.from(catMap.entries()).map(([name, count]) => ({name, count}));
  }

  calculateActivity(): void {
    // Generate activity data for the last 12 periods (e.g., hours or days)
    // We'll count alerts in 2-hour windows for the last 24 hours
    const now = new Date();
    const data = new Array(12).fill(0);
    
    this.alerts.forEach(a => {
      const date = this.getAlertDate(a);
      if (!date) return;
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
    
    if (this.filterTimeRange !== 'all') {
      temp = temp.filter(a => this.matchesTimeRange(a, this.filterTimeRange));
    }

    // Specific Date Filter
    if (this.filterDate) {
      temp = temp.filter(a => {
        const date = this.getAlertDate(a);
        if (!date) return false;
        const alertDateStr = this.toLocalDateKey(date);
        return alertDateStr === this.filterDate;
      });
    }

    // Search filter (Logs/Content)
    if (this.searchQuery.trim() !== '') {
      const q = this.searchQuery.toLowerCase();
      temp = temp.filter(a => 
        a.title?.toLowerCase().includes(q) ||
        a.message?.toLowerCase().includes(q) ||
        this.getAlertCategory(a).toLowerCase().includes(q) ||
        a.priority?.toLowerCase().includes(q) ||
        a.status?.toLowerCase().includes(q) ||
        a.targetType?.toLowerCase().includes(q) ||
        a.targetId?.toLowerCase().includes(q) ||
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

  isActiveAlert(alert: Alert): boolean {
    return (alert.status || 'ACTIVE') === 'ACTIVE';
  }

  // Grouping Logic
  getGroupedAlerts() {
    const groups: { title: string, alerts: Alert[] }[] = [];
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    const todayAlerts = this.filteredAlerts.filter(a => {
      const date = this.getAlertDate(a);
      return !!date && date >= today;
    });
    const yesterdayAlerts = this.filteredAlerts.filter(a => {
      const date = this.getAlertDate(a);
      if (!date) return false;
      return date >= yesterday && date < today;
    });
    const olderAlerts = this.filteredAlerts.filter(a => {
      const date = this.getAlertDate(a);
      return !!date && date < yesterday;
    });

    if (todayAlerts.length > 0) groups.push({ title: 'Today', alerts: todayAlerts });
    if (yesterdayAlerts.length > 0) groups.push({ title: 'Yesterday', alerts: yesterdayAlerts });
    if (olderAlerts.length > 0) groups.push({ title: 'Older', alerts: olderAlerts });

    return groups;
  }

  markAsRead(alert: Alert): void {
    if (alert.status === 'RESOLVED') return;
    this.alertService.markAsRead(alert.id).subscribe(() => {
      alert.read = true;
      alert.status = 'RESOLVED';
      this.calculateStats();
      this.filterAlerts();
    });
  }

  markAllAsReadBulk(): void {
    const userId = this.currentUser?.id;
    const role = this.currentUser?.role;
    
    this.alertService.markAllAsRead(userId, role).subscribe(() => {
      this.alerts.forEach(a => {
        a.read = true;
        a.status = 'RESOLVED';
      });
      this.calculateStats();
      this.filterAlerts();
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
      if (this.selectedIds.has(a.id)) {
        a.read = true;
        a.status = 'RESOLVED';
      }
    });
    this.calculateStats();
    // In a real app, call service.markAsReadBulk(idsToMark)
    idsToMark.forEach(id => this.alertService.markAsRead(id).subscribe());
  }

  getDaysText(createdAt: string): string {
    const date = this.parseDate(createdAt);
    if (!date) return 'Unknown';
    const diffTime = Math.abs(new Date().getTime() - date.getTime());
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'Today';
    return `${diffDays}d ago`;
  }

  getAlertCategory(alert: Alert): string {
    return alert.category || alert.type || 'ALERT';
  }

  private matchesTimeRange(alert: Alert, range: string): boolean {
    const date = this.getAlertDate(alert);
    if (!date) return false;

    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    const yesterday = new Date(today);
    yesterday.setDate(today.getDate() - 1);
    const lastWeek = new Date(today);
    lastWeek.setDate(today.getDate() - 7);

    if (range === 'today') return date >= today && date < tomorrow;
    if (range === 'yesterday') return date >= yesterday && date < today;
    if (range === 'week') return date >= lastWeek && date < tomorrow;
    return true;
  }

  private getAlertDate(alert: Alert): Date | null {
    return this.parseDate(alert.createdAt);
  }

  private parseDate(value?: string): Date | null {
    if (!value) return null;
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }

  private toLocalDateKey(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  getAlertTheme(alert: Alert): 'overdue' | 'urgent' | 'upcoming' | 'info' | 'success' {
    const type = (alert.type || '').toUpperCase();
    const priority = (alert.priority || '').toUpperCase();
    const message = (alert.message || '').toLowerCase();

    if (alert.status === 'RESOLVED') {
      return 'success';
    }
    if (type.includes('EXPIRED') || type.includes('OVERDUE') || message.includes('expired') || message.includes('overdue')) {
      return 'overdue';
    }
    if (priority === 'HIGH' || type === 'ERROR') {
      return 'urgent';
    }
    if (priority === 'MEDIUM' || type === 'WARNING') return 'upcoming';
    if (type === 'SUCCESS') return 'success';
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
