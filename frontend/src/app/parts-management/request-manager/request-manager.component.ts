import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PartRequestService } from '../part-request.service';
import { PartRequest } from '../part-request.model';
import { AuthService } from '../../auth.service';
import { EquipmentService } from '../../equipment/equipment.service';

@Component({
  selector: 'app-request-manager',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './request-manager.component.html',
  styleUrl: './request-manager.component.css'
})
export class RequestManagerComponent implements OnInit {
  user: any;
  allRequests: PartRequest[] = [];
  filteredRequests: PartRequest[] = [];
  
  activeTab: 'PENDING' | 'PROCESSED' = 'PENDING';
  searchQuery: string = '';
  expandedRequestId: string | null = null;
  processingId: string | null = null;

  constructor(
    private partRequestService: PartRequestService,
    private authService: AuthService,
    private equipmentService: EquipmentService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      if (this.user) {
        this.loadAllRequests();
      }
    });
  }

  loadAllRequests(): void {
    this.partRequestService.getAllRequests().subscribe(requests => {
      this.allRequests = requests.sort((a, b) => 
        new Date(b.createdAt || 0).getTime() - new Date(a.createdAt || 0).getTime()
      );
      this.filterRequests();
    });
  }

  setTab(tab: 'PENDING' | 'PROCESSED'): void {
    this.activeTab = tab;
    this.expandedRequestId = null;
    this.filterRequests();
  }

  filterRequests(): void {
    let filtered = this.allRequests;

    // Tab Filter
    if (this.activeTab === 'PENDING') {
      filtered = filtered.filter(r => r.status === 'PENDING');
    } else {
      filtered = filtered.filter(r => r.status !== 'PENDING');
    }

    // Search Filter
    if (this.searchQuery.trim()) {
      const q = this.searchQuery.toLowerCase();
      filtered = filtered.filter(r => 
        (r.requesterName || '').toLowerCase().includes(q) ||
        (r.description || '').toLowerCase().includes(q) ||
        (r.id || '').toLowerCase().includes(q) ||
        (r.items || []).some(item => (item.partName || '').toLowerCase().includes(q))
      );
    }

    this.filteredRequests = filtered;
  }

  toggleExpand(requestId: string | undefined): void {
    if (!requestId) return;
    this.expandedRequestId = this.expandedRequestId === requestId ? null : requestId;
  }

  updateStatus(requestId: string | undefined, status: 'APPROVED' | 'REJECTED', event: Event): void {
    event.stopPropagation(); // prevent expanding the row when clicking action buttons
    if (!requestId) return;

    const requestToApprove = this.allRequests.find(r => r.id === requestId);

    this.processingId = requestId;
    this.partRequestService.updateStatus(requestId, status).subscribe({
      next: (updated) => {
        if (status === 'APPROVED' && requestToApprove && requestToApprove.items) {
          const consumed = requestToApprove.items.map(item => ({
            name: item.partName,
            brand: item.brand,
            type: item.type,
            specification: item.specification,
            qty: item.quantity,
            equipmentId: item.equipmentId
          }));
          this.equipmentService.consumeParts(consumed).subscribe({
            next: () => console.log('Successfully decremented global stock for approved parts.'),
            error: (err) => console.error('Failed to decrement global stock for approved parts.', err)
          });
        }

        // Update the local list
        const index = this.allRequests.findIndex(r => r.id === requestId);
        if (index !== -1) {
          this.allRequests[index] = updated;
        }
        this.filterRequests();
        this.processingId = null;
      },
      error: (err) => {
        console.error('Error updating status', err);
        this.processingId = null;
      }
    });
  }

  getPriorityColor(priority?: string): string {
    switch (priority) {
      case 'High': return '#ef4444'; // Red
      case 'Medium': return '#f59e0b'; // Orange
      case 'Low': return '#3b82f6'; // Blue
      default: return '#64748b'; // Gray
    }
  }

  getStatusBadgeClass(status?: string): string {
    switch (status) {
      case 'APPROVED': return 'badge-approved';
      case 'REJECTED': return 'badge-rejected';
      case 'CANCELLED': return 'badge-cancelled';
      case 'PENDING': return 'badge-pending';
      default: return 'badge-default';
    }
  }
}
