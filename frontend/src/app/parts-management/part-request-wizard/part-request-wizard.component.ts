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
  isOriginalEdit?: boolean;
  maxAvailable?: number;
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
  @Input() initialPartSelection: any = null;
  @Output() closeWizard = new EventEmitter<boolean>();

  currentStep: number = 0;
  totalSteps: number = 2;
  stepLabels: string[] = ['Select Items', 'Review Request'];

  categories: EquipmentCategory[] = [];
  availableTypes: string[] = [];

  // Selection Filters
  selectedCategory: string = '';
  selectedType: string = '';

  initialSelectionProcessed: boolean = false;
  categoriesLoaded: boolean = false;
  stockLoaded: boolean = false;

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
      if (this.editRequest.items && this.editRequest.items.length > 0) {
        this.cart = this.editRequest.items.map(item => ({
          partName: item.partName || '',
          category: item.category || '',
          type: item.type || '',
          brand: item.brand || '',
          specification: item.specification || '',
          quantity: item.quantity || 1,
          isManual: !item.equipmentId,
          equipmentId: item.equipmentId,
          selected: true,
          isOriginalEdit: true
        }));
        this.selectedCategory = this.editRequest.items[0].category || '';
        this.selectedType = this.editRequest.items[0].type || '';
      } else {
        this.cart = [];
        this.selectedCategory = '';
        this.selectedType = '';
      }
      this.globalPriority = (this.editRequest.priority as any) || 'Medium';
      this.globalDescription = this.editRequest.description || '';

      this.currentStep = 1; // skip step 1 directly to review
      this.stepLabels = ['Original Info', 'Edit Request'];
    }
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe(data => {
      this.categories = data;
      this.categoriesLoaded = true;
      // If we already have a selected category (e.g. from editRequest), populate types now
      if (this.selectedCategory) {
        const selectedCat = this.categories.find(c => c.name === this.selectedCategory);
        this.availableTypes = selectedCat ? (selectedCat.types || []).map((t: any) => typeof t === 'string' ? t : t.name) : [];
      }
      this.checkInitialPartSelection();
    });
  }

  loadAllStock(): void {
    this.equipmentService.getAllEquipment().subscribe((data: any[]) => {
      // Pre-filter to only in-stock items.
      // We admitted any category because filtering by specified category/type happens in onTypeChange.
      this.allStock = data.filter((item: any) => {
        return this.getPartStatus(item) === 'In stock';
      });

      // Update maxAvailable for any existing cart items (e.g. from editRequest)
      this.cart.forEach(item => {
        if (!item.isManual) {
          const totalInSystem = this.allStock.filter(p => 
            (p.equipmentName || p.type || '').trim() === item.partName &&
            (p.specification || '').trim() === (item.specification || '').trim()
          ).length;
          item.maxAvailable = totalInSystem;
          
          if (item.quantity > totalInSystem && totalInSystem > 0) {
             // We don't automatically reduce here to avoid wiping out requested counts on load, but we map the limit.
          }
        }
      });
      
      this.stockLoaded = true;
      this.checkInitialPartSelection();
    });
  }

  checkInitialPartSelection(): void {
    if (this.categoriesLoaded && this.stockLoaded && this.initialPartSelection && !this.editRequest && !this.initialSelectionProcessed) {
      this.initialSelectionProcessed = true;
      
      this.selectedCategory = this.initialPartSelection.category;
      this.onCategoryChange();
      this.selectedType = this.initialPartSelection.type;
      this.onTypeChange();
      
      const spec = (this.initialPartSelection.items && this.initialPartSelection.items.length > 0) ? (this.initialPartSelection.items[0].specification || '') : '';
      const name = this.initialPartSelection.name;
      
      const matchingGroup = this.availableStockGroups.find(g => 
        g.name === name && 
        (g.specification || '') === spec &&
        g.availableQte > 0
      );

      if (matchingGroup) {
         matchingGroup.selected = true;
         matchingGroup.requestQte = 1;
         this.addSelectedToCart();
         
         this.customAlert = {
           title: 'Item Added',
           message: `We automatically added 1 unit of "${matchingGroup.name}" to your request list because it was available in stock.`
         };
      }
    }
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
      this.availableTypes = selectedCat ? (selectedCat.types || []).map((t: any) => typeof t === 'string' ? t : t.name) : [];
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
    this.availableTypes = selectedCat ? (selectedCat.types || []).map((t: any) => typeof t === 'string' ? t : t.name) : [];
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
        if (g.requestQte > g.availableQte) {
          g.requestQte = g.availableQte;
        }
        if (g.requestQte <= 0) {
          g.selected = false;
          return;
        }

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
            selected: true,
            maxAvailable: g.items.length // the total raw items found for this group
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

  validateCartQuantity(item: CartItem): void {
    if (item.isManual) return;
    
    if (item.maxAvailable !== undefined && item.quantity > item.maxAvailable) {
      this.customAlert = {
        title: 'Quantity Exceeded',
        message: `You requested ${item.quantity} units of "${item.partName}", but only ${item.maxAvailable} are available in total stock. Reverting to maximum.`
      };
      item.quantity = item.maxAvailable;
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

    // Create the items array
    const requestItems = finalCart.map(item => ({
      partName: item.partName,
      category: item.category,
      type: item.type,
      brand: item.brand,
      specification: item.specification,
      equipmentId: item.equipmentId,
      quantity: item.quantity
    }));

    if (this.editRequest && this.editRequest.id) {
      const updatePayload: Partial<PartRequest> = {
        items: requestItems,
        priority: this.globalPriority,
        description: this.globalDescription
      };

      this.partRequestService.updateRequest(this.editRequest.id, updatePayload).subscribe({
        next: () => {
          this.handleSubmissionComplete(false);
        },
        error: (err) => {
          console.error(err);
          this.handleSubmissionComplete(true);
        }
      });
      return;
    }

    // Normal Create workflow (when not editing)
    const payload: PartRequest = {
      items: requestItems,
      priority: this.globalPriority,
      description: this.globalDescription,
      requesterId: this.user.id,
      requesterName: `${this.user.firstName} ${this.user.lastName}`,
      status: 'PENDING'
    };

    this.partRequestService.createRequest(payload).subscribe({
      next: () => {
        this.handleSubmissionComplete(false);
      },
      error: (err) => {
        console.error(err);
        this.handleSubmissionComplete(true);
      }
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
