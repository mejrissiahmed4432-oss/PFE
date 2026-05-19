import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { SoftwareService } from '../software.service';
import { Software, LicensePool, SoftwareAssignment } from '../software.model';
import { SoftwareWizardComponent } from '../software-wizard/software-wizard.component';
import { LicenseAssignmentModalComponent } from '../license-assignment-modal/license-assignment-modal.component';
import { SoftwareDetailComponent } from '../software-detail/software-detail.component';

@Component({
  selector: 'app-software-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, SoftwareWizardComponent, LicenseAssignmentModalComponent, SoftwareDetailComponent],
  templateUrl: './software-dashboard.component.html',
  styleUrls: ['./software-dashboard.component.css']
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

  constructor(private softwareService: SoftwareService) {}

  ngOnInit() {
    this.loadSoftware();
  }

  loadSoftware() {
    this.softwareService.getAllSoftware().subscribe({
      next: (data) => this.softwareList = data,
      error: (err) => console.error('Failed to load software', err)
    });
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

  deleteSoftware(sw: Software, event: Event) {
    event.stopPropagation();
    if (confirm(`Delete "${sw.name}"? This action cannot be undone.`)) {
      this.softwareService.deleteSoftware(sw.id!).subscribe({
        next: () => this.loadSoftware(),
        error: (err) => alert('Failed to delete: ' + err.message)
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
    }
    this.poolToAssign = null;
  }

  revokeAssignment(assignmentId: string) {
    if(confirm('Are you sure you want to revoke this assignment?')) {
      this.softwareService.revokeAssignment(assignmentId).subscribe({
        next: () => {
          if (this.expandedPoolId) {
             this.togglePoolExpand(this.expandedPoolId); // Refresh
             setTimeout(() => this.togglePoolExpand(this.expandedPoolId!), 100);
          }
        },
        error: (err) => alert('Failed to revoke: ' + err.message)
      });
    }
  }
}
