import { Component, OnInit } from '@angular/core';
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

  myRequests: PartRequest[] = [];

  viewMode: 'table' | 'card' = 'card';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Filters
  filterCategory: string = '';
  filterBrand: string = '';
  filterSupplier: string = '';
  filterShelfId: string = '';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10;

  suppliers: Supplier[] = [];
  shelves: Shelf[] = [];

  categories: EquipmentCategory[] = [];
  availableTypes: string[] = [];
  isWizardOpen: boolean = false;

  user: any;
  categoryNames: string[] = ['All', 'COMPONENT', 'STORAGE'];

  availableStockOptions: any[] = [];
  selectedStockItem: any = null;

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
        this.loadSuppliers();
        this.loadShelves();
      }
    });
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

  loadParts(): void {
    // Instead of global equipment, load the user's approved requests to build their inventory
    this.partRequestService.getMyRequests(this.user.id).subscribe((requests: PartRequest[]) => {
      const approved = requests.filter(r => r.status === 'APPROVED');

      this.parts = approved.map(r => ({
        ...r,
        equipmentName: r.partName,
        brand: '—', // Requests don't strictly bind brand yet, but can be pulled from name
        model: '—',
        qte: r.quantity, // map the requested quantity
        status: 'In stock', // Since it's approved and in possession
        shelfId: r.equipmentId // Use equipment reference
      }));
      this.applyFilters();
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
      const matchBrand = !this.filterBrand || part.brand?.toLowerCase() === this.filterBrand.toLowerCase();
      const matchSupplier = !this.filterSupplier || part.supplier === this.filterSupplier;
      const matchShelf = !this.filterShelfId || part.shelfId === this.filterShelfId;

      return matchSearch && matchCategory && matchBrand && matchSupplier && matchShelf;
    });

    const groupsMap = new Map<string, GroupedPart>();
    this.filteredParts.forEach(item => {
      const key = `${item.category}|${item.type}|${item.brand}|${item.model || ''}`;
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
      group.totalQuantity += (item.qte || 1); // Sum the quantities
    });

    this.groupedParts = Array.from(groupsMap.values()).map(group => {
      const first = group.items[0];
      group.commonLocation = group.items.every(i => i.shelfId === first.shelfId) ? this.getShelfLocation(first.shelfId) : 'Mixed';

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

  getShelfLocation(shelfId?: string): string {
    if (!shelfId) return 'Unassigned';
    const s = this.shelves.find(x => x.id === shelfId);
    return s ? `Shelf ${s.nb}` : 'Unknown';
  }

  openRequestWizard(): void {
    this.isWizardOpen = true;
  }

  onWizardClose(success: boolean): void {
    this.isWizardOpen = false;
    if (success) {
      this.loadMyRequests();
      this.loadParts(); // Refresh inventory if any auto-approved items (future-proofing)
    }
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
