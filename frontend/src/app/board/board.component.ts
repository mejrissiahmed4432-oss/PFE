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
import { NotificationService, Notification } from '../alerts/notification.service';
import { ShelfListComponent } from '../shelf/shelf-list/shelf-list.component';
import { EquipmentService } from '../equipment/equipment.service';
import { CategoryManagerComponent } from '../category-manager/category-manager.component';
import { MessagingComponent } from '../messaging/messaging.component';
import { MessagingService } from '../messaging/messaging.service';
import { SocketService } from '../messaging/socket.service';
import { ScheduleComponent } from '../schedule/schedule.component';
import { PartsManagementComponent } from '../parts-management/parts-management.component';
import { RequestListComponent } from '../parts-management/request-list/request-list.component';
import { RequestManagerComponent } from '../parts-management/request-manager/request-manager.component';
import { TicketsComponent } from '../tickets/tickets.component';
import { ReportsComponent } from '../reports/reports.component';
import { SoftwareDashboardComponent } from '../software-management/software-dashboard/software-dashboard.component';
import { TranslationService } from '../shared/translation.service';
import { PartRequestService } from '../parts-management/part-request.service';
import { EmployeeListComponent } from '../employee/employee-list/employee-list.component';
import { HrDashboardComponent } from '../hr-dashboard/hr-dashboard.component';
import { OsManagementComponent } from '../os-management/os-management.component';
import { UserManagementComponent } from '../user-management/user-management.component';
import { AdminDashboardComponent } from '../admin-dashboard/admin-dashboard.component';
import { TechnicianDepartmentsComponent } from '../technician-departments/technician-departments.component';
import { TechnicianDevicesComponent } from '../technician-devices/technician-devices.component';

import { ToastComponent } from '../shared/toast/toast.component';
import { ProcurementComponent } from '../procurement/procurement.component';
import { ProcurementService } from '../procurement/procurement.service';
import { ItEquipmentManagementComponent } from '../it-equipment-management/it-equipment-management.component';
import { EquipmentReturnsComponent } from '../equipment-returns/equipment-returns.component';


import { TicketService } from '../tickets/ticket.service';
import { RefreshService } from '../shared/refresh.service';
import { ApplicationManagementComponent } from '../application-management/application-management.component';
import { AuditDashboardComponent } from '../audit/audit-dashboard.component';
import { PredictiveMaintenanceComponent } from '../predictive-maintenance/predictive-maintenance.component';
import { TaskManagementComponent } from '../task-management/task-management.component';

@Component({
  selector: 'app-board',
  standalone: true,


  imports: [CommonModule, AiAssistantComponent, EquipmentComponent, ProfileComponent, SettingsComponent, SupplierComponent, DashboardComponent, AlertsComponent, ShelfListComponent, CategoryManagerComponent, MessagingComponent, ScheduleComponent, PartsManagementComponent, RequestListComponent, RequestManagerComponent, TicketsComponent, ReportsComponent, OsManagementComponent, ApplicationManagementComponent, EmployeeListComponent, HrDashboardComponent, UserManagementComponent, ToastComponent, ProcurementComponent, AdminDashboardComponent, TechnicianDepartmentsComponent, TechnicianDevicesComponent, SoftwareDashboardComponent, ItEquipmentManagementComponent, EquipmentReturnsComponent, AuditDashboardComponent, PredictiveMaintenanceComponent, TaskManagementComponent],


  providers: [MessagingService, TicketService],
  templateUrl: './board.component.html',
  styleUrl: './board.component.css'
})
export class BoardComponent implements OnInit {
  user: any;
  selectedLanguage: 'en' | 'fr' = 'en'; // Default is English
  isAssistantOpen: boolean = false;
  isSidebarCollapsed: boolean = false;
  /** Department filter passed to TechnicianDevicesComponent (null = all depts) */
  devicesDeptFilter: string | null = null;

  /** Called when 'View All Devices' is clicked on a dept card */
  onViewDevices(deptName: string): void {
    this.navigateToTab('devices', () => {
      this.devicesDeptFilter = deptName;
    });
  }

