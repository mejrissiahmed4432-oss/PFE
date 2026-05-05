import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Application, EquipmentApplication } from './application.model';
import { ApplicationService } from './application.service';
import { AuthService } from '../auth.service';
import { EquipmentService } from '../equipment/equipment.service';
import { Equipment } from '../equipment/equipment.model';

@Component({
  selector: 'app-application-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './application-management.component.html',
  styleUrl: './application-management.component.css'
})
export class ApplicationManagementComponent implements OnInit {
  applications: Application[] = [];
  filteredApplications: Application[] = [];
  user: any;
  
  // Search & Filters
  searchQuery: string = '';
  filterCategory: string = '';
  filterStatus: string = '';
  
  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;
  
  // UI State
  showWizard: boolean = false;
  isEditMode: boolean = false;
  showInstallModal: boolean = false;
  wizardStep: number = 1;
  showFilters: boolean = false;
  viewMode: 'table' | 'cards' = 'table';
  
  // Form Model
  currentApp: Partial<Application> = this.getEmptyApp();
  
  // Installation State
  selectedEquipmentId: string = '';
  availableEquipment: Equipment[] = [];
  filteredEquipment: Equipment[] = [];
  equipmentSearchQuery: string = '';
  installingApp: Application | null = null;
  isInstalling: boolean = false;

  // Categories
  appCategories: ('Office' | 'Browser' | 'Security' | 'Utility' | 'Other')[] = ['Office', 'Browser', 'Security', 'Utility', 'Other'];
  licenseTypes: ('Free' | 'Paid' | 'Subscription')[] = ['Free', 'Paid', 'Subscription'];

  protected readonly Math = Math;

  constructor(
    private appService: ApplicationService,
    private authService: AuthService,
    private equipmentService: EquipmentService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      this.loadApplications();
    });
    this.loadEquipment();
  }

  loadApplications(): void {
    this.appService.getAllApplications().subscribe(apps => {
      this.applications = apps;
      this.applyFilters();
    });
  }

  loadEquipment(): void {
    this.equipmentService.getAllEquipment().subscribe(equipments => {
      // Filter for Devices only
      this.availableEquipment = equipments.filter(eq => eq.category === 'DEVICE');
      this.applyEquipmentFilters();
    });
  }

  getEmptyApp(): Partial<Application> {
    return {
      name: '',
      version: '',
      vendor: '',
      category: 'Other',
      licenseType: 'Free',
      status: 'Active',
      icon: ''
    };
  }

  applyFilters(): void {
    this.filteredApplications = this.applications.filter(app => {
      const matchesSearch = app.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
                          app.vendor?.toLowerCase().includes(this.searchQuery.toLowerCase());
      const matchesCategory = !this.filterCategory || app.category === this.filterCategory;
      const matchesStatus = !this.filterStatus || app.status === this.filterStatus;
      return matchesSearch && matchesCategory && matchesStatus;
    });
    this.currentPage = 1;
  }

  get paginatedApps(): Application[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredApplications.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredApplications.length / this.itemsPerPage);
  }

  openWizard(app?: Application): void {
    if (app) {
      this.currentApp = { ...app };
      this.isEditMode = true;
    } else {
      this.currentApp = this.getEmptyApp();
      this.isEditMode = false;
    }
    this.showWizard = true;
    this.wizardStep = 1;
  }

  closeWizard(): void {
    this.showWizard = false;
    this.currentApp = this.getEmptyApp();
  }

  getStepTitle(): string {
    switch (this.wizardStep) {
      case 1: return 'Basic Information';
      case 2: return 'License Management';
      case 3: return 'System Requirements';
      case 4: return 'Status & Review';
      default: return '';
    }
  }

  getStepDescription(): string {
    switch (this.wizardStep) {
      case 1: return 'Set the application name, version, and vendor details.';
      case 2: return 'Manage license keys, types, and quantity limits.';
      case 3: return 'Define hardware requirements and OS compatibility.';
      case 4: return 'Final review and lifecycle status assignment.';
      default: return '';
    }
  }

  nextStep(): void {
    if (this.wizardStep < 4) this.wizardStep++;
  }

  prevStep(): void {
    if (this.wizardStep > 1) this.wizardStep--;
  }

  saveApplication(): void {
    if (this.isEditMode && this.currentApp.id) {
      this.appService.updateApplication(this.currentApp.id, this.currentApp as Application).subscribe(() => {
        this.loadApplications();
        this.closeWizard();
      });
    } else {
      this.appService.createApplication(this.currentApp as Application).subscribe(() => {
        this.loadApplications();
        this.closeWizard();
      });
    }
  }

  deleteApplication(id: string): void {
    if (confirm('Are you sure you want to delete this application?')) {
      this.appService.deleteApplication(id).subscribe(() => {
        this.loadApplications();
      });
    }
  }

  openInstallModal(app: Application): void {
    this.installingApp = app;
    this.showInstallModal = true;
    this.selectedEquipmentId = '';
  }

  closeInstallModal(): void {
    this.showInstallModal = false;
    this.installingApp = null;
  }

  installApp(): void {
    if (this.installingApp && this.selectedEquipmentId && this.user) {
      this.isInstalling = true;
      this.appService.installApplication({
        applicationId: this.installingApp.id!,
        equipmentId: this.selectedEquipmentId,
        installedBy: this.user.id
      }).subscribe({
        next: () => {
          this.isInstalling = false;
          this.loadApplications();
          this.closeInstallModal();
          alert('Application installed successfully!');
        },
        error: (err) => {
          this.isInstalling = false;
          alert(err.error?.message || 'Failed to install application');
        }
      });
    }
  }

  applyEquipmentFilters(): void {
    const query = this.equipmentSearchQuery.toLowerCase();
    this.filteredEquipment = this.availableEquipment.filter(eq => {
      const matchName = (eq.equipmentName?.toLowerCase() || '').includes(query);
      const matchBrand = (eq.brand?.toLowerCase() || '').includes(query);
      
      let matchSpec = false;
      if (eq.specifications) {
        for (const key of Object.keys(eq.specifications)) {
          if ((String(eq.specifications[key])?.toLowerCase() || '').includes(query)) {
            matchSpec = true;
            break;
          }
        }
      }
      
      return matchName || matchBrand || matchSpec;
    });
  }

  getSelectedEquipmentName(): string {
    const eq = this.availableEquipment.find(e => e.id === this.selectedEquipmentId);
    return eq?.equipmentName || '';
  }

  downloadInstaller(app: Application): void {
    if (app.installerPath) {
      window.open(app.installerPath, '_blank');
    } else if (app.downloadLink) {
      window.open(app.downloadLink, '_blank');
    } else {
      alert('No installer available for this application.');
    }
  }

  getLicenseUsagePercent(app: Application): number {
    if (!app.totalLicenses || app.totalLicenses === 0) return 0;
    return ((app.usedLicenses || 0) / app.totalLicenses) * 100;
  }

  getLicenseStatusClass(app: Application): string {
    const percent = this.getLicenseUsagePercent(app);
    if (percent >= 90) return 'danger';
    if (percent >= 70) return 'warning';
    return 'success';
  }
}
