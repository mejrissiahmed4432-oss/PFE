import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment.service';
import { Equipment } from '../equipment.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ShelfService } from '../../shelf/shelf.service';
import { Shelf } from '../../shelf/shelf.model';
import { CategoryService } from '../../category-manager/category.service';
import { EquipmentCategory } from '../../category-manager/category.model';
import { ToastService } from '../../shared/toast.service';
import { forkJoin } from 'rxjs';
import * as QRCode from 'qrcode';

import { trigger, state, style, transition, animate } from '@angular/animations';

export interface GroupedEquipment {
  groupId: string;
  name: string;
  brand: string;
  type: string;
  category: string;
  totalQuantity: number;
  items: Equipment[];
  expanded: boolean;
  
  // Aggregate fields
  commonSupplier: string;
  commonPurchaseDate: string;
  commonLocation: string;
  commonWarranty: string;
  commonModel: string;
  statusSummary: { label: string; count: number; cls: string }[];
  allSameAttributes: boolean;
}

@Component({
  selector: 'app-equipment-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-list.component.html',
  styleUrl: './equipment-list.component.css',
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
export class EquipmentListComponent implements OnInit, OnChanges {
  @Input() refreshTrigger: number = 0;
  @Output() editEvent = new EventEmitter<Equipment>();
  @Output() viewEvent = new EventEmitter<Equipment>();
  @Output() addSimilarEvent = new EventEmitter<Equipment>();

  equipments: Equipment[] = [];
  filteredEquipments: Equipment[] = [];
  groupedEquipments: GroupedEquipment[] = [];
  suppliers: Supplier[] = [];
  shelves: Shelf[] = [];
  categoriesList: EquipmentCategory[] = [];
  
  viewMode: 'table' | 'card' = 'table';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10; // Increased for groups
  
  // Filters
  filterCategory: string = '';
  filterType: string = '';
  filterSupplier: string = '';
  filterPurchaseDate: string = '';
  filterShelfId: string | null = null;
  filterShelfNb: string | null = null;
  filterSelectedShelf: string = '';
  @Input() natureFilter: 'Asset' | 'Consumable' | '' = '';

  // QR Modal state
  qrModalEquipment: Equipment | null = null;
  qrModalDataUrl: string = '';
  showQrModal: boolean = false;

  // Group Edit Modal
  showGroupEditModal: boolean = false;
  editingGroup: GroupedEquipment | null = null;
  bulkEditForm = { name: '', brand: '' };
  isBulkSaving: boolean = false;

  // Delete Confirmation Modal
  showDeleteModal: boolean = false;
  deleteModalTitle: string = '';
  deleteModalMessage: string = '';
  itemToDeleteId: string | null = null;
  groupToDeleteIds: string[] | null = null;
  isBulkDelete: boolean = false;

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
    private shelfService: ShelfService,
    private categoryService: CategoryService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    const shelfFilter = this.equipmentService.getShelfFilter();
    this.filterShelfId = shelfFilter.id;
    this.filterShelfNb = shelfFilter.nb;

    this.loadEquipments();
    this.loadSuppliers();
    this.loadShelves();
    this.loadCategories();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['refreshTrigger'] && !changes['refreshTrigger'].firstChange) {
      this.loadEquipments();
    }
    if (changes['natureFilter']) {
      this.applyFilters();
    }
  }

  loadEquipments(): void {
    this.equipmentService.getAllEquipment().subscribe({
      next: (data) => {
        this.equipments = data;
        this.applyFilters();
      },
      error: (err) => console.error('Error fetching equipments', err)
    });
  }

  loadSuppliers(): void {
    this.supplierService.getAllSuppliers().subscribe({
      next: (data) => this.suppliers = data,
      error: (err) => console.error('Error fetching suppliers', err)
    });
  }

  loadShelves(): void {
    this.shelfService.getAllShelves().subscribe({
      next: (data) => this.shelves = data,
      error: (err) => console.error('Error fetching shelves', err)
    });
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data: EquipmentCategory[]) => {
        this.categoriesList = data;
        this.applyFilters();
      },
      error: (err: any) => console.error('Error fetching categories', err)
    });
  }

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  onCategoryChange(): void {
    this.filterType = '';
    this.applyFilters();
  }

  getAvailableTypes(): string[] {
    if (!this.filterCategory) return [];
    const cat = this.categoriesList.find(c => c.name === this.filterCategory);
    return cat?.types?.map(t => t.name) || [];
  }

  applyFilters(): void {
    // 1. First filter the raw list
    this.filteredEquipments = this.equipments.filter(eq => {
      const matchSearch = this.searchQuery ? 
        (eq.equipmentName?.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
         eq.brand?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.model?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.serialNumber?.toLowerCase().includes(this.searchQuery.toLowerCase())) : true;
      
      const matchCategory = this.filterCategory ? this.getCorrectCategory(eq) === this.filterCategory : true;
      const matchType = this.filterType ? eq.type === this.filterType : true;
      const matchSupplier = this.filterSupplier ? eq.supplier === this.filterSupplier : true;
      const matchDate = this.filterPurchaseDate ? (eq.purchaseDate && eq.purchaseDate.startsWith(this.filterPurchaseDate)) : true;
      const matchShelf = this.filterShelfId ? eq.shelfId === this.filterShelfId : true;
      const matchSelectedShelf = this.filterSelectedShelf ? eq.shelfId === this.filterSelectedShelf : true;
      const matchNature = this.natureFilter ? this.getNatureByType(eq.type) === this.natureFilter : true;
      
      return matchSearch && matchCategory && matchType && matchSupplier && matchDate && matchShelf && matchSelectedShelf && matchNature;
    });

    // 2. Group the filtered results
    const groupsMap = new Map<string, GroupedEquipment>();
    
    this.filteredEquipments.forEach(eq => {
      const key = `${eq.category}|${eq.type}|${eq.brand}`;
      if (!groupsMap.has(key)) {
        groupsMap.set(key, {
          groupId: key,
          name: eq.equipmentName || 'Unnamed',
          brand: eq.brand || 'No Brand',
          type: eq.type || 'unknown',
          category: this.getCorrectCategory(eq),
          totalQuantity: 0,
          items: [],
          expanded: false,
          commonSupplier: '',
          commonPurchaseDate: '',
          commonLocation: '',
          commonWarranty: '',
          commonModel: '',
          statusSummary: [],
          allSameAttributes: true
        });
      }
      
      const group = groupsMap.get(key)!;
      group.items.push(eq);
      group.totalQuantity += (eq.qte !== undefined ? eq.qte : 1);
    });

    // 3. Finalize aggregation for each group
    this.groupedEquipments = Array.from(groupsMap.values()).map(group => {
      // Sort items so items with quantity > 0 are first (prioritize active parts over ghost records)
      group.items.sort((a, b) => {
        const qteA = a.qte !== undefined ? a.qte : 1;
        const qteB = b.qte !== undefined ? b.qte : 1;
        return qteB - qteA;
      });
      const first = group.items[0];
      
      // Calculate common attributes
      const allNames = group.items.map(item => item.equipmentName || '—');
      const uniqueNames = [...new Set(allNames)];
      group.name = uniqueNames.length === 1 ? uniqueNames[0] : `${group.brand} ${group.type}`;

      group.commonSupplier = group.items.every(item => item.supplier === first.supplier) ? (first.supplier || '—') : '—';
      group.commonPurchaseDate = group.items.every(item => item.purchaseDate === first.purchaseDate) ? (first.purchaseDate || '') : '';
      // Calculate common location: prioritize the physical shelf of Available items
      const availableItems = group.items.filter(item => this.getEquipmentStatus(item).label === 'Available');
      if (availableItems.length > 0) {
        const firstAvail = availableItems[0];
        const allSameShelf = availableItems.every(item => item.shelfId === firstAvail.shelfId);
        group.commonLocation = allSameShelf ? this.getShelfLocation(firstAvail.shelfId, 'Available') : 'Mixed Shelves';
      } else {
        // No available items, show location of the first allocated/installed unit
        group.commonLocation = this.getShelfLocation(first.shelfId, first.status);
      }

      group.commonWarranty = group.items.every(item => item.warrantyExpiration === first.warrantyExpiration) ? (first.warrantyExpiration || '') : '';
      group.commonModel = group.items.every(item => item.model === first.model) ? (first.model || '—') : 'Mixed';

      // Advanced attribute check for Actions visibility
      group.allSameAttributes = group.items.every(item => 
        item.supplier === first.supplier &&
        item.purchaseDate === first.purchaseDate &&
        item.shelfId === first.shelfId &&
        item.warrantyExpiration === first.warrantyExpiration &&
        item.model === first.model &&
        item.purchasePrice === first.purchasePrice &&
        item.category === first.category &&
        JSON.stringify(item.specifications || {}) === JSON.stringify(first.specifications || {})
      );

      // Status Summary — capture all unique status results
      const statusCounts: Record<string, { label: string; count: number; cls: string }> = {};
      group.items.forEach(item => {
        const qte = item.qte !== undefined ? item.qte : 1;
        // Skip 0-quantity ghost records in the summary if the group has active items
        if (qte === 0 && group.totalQuantity > 0) return;

        const s = this.getEquipmentStatus(item);
        const label = s.label.toLowerCase();
        
        if (!statusCounts[label]) {
          statusCounts[label] = { 
            label: label, 
            count: 0, 
            cls: s.cls 
          };
          if (label === 'active') statusCounts[label].label = 'avail';
          if (label === 'expired') statusCounts[label].label = 'maint';
        }
        statusCounts[label].count += qte === 0 ? 1 : qte;
      });
      group.statusSummary = Object.values(statusCounts).sort((a,b) => b.count - a.count);

      return group;
    });

    this.currentPage = 1; 
  }

  toggleGroup(group: GroupedEquipment): void {
    group.expanded = !group.expanded;
  }

  clearShelfFilter(): void {
    this.equipmentService.setShelfFilter(null, null);
    this.filterShelfId = null;
    this.filterShelfNb = null;
    this.filterSelectedShelf = '';
    this.applyFilters();
  }

  deleteEquipment(id?: string, event?: Event): void {
    if (event) event.stopPropagation();
    if (!id) return;
    
    this.itemToDeleteId = id;
    this.groupToDeleteIds = null;
    this.isBulkDelete = false;
    this.deleteModalTitle = 'Delete Equipment';
    this.deleteModalMessage = 'Are you sure you want to delete this specific unit? This action cannot be undone.';
    this.showDeleteModal = true;
  }

  deleteGroup(group: GroupedEquipment, event: Event): void {
    event.stopPropagation();
    this.itemToDeleteId = null;
    this.groupToDeleteIds = group.items.map(item => item.id!);
    this.isBulkDelete = true;
    this.deleteModalTitle = 'Delete Group';
    this.deleteModalMessage = `Are you sure you want to delete all ${group.totalQuantity} items in this group? This action cannot be undone.`;
    this.showDeleteModal = true;
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.itemToDeleteId = null;
    this.groupToDeleteIds = null;
  }

  confirmDelete(): void {
    if (this.isBulkDelete && this.groupToDeleteIds) {
      this.equipmentService.deleteBulkEquipment(this.groupToDeleteIds).subscribe({
        next: () => {
          this.toastService.success(`Successfully deleted ${this.groupToDeleteIds?.length} items.`);
          this.loadEquipments();
          this.closeDeleteModal();
        },
        error: (err) => {
          console.error('Error deleting group', err);
          this.closeDeleteModal();
        }
      });
    } else if (this.itemToDeleteId) {
      this.equipmentService.deleteEquipment(this.itemToDeleteId).subscribe({
        next: () => {
          this.toastService.success(`Equipment deleted successfully.`);
          this.loadEquipments();
          this.closeDeleteModal();
        },
        error: (err) => {
          console.error('Error deleting equipment', err);
          this.closeDeleteModal();
        }
      });
    }
  }

  // ─── Group Edit ───────────────────────────────────
  openGroupEditModal(group: GroupedEquipment, event: Event): void {
    event.stopPropagation();
    this.editingGroup = group;
    this.bulkEditForm = {
      name: group.name,
      brand: group.brand
    };
    this.showGroupEditModal = true;
  }

  closeGroupEditModal(): void {
    this.showGroupEditModal = false;
    this.editingGroup = null;
  }

  saveGroupEdit(): void {
    if (!this.editingGroup) return;
    if (!this.bulkEditForm.name || !this.bulkEditForm.brand) {
      alert('Group name and Brand are required.');
      return;
    }

    this.isBulkSaving = true;
    const ids = this.editingGroup.items.map(item => item.id!);
    const newGroupName = this.bulkEditForm.name;
    const newBrand = this.bulkEditForm.brand;

    // Only send brand to backend — individual equipment names are intentionally NOT updated
    this.equipmentService.updateBulkBasicInfo(
      ids,
      null as any,
      newBrand,
      null as any
    ).subscribe({
      next: () => {
        this.toastService.success(`Group "${newGroupName}" updated successfully.`);
        this.isBulkSaving = false;
        if (this.editingGroup) {
          // Update group display name locally (does NOT cascade to individual equipment names)
          this.editingGroup.name = newGroupName;
          // Update brand on the group and all its items locally
          this.editingGroup.brand = newBrand;
          this.editingGroup.items.forEach(item => { item.brand = newBrand; });
          // Sync brand back to the raw equipments array
          this.editingGroup.items.forEach(item => {
            const eq = this.equipments.find(e => e.id === item.id);
            if (eq) eq.brand = newBrand;
          });
        }
        this.closeGroupEditModal();
      },
      error: (err: any) => {
        this.isBulkSaving = false;
        console.error('Error updating group', err);
        alert('Failed to update group. Please try again.');
      }
    });
  }

  get paginatedGroups(): GroupedEquipment[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return this.groupedEquipments.slice(startIndex, startIndex + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.groupedEquipments.length / this.itemsPerPage);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  get pages(): number[] {
    return Array(this.totalPages).fill(0).map((x, i) => i + 1);
  }

  // ─── QR Modal ───────────────────────────────────────
  openQrModal(eq: Equipment, event: Event): void {
    event.stopPropagation();
    this.qrModalEquipment = eq;
    this.showQrModal = true;
    this.qrModalDataUrl = '';
    const qrData = JSON.stringify({ id: eq.id, name: eq.equipmentName, serial: eq.serialNumber });
    QRCode.toDataURL(qrData, { width: 240, margin: 1, color: { dark: '#1e293b', light: '#ffffff' } })
      .then((url: string) => { this.qrModalDataUrl = url; })
      .catch((err: any) => console.error('QR generation failed', err));
  }

  closeQrModal(): void {
    this.showQrModal = false;
    this.qrModalEquipment = null;
    this.qrModalDataUrl = '';
  }

  downloadQrModal(): void {
    if (!this.qrModalDataUrl) return;
    const a = document.createElement('a');
    a.href = this.qrModalDataUrl;
    a.download = `QR_${this.qrModalEquipment?.equipmentName || 'equipment'}.png`;
    a.click();
  }

  printQrModal(): void {
    if (!this.qrModalDataUrl) return;
    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`
      <html><head><title>QR - ${this.qrModalEquipment?.equipmentName}</title>
      <style>body{font-family:Arial,sans-serif;display:flex;flex-direction:column;align-items:center;padding:30px;}
      h2{margin:10px 0 4px;font-size:16px;}p{margin:2px 0;font-size:12px;color:#64748b;}
      img{border:1px solid #e2e8f0;border-radius:8px;padding:8px;}</style></head>
      <body><img src="${this.qrModalDataUrl}" width="220" height="220"/>
      <h2>${this.qrModalEquipment?.equipmentName}</h2>
      <p>S/N: ${this.qrModalEquipment?.serialNumber || 'N/A'}</p>
      <p>ID: ${this.qrModalEquipment?.id}</p>
      </body></html>`);
    win.document.close();
    win.focus();
    win.print();
    win.close();
  }

  getTypeKey(type?: string): string {
    const t = type?.toLowerCase() || '';
    if (t.includes('laptop')) return 'laptop';
    if (t.includes('pc') || t.includes('computer') || t.includes('desktop')) return 'pc';
    if (t.includes('monitor') || t.includes('screen') || t.includes('display')) return 'monitor';
    if (t.includes('server')) return 'server';
    if (t.includes('print')) return 'printer';
    if (t.includes('scan')) return 'scanner';
    if (t.includes('project')) return 'projector';
    if (t.includes('rout') || t.includes('switch') || t.includes('hub')) return 'router';
    if (t.includes('ups') || t.includes('power')) return 'ups';
    if (t.includes('tab') || t.includes('ipad')) return 'tablet';
    if (t.includes('phone') || t.includes('mobile')) return 'phone';
    if (t.includes('key')) return 'keyboard';
    if (t.includes('mouse')) return 'mouse';
    if (t.includes('head') || t.includes('ear') || t.includes('audio')) return 'headset';
    if (t.includes('ram') || t.includes('memory') || t.includes('ddr')) return 'ram';
    if (t.includes('hard') || t.includes('hdd') || t.includes('ssd') || t.includes('drive') || t.includes('storage')) return 'hdd';
    if (t.includes('cable') || t.includes('wire') || t.includes('cord')) return 'cables';
    
    return 'default';
  }

  isWarrantyExpired(date?: string): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }

  getEquipmentStatus(eq: any): { label: string; cls: string } {
    if (eq.qte === 0) return { label: 'Out of Stock', cls: 'unassigned' };
    if (eq.status) {
      const s = eq.status.toLowerCase();
      if (s === 'broken') return { label: 'Broken', cls: 'expired' }; 
      if (s === 'maintenance') return { label: 'Maintenance', cls: 'maintenance' };
      if (s === 'out of stock') return { label: 'Out of Stock', cls: 'unassigned' };
      if (s === 'in use') return { label: 'In Use', cls: 'in-use' };
      if (s === 'available' || s === 'in stock') return { label: 'Available', cls: 'active' };
      if (s === 'allocated') return { label: 'Allocated', cls: 'allocated' };
      if (s === 'installed' || s === 'assigned') return { label: 'Installed', cls: 'assigned' };
      
      // Fallback for any other status string
      return { label: eq.status, cls: 'unassigned' };
    }

    if (!eq.shelfId) return { label: 'Unassigned', cls: 'unassigned' };
    if (eq.warrantyExpiration && new Date(eq.warrantyExpiration) < new Date()) {
      return { label: 'Expired', cls: 'expired' };
    }
    return { label: 'Available', cls: 'active' };
  }

  getShelfLocation(shelfId?: string, status?: string): string {
    if (!shelfId || shelfId === '') {
      if (status === 'Allocated') return 'Allocated (Not on Shelf)';
      if (status === 'Assigned' || status === 'Installed') return 'Installed (Not on Shelf)';
      return 'Unassigned';
    }
    if (shelfId === 'MAINTENANCE_AREA') return 'Maintenance Area';
    if (shelfId === 'SCRAP_YARD') return 'Scrap Yard';
    if (shelfId === 'OUT_OF_STOCK') return 'Out of Stock';
    
    const s = this.shelves.find(x => x.id === shelfId);
    if (s) return `Shelf ${s.nb}`;

    // If it's a long technical ID, don't show it to the user
    if (shelfId && shelfId.length > 10) {
      return 'Unknown Shelf';
    }
    
    return shelfId;
  }

  downloadDocument(fileData?: string, fileName?: string): void {
    if (!fileData || !fileName) return;
    const a = document.createElement('a');
    a.href = fileData;
    a.download = fileName;
    a.click();
  }

  getCorrectCategory(eq: Equipment): string {
    if (eq.category && eq.category !== 'Asset' && eq.category !== 'Consumable') {
      return eq.category;
    }

    // Try to find the correct category from categoriesList based on type
    if (this.categoriesList.length > 0 && eq.type) {
      const typeLower = eq.type.toLowerCase();
      const found = this.categoriesList.find(c =>
        c.types?.some(t => t.name.toLowerCase() === typeLower)
      );
      if (found) return found.name || 'Asset';
    }

    return eq.category || 'Asset';
  }

  getNatureByType(typeName: string | undefined): string {
    if (!typeName) return 'Asset';
    const typeLower = typeName.toLowerCase();
    
    for (const cat of this.categoriesList) {
      const foundType = cat.types?.find(t => t.name.toLowerCase() === typeLower);
      if (foundType && foundType.nature) {
        return foundType.nature;
      }
    }
    
    // Fallback logic for common types if nature is not set yet
    if (['laptop', 'computer', 'server', 'router', 'printer', 'monitor', 'ups'].some(t => typeLower.includes(t))) {
      return 'Asset';
    }
    if (['ram', 'hdd', 'ssd', 'cable', 'battery', 'mouse', 'keyboard'].some(t => typeLower.includes(t))) {
      return 'Consumable';
    }
    
    return 'Asset';
  }
}
