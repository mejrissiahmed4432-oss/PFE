import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ToastService } from '../../shared/toast.service';
import { SoftwareService } from '../software.service';
import { Software, LicensePool, SoftwareAssignment } from '../software.model';
import { SoftwareWizardComponent } from '../software-wizard/software-wizard.component';
import { LicenseAssignmentModalComponent } from '../license-assignment-modal/license-assignment-modal.component';
import { SoftwareDetailComponent } from '../software-detail/software-detail.component';
import { trigger, style, transition, animate } from '@angular/animations';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-software-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SoftwareWizardComponent, LicenseAssignmentModalComponent, SoftwareDetailComponent],
  templateUrl: './software-dashboard.component.html',
  styleUrls: ['./software-dashboard.component.css'],
  animations: [
    trigger('expandAnimation', [
      transition(':enter', [
        style({ height: '0', opacity: 0, overflow: 'hidden' }),
        animate('300ms ease-out', style({ height: '*', opacity: 1 }))
      ]),
      transition(':leave', [
        style({ height: '*', opacity: 1, overflow: 'hidden' }),
        animate('250ms ease-in', style({ height: '0', opacity: 0 }))
      ])
    ])
  ]
})
export class SoftwareDashboardComponent implements OnInit {
  softwareList: Software[] = [];
  expandedSoftwareId: string | null = null;
  poolsForExpanded: LicensePool[] = [];
  
  expandedPoolId: string | null = null;
  assignmentsForExpanded: SoftwareAssignment[] = [];

  // View state: 'list' or 'detail'
  currentView: 'list' | 'detail' = 'list';
  selectedSoftwareId: string = '';

  // Wizard state
  showWizard = false;
  softwareToEdit: Software | null = null;

  // Reveal keys state
  showPasswordModal = false;
  poolToReveal: string | null = null;
  adminPassword = '';
  revealedKeys: string[] = [];

  // Assignment state
  showAssignmentModal = false;
  poolToAssign: LicensePool | null = null;

  // Revoke state
  showRevokeConfirm = false;
  assignmentToRevoke: string | null = null;

  stats = {
    totalSoftware: 0,
    totalSeats: 0,
    assignedSeats: 0,
    availableSeats: 0
  };

  // Tracking lists
  recentAddedSoftware: Software[] = [];
  recentAssignments: any[] = [];
  expiringSoonPools: any[] = [];
  isLoadingDetails = false;

  constructor(
    private softwareService: SoftwareService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    this.loadSoftware();
  }

  loadSoftware() {
    this.softwareService.getAllSoftware().subscribe({
      next: (data) => {
        this.softwareList = data;
        this.calculateStats();
        this.loadDashboardLists();
      },
      error: (err) => console.error('Failed to load software', err)
    });
  }

  loadDashboardLists() {
    if (!this.softwareList || this.softwareList.length === 0) {
      this.recentAddedSoftware = [];
      this.recentAssignments = [];
      this.expiringSoonPools = [];
      return;
    }

    const validSoftware = this.softwareList.filter(sw => sw && sw.id);
    if (validSoftware.length === 0) {
      this.recentAddedSoftware = [];
      this.recentAssignments = [];
      this.expiringSoonPools = [];
      this.isLoadingDetails = false;
      return;
    }

    this.isLoadingDetails = true;

    const requests = validSoftware.map(sw => {
      return forkJoin({
        software: of(sw),
        pools: this.softwareService.getPoolsBySoftware(sw.id!).pipe(
          catchError(err => {
            console.warn(`Failed to load pools for software: ${sw.name}`, err);
            return of([]);
          })
        ),
        assignments: this.softwareService.getAssignmentsBySoftware(sw.id!).pipe(
          catchError(err => {
            console.warn(`Failed to load assignments for software: ${sw.name}`, err);
            return of([]);
          })
        )
      });
    });

    forkJoin(requests).subscribe({
      next: (results) => {
        const allPools: any[] = [];
        const allAssignments: any[] = [];

        results.forEach(res => {
          if (res.pools) {
            res.pools.forEach((p: any) => {
              p.softwareName = res.software.name;
              allPools.push(p);
            });
          }
          if (res.assignments) {
            res.assignments.forEach((asm: any) => {
              asm.softwareName = res.software.name;
              allAssignments.push(asm);
            });
          }
        });

        this.processDashboardLists(allPools, allAssignments);
        this.isLoadingDetails = false;
      },
      error: (err) => {
        console.error('Failed to load dashboard lists details', err);
        this.isLoadingDetails = false;
      }
    });
  }

