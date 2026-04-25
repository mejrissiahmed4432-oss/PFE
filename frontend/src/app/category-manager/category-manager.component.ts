import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { CategoryType, EquipmentCategory } from './category.model';
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
  expandedCategories: { [key: string]: boolean } = {};

  // Duplicate checks
  isCategoryNameDuplicate = false;
  errorMessage: string | null = null;

  // For Adding/Renaming categories
  isAddingCategory = false;
  newCategoryName = '';
  editingCategoryId: string | null = null;
  editingCategoryName = '';

  // Delete Confirmation Modal
  showDeleteModal = false;
  deleteModalTitle = '';
  deleteModalMessage = '';
  deleteActionType: 'category' | 'type' | null = null;
  itemToDeleteId: string | null = null;
  typeToDeleteName: string | null = null;

  // === Type Management Modal ===
  showTypeModal = false;
  typeModalMode: 'add' | 'edit' = 'add';
  typeModalCategoryId: string | null = null;
  typeModalForm: CategoryType = { name: '', requiresQrCode: false, specificationFields: [] };
  typeModalOriginalName = '';          // used when editing
  typeModalNameError: string | null = null;
  typeModalQrError: string | null = null;
  typeModalHasEquipment = false;       // whether equipment already uses this type
  newSpecField = '';
  showSpecFields = false;

  constructor(private categoryService: CategoryService) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.isLoading = true;
    this.categoryService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;
        this.isLoading = false;
      },
      error: () => { this.isLoading = false; }
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
    this.isCategoryNameDuplicate = false;
  }

  saveNewCategory(): void {
    if (!this.newCategoryName.trim() || this.isCategoryNameDuplicate) return;
    const newCat: EquipmentCategory = { name: this.newCategoryName.trim(), types: [] };
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
        }
      }
    });
  }

  startRename(cat: EquipmentCategory): void {
    if (!cat.id || !cat.name) return;
    this.editingCategoryId = cat.id;
    this.editingCategoryName = cat.name;
    this.isCategoryNameDuplicate = false;
    this.errorMessage = null;
  }

  cancelRename(): void {
    this.editingCategoryId = null;
    this.editingCategoryName = '';
    this.isCategoryNameDuplicate = false;
  }

  checkCategoryDuplicate(name: string, id?: string): void {
    const cleanName = name.trim().toLowerCase();
    if (!cleanName) { this.isCategoryNameDuplicate = false; return; }
    this.isCategoryNameDuplicate = this.categories.some(c =>
      c.id !== id && c.name?.toLowerCase() === cleanName
    );
  }

  saveRename(cat: EquipmentCategory): void {
    if (!this.editingCategoryName.trim() || !cat.id || this.isCategoryNameDuplicate) return;
    const categoryId = cat.id;
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
        }
      }
    });
  }

  deleteCategory(id: string | undefined, event: Event): void {
    event.stopPropagation();
    if (!id) return;
    this.deleteActionType = 'category';
    this.itemToDeleteId = id;
    this.deleteModalTitle = 'Delete Category';
    this.deleteModalMessage = 'Are you sure you want to delete this category? This action cannot be undone.';
    this.errorMessage = null;
    this.showDeleteModal = true;
  }

  removeType(categoryId: string | undefined, type: CategoryType, event: Event): void {
    event.stopPropagation();
    if (!categoryId) return;
    this.deleteActionType = 'type';
    this.itemToDeleteId = categoryId;
    this.typeToDeleteName = type.name;
    this.deleteModalTitle = 'Remove Type';
    this.deleteModalMessage = `Are you sure you want to remove '${type.name}'? This action cannot be undone.`;
    this.errorMessage = null;
    this.showDeleteModal = true;
  }

  confirmDelete(): void {
    if (this.deleteActionType === 'category' && this.itemToDeleteId) {
      const categoryId = this.itemToDeleteId;
      this.categoryService.deleteCategory(categoryId).subscribe({
        next: () => {
          this.categories = this.categories.filter(c => c.id !== categoryId);
          delete this.expandedCategories[categoryId];
          this.closeDeleteModal();
        },
        error: (err) => {
          if (err.status === 409) {
            this.errorMessage = typeof err.error === 'string' ? err.error : 'Cannot delete: category is in use.';
          } else { this.closeDeleteModal(); }
        }
      });
    } else if (this.deleteActionType === 'type' && this.itemToDeleteId && this.typeToDeleteName) {
      const id = this.itemToDeleteId;
      const typeName = this.typeToDeleteName;
      this.categoryService.removeTypeFromCategory(id, typeName).subscribe({
        next: (updatedCategory) => {
          const index = this.categories.findIndex(c => c.id === id);
          if (index !== -1) this.categories[index] = updatedCategory;
          this.closeDeleteModal();
        },
        error: (err) => {
          if (err.status === 409) {
            this.errorMessage = typeof err.error === 'string' ? err.error : 'Cannot remove type: it is currently in use.';
          } else { this.closeDeleteModal(); }
        }
      });
    }
  }

  closeDeleteModal(): void {
    this.showDeleteModal = false;
    this.deleteActionType = null;
    this.itemToDeleteId = null;
    this.typeToDeleteName = null;
    this.errorMessage = null;
  }

  // === Type Modal ===

  openAddTypeModal(categoryId: string | undefined): void {
    if (!categoryId) return;
    this.typeModalMode = 'add';
    this.typeModalCategoryId = categoryId;
    this.typeModalForm = { name: '', requiresQrCode: false, specificationFields: [] };
    this.newSpecField = '';
    this.typeModalOriginalName = '';
    this.typeModalNameError = null;
    this.typeModalQrError = null;
    this.typeModalHasEquipment = false;
    this.showSpecFields = false;
    // Expand category so user sees the new type after save
    this.expandedCategories[categoryId] = true;
    this.showTypeModal = true;
  }

  openEditTypeModal(categoryId: string | undefined, type: CategoryType): void {
    if (!categoryId) return;
    this.typeModalMode = 'edit';
    this.typeModalCategoryId = categoryId;
    this.typeModalForm = { name: type.name, requiresQrCode: type.requiresQrCode, specificationFields: type.specificationFields ? [...type.specificationFields] : [] };
    this.newSpecField = '';
    this.typeModalOriginalName = type.name;
    this.typeModalNameError = null;
    this.typeModalQrError = null;
    // We'll check dynamically if equipment is linked (disable QR toggle hint)
    this.typeModalHasEquipment = false;
    this.showSpecFields = false;
    this.showTypeModal = true;
  }

  checkTypeNameInModal(): void {
    const name = this.typeModalForm.name.trim().toLowerCase();
    if (!name) { this.typeModalNameError = 'Type name is required.'; return; }
    const cat = this.categories.find(c => c.id === this.typeModalCategoryId);
    if (!cat || !cat.types) { this.typeModalNameError = null; return; }
    const duplicate = cat.types.some(t =>
      t.name.toLowerCase() === name &&
      (this.typeModalMode === 'add' || t.name.toLowerCase() !== this.typeModalOriginalName.toLowerCase())
    );
    this.typeModalNameError = duplicate ? `Type '${this.typeModalForm.name.trim()}' already exists in this category.` : null;
  }

  saveTypeModal(): void {
    this.checkTypeNameInModal();
    if (this.typeModalNameError || !this.typeModalForm.name.trim()) return;
    if (!this.typeModalCategoryId) return;

    const payload: CategoryType = {
      name: this.typeModalForm.name.trim(),
      requiresQrCode: this.typeModalForm.requiresQrCode,
      specificationFields: this.typeModalForm.specificationFields
    };

    if (this.typeModalMode === 'add') {
      this.categoryService.addTypeToCategory(this.typeModalCategoryId, payload).subscribe({
        next: (updatedCategory) => {
          const index = this.categories.findIndex(c => c.id === this.typeModalCategoryId);
          if (index !== -1) this.categories[index] = updatedCategory;
          this.closeTypeModal();
        },
        error: (err) => {
          if (err.status === 409) {
            this.typeModalNameError = typeof err.error === 'string' ? err.error : 'Duplicate type name.';
          }
        }
      });
    } else {
      this.categoryService.updateTypeInCategory(this.typeModalCategoryId, this.typeModalOriginalName, payload).subscribe({
        next: (updatedCategory) => {
          const index = this.categories.findIndex(c => c.id === this.typeModalCategoryId);
          if (index !== -1) this.categories[index] = updatedCategory;
          this.closeTypeModal();
        },
        error: (err) => {
          if (err.status === 409) {
            const msg = typeof err.error === 'string' ? err.error : 'Conflict updating type.';
            if (msg.toLowerCase().includes('qr')) {
              this.typeModalQrError = msg;
            } else {
              this.typeModalNameError = msg;
            }
          }
        }
      });
    }
  }

  closeTypeModal(): void {
    this.showTypeModal = false;
    this.typeModalCategoryId = null;
    this.typeModalNameError = null;
    this.typeModalQrError = null;
  }

  addSpecField(): void {
    const val = this.newSpecField.trim();
    if (val) {
      if (!this.typeModalForm.specificationFields) {
        this.typeModalForm.specificationFields = [];
      }
      if (!this.typeModalForm.specificationFields.includes(val)) {
        this.typeModalForm.specificationFields.push(val);
      }
      this.newSpecField = '';
    }
  }

  removeSpecField(index: number): void {
    if (this.typeModalForm.specificationFields) {
      this.typeModalForm.specificationFields.splice(index, 1);
    }
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
