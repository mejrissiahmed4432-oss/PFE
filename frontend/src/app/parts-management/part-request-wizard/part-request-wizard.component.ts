import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../../equipment/equipment.service';
import { PartRequestService } from '../part-request.service';
import { CategoryService } from '../../category-manager/category.service';
import { PartRequest } from '../part-request.model';
import { EquipmentCategory } from '../../category-manager/category.model';

interface CartItem {
  partName: string;
  category: string;
  type: string;
  brand?: string;
  specification: string;
  quantity: number;
  isManual: boolean;
  equipmentId?: string;
  selected: boolean;
}

interface StockGroup {
  id: string; // generated key
  name: string;
  brand: string;
  specification: string;
  category: string;
  type: string;
  availableQte: number;
  selected: boolean;
  requestQte: number;
  items: any[]; // the raw equipment from backend
}

@Component({
  selector: 'app-part-request-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './part-request-wizard.component.html',
  styleUrl: './part-request-wizard.component.css'
})
export class PartRequestWizardComponent implements OnInit {
  @Input() visible: boolean = false;
  @Input() user: any;
  @Input() editRequest: PartRequest | null = null;
  @Output() closeWizard = new EventEmitter<boolean>();

  currentStep: number = 0;
  totalSteps: number = 2;
  stepLabels: string[] = ['Select Items', 'Review Request'];

  categories: EquipmentCategory[] = [];
  availableTypes: string[] = [];

  // Selection Filters
  selectedCategory: string = '';
  selectedType: string = '';

  // Data
  allStock: any[] = [];
  availableStockGroups: StockGroup[] = [];

  // Cart
  cart: CartItem[] = [];

  // Manual fallback
  showManualEntry: boolean = false;
  manualMode: 'same' | 'per_unit' = 'same';
  manualSpec: string = '';
  manualQte: number = 1;
  manualRows: { specification: string, quantity: number }[] = [{ specification: '', quantity: 1 }];

  // Global settings for the request
  globalPriority: 'Low' | 'Medium' | 'High' = 'Medium';
  globalDescription: string = '';

  isSubmitting: boolean = false;

  customAlert: { title: string, message: string } | null = null;

  constructor(
    private equipmentService: EquipmentService,
    private partRequestService: PartRequestService,
    private categoryService: CategoryService
  ) { }

  get progressPct(): number {
    return ((this.currentStep + 1) / this.totalSteps) * 100;
  }

  getDotNumber(stepIndex: number): string {
    if (stepIndex < this.currentStep) return '✓';
    return (stepIndex + 1).toString();
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadAllStock();

    if (this.editRequest) {
      this.cart = [{
        partName: this.editRequest.partName || '',
        category: this.editRequest.category || '',
        type: this.editRequest.type || '',
        specification: this.editRequest.specification || '',
        quantity: this.editRequest.quantity || 1,
        isManual: true,
        equipmentId: this.editRequest.equipmentId,
        selected: true
      }];
      this.globalPriority = (this.editRequest.priority as any) || 'Medium';
      this.globalDescription = this.editRequest.description || '';

      this.selectedCategory = this.editRequest.category || '';
      this.selectedType = this.editRequest.type || '';

      this.currentStep = 1; // skip step 1 directly to review
      this.stepLabels = ['Original Info', 'Edit Request'];
    }
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe(data => {
      this.categories = data;
      // If we already have a selected category (e.g. from editRequest), populate types now
      if (this.selectedCategory) {
        const selectedCat = this.categories.find(c => c.name === this.selectedCategory);
        this.availableTypes = selectedCat ? (selectedCat.types || []) : [];
      }
    });
  }

  loadAllStock(): void {
    this.equipmentService.getAllEquipment().subscribe((data: any[]) => {
      const partCategories = ['COMPONENT', 'STORAGE', 'PARTS', 'CONSUMABLE'];
      // Pre-filter to only in-stock relevant items
      this.allStock = data.filter((item: any) => {
        const cat = (item.category || '').toUpperCase();
        const isInStock = this.getPartStatus(item) === 'In stock';
        const isPart = partCategories.includes(cat) || cat.includes('COMPONENT') || cat.includes('STORAGE') || cat.includes('CONSUMABLE');
        return isPart && isInStock;
      });
    });
  }

  getPartStatus(item: any): string {
    if (item.status) {
      const s = item.status.toLowerCase();
      if (s === 'broken' || s === 'maintenance' || s === 'out of stock') return 'Out of stock';
      if (s === 'in stock') return 'In stock';
    }
    if (!item.shelfId) return 'Out of stock';
    return 'In stock';
  }

  close(): void {
    this.closeWizard.emit(false);
  }