  processDashboardLists(allPools: any[], allAssignments: any[]) {
    // 1. Recent Added (Sorted by createdAt desc)
    this.recentAddedSoftware = [...this.softwareList]
      .sort((a, b) => {
        const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
        const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 5);

    // 2. Recent Assignments (ACTIVE status preferred, sorted by assignedAt descending)
    this.recentAssignments = allAssignments
      .filter(asm => asm.status === 'ACTIVE' || !asm.status)
      .sort((a, b) => {
        const dateA = a.assignedAt ? new Date(a.assignedAt).getTime() : 0;
        const dateB = b.assignedAt ? new Date(b.assignedAt).getTime() : 0;
        return dateB - dateA;
      })
      .slice(0, 5);

    // 3. Expiring Soon Pools (expirationDate exists, is in the future, within 10 days)
    const tenDaysFromNow = new Date();
    tenDaysFromNow.setDate(tenDaysFromNow.getDate() + 10);
    const now = new Date();

    this.expiringSoonPools = allPools
      .filter(p => {
        if (!p.expirationDate) return false;
        const expDate = new Date(p.expirationDate);
        return expDate > now && expDate <= tenDaysFromNow;
      })
      .sort((a, b) => {
        const dateA = new Date(a.expirationDate!).getTime();
        const dateB = new Date(b.expirationDate!).getTime();
        return dateA - dateB;
      })
      .slice(0, 5);
  }

  calculateStats() {
    let totalSoftware = this.softwareList.length;
    let totalSeats = 0;
    let availableSeats = 0;
    this.softwareList.forEach(sw => {
      totalSeats += sw.totalSeats || 0;
      availableSeats += sw.availableSeats || 0;
    });
    let assignedSeats = totalSeats - availableSeats;
    this.stats = {
      totalSoftware,
      totalSeats,
      assignedSeats,
      availableSeats
    };
  }

  toggleSoftwareExpand(softwareId: string) {
    if (this.expandedSoftwareId === softwareId) {
      this.expandedSoftwareId = null;
      this.poolsForExpanded = [];
      this.expandedPoolId = null;
    } else {
      this.expandedSoftwareId = softwareId;
      this.expandedPoolId = null;
      this.softwareService.getPoolsBySoftware(softwareId).subscribe({
        next: (pools) => this.poolsForExpanded = pools,
        error: (err) => console.error('Failed to load pools', err)
      });
    }
  }

  togglePoolExpand(poolId: string) {
    if (this.expandedPoolId === poolId) {
      this.expandedPoolId = null;
      this.assignmentsForExpanded = [];
    } else {
      this.expandedPoolId = poolId;
      this.softwareService.getAssignmentsBySoftware(this.expandedSoftwareId!).subscribe({
        next: (assignments) => {
          this.assignmentsForExpanded = assignments.filter(a => a.licensePoolId === poolId);
        },
        error: (err) => console.error('Failed to load assignments', err)
      });
    }
  }

  openWizard() {
    this.softwareToEdit = null;
    this.showWizard = true;
  }

  editSoftware(sw: Software, event: Event) {
    event.stopPropagation();
    this.softwareToEdit = sw;
    this.showWizard = true;
  }

  viewDetail(swId: string) {
    this.selectedSoftwareId = swId;
    this.currentView = 'detail';
  }

  handleGoBack() {
    this.currentView = 'list';
    this.selectedSoftwareId = '';
  }

  handleDeleted() {
    this.currentView = 'list';
    this.selectedSoftwareId = '';
    this.loadSoftware();
  }

  showDeleteConfirm = false;
  softwareToDelete: Software | null = null;

  deleteSoftware(sw: Software, event: Event) {
    event.stopPropagation();
    this.softwareToDelete = sw;
    this.showDeleteConfirm = true;
  }

  confirmDeleteSoftware() {
    if (this.softwareToDelete?.id) {
      this.softwareService.deleteSoftware(this.softwareToDelete.id).subscribe({
        next: () => {
          this.toastService.delete('Software deleted successfully.');
          this.showDeleteConfirm = false;
          this.softwareToDelete = null;
          this.loadSoftware();
        },
        error: (err) => {
          this.toastService.error('Failed to delete: ' + err.message);
          this.showDeleteConfirm = false;
          this.softwareToDelete = null;
        }
      });
    }
  }

  onWizardClose(success: boolean) {
    this.showWizard = false;
    this.softwareToEdit = null;
    if (success) {
      this.loadSoftware();
    }
  }

  promptRevealKeys(poolId: string) {
    this.poolToReveal = poolId;
    this.adminPassword = '';
    this.revealedKeys = [];
    this.showPasswordModal = true;
  }

  submitRevealKeys() {
    if (this.poolToReveal && this.adminPassword) {
      this.softwareService.revealKeys(this.poolToReveal, this.adminPassword).subscribe({
        next: (keys) => {
          this.revealedKeys = keys;
          if(keys.length === 0) {
            alert("No keys stored for this pool.");
          }
        },
        error: (err) => {
          alert('Incorrect password or error revealing keys.');
          console.error(err);
        }
      });
    }
  }

  closePasswordModal() {
    this.showPasswordModal = false;
    this.poolToReveal = null;
    this.adminPassword = '';
    this.revealedKeys = [];
  }

  openAssignmentModal(pool: LicensePool) {
    this.poolToAssign = pool;
    this.showAssignmentModal = true;
  }

  onAssignmentClose(assigned: boolean) {
    this.showAssignmentModal = false;
    if (assigned && this.expandedSoftwareId) {
      // Refresh pools and assignments
      this.softwareService.getPoolsBySoftware(this.expandedSoftwareId).subscribe(pools => this.poolsForExpanded = pools);
      if (this.expandedPoolId) {
        this.togglePoolExpand(this.expandedPoolId); // toggle off
        setTimeout(() => this.togglePoolExpand(this.expandedPoolId!), 100); // toggle on to refresh
      }
      this.loadSoftware();
    }
    this.poolToAssign = null;
  }

  revokeAssignment(assignmentId: string) {
    this.assignmentToRevoke = assignmentId;
    this.showRevokeConfirm = true;
  }

  confirmRevoke() {
    if (this.assignmentToRevoke) {
      this.softwareService.revokeAssignment(this.assignmentToRevoke).subscribe({
        next: () => {
          this.toastService.success('License assignment has been successfully revoked.');
          this.showRevokeConfirm = false;
          this.assignmentToRevoke = null;
          if (this.expandedPoolId) {
             this.togglePoolExpand(this.expandedPoolId); // Refresh
             setTimeout(() => this.togglePoolExpand(this.expandedPoolId!), 100);
          }
          this.loadSoftware();
        },
        error: (err) => {
          this.toastService.error('Failed to revoke assignment: ' + err.message);
          this.showRevokeConfirm = false;
          this.assignmentToRevoke = null;
        }
      });
    }
  }

  // Dynamic Category SVGs classifiers
  isDevTool(name: string, type: string): boolean {
    const n = name?.toLowerCase() || '';
    const t = type?.toLowerCase() || '';
    return n.includes('intellij') || n.includes('vs code') || n.includes('visual studio') || n.includes('eclipse') || n.includes('pycharm') || n.includes('webstorm') || t.includes('dev') || t.includes('ide') || t.includes('development');
  }

  isProductivityTool(name: string, type: string): boolean {
    const n = name?.toLowerCase() || '';
    const t = type?.toLowerCase() || '';
    return n.includes('office') || n.includes('slack') || n.includes('zoom') || n.includes('teams') || n.includes('excel') || n.includes('word') || t.includes('productivity') || t.includes('office') || t.includes('collaboration');
  }

  isCreativeTool(name: string, type: string): boolean {
    const n = name?.toLowerCase() || '';
    const t = type?.toLowerCase() || '';
    return n.includes('adobe') || n.includes('photoshop') || n.includes('illustrator') || n.includes('figma') || n.includes('creative') || t.includes('design') || t.includes('creative') || t.includes('graphics');
  }

  isDatabaseTool(name: string, type: string): boolean {
    const n = name?.toLowerCase() || '';
    const t = type?.toLowerCase() || '';
    return n.includes('sql') || n.includes('oracle') || n.includes('postgres') || n.includes('mongo') || t.includes('database') || t.includes('db') || t.includes('backend');
  }

  isOSTool(name: string, type: string): boolean {
    const n = name?.toLowerCase() || '';
    const t = type?.toLowerCase() || '';
    return n.includes('windows') || n.includes('macos') || n.includes('linux') || n.includes('ubuntu') || t.includes('os') || t.includes('system') || t.includes('operating');
  }

  isDefaultTool(name: string, type: string): boolean {
    return !this.isDevTool(name, type) && !this.isProductivityTool(name, type) && !this.isCreativeTool(name, type) && !this.isDatabaseTool(name, type) && !this.isOSTool(name, type);
  }

  // Seat health percentage driving the visual progress bar
  getSeatPercent(sw: Software): number {
    if (!sw.totalSeats) return 0;
    return Math.round(((sw.availableSeats || 0) / sw.totalSeats) * 100);
  }

  // Seat alert states
  isSeatsExhausted(pool: LicensePool): boolean {
    return (pool.availableSeats !== undefined ? pool.availableSeats : 0) <= 0;
  }

  isSeatsLow(pool: LicensePool): boolean {
    const avail = pool.availableSeats !== undefined ? pool.availableSeats : 0;
    return avail > 0 && (avail <= 2 || (pool.totalSeats > 0 && (avail / pool.totalSeats) <= 0.15));
  }

  // Expiration date checks
  isExpired(expirationDate?: string): boolean {
    if (!expirationDate) return false;
    const exp = new Date(expirationDate);
    const now = new Date();
    return exp.getTime() < now.getTime();
  }

  isExpiringSoon(expirationDate?: string): boolean {
    if (!expirationDate) return false;
    const exp = new Date(expirationDate);
    const now = new Date();
    const diffTime = exp.getTime() - now.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    return diffDays >= 0 && diffDays <= 30;
  }
}
