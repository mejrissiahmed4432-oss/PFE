import { Component, OnInit, Input, Output, EventEmitter, SimpleChanges, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SupplierService } from '../supplier.service';
import { Supplier } from '../supplier.model';

@Component({
  selector: 'app-supplier-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.css'
})
export class SupplierListComponent implements OnInit, OnChanges {
  @Input() refreshTrigger: number = 0;
  @Output() editEvent = new EventEmitter<Supplier>();
  @Output() viewEvent = new EventEmitter<Supplier>();

  suppliers: Supplier[] = [];
  filteredSuppliers: Supplier[] = [];
  
  viewMode: 'table' | 'card' = 'table';
  searchQuery: string = '';
  showFilters: boolean = false;

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 6;
  
  // Filters
  filterCategory: string = '';

  constructor(private supplierService: SupplierService) {}

  ngOnInit(): void {
    this.loadSuppliers();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['refreshTrigger'] && !changes['refreshTrigger'].firstChange) {
      this.loadSuppliers();
    }
  }

  loadSuppliers(): void {
    this.supplierService.getAllSuppliers().subscribe({
      next: (data) => {
        this.suppliers = data;
        this.applyFilters();
      },
      error: (err) => console.error('Error fetching suppliers', err)
    });
  }

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  applyFilters(): void {
    this.filteredSuppliers = this.suppliers.filter(s => {
      const matchSearch = this.searchQuery ? 
        (s.companyName?.toLowerCase().includes(this.searchQuery.toLowerCase()) || 
         s.contactPerson?.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
         s.email?.toLowerCase().includes(this.searchQuery.toLowerCase())) : true;
      
      const matchCategory = this.filterCategory ? s.category === this.filterCategory : true;
      
      return matchSearch && matchCategory;
    });
    this.currentPage = 1;
  }

  updateRating(supplier: Supplier, newRating: number): void {
    if (!supplier.id) return;
    
    // Optimistic update
    const oldRating = supplier.rating;
    supplier.rating = newRating;

    this.supplierService.updateSupplier(supplier.id, supplier).subscribe({
      next: () => {
        // Success
      },
      error: (err) => {
        console.error('Error updating rating', err);
        supplier.rating = oldRating; // Rollback
      }
    });
  }

  deleteSupplier(id?: string): void {
    if (id && confirm('Are you sure you want to delete this supplier?')) {
      this.supplierService.deleteSupplier(id).subscribe({
        next: () => this.loadSuppliers(),
        error: (err) => console.error('Error deleting supplier', err)
      });
    }
  }

  get paginatedSuppliers(): Supplier[] {
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    return this.filteredSuppliers.slice(startIndex, startIndex + this.itemsPerPage);
  }

  get totalPages(): number {
    return Math.ceil(this.filteredSuppliers.length / this.itemsPerPage);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
    }
  }

  get pages(): number[] {
    return Array(this.totalPages).fill(0).map((x, i) => i + 1);
  }
}
