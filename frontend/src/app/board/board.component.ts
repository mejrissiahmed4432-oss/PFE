import { Component, OnDestroy, OnInit, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subscription } from 'rxjs';
import { AuthService } from '../auth.service';
import { Router } from '@angular/router';
import { AiAssistantComponent } from '../ai-assistant/ai-assistant.component';
import { EquipmentComponent } from '../equipment/equipment.component';
import { ProfileComponent } from '../profile/profile.component';
import { SettingsComponent } from '../settings/settings.component';
import { SupplierComponent } from '../supplier/supplier.component';
import { DashboardComponent } from '../dashboard/dashboard.component';
import { AlertsComponent } from '../alerts/alerts.component';
import { AlertService, Alert } from '../alerts/alert.service';
import { ShelfListComponent } from '../shelf/shelf-list/shelf-list.component';
import { EquipmentService } from '../equipment/equipment.service';
import { CategoryManagerComponent } from '../category-manager/category-manager.component';
import { MessagingComponent } from '../messaging/messaging.component';
import { MessagingService } from '../messaging/messaging.service';
import { SocketService } from '../messaging/socket.service';
import { ScheduleComponent } from '../schedule/schedule.component';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, AiAssistantComponent, EquipmentComponent, ProfileComponent, SettingsComponent, SupplierComponent, DashboardComponent, AlertsComponent, ShelfListComponent, CategoryManagerComponent, MessagingComponent, ScheduleComponent],
  providers: [MessagingService],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css'
})
export class BoardComponent implements OnInit {
  user: any;
  selectedLanguage: 'en' | 'fr' = 'en'; // Default is English
  isAssistantOpen: boolean = false;
  isSidebarCollapsed: boolean = false;
  activeTab: string = 'dashboard'; // Defaulting to dashboard for view
  unreadAlertsCount: number = 0;
  unreadMessagesCount: number = 0;
  private userSub: Subscription | undefined;
  private pollSub: Subscription | undefined;
  private socketSub: Subscription | undefined;

  isNotificationsOpen: boolean = false;
  notificationsList: any[] = [];
  unreadNotificationsCount: number = 0;

  constructor(
    private authService: AuthService, 
    private router: Router,
    private alertService: AlertService,
    private equipmentService: EquipmentService,
    private messagingService: MessagingService,
    private socketService: SocketService
  ) { }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.notif-container')) {
       this.isNotificationsOpen = false;
    }
  }

  ngOnInit(): void {
    this.userSub = this.authService.user$.subscribe(user => {
      this.user = user;
      if (!this.user) {
        this.router.navigate(['/login']);
      } else {
        this.loadUnreadCount();
        
        // 1. WebSocket Subscription for instant message updates
        this.socketSub?.unsubscribe();
        this.socketSub = this.socketService.onUnreadCount.subscribe(count => {
          this.unreadMessagesCount = count;
        });

        // Live Alerts Subscription
        this.socketService.onAlertUpdate.subscribe(() => {
          this.loadUnreadCount();
        });

        // 2. Reduce non-realtime polling to once per minute (standard dashboard sync)
        if (!this.pollSub) {
          import('rxjs').then(({ interval }) => {
            this.pollSub = interval(60000).subscribe(() => this.loadUnreadCount());
          });
        }
      }
    });
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.pollSub?.unsubscribe();
    this.socketSub?.unsubscribe();
  }

  loadUnreadCount(): void {
    this.alertService.getAlerts().subscribe(alerts => {
      const notifs = alerts.filter(a => a.type === 'INFO' || a.type === 'SUCCESS');
      const urgent = alerts.filter(a => a.type === 'WARNING' || a.type === 'ERROR');

      this.unreadAlertsCount = alerts.filter(a => !a.read).length;
      
      this.notificationsList = notifs
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .slice(0, 10)
        .map(n => this.mapAlertToNotification(n));
      this.unreadNotificationsCount = notifs.filter(n => !n.read).length;
    });
    
    this.messagingService.getUnreadCount().subscribe((res: any) => {
      this.unreadMessagesCount = res.count || 0;
    });
  }

  private mapAlertToNotification(alert: Alert): any {
    let icon = 'bell';
    const cat = alert.category?.toUpperCase() || '';
    if (cat.includes('MAINTENANCE')) icon = 'wrench';
    else if (cat.includes('EQUIPMENT') || cat.includes('STOCK')) icon = 'link';
    else if (cat.includes('INSURANCE')) icon = 'shield';
    
    return {
      id: alert.id,
      title: alert.title || alert.message,
      time: this.formatTimeAgo(alert.createdAt),
      icon: icon,
      bgColor: alert.read ? '#f8fafc' : '#ecfdf5',
      isNew: !alert.read
    };
  }

  private formatTimeAgo(dateString: string): string {
    if (!dateString) return '';
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    if (diffMs < 0) return 'Just now';
    const diffMins = Math.floor(diffMs / 60000);
    if (diffMins < 60) return `${diffMins}m ago`;
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return `${diffHours}h ago`;
    return `${Math.floor(diffHours / 24)}d ago`;
  }

  markAllNotificationsAsRead(event: Event): void {
    event.stopPropagation();
    this.notificationsList.forEach(n => { n.isNew = false; n.bgColor = '#f8fafc'; });
    this.unreadNotificationsCount = 0;
    
    this.alertService.getUnreadAlerts().subscribe(alerts => {
      const unreadNotifs = alerts.filter(a => a.type === 'INFO' || a.type === 'SUCCESS');
      unreadNotifs.forEach(alert => {
        this.alertService.markAsRead(alert.id).subscribe();
      });
    });
  }

  deleteNotification(id: string, event: Event): void {
    event.stopPropagation();
    this.notificationsList = this.notificationsList.filter(n => n.id !== id);
    this.alertService.deleteAlert(id).subscribe();
  }

  deleteAllNotifications(event: Event): void {
    event.stopPropagation();
    const idsToDelete = this.notificationsList.map(n => n.id);
    this.notificationsList = [];
    this.unreadNotificationsCount = 0;
    
    idsToDelete.forEach(id => {
      this.alertService.deleteAlert(id).subscribe();
    });
  }

  selectLanguage(lang: 'en' | 'fr'): void {
    this.selectedLanguage = lang;
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  toggleAssistant(): void {
    this.isAssistantOpen = !this.isAssistantOpen;
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.isNotificationsOpen = !this.isNotificationsOpen;
  }

  getPageTitle(): string {
    switch (this.activeTab) {
      case 'dashboard': return 'Dashboard';
      case 'stock': return 'Stock (Shelves)';
      case 'equipment': return 'Equipment Management';
      case 'suppliers': return 'Suppliers';
      case 'orders': return 'Orders';
      case 'alerts': return 'Service Reminders';
      case 'reports': return 'Reports';
      case 'messages': return 'Messages';
      case 'schedule': return 'Schedule & Tasks';
      case 'categories': return 'Equipment Categories';
      case 'profile': return 'My Profile';
      case 'settings': return 'Account Settings';
      default: return 'Medina It Manage';
    }
  }

  clearFiltersAndGoToEquipment(): void {
    this.equipmentService.setShelfFilter(null, null);
    this.activeTab = 'equipment';
  }

  openMessages(): void {
    this.activeTab = 'messages';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
   
}
