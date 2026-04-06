import { Component, OnDestroy, OnInit } from '@angular/core';
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
import { AlertService } from '../alerts/alert.service';
import { ShelfListComponent } from '../shelf/shelf-list/shelf-list.component';
import { EquipmentService } from '../equipment/equipment.service';
import { CategoryManagerComponent } from '../category-manager/category-manager.component';
import { MessagingComponent } from '../messaging/messaging.component';
import { MessagingService } from '../messaging/messaging.service';
import { SocketService } from '../messaging/socket.service';

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, AiAssistantComponent, EquipmentComponent, ProfileComponent, SettingsComponent, SupplierComponent, DashboardComponent, AlertsComponent, ShelfListComponent, CategoryManagerComponent, MessagingComponent],
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

  constructor(
    private authService: AuthService, 
    private router: Router,
    private alertService: AlertService,
    private equipmentService: EquipmentService,
    private messagingService: MessagingService,
    private socketService: SocketService
  ) { }

  ngOnInit(): void {
    this.userSub = this.authService.user$.subscribe(user => {
      this.user = user;
      if (!this.user) {
        this.router.navigate(['/login']);
      } else {
        this.loadUnreadCount();
        
        // 1. WebSocket Subscription for instant updates
        this.socketSub?.unsubscribe();
        this.socketSub = this.socketService.onUnreadCount.subscribe(count => {
          this.unreadMessagesCount = count;
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
    this.alertService.getUnreadAlerts().subscribe(alerts => {
      this.unreadAlertsCount = alerts.length;
    });
    
    this.messagingService.getUnreadCount().subscribe((res: any) => {
      this.unreadMessagesCount = res.count || 0;
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

  getPageTitle(): string {
    switch (this.activeTab) {
      case 'dashboard': return 'Dashboard';
      case 'stock': return 'Stock (Shelves)';
      case 'equipment': return 'Equipment Management';
      case 'suppliers': return 'Suppliers';
      case 'orders': return 'Orders';
      case 'alerts': return 'Alerts';
      case 'reports': return 'Reports';
      case 'messages': return 'Messages';
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
