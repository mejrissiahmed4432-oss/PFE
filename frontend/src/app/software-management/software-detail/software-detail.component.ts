import { Component, OnInit, OnChanges, Input, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareService } from '../software.service';
import { ToastService } from '../../shared/toast.service';
import { Software, LicensePool, SoftwareAssignment } from '../software.model';
import { LicenseAssignmentModalComponent } from '../license-assignment-modal/license-assignment-modal.component';

@Component({
  selector: 'app-software-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, LicenseAssignmentModalComponent],
  templateUrl: './software-detail.component.html',
  styleUrls: ['./software-detail.component.css']
})
export class SoftwareDetailComponent implements OnInit, OnChanges {
  @Input() softwareId: string = '';
  @Output() goBack = new EventEmitter<void>();
  @Output() softwareDeleted = new EventEmitter<void>();

  software: Software | null = null;
  pools: LicensePool[] = [];
  assignments: SoftwareAssignment[] = [];
  isLoading = true;

  activeTab: 'overview' | 'pool' | 'assignments' | 'keys' | 'activity' = 'overview';

  // Key reveal
  showPasswordModal = false;
  poolToReveal: string | null = null;
  adminPassword = '';
  revealedKeys: string[] = [];
  revealError = '';

  // Assignments
  showAssignmentModal = false;
  poolToAssign: LicensePool | null = null;
  showRevokeConfirm = false;
  assignmentToRevoke: string | null = null;

  // Delete confirm
  showDeleteConfirm = false;

  constructor(
    private softwareService: SoftwareService,
    private toastService: ToastService
  ) {}

  ngOnInit() {
    if (this.softwareId) this.loadSoftware(this.softwareId);
  }

  ngOnChanges(changes: SimpleChanges) {
    if (changes['softwareId'] && changes['softwareId'].currentValue) {
      this.activeTab = 'overview';
      this.loadSoftware(changes['softwareId'].currentValue);
    }
  }

  loadSoftware(id: string) {
    this.isLoading = true;
    this.softwareService.getSoftwareById(id).subscribe({
      next: (sw) => {
        this.software = sw;
        this.isLoading = false;
        this.loadPools(id);
        this.loadAllAssignments(id);
      },
      error: () => {
        this.isLoading = false;
        this.goBack.emit();
      }
    });
  }

  loadPools(softwareId: string) {
    this.softwareService.getPoolsBySoftware(softwareId).subscribe({
      next: (pools) => this.pools = pools,
      error: (err) => console.error('Failed to load pools', err)
    });
  }

  get activeAssignments() {
    return this.assignments.filter(a => a.status?.toUpperCase() === 'ACTIVE');
  }

  get historyAssignments() {
    return this.assignments.filter(a => a.status?.toUpperCase() !== 'ACTIVE');
  }

  loadAllAssignments(softwareId: string) {
    this.softwareService.getAssignmentsBySoftware(softwareId).subscribe({
      next: (asms) => this.assignments = asms,
      error: (err) => console.error('Failed to load assignments', err)
    });
  }

  onGoBack() {
    this.goBack.emit();
  }

  deleteSoftware() {
    if (this.software?.id) {
      this.softwareService.deleteSoftware(this.software.id).subscribe({
        next: () => {
          this.toastService.delete('Software deleted successfully.');
          this.showDeleteConfirm = false;
          this.softwareDeleted.emit();
        },
        error: (err) => this.toastService.error('Failed to delete: ' + err.message)
      });
    }
  }

  promptRevealKeys(poolId: string) {
    this.poolToReveal = poolId;
    this.adminPassword = '';
    this.revealedKeys = [];
    this.revealError = '';
    this.showPasswordModal = true;
  }

  submitRevealKeys() {
    if (!this.poolToReveal || !this.adminPassword) return;
    this.softwareService.revealKeys(this.poolToReveal, this.adminPassword).subscribe({
      next: (keys) => {
        this.revealedKeys = keys;
        if (keys.length === 0) {
          this.revealError = 'No keys stored for this pool.';
        }
      },
      error: () => {
        this.revealError = 'Incorrect password or unauthorized.';
      }
    });
  }

  closePasswordModal() {
    this.showPasswordModal = false;
    this.poolToReveal = null;
    this.adminPassword = '';
    this.revealedKeys = [];
    this.revealError = '';
  }

  openAssignmentModal(pool?: LicensePool) {
    if (pool) {
      this.poolToAssign = pool;
    } else if (this.pools.length > 0) {
      this.poolToAssign = this.pools[0]; // default to first pool
    }
    
    if (this.poolToAssign) {
      this.showAssignmentModal = true;
    } else {
      alert('Please create a License Pool first before assigning users.');
    }
  }

  onAssignmentClose(success: boolean) {
    this.showAssignmentModal = false;
    this.poolToAssign = null;
    if (success && this.software?.id) {
      this.loadSoftware(this.software.id);
    }
  }

  promptRevoke(asmId: string) {
    this.assignmentToRevoke = asmId;
    this.showRevokeConfirm = true;
  }

  confirmRevoke() {
    if (this.assignmentToRevoke) {
      this.softwareService.revokeAssignment(this.assignmentToRevoke).subscribe({
        next: () => {
          this.toastService.success('License successfully revoked.');
          this.showRevokeConfirm = false;
          this.assignmentToRevoke = null;
          if (this.software?.id) this.loadSoftware(this.software.id);
        },
        error: (err) => this.toastService.error('Failed to revoke: ' + err.message)
      });
    }
  }

  getStatusClass(status: string): string {
    return status?.toLowerCase() === 'active' ? 'status-active' : 'status-inactive';
  }

  getLicenseModelClass(model: string): string {
    const map: Record<string, string> = {
      'SUBSCRIPTION': 'badge-blue',
      'VOLUME': 'badge-purple',
      'OEM': 'badge-amber',
      'PERPETUAL': 'badge-green'
    };
    return map[model] || 'badge-gray';
  }

  totalSeats(): number {
    return this.pools.reduce((sum, p) => sum + (p.totalSeats || 0), 0);
  }

  availableSeats(): number {
    return this.pools.reduce((sum, p) => sum + (p.availableSeats || 0), 0);
  }

  // Dynamic Category Classifiers
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