  goToStep(step: number): void {
    if (step === 1 && this.cart.length === 0) {
      this.customAlert = {
        title: 'Empty List',
        message: 'Please add at least one item to your request list first.'
      };
      return;
    }
    // When going back to step 0, re-populate availableTypes from the selected category
    if (step === 0 && this.selectedCategory) {
      const selectedCat = this.categories.find(c => c.name === this.selectedCategory);
      this.availableTypes = selectedCat ? (selectedCat.types || []) : [];
      // Re-run type filter to restore stock groups
      if (this.selectedType) {
        this.onTypeChange();
      }
    }
    this.currentStep = step;
  }

  hasSelectedItems(): boolean {
    if (this.availableStockGroups.some(g => g.selected)) return true;
    if (this.showManualEntry) {
      if (this.manualMode === 'same' && this.manualSpec.trim()) return true;
      if (this.manualRows.some(r => r.specification.trim())) return true;
    }
    return false;
  }

  validateQuantity(g: StockGroup): void {
    if (g.requestQte > g.availableQte) {
      this.customAlert = {
        title: 'Quantity Exceeded',
        message: `You requested ${g.requestQte} units of "${g.name}", but only ${g.availableQte} are available in stock. Please lower your stock selection and use the custom entry section to request the remaining units.`
      };
      g.requestQte = g.availableQte; // Force revert to max
    }
  }

  hasSelectedSpecs(): boolean {
    return this.availableStockGroups.some(g => g.selected && g.specification && g.specification.trim() !== '');
  }

  getSelectedGroupsWithSpecs(): StockGroup[] {
    return this.availableStockGroups.filter(g => g.selected && g.specification && g.specification.trim() !== '');
  }

  addManualRow(): void {
    this.manualRows.push({ specification: '', quantity: 1 });
  }

  removeManualRow(index: number): void {
    if (this.manualRows.length > 1) {
      this.manualRows.splice(index, 1);
    } else {
      this.manualRows = [{ specification: '', quantity: 1 }];
    }
  }

  onCategoryChange(): void {
    const selectedCat = this.categories.find(c => c.name === this.selectedCategory);
    this.availableTypes = selectedCat ? (selectedCat.types || []) : [];
    this.selectedType = '';
    this.availableStockGroups = [];
  }

  onTypeChange(): void {
    if (!this.selectedCategory || !this.selectedType) {
      this.availableStockGroups = [];
      return;
    }

    const reqCat = this.selectedCategory.toLowerCase().trim();
    const reqType = this.selectedType.toLowerCase().trim();

    // 1. Filter raw items
    const filtered = this.allStock.filter(p => {
      const pType = (p.type || '').toLowerCase().trim();
      const pCat = (p.category || '').toLowerCase().trim();

      const isTypeMatch = (pType === reqType);
      const isCatMatch = (pCat === reqCat || pCat === 'consumable');
      return isTypeMatch && isCatMatch;
    });

    // 2. Group identical items
    const map = new Map<string, StockGroup>();
    filtered.forEach(item => {
      const eName = (item.equipmentName || item.type || '').trim();
      const eBrand = (item.brand || 'No Brand').trim();
      const eSpec = (item.specification || '').trim();

      const key = `${eName.toLowerCase()}|${eBrand.toLowerCase()}|${eSpec.toLowerCase()}`;

      if (!map.has(key)) {
        map.set(key, {
          id: key,
          name: eName,
          brand: eBrand,
          specification: eSpec,
          category: item.category,
          type: item.type,
          availableQte: 0,
          selected: false,
          requestQte: 1,
          items: []
        });
      }
      const group = map.get(key)!;
      group.items.push(item);
      group.availableQte++;
    });

    this.availableStockGroups = Array.from(map.values()).map(group => {
      // Find matching items in cart (same name and spec), including manual entries from edit mode
      const cartQte = this.cart
        .filter(c => c.selected && c.partName === group.name && (c.specification || '') === (group.specification || ''))
        .reduce((sum, item) => sum + item.quantity, 0);

      group.availableQte = Math.max(0, group.availableQte - cartQte);
      // If this item is already fully consumed by the cart, mark it as already-added
      group.selected = false;
      return group;
    });

    this.manualSpec = '';
    this.manualQte = 1;
  }

