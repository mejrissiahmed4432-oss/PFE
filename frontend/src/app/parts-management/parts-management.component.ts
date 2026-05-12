import { Component, OnInit, Input, OnChanges, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment/equipment.service';
import { AuthService } from '../auth.service';
import { PartRequestService } from './part-request.service';
import { PartRequest } from './part-request.model';
import { CategoryService } from '../category-manager/category.service';
import { EquipmentCategory } from '../category-manager/category.model';
import { SupplierService } from '../supplier/supplier.service';
import { Supplier } from '../supplier/supplier.model';
import { ShelfService } from '../shelf/shelf.service';
import { Shelf } from '../shelf/shelf.model';

import { PartRequestWizardComponent } from './part-request-wizard/part-request-wizard.component';

export interface GroupedPart {
  groupId: string;
  name: string;
  brand: string;
  type: string;
  category: string;
  model: string;
  totalQuantity: number;
  items: any[];
  expanded: boolean;
  statusSummary: { label: string; count: number; cls: string }[];
  commonLocation: string;
}

@Component({
  selector: 'app-parts-management',
  standalone: true,
  imports: [CommonModule, FormsModule, PartRequestWizardComponent],
  templateUrl: './parts-management.component.html',
  styleUrl: './parts-management.component.css'
})
export class PartsManagementComponent implements OnInit {
  parts: any[] = [];
  filteredParts: any[] = [];
  groupedParts: GroupedPart[] = [];
  @Input() resourceFilter: string = '';

  myRequests: PartRequest[] = [];

  viewMode: 'table' | 'card' = 'table';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Filters
  filterCategory: string = '';
  filterType: string = '';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;

  suppliers: Supplier[] = [];
  shelves: Shelf[] = [];

  categories: EquipmentCategory[] = [];
  availableTypes: string[] = [];
  isWizardOpen: boolean = false;
  preselectedGroup: any = null;

  user: any;
  categoryNames: string[] = ['All', 'COMPONENT', 'STORAGE'];

  availableStockOptions: any[] = [];
  selectedStockItem: any = null;

  // Custom Alert State
  alertConfig = {
    show: false,
    title: '',
    message: '',
    type: 'success' as 'success' | 'warning' | 'error',
    isConfirm: false,
    onConfirm: () => { }
  };

  constructor(
    private equipmentService: EquipmentService,
    private partRequestService: PartRequestService,
    private authService: AuthService,
    private categoryService: CategoryService,
    private supplierService: SupplierService,
    private shelfService: ShelfService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.user = user;
      if (this.user) {
        this.loadParts();
        this.loadMyRequests();
        this.loadCategories();
      }
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['resourceFilter']) {
      this.applyFilters();
    }
  }

  loadSuppliers(): void {
    this.supplierService.getAllSuppliers().subscribe(data => this.suppliers = data);
  }

  loadShelves(): void {
    this.shelfService.getAllShelves().subscribe(data => this.shelves = data);
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe(data => {
      this.categories = data;
    });
  }

  onCategoryFilterChange(): void {
    this.filterType = '';
    const selectedCat = this.categories.find(c => c.name === this.filterCategory);
    this.availableTypes = selectedCat ? (selectedCat.types || []).map((t: any) => typeof t === 'string' ? t : t.name) : [];
    this.applyFilters();
  }

  loadParts(): void {
    // Load the user's approved requests to build their inventory
    this.partRequestService.getMyRequests(this.user.id).subscribe((requests: PartRequest[]) => {
      const approved = requests.filter(r => r.status === 'APPROVED');

      this.parts = approved.flatMap(r =>
        (r.items || [])
          .filter(item => !item.returned)
          .map(item => {
            const isMatched = !!item.equipmentId && !!item.matchedEquipmentName;
            return {
              ...r,
              requestId: r.id,
              originalItem: item,
              equipmentName: isMatched ? item.matchedEquipmentName : item.partName,
              category: item.category || 'Unknown',
              type: item.type || 'Unknown',
              specification: isMatched ? (item.matchedSpecification || item.specification) : item.specification,
              brand: item.brand || '—',
              model: '—',
              qte: item.quantity,
              status: item.quantity === 0 ? 'Out of stock' : 'In stock',
              shelfId: item.equipmentId,
              equipmentId: item.equipmentId,
              isCustom: !item.equipmentId,
              isMatched: isMatched
            };
          })
      );

      // Inject Mock Resources if real ones aren't found
      this.injectMockResources();

      this.applyFilters();
    });
  }

  private injectMockResources(): void {
    const mocks = [
      // Operating Systems
      { equipmentName: 'Windows 11 Pro (23H2)', category: 'Operating Systems', type: 'OS', brand: 'Microsoft', qte: 1, status: 'In stock', specification: 'Retail License' },
      { equipmentName: 'Ubuntu 24.04 LTS', category: 'Operating Systems', type: 'OS', brand: 'Canonical', qte: 1, status: 'In stock', specification: 'Stable Release' },

      // Applications
      { equipmentName: 'Adobe Creative Cloud', category: 'Applications', type: 'Software', brand: 'Adobe', qte: 5, status: 'In stock', specification: 'Suite 2024' },

      { equipmentName: 'Microsoft Office 365', category: 'Applications', type: 'Software', brand: 'Microsoft', qte: 12, status: 'In stock', specification: 'Business Premium' }

    ];

    // Only add mocks if we don't have real items for these categories
    mocks.forEach(m => {
      const exists = this.parts.some(p => p.category === m.category);
      if (!exists) {
        this.parts.push({
          ...m,
          requestId: 'mock-' + Math.random(),
          isCustom: true,
          isMatched: false
        });
      }
    });
  }

  loadMyRequests(): void {
    if (this.user?.id) {
      this.partRequestService.getMyRequests(this.user.id).subscribe(requests => {
        this.myRequests = requests;
      });
    }
  }


  applyFilters(): void {
    const query = this.searchQuery.toLowerCase();

    this.filteredParts = this.parts.filter(part => {
      const matchSearch =
        (part.equipmentName?.toLowerCase() || '').includes(query) ||
        (part.type?.toLowerCase() || '').includes(query) ||
        (part.specification?.toLowerCase() || '').includes(query) ||
        (part.brand?.toLowerCase() || '').includes(query);

      const matchCategory = !this.filterCategory || part.category === this.filterCategory;
      const matchType = !this.filterType || part.type === this.filterType;

      // New Resource Filter Logic
      let matchResource = true;
      if (this.resourceFilter && this.resourceFilter !== 'Parts') {
        // If filter is "Operating Systems", "Applications", etc.
        // We look for categories or types that match
        const filterLower = this.resourceFilter.toLowerCase();
        matchResource = (part.category?.toLowerCase() === filterLower) ||
          (part.type?.toLowerCase() === filterLower) ||
          (part.equipmentName?.toLowerCase().includes(filterLower));
      } else if (this.resourceFilter === 'Parts') {
        // "Parts" refers to physical components, excluding Software/OS/Apps
        const softwareTerms = ['os', 'operating system', 'application', 'app', 'driver', 'tool', 'software'];
        matchResource = !softwareTerms.some(term =>
          (part.category?.toLowerCase().includes(term)) ||
          (part.type?.toLowerCase().includes(term))
        );
      }

      return matchSearch && matchCategory && matchType && matchResource;
    });

    const groupsMap = new Map<string, GroupedPart>();
    this.filteredParts.forEach(item => {
      const key = `${item.category}|${item.type}|${item.brand}|${item.model || ''}|${item.specification || ''}`;
      if (!groupsMap.has(key)) {
        groupsMap.set(key, {
          groupId: key,
          name: item.equipmentName || 'Unnamed Part',
          brand: item.brand || 'No Brand',
          type: item.type || 'unknown',
          category: item.category || 'Part',
          model: item.model || '—',
          totalQuantity: 0,
          items: [],
          expanded: false,
          statusSummary: [],
          commonLocation: ''
        });
      }
      const group = groupsMap.get(key)!;
      group.items.push(item);
      group.totalQuantity += (item.qte !== undefined ? item.qte : 1); // Only fallback to 1 if field is missing, not if it's 0
    });

    this.groupedParts = Array.from(groupsMap.values()).map(group => {
      const first = group.items[0];
      const availableItems = group.items.filter(i => i.status === 'Available');
      if (availableItems.length > 0) {
        const firstAvail = availableItems[0];
        const allSameShelf = availableItems.every(i => i.shelfId === firstAvail.shelfId);
        group.commonLocation = allSameShelf ? this.getShelfLocation(firstAvail.shelfId, 'Available') : 'Mixed Shelves';
      } else {
        group.commonLocation = this.getShelfLocation(first.shelfId, first.status);
      }

      const counts: Record<string, number> = {};
      group.items.forEach(i => {
        const s = i.status;
        counts[s] = (counts[s] || 0) + 1;
      });
      group.statusSummary = Object.entries(counts).map(([label, count]) => ({
        label: label,
        count: count,
        cls: this.getStatusColor(label)
      }));

      return group;
    });

    this.currentPage = 1;
  }

  get paginatedGroups(): GroupedPart[] {
    const start = (this.currentPage - 1) * this.itemsPerPage;
    return this.groupedParts.slice(start, start + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.groupedParts.length / this.itemsPerPage);
  }

  changePage(p: number): void {
    if (p >= 1 && p <= this.totalPages) this.currentPage = p;
  }

  get pages(): number[] {
    return Array(this.totalPages).fill(0).map((_, i) => i + 1);
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  toggleGroup(group: GroupedPart): void {
    group.expanded = !group.expanded;
  }

  getShelfLocation(shelfId?: string, status?: string): string {
    if (!shelfId) {
      if (status === 'Allocated') return 'Allocated (Not on Shelf)';
      if (status === 'Assigned' || status === 'Installed') return 'Installed (Not on Shelf)';
      return 'Unassigned';
    }
    const s = this.shelves.find(x => x.id === shelfId);
    return s ? `Shelf ${s.nb}` : 'Unknown';
  }

  openRequestWizard(group?: any): void {
    this.preselectedGroup = group || null;
    this.isWizardOpen = true;
  }

  onWizardClose(success: boolean): void {
    this.isWizardOpen = false;
    this.preselectedGroup = null;
    if (success) {
      this.loadMyRequests();
      this.loadParts(); // Refresh inventory if any auto-approved items (future-proofing)
    }
  }

  returnPartToStock(item: any): void {
    if (!item.equipmentId) {
      console.warn("Cannot return a part without an equipmentId");
      return;
    }

    this.showConfirm(
      'Return to Stock',
      `Are you sure you want to return "${item.equipmentName}" to stock? This will make the part available for other technicians.`,
      () => {
        this.equipmentService.returnPart(item.equipmentId).subscribe({
          next: () => {
            // Show Success Alert like the photo
            this.showAlert(
              'Part Returned',
              `You have successfully returned "${item.equipmentName}" to the stock inventory.`,
              'success'
            );

            // Also update the request item to mark as returned so it disappears from UI
            if (item.requestId && item.originalItem) {
              this.partRequestService.getMyRequests(this.user.id).subscribe(requests => {
                const requestToUpdate = requests.find(r => r.id === item.requestId);
                if (requestToUpdate) {
                  // Find the exact item in the request
                  const reqItem = requestToUpdate.items.find(i =>
                    i.partName === item.originalItem.partName &&
                    i.equipmentId === item.originalItem.equipmentId
                  );
                  if (reqItem) {
                    reqItem.returned = true;
                    this.partRequestService.updateRequest(item.requestId, requestToUpdate).subscribe({
                      next: () => {
                        this.loadParts(); // refresh UI
                      }
                    });
                  }
                }
              });
            } else {
              this.loadParts();
            }
          },
          error: (err) => {
            this.showAlert('Error', 'Failed to return the part to stock.', 'error');
            console.error("Error returning part", err);
          }
        });
      }
    );
  }

  showAlert(title: string, message: string, type: 'success' | 'warning' | 'error' = 'success') {
    this.alertConfig = {
      show: true,
      title,
      message,
      type,
      isConfirm: false,
      onConfirm: () => { }
    };
  }

  showConfirm(title: string, message: string, onConfirm: () => void) {
    this.alertConfig = {
      show: true,
      title,
      message,
      type: 'warning',
      isConfirm: true,
      onConfirm
    };
  }

  handleConfirm() {
    this.alertConfig.onConfirm();
    this.closeAlert();
  }

  closeAlert() {
    this.alertConfig.show = false;
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'In stock': return 'status-in-stock';
      case 'Low stock': return 'status-low-stock';
      case 'Out of stock': return 'status-out-of-stock';
      default: return 'status-default';
    }
  }
}
