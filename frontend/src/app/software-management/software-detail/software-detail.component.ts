import { Component, OnInit, OnChanges, Input, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareService } from '../software.service';
import { Software, LicensePool, SoftwareAssignment } from '../software.model';

@Component({
  selector: 'app-software-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
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

  // Delete confirm
  showDeleteConfirm = false;

  constructor(private softwareService: SoftwareService) {}

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
        next: () => this.softwareDeleted.emit(),
        error: (err) => alert('Failed to delete: ' + err.message)
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
}