  /** Open devices with no filter (from sidebar) */
  openAllDevices(): void {
    this.navigateToTab('devices', () => {
      this.devicesDeptFilter = null;
    });
  }
  activeTab: string = 'dashboard'; // Defaulting to dashboard for view
  navResetKey = 0;
  selectedNatureFilter: 'Asset' | 'Consumable' | '' = '';
  selectedResourceFilter: string = '';
  selectedEqTab: 'available' | 'in-use' | 'history' = 'available';
  unreadAlertsCount: number = 0;
  unreadMessagesCount: number = 0;
  isDarkMode: boolean = false;
  private userSub: Subscription | undefined;
  private pollSub: Subscription | undefined;
  private socketSub: Subscription | undefined;
  private procSub: Subscription | undefined;

  isNotificationsOpen: boolean = false;
  isLanguageOpen: boolean = false;
  notificationsList: any[] = [];
  unreadNotificationsCount: number = 0;

  pendingRequestsCount: number = 0;
  pendingProcurementCount: number = 0;
  activeTicketCount: number = 0;
  private refreshSub?: Subscription;


  constructor(
    private authService: AuthService,
    private router: Router,
    private alertService: AlertService,
    private equipmentService: EquipmentService,
    private messagingService: MessagingService,
    private socketService: SocketService,
    private notificationService: NotificationService,

    public ts: TranslationService,
    private partRequestService: PartRequestService,
    private procurementService: ProcurementService,
    private ticketService: TicketService,
    private refreshService: RefreshService

  ) { }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent) {
    const target = event.target as HTMLElement;
    if (!target.closest('.notif-container') && !target.closest('.notif-dropdown')) {
      this.isNotificationsOpen = false;
    }
    if (!target.closest('.lang-switcher-container')) {
      this.isLanguageOpen = false;
    }
  }

  ngOnInit(): void {
    // Sync language from service
    this.selectedLanguage = this.ts.getLanguage();

    this.userSub = this.authService.user$.subscribe(user => {
      this.user = user;
      if (!this.user) {
        this.router.navigate(['/login']);
      } else {
        if (this.user.role === 'TECHNICIAN') {
          this.activeTab = 'departments';
        } else if (this.user.role === 'IT_MANAGER') {
          this.activeTab = 'user-management';
        } else if (this.user.role === 'ADMIN') {
          this.activeTab = 'admin-dashboard';
        }
        this.loadUnreadCount();

        // 1. WebSocket Subscription for instant message updates
        this.socketSub?.unsubscribe();
        this.socketSub = this.socketService.onUnreadCount.subscribe(count => {
          this.unreadMessagesCount = count;
        });

        // 1.5 Auto-logout if deactivated or deleted by IT Manager
        this.socketService.onUserStatus.subscribe((event: any) => {
          if (event.userId === this.user.id && (event.status === 'INACTIVE' || event.status === 'DELETED')) {
            console.warn('Account deactivated or deleted by IT Manager. Auto-logging out...');
            this.logout();
          }
        });

        // Live Alerts Subscription
        this.socketService.onAlertUpdate.subscribe(() => {
          this.loadUnreadCount();
        });

        // Live Notifications Subscription
        this.socketService.onNotificationUpdate.subscribe(() => {
          this.loadUnreadCount();
          this.loadTicketCount();
        });

        // 2. Reduce non-realtime polling to once per minute (standard dashboard sync)
        if (!this.pollSub) {
          import('rxjs').then(({ interval }) => {
            this.pollSub = interval(60000).subscribe(() => this.loadUnreadCount());
          });
        }
        // 3. Procurement Refresh
        this.procSub?.unsubscribe();
        this.procSub = this.procurementService.requestCreated$.subscribe(() => {
          this.loadUnreadCount();
        });

        // 4. Ticket Count for Technician
        this.loadTicketCount();
        this.refreshSub = this.refreshService.refresh$.subscribe(() => {
          this.loadTicketCount();
        });
      }
    });

    // Theme initialization
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme === 'dark') {
      this.isDarkMode = true;
      document.body.classList.add('dark-mode');
    }
  }

  ngOnDestroy(): void {
    this.userSub?.unsubscribe();
    this.pollSub?.unsubscribe();
    this.socketSub?.unsubscribe();
    this.procSub?.unsubscribe();
  }

  loadUnreadCount(): void {
    if (!this.user) return;
    const userId = this.user.id;
    const role = this.user.role;

    // 1. Load System Alerts (Stock/Warranty/System)
    this.alertService.getAlerts(userId, role).subscribe(alerts => {
      this.unreadAlertsCount = alerts.filter(a => (a.status || 'ACTIVE') === 'ACTIVE').length;
    });

    // 2. Load Notifications (CRUD/User Actions)
    this.notificationService.getNotifications(userId, role).subscribe(notifications => {
      this.notificationsList = notifications
        .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
        .map(n => this.mapNotifToNotificationItem(n));
      this.unreadNotificationsCount = notifications.filter(n => !n.read).length;
    });

    // 3. Load Messages
    this.messagingService.getUnreadCount().subscribe((res: any) => {
      this.unreadMessagesCount = res.count || 0;
    });


    // 4. Load Pending Requests (for managers)
    if (role === 'STOCK_MANAGER' || role === 'IT_MANAGER' || role === 'ADMIN') {
      // Part Requests
      this.partRequestService.getAllRequests().subscribe(requests => {
        this.pendingRequestsCount = requests.filter(r => r.status === 'PENDING').length;
      });

      // Procurement Requests
      this.procurementService.getAllRequests().subscribe(requests => {
        if (role === 'IT_MANAGER') {
          this.pendingProcurementCount = requests.filter(r => r.status === 'PENDING_IT_APPROVAL' || r.status === 'APPROVED' || r.status === 'RESPONDED').length;
        } else if (role === 'STOCK_MANAGER' || role === 'ADMIN') {
          // Stock Managers care about approved requests that need RFQs or further action
          this.pendingProcurementCount = requests.filter(r => r.status === 'APPROVED' || r.status === 'PENDING_IT_APPROVAL' || r.status === 'RESPONDED').length;
        }
      });
    }
  }

  private mapNotifToNotificationItem(notif: Notification): any {
    let icon = 'bell';
    const cat = notif.category?.toUpperCase() || '';
    if (cat.includes('EQUIPMENT')) icon = 'link';
    else if (cat.includes('SHELF')) icon = 'link';
    else if (cat.includes('SUPPLIER')) icon = 'wrench';

    return {
      id: notif.id,
      title: notif.title || notif.message,
      time: this.formatTimeAgo(notif.createdAt),
      icon: icon,
      bgColor: notif.read ? '#f8fafc' : '#ecfdf5',
      isNew: !notif.read
    };
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
    if (!this.user) return;
    this.notificationService.getUnreadNotifications(this.user.id, this.user.role).subscribe(notifications => {
      notifications.forEach(n => {
        this.notificationService.markAsRead(n.id).subscribe();
      });
      this.notificationsList.forEach(n => { n.isNew = false; n.bgColor = '#f8fafc'; });
      this.unreadNotificationsCount = 0;
    });
  }

  deleteNotification(id: string, event: Event): void {
    event.stopPropagation();
    this.notificationsList = this.notificationsList.filter(n => n.id !== id);
    this.notificationService.deleteNotification(id).subscribe();
  }

  deleteAllNotifications(event: Event): void {
    event.stopPropagation();
    this.notificationsList = [];
    this.unreadNotificationsCount = 0;
    this.notificationService.deleteAllNotifications().subscribe();
  }

  selectLanguage(lang: 'en' | 'fr'): void {
    this.selectedLanguage = lang;
    this.ts.setLanguage(lang);
    this.isLanguageOpen = false;
  }

  toggleLanguage(event: Event): void {
    event.stopPropagation();
    this.isLanguageOpen = !this.isLanguageOpen;
    this.isNotificationsOpen = false; // Close other dropdowns
  }

  t(key: string): string {
    return this.ts.translate(key);
  }

  toggleSidebar(): void {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
  }

  toggleAssistant(): void {
    console.log('Toggling assistant. Current state:', this.isAssistantOpen);
    this.isAssistantOpen = !this.isAssistantOpen;
    console.log('New state:', this.isAssistantOpen);
  }

  toggleNotifications(event: Event): void {
    event.stopPropagation();
    this.isNotificationsOpen = !this.isNotificationsOpen;
    this.isLanguageOpen = false; // Close other dropdowns
  }

  getPageTitle(): string {
    switch (this.activeTab) {
      case 'dashboard': return this.t('Dashboard');
      case 'stock': return this.t('Stock');
      case 'equipment': return this.t('Inventory');
      case 'suppliers': return this.t('Suppliers');
      case 'orders': return this.t('Orders');
      case 'alerts': return 'Service Reminders';
      case 'reports': return this.t('Reports');
      case 'messages': return this.t('Messages');
      case 'schedule': return this.t('Schedule');
      case 'categories': return this.t('Categories');
      case 'profile': return this.t('Profile');
      case 'settings': return this.t('Settings');
      case 'parts': return this.selectedResourceFilter ? `${this.t('Resources')} - ${this.selectedResourceFilter}` : this.t('Resources');
      case 'requests': return 'My Part Requests';
      case 'manager-requests': return 'Incoming Part Requests';

      case 'user-management': return 'User Access Management';
      case 'task-management': return 'Task Management';

      case 'procurement': return 'Procurement & Orders';

      case 'eq-management': return 'Equipment Management';
      case 'eq-returns': return 'Equipment Returns';
      case 'predictions': return 'Predictive Maintenance';


      case 'tickets': return this.t('Tickets');

      case 'employees': return this.t('Employee Directory');
      case 'audit': return 'Audit & Blockchain Traceability';
      case 'admin-dashboard': return 'Admin Management Console';

      default: return 'Medina It Manage';
    }
  }

  navigateToTab(tab: string, setup?: () => void): void {
    if (this.activeTab === tab) {
      this.navResetKey++;
    }
    setup?.();
    this.activeTab = tab;
  }

  clearFiltersAndGoToEquipment(nature: 'Asset' | 'Consumable' | '' = ''): void {
    this.equipmentService.setShelfFilter(null, null);
    this.navigateToTab('equipment', () => {
      this.selectedNatureFilter = nature;
    });
  }

  setEqTab(tab: 'available' | 'in-use' | 'history'): void {
    this.navigateToTab('eq-management', () => {
      this.selectedEqTab = tab;
    });
  }

  setResourceFilter(filter: string): void {
    this.navigateToTab('parts', () => {
      if (filter === '') {
        if (this.user?.role === 'IT_MANAGER') {
          this.selectedResourceFilter = 'Software';
        } else {
          this.selectedResourceFilter = 'Parts';
        }
      } else {
        this.selectedResourceFilter = filter;
      }
    });
  }

  openMessages(): void {
    this.navigateToTab('messages');
  }

  private loadTicketCount() {
    if (!this.user) return;

    if (this.user.role === 'TECHNICIAN') {
      this.ticketService.getTicketsForTechnician(this.user.id).subscribe(tickets => {
        const active = tickets.filter(t =>
          t.status !== 'Completed' && t.status !== 'Resolved' && t.status !== 'Closed' && t.status !== 'Cancelled'
        );
        this.activeTicketCount = active.length;
      });
    } else {
      this.ticketService.getTicketsByUser(this.user.id).subscribe(tickets => {
        const active = tickets.filter(t =>
          t.status !== 'Completed' && t.status !== 'Resolved' && t.status !== 'Closed' && t.status !== 'Cancelled'
        );
        this.activeTicketCount = active.length;
      });
    }
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  toggleDarkMode(): void {
    this.isDarkMode = !this.isDarkMode;
    if (this.isDarkMode) {
      document.body.classList.add('dark-mode');
      localStorage.setItem('theme', 'dark');
    } else {
      document.body.classList.remove('dark-mode');
      localStorage.setItem('theme', 'light');
    }
  }

}
