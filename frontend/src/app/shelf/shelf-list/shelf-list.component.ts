import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ShelfService } from '../shelf.service';
import { Shelf } from '../shelf.model';
import { ShelfFormComponent } from '../shelf-form/shelf-form.component';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../../equipment/equipment.service';
import { Equipment } from '../../equipment/equipment.model';

@Component({
  selector: 'app-shelf-list',
  standalone: true,
  imports: [CommonModule, ShelfFormComponent, FormsModule],
  templateUrl: './shelf-list.component.html',
  styleUrl: './shelf-list.component.css'
})
export class ShelfListComponent implements OnInit {
  shelves: Shelf[] = [];
  filteredShelves: Shelf[] = [];
  paginatedShelves: Shelf[] = [];
  
  // View controls
  viewMode: 'table' | 'card' = 'table';
  showFilters: boolean = false;
  
  // Search & Filter fields
  searchQuery: string = '';
  filterStatus: string = '';
  filterType: string = '';
  filterNb: string = '';

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 0;
  pages: number[] = [];

  showForm: boolean = false;
  selectedShelf: Shelf | null = null;
  
  @Output() navigate = new EventEmitter<string>();

  constructor(
    private shelfService: ShelfService,
    private equipmentService: EquipmentService
  ) {}

  ngOnInit(): void {
    this.loadShelves();
  }

  loadShelves(): void {
    this.shelfService.getAllShelves().subscribe({
      next: (data) => {
        this.shelves = data;
        this.applyFilters();
      },
      error: (error) => console.error('Error fetching shelves', error)
    });
  }

  toggleFilters(): void {
    this.showFilters = !this.showFilters;
  }

  toggleViewMode(mode: 'table' | 'card'): void {
    this.viewMode = mode;
  }

  applyFilters(): void {
    this.filteredShelves = this.shelves.filter(s => {
      const searchLower = this.searchQuery.toLowerCase();
      const matchSearch = !this.searchQuery || 
        (s.equipmentType && s.equipmentType.toLowerCase().includes(searchLower)) ||
        (s.nb && s.nb.toString().includes(this.searchQuery));
      
      const matchStatus = !this.filterStatus || (s.status && s.status === this.filterStatus);
      const matchType = !this.filterType || (s.equipmentType && s.equipmentType.toLowerCase().includes(this.filterType.toLowerCase()));
      const matchNb = !this.filterNb || (s.nb && s.nb.toString().includes(this.filterNb));
      
      return matchSearch && matchStatus && matchType && matchNb;
    });
    
    this.currentPage = 1;
    this.updatePagination();
  }

  updatePagination(): void {
    this.totalPages = Math.ceil(this.filteredShelves.length / this.itemsPerPage);
    this.pages = Array.from({ length: this.totalPages }, (_, i) => i + 1);
    
    const startIndex = (this.currentPage - 1) * this.itemsPerPage;
    const endIndex = startIndex + this.itemsPerPage;
    this.paginatedShelves = this.filteredShelves.slice(startIndex, endIndex);
  }

  changePage(page: number): void {
    if (page >= 1 && page <= this.totalPages) {
      this.currentPage = page;
      this.updatePagination();
    }
  }

  openAddForm(): void {
    this.selectedShelf = null;
    this.showForm = true;
  }

  openEditForm(shelf: Shelf): void {
    this.selectedShelf = { ...shelf };
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.selectedShelf = null;
    this.loadShelves();
  }

  deleteShelf(id: string | undefined): void {
    if (id && confirm('Are you sure you want to delete this shelf?')) {
      this.shelfService.deleteShelf(id).subscribe({
        next: () => this.loadShelves(),
        error: (error) => console.error('Error deleting shelf', error)
      });
    }
  }

  viewEquipment(shelf: Shelf): void {
    this.equipmentService.setShelfFilter(shelf.id || null, shelf.nb?.toString() || null);
    this.navigate.emit('equipment');
  }
}
