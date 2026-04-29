import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { OsService } from './os.service';
import { OperatingSystem } from './os.model';
import { OsWizardComponent } from './os-wizard/os-wizard.component';
import { EquipmentService } from '../equipment/equipment.service';

@Component({
  selector: 'app-os-management',
  standalone: true,
  imports: [CommonModule, FormsModule, OsWizardComponent],
  templateUrl: './os-management.component.html',
  styleUrl: './os-management.component.css'
})
export class OsManagementComponent implements OnInit {
  operatingSystems: OperatingSystem[] = [];
  filteredOS: OperatingSystem[] = [];
  searchQuery: string = '';

  isWizardOpen: boolean = false;
  isDetailsOpen: boolean = false;
  selectedOS: OperatingSystem | null = null;
  selectedOSForDetails: OperatingSystem | null = null;
  user: any;
  showFilters: boolean = false;
  viewMode: 'table' | 'cards' = 'table';
  filterStatus: string = '';
  filterLicenseType: string = '';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;

  // Install Panel
  isInstallPanelOpen: boolean = false;
  osToInstall: OperatingSystem | null = null;
  equipments: any[] = [];
  filteredEquipments: any[] = [];
  equipmentSearchQuery: string = '';
  selectedEquipmentId: string = '';
  isInstalling: boolean = false;

  // Toast
  toastMessage: string = '';
  toastSuccess: boolean = true;
  private toastTimer: any = null;
  protected readonly Math = Math;

  constructor(
    private osService: OsService,
    private equipmentService: EquipmentService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      if (this.user) {
        this.loadOperatingSystems();
        this.loadEquipments();
      }
    });
  }

  loadOperatingSystems(): void {
    this.osService.getAllOperatingSystems().subscribe({
      next: (data) => {
        this.operatingSystems = data;
        this.applyFilters();
      },
      error: (err) => console.error('Failed to load OS data', err)
    });
  }

  loadEquipments(): void {
    this.equipmentService.getAllEquipment().subscribe({
      next: (data) => {
        // Filter for Devices only
        this.equipments = data.filter(eq => eq.category === 'DEVICE');
        this.applyEquipmentFilters();
      },
      error: (err) => console.error('Failed to load equipments', err)
    });
  }

  applyFilters(): void {
    const query = this.searchQuery.toLowerCase();
    this.filteredOS = this.operatingSystems.filter(os => {
      const matchesSearch = (os.name?.toLowerCase() || '').includes(query) ||
                            (os.version?.toLowerCase() || '').includes(query) ||
                            (os.edition?.toLowerCase() || '').includes(query);
      const matchesStatus = !this.filterStatus || os.status === this.filterStatus;
      const matchesLicense = !this.filterLicenseType || os.licenseType === this.filterLicenseType;
      return matchesSearch && matchesStatus && matchesLicense;
    });
    this.currentPage = 1;
  }

  get paginatedOS(): OperatingSystem[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredOS.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredOS.length / this.itemsPerPage);
  }

  changePage(p: number): void {
    if (p >= 1 && p <= this.totalPages) this.currentPage = p;
  }

  get pages(): number[] {
    return Array(this.totalPages).fill(0).map((_, i) => i + 1);
  }

  openAddWizard(): void {
    this.selectedOS = null;
    this.isWizardOpen = true;
  }

  openEditWizard(os: OperatingSystem): void {
    this.selectedOS = os;
    this.isWizardOpen = true;
  }

  onWizardClose(success: boolean): void {
    this.isWizardOpen = false;
    this.selectedOS = null;
    if (success) {
      this.loadOperatingSystems();
    }
  }

  openDetails(os: OperatingSystem): void {
    this.selectedOSForDetails = os;
    this.isDetailsOpen = true;
  }

  closeDetails(): void {
    this.isDetailsOpen = false;
    this.selectedOSForDetails = null;
  }

  deleteOperatingSystem(id: string): void {
    if (confirm('Are you sure you want to delete this Operating System? This action cannot be undone.')) {
      this.osService.deleteOperatingSystem(id).subscribe({
        next: () => {
          this.loadOperatingSystems();
          this.showToast('Operating System deleted successfully', true);
        },
        error: (err) => {
          console.error(err);
          this.showToast(err.error?.message || 'Failed to delete OS. It might be installed on devices.', false);
        }
      });
    }
  }

  // Install Panel Logic
  openInstallPanel(os: OperatingSystem): void {
    if (os.status === 'Deprecated') {
      alert('Cannot install a deprecated Operating System.');
      return;
    }
    if (os.licenseType !== 'Free' && (os.usedLicenses || 0) >= (os.totalLicenses || 0)) {
      alert('No licenses available for this OS.');
      return;
    }

    this.loadEquipments(); // Refresh equipment list
    this.osToInstall = os;
    this.selectedEquipmentId = '';
    this.equipmentSearchQuery = '';
    this.isInstallPanelOpen = true;
  }

  closeInstallPanel(): void {
    this.isInstallPanelOpen = false;
    this.osToInstall = null;
  }

  getSelectedEquipmentName(): string {
    const eq = this.filteredEquipments.find(e => e.id === this.selectedEquipmentId)
      || this.equipments.find(e => e.id === this.selectedEquipmentId);
    return eq ? eq.equipmentName : '';
  }

  showToast(message: string, success: boolean = true): void {
    this.toastMessage = message;
    this.toastSuccess = success;
    if (this.toastTimer) clearTimeout(this.toastTimer);
    this.toastTimer = setTimeout(() => { this.toastMessage = ''; }, 4000);
  }

  applyEquipmentFilters(): void {
    const query = this.equipmentSearchQuery.toLowerCase();
    this.filteredEquipments = this.equipments.filter(eq => {
      const matchName = (eq.equipmentName?.toLowerCase() || '').includes(query);
      const matchBrand = (eq.brand?.toLowerCase() || '').includes(query);
      
      // Check specifications map if needed
      let matchSpec = false;
      if (eq.specifications) {
        for (const key of Object.keys(eq.specifications)) {
          if ((eq.specifications[key]?.toLowerCase() || '').includes(query)) {
            matchSpec = true;
            break;
          }
        }
      }
      
      return matchName || matchBrand || matchSpec;
    });
  }

  submitInstall(): void {
    if (!this.selectedEquipmentId || !this.osToInstall || !this.osToInstall.id) {
      this.showToast('Please select a device first.', false);
      return;
    }

    const deviceName = this.getSelectedEquipmentName();
    this.isInstalling = true;
    const request = {
      osId: this.osToInstall.id!,
      equipmentId: this.selectedEquipmentId,
      installedBy: this.user.id
    };

    this.osService.installOS(request).subscribe({
      next: () => {
        this.isInstalling = false;
        this.closeInstallPanel();
        this.loadOperatingSystems();
        this.showToast(`✓ ${this.osToInstall?.name} ${this.osToInstall?.version} successfully installed on ${deviceName}`, true);
      },
      error: (err) => {
        console.error(err);
        this.isInstalling = false;
        this.showToast(err.error?.message || 'Installation failed. Please try again.', false);
      }
    });
  }

  getLicenseUsageClass(os: OperatingSystem): string {
    const percent = this.getLicenseUsagePercent(os);
    if (percent >= 90) return 'danger';
    if (percent >= 70) return 'warning';
    return 'success';
  }

  getLicenseUsagePercent(os: OperatingSystem): number {
    if (!os.totalLicenses || os.totalLicenses === 0) return 0;
    return ((os.usedLicenses || 0) / os.totalLicenses) * 100;
  }
}
