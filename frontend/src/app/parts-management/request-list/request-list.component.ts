import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PartRequestService } from '../part-request.service';
import { PartRequest } from '../part-request.model';
import { AuthService } from '../../auth.service';
import { FormsModule } from '@angular/forms';
import { PartRequestWizardComponent } from '../part-request-wizard/part-request-wizard.component';

@Component({
  selector: 'app-request-list',
  standalone: true,
  imports: [CommonModule, FormsModule, PartRequestWizardComponent],
  templateUrl: './request-list.component.html',
  styleUrl: './request-list.component.css'
})
export class RequestListComponent implements OnInit {
  requests: any[] = [];
  filteredRequests: any[] = [];
  user: any;
  activeTab: 'active' | 'history' = 'active';

  isWizardOpen: boolean = false;
  requestToEdit: PartRequest | null = null;

  constructor(
    private partRequestService: PartRequestService,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      if (this.user?.id) {
        this.loadRequests();
      }
    });
  }

  loadRequests(): void {
    this.partRequestService.getMyRequests(this.user.id).subscribe({
      next: (data) => {
        this.requests = data.sort((a, b) =>
          new Date(b.createdAt || '').getTime() - new Date(a.createdAt || '').getTime()
        ).map(r => ({
          ...r,
          isEditing: false,
          editQuantity: r.quantity,
          editPriority: r.priority,
          editSpecification: r.specification
        }));
        this.applyFilters();
      },
      error: (err) => console.error('Error loading requests', err)
    });
  }

  applyFilters(): void {
    if (this.activeTab === 'active') {
      this.filteredRequests = this.requests.filter(r => r.status === 'PENDING');
    } else {
      this.filteredRequests = this.requests.filter(r => r.status !== 'PENDING');
    }
  }

  getPendingCount(): number {
    return this.requests.filter(r => r.status === 'PENDING').length;
  }

  setTab(tab: 'active' | 'history'): void {
    this.activeTab = tab;
    this.applyFilters();
  }

  openEditWizard(request: any): void {
    this.requestToEdit = request;
    this.isWizardOpen = true;
  }

  onWizardClose(success: boolean): void {
    this.isWizardOpen = false;
    this.requestToEdit = null;
    if (success) {
      this.loadRequests(); // refresh list after a successful edit
    }
  }

  cancelRequest(request: any): void {
    if (confirm(`Are you sure you want to cancel the request for ${request.partName}?`)) {
      this.partRequestService.deleteRequest(request.id).subscribe({
        next: () => {
          this.requests = this.requests.filter(r => r.id !== request.id);
          this.applyFilters();
        },
        error: err => console.error("Error deleting", err)
      });
    }
  }

  getStatusClass(status?: string): string {
    switch (status) {
      case 'APPROVED': return 'status-approved';
      case 'REJECTED': return 'status-rejected';
      default: return 'status-pending';
    }
  }

  getPriorityClass(priority?: string): string {
    switch (priority) {
      case 'High': return 'priority-high';
      case 'Medium': return 'priority-medium';
      default: return 'priority-low';
    }
  }
}
