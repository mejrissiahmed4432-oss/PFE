import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonalRequestService, PersonalRequest } from '../personal-requests/personal-request.service';

@Component({
  selector: 'app-request-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './request-management.component.html',
  styleUrls: ['./request-management.component.css']
})
export class RequestManagementComponent implements OnInit {

  tabs = [
    { key: 'pending' as const, label: 'Pending Review' },
    { key: 'history' as const, label: 'History' }
  ];

  activeTab: 'pending' | 'history' = 'pending';

  allRequests: PersonalRequest[] = [];
  filteredRequests: PersonalRequest[] = [];

  searchTerm = '';

  showDetailsModal = false;
  showReviewModal = false;
  selectedRequest: PersonalRequest | null = null;
  isApproving = true;
  reviewNote = '';
  submitting = false;

  get pendingCount(): number {
    return this.activeTab === 'pending' ? this.allRequests.length : 0;
  }

  get uniqueRequesters(): number {
    return new Set(this.allRequests.map(r => r.userId)).size;
  }

  get totalItemsRequested(): number {
    return this.allRequests.reduce((sum, r) => sum + (r.requestedItems?.length || 0), 0);
  }

  get approvedCount(): number {
    return this.activeTab === 'history' ? this.allRequests.filter(r => r.status === 'APPROVED').length : 0;
  }

  get rejectedCount(): number {
    return this.activeTab === 'history' ? this.allRequests.filter(r => r.status === 'REJECTED').length : 0;
  }

  constructor(private prService: PersonalRequestService) {}

  ngOnInit(): void {
    this.loadByStatus();
  }

  loadByStatus() {
    if (this.activeTab === 'pending') {
      this.prService.getPendingRequests().subscribe(res => {
        this.allRequests = res;
        this.applyFilter();
      });
    } else {
      this.prService.getHistory().subscribe(res => {
        this.allRequests = res;
        this.applyFilter();
      });
    }
  }

  setTab(tab: 'pending' | 'history') {
    this.activeTab = tab;
    this.searchTerm = '';
    this.loadByStatus();
  }

  applyFilter() {
    const term = this.searchTerm.toLowerCase().trim();
    if (!term) {
      this.filteredRequests = [...this.allRequests];
      return;
    }
    this.filteredRequests = this.allRequests.filter(r =>
      r.userName?.toLowerCase().includes(term) ||
      r.requestedItems?.some(i => i.itemName?.toLowerCase().includes(term)) ||
      r.reason?.toLowerCase().includes(term)
    );
  }

  viewDetails(req: PersonalRequest) {
    this.selectedRequest = req;
    this.showDetailsModal = true;
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.selectedRequest = null;
  }

  openReviewModal(req: PersonalRequest, isApproving: boolean) {
    this.selectedRequest = req;
    this.isApproving = isApproving;
    this.reviewNote = '';
    this.showReviewModal = true;
  }

  closeReviewModal() {
    this.showReviewModal = false;
    this.selectedRequest = null;
    this.submitting = false;
  }

  submitReview() {
    if (!this.selectedRequest?.id) return;
    this.submitting = true;

    const obs = this.isApproving
      ? this.prService.approveRequest(this.selectedRequest.id, this.reviewNote)
      : this.prService.rejectRequest(this.selectedRequest.id, this.reviewNote);

    obs.subscribe({
      next: () => {
        this.closeReviewModal();
        this.loadByStatus();
      },
      error: () => { this.submitting = false; }
    });
  }
}
