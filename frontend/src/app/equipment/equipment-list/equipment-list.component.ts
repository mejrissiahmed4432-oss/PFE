import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../equipment.service';
import { Equipment } from '../equipment.model';

@Component({
  selector: 'app-equipment-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-list.component.html',
  styleUrl: './equipment-list.component.css'
})
export class EquipmentListComponent implements OnInit, OnChanges {
  @Input() refreshTrigger: number = 0;
  @Output() editEvent = new EventEmitter<Equipment>();
  @Output() viewEvent = new EventEmitter<Equipment>();

  equipments: Equipment[] = [];
  filteredEquipments: Equipment[] = [];
  
  viewMode: 'table' | 'card' = 'table';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 6;
  
  // Filters
  filterCategory: string = '';
  filterBrand: string = '';
  filterSupplier: string = '';
  filterLocation: string = '';
  filterPurchaseDate: string = '';

  constructor(private equipmentService: EquipmentService) {}

  ngOnInit(): void {
    this.loadEquipments();
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

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  applyFilters(): void {
    this.filteredEquipments = this.equipments.filter(eq => {
      const matchSearch = this.searchQuery ? 
        (eq.equipmentName?.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
         eq.brand?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.model?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         eq.serialNumber?.toLowerCase().includes(this.searchQuery.toLowerCase())) : true;
      
      const matchCategory = this.filterCategory ? eq.category === this.filterCategory : true;
      const matchBrand = this.filterBrand ? eq.brand === this.filterBrand : true;
      const matchSupplier = this.filterSupplier ? eq.supplier === this.filterSupplier : true;
      const matchLocation = this.filterLocation ? eq.location === this.filterLocation : true;
      const matchDate = this.filterPurchaseDate ? (eq.purchaseDate && eq.purchaseDate.startsWith(this.filterPurchaseDate)) : true;
      
      return matchSearch && matchCategory && matchBrand && matchSupplier && matchLocation && matchDate;
    });
    this.currentPage = 1; // Reset pagination on filter
  }

  deleteEquipment(id?: string): void {
    if (id && confirm('Are you sure you want to delete this equipment?')) {
      this.equipmentService.deleteEquipment(id).subscribe({
        next: () => this.loadEquipments(),
        error: (err) => console.error('Error deleting equipment', err)
      });
    }
  }

  get paginatedEquipments(): Equipment[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredEquipments.slice(startIndex, startIndex + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredEquipments.length / this.itemsPerPage);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  // Get pages array for pagination UI
  get pages(): number[] {
    return Array(this.totalPages).fill(0).map((x, i) => i + 1);
  }
}
