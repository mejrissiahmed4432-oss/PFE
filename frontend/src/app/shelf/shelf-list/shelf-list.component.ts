import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ShelfService } from '../shelf.service';
import { Shelf } from '../shelf.model';
import { ShelfFormComponent } from '../shelf-form/shelf-form.component';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../../equipment/equipment.service';
import { CategoryService } from '../../category-manager/category.service';
import { EquipmentCategory, CategoryType } from '../../category-manager/category.model';

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

  // Dynamic Filters
  categories: EquipmentCategory[] = [];
  selectedCategoryId: string = '';
  availableTypes: CategoryType[] = [];

  // Pagination
  currentPage: number = 1;
  itemsPerPage: number = 6;
  totalPages: number = 0;
  pages: number[] = [];

  showForm: boolean = false;
  selectedShelf: Shelf | null = null;

  // Delete Confirmation Modal
  showDeleteModal: boolean = false;
  shelfToDeleteId: string | null = null;
  errorMessage: string | null = null;
  
  @Output() navigate = new EventEmitter<string>();

  constructor(
    private shelfService: ShelfService,
    private equipmentService: EquipmentService,
    private categoryService: CategoryService
  ) {}

  ngOnInit(): void {
    this.loadShelves();
    this.loadCategories();
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => this.categories = data,
      error: (error) => console.error('Error fetching categories', error)
    });
  }

  onCategoryChange(): void {
    this.filterType = ''; // Reset type filter when category changes
    const category = this.categories.find(c => c.id === this.selectedCategoryId);
    this.availableTypes = category?.types || [];
    this.applyFilters();
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
    const selectedCategory = this.categories.find(c => c.id === this.selectedCategoryId);
    const categoryTypeNames = selectedCategory?.types?.map(t => t.name.toLowerCase()) || [];

    this.filteredShelves = this.shelves.filter(s => {
      const searchLower = this.searchQuery.toLowerCase();
      const matchSearch = !this.searchQuery || 
        (s.equipmentType && s.equipmentType.toLowerCase().includes(searchLower)) ||
        (s.nb && s.nb.toString().includes(this.searchQuery));
      
      const matchStatus = !this.filterStatus || (s.status && s.status === this.filterStatus);
      
      // If a specific type is selected, match it exactly
      const sTypeLower = s.equipmentType?.toLowerCase() || '';
      const matchType = !this.filterType || sTypeLower === this.filterType.toLowerCase();
      
      // If a category is selected but no type, match if the shelf's type belongs to that category
      const matchCategory = !this.selectedCategoryId || this.filterType || categoryTypeNames.includes(sTypeLower);
      
      const matchNb = !this.filterNb || (s.nb && s.nb.toString().includes(this.filterNb));
      
      return matchSearch && matchStatus && matchType && matchCategory && matchNb;
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
    if (!id) return;
    this.shelfToDeleteId = id;
    this.showDeleteModal = true;
  }

  confirmDelete(): void {
    if (this.shelfToDeleteId) {
      this.shelfService.deleteShelf(this.shelfToDeleteId).subscribe({
        next: () => {
          this.loadShelves();
          this.closeDeleteModal();
        },
        error: (err) => {
          if (err.status === 409) {
            this.errorMessage = typeof err.error === 'string' ? err.error : 'Cannot delete shelf: it is currently in use.';
          } else {
            console.error('Error deleting shelf', err);
            this.closeDeleteModal();
          }
        }
      });
    }
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.shelfToDeleteId = null;
    this.errorMessage = null;
  }

  viewEquipment(shelf: Shelf): void {
    this.equipmentService.setShelfFilter(shelf.id || null, shelf.nb?.toString() || null);
    this.navigate.emit('equipment');
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
}
