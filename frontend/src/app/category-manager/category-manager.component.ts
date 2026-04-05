import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { EquipmentCategory } from './category.model';
import { CategoryService } from './category.service';

@Component({
  selector: 'app-category-manager',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './category-manager.component.html',
  styleUrl: './category-manager.component.css'
})
export class CategoryManagerComponent implements OnInit {
  categories: EquipmentCategory[] = [];
  isLoading = true;
  newTypeName = '';
  activeCategoryId: string | null = null;
  expandedCategories: { [key: string]: boolean } = {};

  // Duplicate checks
  isCategoryNameDuplicate = false;
  isTypeNameDuplicate = false;

  // For Adding/Renaming categories
  isAddingCategory = false;
  newCategoryName = '';
  editingCategoryId: string | null = null;
  editingCategoryName = '';
  errorMessage: string | null = null;

  constructor(private categoryService: CategoryService) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading = true;
    this.categoryService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;
        
        // Auto-expand first category if there are categories and none expanded yet
        if (this.categories.length > 0 && Object.keys(this.expandedCategories).length === 0) {
          if (this.categories[0].id) {
            this.expandedCategories[this.categories[0].id] = true;
          }
        }
        
        this.isLoading = false;
      },
      error: (error) => {
        console.error('Error fetching categories:', error);
        this.isLoading = false;
      }
    });
  }

  toggleCategory(id: string | undefined): void {
    if (!id) return;
    this.expandedCategories[id] = !this.expandedCategories[id];
  }

  // --- Category Actions ---
  startAddCategory(): void {
    this.isAddingCategory = true;
    this.newCategoryName = '';
    this.errorMessage = null;
  }

  cancelAddCategory(): void {
    this.isAddingCategory = false;
    this.newCategoryName = '';
  }

  saveNewCategory(): void {
    if (!this.newCategoryName.trim()) return;
    
    // Local uniqueness check
    const exists = this.categories.some(c => c.name?.toLowerCase() === this.newCategoryName.trim().toLowerCase());
    if (exists) {
      this.errorMessage = `Category '${this.newCategoryName.trim()}' already exists.`;
      setTimeout(() => this.errorMessage = null, 5000);
      return;
    }

    const newCat: EquipmentCategory = {
      name: this.newCategoryName.trim(),
      types: []
    };
    this.categoryService.createCategory(newCat).subscribe({
      next: (data) => {
        this.categories.push(data);
        this.isAddingCategory = false;
        this.newCategoryName = '';
        if (data.id) this.expandedCategories[data.id] = true;
        this.errorMessage = null;
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = typeof err.error === 'string' ? err.error : 'Duplicate category name.';
          setTimeout(() => this.errorMessage = null, 5000);
        } else {
          console.error('Error creating category', err);
        }
      }
    });
  }

  startRename(cat: EquipmentCategory): void {
    if (!cat.id || !cat.name) return;
    this.editingCategoryId = cat.id || null;
    this.editingCategoryName = cat.name;
    this.errorMessage = null;
  }

  cancelRename(): void {
    this.editingCategoryId = null;
    this.editingCategoryName = '';
    this.isCategoryNameDuplicate = false;
  }

  checkCategoryDuplicate(name: string, id?: string): void {
    const cleanName = name.trim().toLowerCase();
    if (!cleanName) {
      this.isCategoryNameDuplicate = false;
      return;
    }
    this.isCategoryNameDuplicate = this.categories.some(c => 
      c.id !== id && c.name?.toLowerCase() === cleanName
    );
  }

  saveRename(cat: EquipmentCategory): void {
    if (!this.editingCategoryName.trim() || !cat.id) return;
    
    // Local uniqueness check
    const exists = this.categories.some(c => 
      c.id !== cat.id && c.name?.toLowerCase() === this.editingCategoryName.trim().toLowerCase()
    );
    if (exists) {
      this.errorMessage = `Another category already has the name '${this.editingCategoryName.trim()}'.`;
      setTimeout(() => this.errorMessage = null, 5000);
      return;
    }

    const categoryId = cat.id; // Local variable for type safety
    const updated = { ...cat, name: this.editingCategoryName.trim() };
    this.categoryService.updateCategory(categoryId, updated).subscribe({
      next: (res) => {
        const idx = this.categories.findIndex(c => c.id === categoryId);
        if (idx !== -1) this.categories[idx] = res;
        this.editingCategoryId = null;
        this.errorMessage = null;
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = typeof err.error === 'string' ? err.error : 'Conflict detected.';
          setTimeout(() => this.errorMessage = null, 5000);
        } else {
          console.error('Error renaming category', err);
        }
      }
    });
  }

  deleteCategory(id: string | undefined, event: Event): void {
    event.stopPropagation();
    if (!id) return;
    
    const categoryId = id; // Local variable for type safety
    if (confirm('Are you sure you want to delete this category?')) {
      this.categoryService.deleteCategory(categoryId).subscribe({
        next: () => {
          this.categories = this.categories.filter(c => c.id !== categoryId);
          delete this.expandedCategories[categoryId];
          this.errorMessage = null;
        },
        error: (err) => {
          if (err.status === 409) {
             this.errorMessage = typeof err.error === 'string' ? err.error : 'Cannot delete category: it is currently in use.';
             // Auto-hide error after 8s
             setTimeout(() => this.errorMessage = null, 8000);
          } else {
            console.error('Error deleting category', err);
          }
        }
      });
    }
  }

  activateTypeInput(categoryId: string | undefined): void {
    if (!categoryId) return;
    this.activeCategoryId = categoryId;
    this.newTypeName = '';
    this.isTypeNameDuplicate = false;
    // Ensure the category is expanded when adding a type
    this.expandedCategories[categoryId] = true;
  }

  cancelTypeInput(): void {
    this.activeCategoryId = null;
    this.newTypeName = '';
    this.isTypeNameDuplicate = false;
  }

  checkTypeDuplicate(catId: string, type: string): void {
    const cleanType = type.trim().toLowerCase();
    const category = this.categories.find(c => c.id === catId);
    if (!category || !category.types || !cleanType) {
      this.isTypeNameDuplicate = false;
      return;
    }
    this.isTypeNameDuplicate = category.types.some(t => t.toLowerCase() === cleanType);
  }

  addTypeToCategory(categoryId: string | undefined): void {
    if (!categoryId || !this.newTypeName.trim()) return;
    
    const id = categoryId; // Local variable for clarity
    const category = this.categories.find(c => c.id === id);
    if (category && category.types) {
      const exists = category.types.some(t => t.toLowerCase() === this.newTypeName.trim().toLowerCase());
      if (exists) {
        this.errorMessage = `Type '${this.newTypeName.trim()}' already exists in this category.`;
        setTimeout(() => this.errorMessage = null, 5000);
        return;
      }
    }

    this.categoryService.addTypeToCategory(id, this.newTypeName.trim()).subscribe({
      next: (updatedCategory) => {
        // Update local list
        const index = this.categories.findIndex(c => c.id === id);
        if (index !== -1) {
          this.categories[index] = updatedCategory;
        }
        this.newTypeName = '';
        this.activeCategoryId = null; // Close input box
        this.errorMessage = null;
      },
      error: (err) => {
        if (err.status === 409) {
          this.errorMessage = typeof err.error === 'string' ? err.error : 'Duplicate type detected.';
          setTimeout(() => this.errorMessage = null, 5000);
        } else {
          console.error('Error adding type:', err);
        }
      }
    });
  }

  removeType(categoryId: string | undefined, type: string, event: Event): void {
    event.stopPropagation();
    if (!categoryId) return;
    
    const id = categoryId; // Local variable for clarity
    if (confirm(`Are you sure you want to remove '${type}'?`)) {
      this.categoryService.removeTypeFromCategory(id, type).subscribe({
        next: (updatedCategory) => {
          const index = this.categories.findIndex(c => c.id === id);
          if (index !== -1) {
            this.categories[index] = updatedCategory;
          }
        },
        error: (error) => {
          console.error('Error removing type:', error);
        }
      });
    }
  }

  // Pre-seed default categories if empty, useful for initial testing
  
}
