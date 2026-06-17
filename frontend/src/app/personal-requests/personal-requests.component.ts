import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PersonalRequestService, PersonalRequest, RequestedItem } from './personal-request.service';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-personal-requests',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './personal-requests.component.html',
  styleUrls: ['./personal-requests.component.css']
})
export class PersonalRequestsComponent implements OnInit {
  user: any = null;

  tabs = [
    { key: 'pending' as const, label: 'Pending' },
    { key: 'approved' as const, label: 'Approved' },
    { key: 'rejected' as const, label: 'Rejected' }
  ];

  activeTab: 'pending' | 'approved' | 'rejected' = 'pending';

  allRequests: PersonalRequest[] = [];
  filteredRequests: PersonalRequest[] = [];

  showNewRequestModal = false;
  showDetailsModal = false;
  showEquipmentDetailsModal = false;

  selectedRequest: PersonalRequest | null = null;
  selectedEquipmentDetails: any = null;

  availableEquipment: any[] = [];
  availableSoftware: any[] = [];

  selectedEquipmentIds: Set<string> = new Set();
  selectedSoftwareIds: Set<string> = new Set();
  requestReason: string = '';

  // List search
  searchTerm: string = '';

  // Modal search
  equipmentSearch: string = '';
  softwareSearch: string = '';

  get filteredEquipment(): any[] {
    const t = this.equipmentSearch.toLowerCase().trim();
    const available = this.availableEquipment.filter(e =>
      e.status === 'Available' || e.status === 'AVAILABLE'
    );
    if (!t) return available;
    return available.filter(e =>
      e.equipmentName?.toLowerCase().includes(t) ||
      e.brand?.toLowerCase().includes(t) ||
      e.type?.toLowerCase().includes(t)
    );
  }

  get filteredSoftware(): any[] {
    const t = this.softwareSearch.toLowerCase().trim();
    if (!t) return this.availableSoftware;
    return this.availableSoftware.filter(s =>
      s.name?.toLowerCase().includes(t) ||
      s.vendor?.toLowerCase().includes(t) ||
      s.type?.toLowerCase().includes(t)
    );
  }

  constructor(
    private prService: PersonalRequestService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(u => {
      this.user = u;
      this.loadRequests();
    });
  }

  loadRequests() {
    if (!this.user) return;
    this.prService.getMyRequests(this.user.id).subscribe(res => {
      this.allRequests = res;
      this.filterRequests();
    });
  }

  setTab(tab: 'pending' | 'approved' | 'rejected') {
    this.activeTab = tab;
    this.searchTerm = '';
    this.filterRequests();
  }

  filterRequests() {
    const term = this.searchTerm.toLowerCase().trim();
    const byStatus = this.allRequests.filter(r => r.status === this.activeTab.toUpperCase());
    if (!term) {
      this.filteredRequests = byStatus;
      return;
    }
    this.filteredRequests = byStatus.filter(r =>
      r.requestedItems?.some(i => i.itemName?.toLowerCase().includes(term)) ||
      r.reason?.toLowerCase().includes(term)
    );
  }

  getCount(status: string): number {
    return this.allRequests.filter(r => r.status === status).length;
  }

  openNewRequestModal() {
    this.showNewRequestModal = true;
    this.selectedEquipmentIds.clear();
    this.selectedSoftwareIds.clear();

    this.prService.getAvailableEquipment().subscribe(res => this.availableEquipment = res);
    this.prService.getAvailableSoftware().subscribe(res => this.availableSoftware = res);
  }

  closeNewRequestModal() {
    this.showNewRequestModal = false;
    this.requestReason = '';
    this.equipmentSearch = '';
    this.softwareSearch = '';
  }

  toggleEquipmentSelection(id: string) {
    if (this.selectedEquipmentIds.has(id)) this.selectedEquipmentIds.delete(id);
    else this.selectedEquipmentIds.add(id);
  }

  toggleSoftwareSelection(id: string) {
    if (this.selectedSoftwareIds.has(id)) this.selectedSoftwareIds.delete(id);
    else this.selectedSoftwareIds.add(id);
  }

  viewEquipmentDetails(eq: any, event: Event) {
    event.stopPropagation(); // prevent checking the checkbox
    this.selectedEquipmentDetails = eq;
    this.showEquipmentDetailsModal = true;
  }

  closeEquipmentDetailsModal() {
    this.showEquipmentDetailsModal = false;
    this.selectedEquipmentDetails = null;
  }

  objectEntries(obj: any): [string, any][] {
    if (!obj || typeof obj !== 'object') return [];
    return Object.entries(obj);
  }

  createRequest() {
    const requestedItems: RequestedItem[] = [];

    this.selectedEquipmentIds.forEach(id => {
      const eq = this.availableEquipment.find(e => e.id === id);
      if (eq) requestedItems.push({ itemId: eq.id, itemType: 'EQUIPMENT', itemName: eq.equipmentName, brand: eq.brand, model: eq.model, type: eq.type });
    });

    this.selectedSoftwareIds.forEach(id => {
      const sw = this.availableSoftware.find(s => s.id === id);
      if (sw) requestedItems.push({ itemId: sw.id, itemType: 'SOFTWARE', itemName: sw.name, type: sw.type, version: sw.version });
    });

    const newReq: PersonalRequest = {
      userId: this.user.id,
      userName: `${this.user.firstName} ${this.user.lastName}`,
      requestedItems,
      reason: this.requestReason
    };

    this.prService.createRequest(newReq).subscribe(() => {
      this.closeNewRequestModal();
      this.loadRequests();
    });
  }

  deleteRequest(id: string) {
    if (confirm('Are you sure you want to delete this pending request?')) {
      this.prService.deleteRequest(id).subscribe(() => this.loadRequests());
    }
  }

  viewDetails(req: PersonalRequest) {
    this.selectedRequest = req;
    this.showDetailsModal = true;
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.selectedRequest = null;
  }
}