  addSelectedToCart(): void {
    let added = false;

    this.availableStockGroups.forEach(g => {
      if (g.selected) {
        // Check against ALL cart items (including manual ones from edit mode)
        const existingItem = this.cart.find(c =>
          c.partName === g.name &&
          (c.specification || '') === (g.specification || '')
        );

        if (existingItem) {
          existingItem.quantity += g.requestQte;
          existingItem.isManual = false; // promote to stock item
          existingItem.equipmentId = existingItem.equipmentId || g.items[0]?.id;
          existingItem.selected = true;
        } else {
          this.cart.push({
            partName: g.name,
            brand: g.brand,
            category: this.selectedCategory,
            type: this.selectedType,
            specification: g.specification,
            quantity: g.requestQte,
            isManual: false,
            equipmentId: g.items[0]?.id,
            selected: true
          });
        }

        g.selected = false;
        g.requestQte = 1;
        added = true;
      }
    });

    // Add manual items
    if (this.showManualEntry) {
      if (this.manualMode === 'same' && this.manualSpec.trim()) {
        const existingManual = this.cart.find(c =>
          c.isManual &&
          c.type === this.selectedType &&
          c.specification === this.manualSpec.trim()
        );

        if (existingManual) {
          existingManual.quantity += this.manualQte;
          existingManual.selected = true;
        } else {
          this.cart.push({
            partName: `${this.selectedType} (Custom)`,
            category: this.selectedCategory,
            type: this.selectedType,
            specification: this.manualSpec.trim(),
            quantity: this.manualQte,
            isManual: true,
            selected: true
          });
        }

        this.manualSpec = '';
        this.manualQte = 1;
        added = true;
      } else if (this.manualMode === 'per_unit') {
        let anyRowAdded = false;
        this.manualRows.forEach(row => {
          if (row.specification.trim()) {
            const existingManual = this.cart.find(c =>
              c.isManual &&
              c.type === this.selectedType &&
              c.specification === row.specification.trim()
            );

            if (existingManual) {
              existingManual.quantity += row.quantity;
              existingManual.selected = true;
            } else {
              this.cart.push({
                partName: `${this.selectedType} (Custom)`,
                category: this.selectedCategory,
                type: this.selectedType,
                specification: row.specification.trim(),
                quantity: row.quantity,
                isManual: true,
                selected: true
              });
            }
            anyRowAdded = true;
          }
        });

        if (anyRowAdded) {
          this.manualRows = [{ specification: '', quantity: 1 }];
          added = true;
        }
      }
    }

    if (!added) {
      this.customAlert = {
        title: 'Nothing Selected',
        message: 'Please select at least one item or fill out the specification.'
      };
    } else {
      // Optional: Auto advance or show success logic
    }
  }

  submitCart(): void {
    const finalCart = this.cart.filter(c => c.selected);
    if (finalCart.length === 0) {
      this.customAlert = {
        title: 'Validation Error',
        message: 'You have unchecked all items in step 2. Please check at least one item to submit.'
      };
      return;
    }

    this.isSubmitting = true;

    if (this.editRequest && this.editRequest.id) {
      const updatedItem = finalCart[0];
      const payload: Partial<PartRequest> = {
        partName: updatedItem.partName,
        category: updatedItem.category,
        type: updatedItem.type,
        equipmentId: updatedItem.equipmentId,
        quantity: updatedItem.quantity,
        priority: this.globalPriority,
        specification: updatedItem.specification,
        description: this.globalDescription
      };
      this.partRequestService.updateRequest(this.editRequest.id, payload).subscribe({
        next: () => this.handleSubmissionComplete(false),
        error: (err) => {
          console.error(err);
          this.handleSubmissionComplete(true);
        }
      });
      return;
    }

    let completed = 0;
    const total = finalCart.length;
    let hasError = false;

    finalCart.forEach(item => {
      const payload: PartRequest = {
        partName: item.partName,
        category: item.category,
        type: item.type,
        specification: item.specification,
        equipmentId: item.equipmentId,
        quantity: item.quantity,
        priority: this.globalPriority,
        description: this.globalDescription,
        requesterId: this.user.id,
        requesterName: `${this.user.firstName} ${this.user.lastName}`,
        status: 'PENDING'
      };

      this.partRequestService.createRequest(payload).subscribe({
        next: () => {
          completed++;
          if (completed === total) {
            this.handleSubmissionComplete(hasError);
          }
        },
        error: (err) => {
          console.error(err);
          hasError = true;
          completed++;
          if (completed === total) {
            this.handleSubmissionComplete(hasError);
          }
        }
      });
    });
  }

  private handleSubmissionComplete(hasError: boolean): void {
    this.isSubmitting = false;
    if (hasError) {
      this.customAlert = {
        title: 'Submission Failed',
        message: 'Some items failed to submit. Please check your history.'
      };
    } else {
      this.cart = []; // empty cart
      this.closeWizard.emit(true); // Emit true for success!
    }
  }

  closeAlert(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.customAlert = null;
  }
}
