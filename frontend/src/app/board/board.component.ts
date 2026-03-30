import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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

@Component({
  selector: 'app-board',
  standalone: true,
  imports: [CommonModule, AiAssistantComponent, EquipmentComponent, ProfileComponent, SettingsComponent, SupplierComponent, DashboardComponent, AlertsComponent, ShelfListComponent],
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

  constructor(
    private authService: AuthService, 
    private router: Router,
    private alertService: AlertService,
    private equipmentService: EquipmentService
  ) { }

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser();
    if (!this.user) {
      this.router.navigate(['/login']);
    } else {
      this.loadUnreadCount();
    }
  }

  loadUnreadCount(): void {
    this.alertService.getUnreadAlerts().subscribe(alerts => {
      this.unreadAlertsCount = alerts.length;
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
      case 'profile': return 'My Profile';
      case 'settings': return 'Account Settings';
      default: return 'Medina It Manage';
    }
  }

  clearFiltersAndGoToEquipment(): void {
    this.equipmentService.setShelfFilter(null, null);
    this.activeTab = 'equipment';
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
   
}
