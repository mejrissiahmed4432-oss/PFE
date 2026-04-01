import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment.service';
import { Equipment } from '../equipment.model';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ShelfService } from '../../shelf/shelf.service';
import { Shelf } from '../../shelf/shelf.model';
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
  
  viewMode: 'table' | 'card' = 'table';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 10; // Increased for groups
  
  // Filters
  filterCategory: string = '';
  filterBrand: string = '';
  filterSupplier: string = '';
  filterPurchaseDate: string = '';
  filterShelfId: string | null = null;
  filterShelfNb: string | null = null;
  filterSelectedShelf: string = '';

  // QR Modal state
  qrModalEquipment: Equipment | null = null;
  qrModalDataUrl: string = '';
  showQrModal: boolean = false;

  // Group Edit Modal
  showGroupEditModal: boolean = false;
  editingGroup: GroupedEquipment | null = null;
  bulkEditForm = { name: '', brand: '', model: '' };
  isBulkSaving: boolean = false;

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
    private shelfService: ShelfService
  ) {}

  ngOnInit(): void {
    const shelfFilter = this.equipmentService.getShelfFilter();
    this.filterShelfId = shelfFilter.id;
    this.filterShelfNb = shelfFilter.nb;

    this.loadEquipments();
    this.loadSuppliers();
    this.loadShelves();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['refreshTrigger'] && !changes['refreshTrigger'].firstChange) {
      this.loadEquipments();
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

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  applyFilters(): void {
    // 1. First filter the raw list
    this.filteredEquipments = this.equipments.filter(eq => {
      const matchSearch = this.searchQuery ? 
        (eq.equipmentName?.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
         eq.brand?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.model?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.serialNumber?.toLowerCase().includes(this.searchQuery.toLowerCase())) : true;
      
      const matchCategory = this.filterCategory ? eq.category === this.filterCategory : true;
      const matchBrand = this.filterBrand ? eq.brand === this.filterBrand : true;
      const matchSupplier = this.filterSupplier ? eq.supplier === this.filterSupplier : true;
      const matchDate = this.filterPurchaseDate ? (eq.purchaseDate && eq.purchaseDate.startsWith(this.filterPurchaseDate)) : true;
      const matchShelf = this.filterShelfId ? eq.shelfId === this.filterShelfId : true;
      const matchSelectedShelf = this.filterSelectedShelf ? eq.shelfId === this.filterSelectedShelf : true;
      
      return matchSearch && matchCategory && matchBrand && matchSupplier && matchDate && matchShelf && matchSelectedShelf;
    });

    // 2. Group the filtered results
    const groupsMap = new Map<string, GroupedEquipment>();
    
    this.filteredEquipments.forEach(eq => {
      const key = `${eq.brand}|${eq.type}|${eq.equipmentName}`;
      if (!groupsMap.has(key)) {
        groupsMap.set(key, {
          groupId: key,
          name: eq.equipmentName || 'Unnamed',
          brand: eq.brand || 'No Brand',
          type: eq.type || 'unknown',
          category: eq.category || 'Asset',
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
      group.totalQuantity++;
    });

    // 3. Finalize aggregation for each group
    this.groupedEquipments = Array.from(groupsMap.values()).map(group => {
      const first = group.items[0];
      
      // Calculate common attributes
      group.commonSupplier = group.items.every(item => item.supplier === first.supplier) ? (first.supplier || '—') : '—';
      group.commonPurchaseDate = group.items.every(item => item.purchaseDate === first.purchaseDate) ? (first.purchaseDate || '') : '';
      group.commonLocation = group.items.every(item => item.shelfId === first.shelfId) ? this.getShelfLocation(first.shelfId) : 'Mixed';
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
        item.cpu === first.cpu &&
        item.ram === first.ram &&
        item.storage === first.storage
      );

      // Status Summary — capture all unique status results
      const statusCounts: Record<string, { label: string; count: number; cls: string }> = {};
      group.items.forEach(item => {
        const s = this.getEquipmentStatus(item);
        const label = s.label.toLowerCase(); // Consistent keying
        
        // Keying based on original derived label to ensure distinct chips for each
        if (!statusCounts[label]) {
          statusCounts[label] = { 
            label: label, 
            count: 0, 
            cls: s.cls 
          };
          if (label === 'active') statusCounts[label].label = 'avail';
          if (label === 'expired') statusCounts[label].label = 'maint';
        }
        statusCounts[label].count++;
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
    if (id && confirm('Are you sure you want to delete this specific unit?')) {
      this.equipmentService.deleteEquipment(id).subscribe({
        next: () => this.loadEquipments(),
        error: (err) => console.error('Error deleting equipment', err)
      });
    }
  }

  deleteGroup(group: GroupedEquipment, event: Event): void {
    event.stopPropagation();
    if (confirm(`Are you sure you want to delete all ${group.totalQuantity} items in this group? This action cannot be undone.`)) {
      const ids = group.items.map(item => item.id!);
      this.equipmentService.deleteBulkEquipment(ids).subscribe({
        next: () => {
          this.loadEquipments();
        },
        error: (err) => console.error('Error deleting group', err)
      });
    }
  }

  // ─── Group Edit ───────────────────────────────────
  openGroupEditModal(group: GroupedEquipment, event: Event): void {
    event.stopPropagation();
    this.editingGroup = group;
    this.bulkEditForm = {
      name: group.name,
      brand: group.brand,
      model: group.commonModel === 'Mixed' ? '' : group.commonModel
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
      alert('Name and Brand are required.');
      return;
    }

    this.isBulkSaving = true;
    const ids = this.editingGroup.items.map(item => item.id!);
    
    this.equipmentService.updateBulkBasicInfo(
      ids, 
      this.bulkEditForm.name, 
      this.bulkEditForm.brand, 
      this.bulkEditForm.model
    ).subscribe({
      next: () => {
        this.isBulkSaving = false;
        this.closeGroupEditModal();
        this.loadEquipments();
      },
      error: (err) => {
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
    const map: Record<string, string> = {
      'laptop': 'laptop', 'pc': 'pc', 'monitor': 'monitor', 'server': 'server',
      'printer': 'printer', 'scanner': 'scanner', 'projector': 'projector',
      'router': 'router', 'switch': 'router', 'ups': 'ups',
      'tablet': 'tablet', 'phone': 'phone',
      'keyboard': 'keyboard', 'mouse': 'mouse', 'headset': 'headset',
      'ram': 'ram', 'hard drive': 'hdd', 'ssd': 'hdd', 'cables': 'cables'
    };
    return map[type?.toLowerCase() || ''] || 'default';
  }

  isWarrantyExpired(date?: string): boolean {
    if (!date) return false;
    return new Date(date) < new Date();
  }

  getEquipmentStatus(eq: any): { label: string; cls: string } {
    if (eq.status) {
      const s = eq.status.toLowerCase();
      if (s === 'broken') return { label: 'Broken', cls: 'expired' }; 
      if (s === 'maintenance') return { label: 'Maintenance', cls: 'maintenance' };
      if (s === 'out of stock') return { label: 'Out of Stock', cls: 'unassigned' };
      if (s === 'in stock') return { label: 'In Stock', cls: 'active' };
      
      // Fallback for any other status string
      return { label: eq.status, cls: 'unassigned' };
    }

    if (!eq.shelfId) return { label: 'Unassigned', cls: 'unassigned' };
    if (eq.warrantyExpiration && new Date(eq.warrantyExpiration) < new Date()) {
      return { label: 'Expired', cls: 'expired' };
    }
    return { label: 'In Stock', cls: 'active' };
  }

  getShelfLocation(shelfId?: string): string {
    if (!shelfId || shelfId === '') return 'Unassigned';
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
}
