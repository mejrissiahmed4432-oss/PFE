import {
  Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable, Subject, of, from, EMPTY } from 'rxjs';
import { debounceTime, distinctUntilChanged, groupBy, mergeMap, filter, switchMap, map, catchError, timeout } from 'rxjs/operators';
import { Equipment } from '../equipment.model';
import { EquipmentService } from '../equipment.service';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ShelfService } from '../../shelf/shelf.service';
import { Shelf } from '../../shelf/shelf.model';
import { CategoryService } from '../../category-manager/category.service';
import { CategoryType, EquipmentCategory } from '../../category-manager/category.model';
import * as QRCode from 'qrcode';

// ─── Interfaces ───────────────────────────────────────────────────────────
export interface UnitRow {
  name: string;
  brand: string;
  model: string;
  specifications: { [key: string]: string };
  serialNumber: string;
  purchaseDate: string;
  supplierId: string;
  supplier: string;
  purchasePrice: number;
  invoiceRef: string;
  warrantyEnd: string;
  invoiceFile?: File;
  invoiceFileName?: string;
  invoiceFileData?: string;
  selectedForInvoice?: boolean;
  warrantyFile?: File;
  warrantyFileName?: string;
  warrantyFileData?: string;
  selectedForWarranty?: boolean;
  selectedForGeneralSync?: boolean;
  selectedForSpecSync?: boolean;
  selectedForSN?: boolean;

}

// ─── Component ────────────────────────────────────────────────────────────
@Component({
  selector: 'app-equipment-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-wizard.component.html',
  styleUrl: './equipment-wizard.component.css'
})
export class EquipmentWizardComponent implements OnInit, OnChanges {
  @Input() visible: boolean = false;
  @Input() prefillData: Equipment | null = null;
  @Output() closeEvent = new EventEmitter<boolean>();

  // ── Wizard navigation ──────────────────────────────────────────────────
  currentStep: number = 0;
  isSaving: boolean = false;
  saveError: string = '';
  capacityError: string = '';
  isCheckingCapacity: boolean = false;

  // Serial Number Uniqueness State
  snStatusMap: Map<string, { checking: boolean, unique: boolean, error?: string }> = new Map();
  private snSubject = new Subject<{ sn: string, index?: number }>();

  // ── External data ──────────────────────────────────────────────────────
  suppliers: Supplier[] = [];

  // ── Step 0: Setup ─────────────────────────────────────────────────────
  quantity: number = 1;
  category: 'Asset' | 'Consumable' = 'Asset';
  selectedCategoryName: string = '';
  type: string = '';
  configMode: 'same' | 'different' = 'same';

  categories: EquipmentCategory[] = [];
  availableTypes: CategoryType[] = [];

  // ── Step 1: General Info ──────────────────────────────────────────────
  sharedName: string = '';
  sharedBrand: string = '';
  sharedModel: string = '';
  sharedNotes: string = '';

  // ── Step 2: Specifications ────────────────────────────────────────────
  specMode: 'same' | 'different' = 'same';
  sharedSpecifications: { [key: string]: string } = {};
  specKeys: string[] = [];
  sharedSerial: string = '';

  // ── Step 4: Purchase Info ─────────────────────────────────────────────
  purchaseMode: 'same' | 'different' = 'same';
  sharedPurchaseDate: string = '';
  sharedSupplierId: string = '';
  sharedSupplier: string = '';
  sharedPriceMode: 'total' | 'per-unit' = 'per-unit';
  sharedPrice: number = 0;
  sharedInvoiceRef: string = '';
  sharedInvoiceFile: File | null = null;
  sharedInvoiceFileName: string = '';
  sharedInvoiceFileData: string = '';

  // ── Step 5: Warranty ──────────────────────────────────────────────────
  warrantyMode: 'shared' | 'individual' = 'shared';
  sharedWarrantyEnd: string = '';
  sharedWarrantyFile: File | null = null;
  sharedWarrantyFileName: string = '';
  sharedWarrantyFileData: string = '';

  // ── Step 6: Storage Assignment ────────────────────────────────────────
  availableShelves: Shelf[] = [];
  shelfAssignments: { shelf: Shelf; assignCount: number }[] = [];

  // ── Per‑unit rows (DIFFERENT mode) ────────────────────────────────────
  units: UnitRow[] = [];

  // ── Type lists ────────────────────────────────────────────────────────
  readonly equipmentTypes = [
    'pc', 'laptop', 'server', 'monitor', 'printer', 'scanner',
    'projector', 'router', 'switch', 'ups', 'tablet', 'phone',
    'ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'
  ];
  readonly consumableTypes = ['ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'];
  readonly computerTypes = ['pc', 'laptop', 'server', 'tablet'];

  // ── Step labels ───────────────────────────────────────────────────────
  readonly stepLabels = [
    'Entry Mode', 'General Info', 'Specifications',
    'Serial Numbers', 'Purchase Info', 'Warranty',
    'Storage Assignment', 'Review & Confirm'
  ];

  // ── Pagination for Review Step ────────────────────────────────────────
  reviewCurrentPage: number = 1;
  reviewPageSize: number = 5;

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
    private shelfService: ShelfService,
    private categoryService: CategoryService
  ) { }

  private formatLocalDate(dateObjOrString: Date | string): string {
    const d = typeof dateObjOrString === 'string' ? new Date(dateObjOrString) : dateObjOrString;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }

  ngOnInit(): void {
    this.supplierService.getAllSuppliers().subscribe({ next: d => this.suppliers = d });
    this.categoryService.getAllCategories().subscribe({ next: d => {
      this.categories = d;
      if (this.prefillData) {
        this.populateFromPrefill(this.prefillData);
      }
    }});
    this.sharedPurchaseDate = this.formatLocalDate(new Date());

    // ─── SN Uniqueness Validation Pipeline (Robust) ────────────────────────
    // We group by SN to allow multiple concurrent debounced checks.
    // We use switchMap inside the group to cancel previous checks if the same SN is typed quickly.
    this.snSubject.pipe(
      filter(data => !!data.sn && data.sn.length === 10),
      groupBy(data => data.sn),
      mergeMap(group => group.pipe(
        debounceTime(500),
        switchMap(data => {
          const allSNs = this.quantity === 1 ? [this.sharedSerial] : this.units.map(u => u.serialNumber);
          const duplicates = allSNs.filter(s => s === data.sn).length;
          if (duplicates > 1) {
            this.snStatusMap.set(data.sn, { checking: false, unique: false, error: 'Duplicate in current batch' });
            return EMPTY;
          }
          return this.equipmentService.checkSerialNumberUnique(data.sn).pipe(
            map(isUnique => ({ sn: data.sn, isUnique })),
            timeout(5000),
            catchError(err => {
              console.error('SN Check Error:', err);
              return of({ sn: data.sn, isUnique: true });
            })
          );
        })
      ))
    ).subscribe(res => {
      this.snStatusMap.set(res.sn, { checking: false, unique: res.isUnique });
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['prefillData'] && this.prefillData) {
      this.populateFromPrefill(this.prefillData);
    }
  }

  private populateFromPrefill(data: Equipment): void {
    this.type = data.type || '';
    if (data.category === 'Asset' || data.category === 'Consumable') {
      this.category = data.category as 'Asset' | 'Consumable';
    } else {
      this.selectedCategoryName = data.category || '';
      if (['STORAGE', 'COMPONENT'].includes(this.selectedCategoryName.toUpperCase())) {
        this.category = 'Consumable';
      } else {
        this.category = 'Asset';
      }
    }
    if (this.type) {
      const cat = this.categories.find(c =>
        c.types?.map(t => t.name.toLowerCase()).includes(this.type.toLowerCase())
      );
      if (cat) {
        this.selectedCategoryName = cat.name || '';
        this.availableTypes = cat.types || [];
      }
    }
    this.sharedName = data.equipmentName || '';
    this.sharedBrand = data.brand || '';
    this.sharedModel = data.model || '';
    this.sharedNotes = data.note || '';
    // Specs
    this.sharedSpecifications = data.specifications ? { ...data.specifications } : {};
    
    // Purchase
    this.sharedPurchaseDate = data.purchaseDate ? this.formatLocalDate(data.purchaseDate.toString()) : '';
    this.sharedSupplierId = data.supplierId || '';
    this.sharedSupplier = data.supplier || '';
    this.sharedPrice = data.purchasePrice || 0;
    this.sharedPriceMode = 'per-unit';
    this.sharedInvoiceRef = data.invoiceRef || '';
    // Invoice document — set filename immediately; fetch binary data if missing
    this.sharedInvoiceFileName = data.invoiceFileName || '';
    this.sharedInvoiceFileData = data.invoiceFileData || '';
    if (data.id && data.invoiceFileName && !data.invoiceFileData) {
      this.equipmentService.getInvoiceFile(data.id)
        .pipe(catchError(() => of('')))
        .subscribe((fileData: string) => { this.sharedInvoiceFileData = fileData || ''; });
    }
    // Warranty document — set filename immediately; fetch binary data if missing
    this.sharedWarrantyEnd = data.warrantyExpiration ? this.formatLocalDate(data.warrantyExpiration.toString()) : '';

    this.sharedWarrantyFileName = data.warrantyFileName || '';
    this.sharedWarrantyFileData = data.warrantyFileData || '';
    if (data.id && data.warrantyFileName && !data.warrantyFileData) {
      this.equipmentService.getWarrantyFile(data.id)
        .pipe(catchError(() => of('')))
        .subscribe((fileData: string) => { this.sharedWarrantyFileData = fileData || ''; });
    }
    // Quantity & Serial
    this.quantity = 1;
    this.sharedSerial = '';
    this.configMode = 'same';
    this.specMode = 'same';
    this.purchaseMode = 'same';
    this.warrantyMode = 'shared';
    this.currentStep = 0;
    this.updateSpecKeys();
    this.validateStockCapacity();
  }

  // ── Computed helpers ──────────────────────────────────────────────────
  get isComputerType(): boolean { return this.computerTypes.includes(this.type); }
  get isConsumable(): boolean { return this.consumableTypes.includes(this.type); }
  get isDeviceCategory(): boolean {
    const cat = this.selectedCategoryName?.toUpperCase();
    return cat === 'DEVICE' || cat === 'SERVER';
  }
  get totalSteps(): number { return 8; }   // 0‑7
  get progressPct(): number { return Math.round((this.currentStep / (this.totalSteps - 1)) * 100); }

  // ── Dynamic display steps when Serial Numbers step is skipped ────────────
  get displayTotalSteps(): number {
    return (this.quantity > 1 && this.specMode === 'different') ? 7 : 8;
  }

  get displayStep(): number {
    if (this.quantity > 1 && this.specMode === 'different' && this.currentStep > 2) {
      return this.currentStep; // currentStep is 0-indexed, so index 4 -> Step 4
    }
    return this.currentStep + 1;
  }

  get typeRequiresQr(): boolean {
    if (!this.selectedCategoryName || !this.type) return true;
    const cat = this.categories.find(c => c.name === this.selectedCategoryName);
    if (!cat) return true;
    const typeObj = cat.types?.find(t => t.name === this.type);
    return typeObj ? typeObj.requiresQrCode : true;
  }


  getDotNumber(index: number): number {
    if (this.quantity > 1 && this.specMode === 'different' && index > 2) {
      return index; // instead of index + 1
    }
    return index + 1;
  }
  get perUnitPrice(): number {
    if (this.sharedPriceMode === 'per-unit') return this.sharedPrice;
    return this.quantity ? this.sharedPrice / this.quantity : 0;
  }

  // ── Step 0 handlers ───────────────────────────────────────────────────
  onCategoryChange(): void {
    const selectedCat = this.categories.find(c => c.name === this.selectedCategoryName);
    if (selectedCat) {
      this.availableTypes = selectedCat.types || [];

      // Auto-set internal category tag
      if (this.selectedCategoryName === 'COMPONENT' ||
        this.selectedCategoryName === 'STORAGE' ||
        this.selectedCategoryName === 'PERIPHERAL') {
        // This is a simplified check based on previous logic "ram, hard drive, ssd..."
        // We can refine this if needed, but for now we'll stick to a heuristic
        // or just let it be Asset by default unless specifically known.
      }
    } else {
      this.availableTypes = [];
    }

    // Reset type if not in new list
    if (this.type && !this.availableTypes.map(t => t.name.toLowerCase()).includes(this.type.toLowerCase())) {
      this.type = '';
    }
    this.updateSpecKeys();
    this.validateStockCapacity();
  }

  onTypeChange(): void {
    // If category is one of STORAGE, COMPONENT, it's often a Consumable in the previous logic
    if (['STORAGE', 'COMPONENT'].includes(this.selectedCategoryName)) {
      this.category = 'Consumable';
    } else if (['ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'].includes(this.type.toLowerCase())) {
      this.category = 'Consumable';
    } else {
      this.category = 'Asset';
    }
    this.updateSpecKeys();
    this.validateStockCapacity();
  }

  private updateSpecKeys(): void {
    if (this.type && this.availableTypes) {
      const selectedTypeObj = this.availableTypes.find(t => t.name === this.type);
      this.specKeys = selectedTypeObj?.specificationFields || [];
    } else {
      this.specKeys = [];
    }
    
    // Initialize keys in sharedSpecifications if missing
    this.specKeys.forEach(k => {
      if (this.sharedSpecifications[k] === undefined) {
        this.sharedSpecifications[k] = '';
      }
    });
    
    // Initialize in all units
    this.units.forEach(u => {
      this.specKeys.forEach(k => {
        if (u.specifications[k] === undefined) {
          u.specifications[k] = '';
        }
      });
    });
  }

  onQuantityChange(): void {
    this.rebuildUnits();
    this.validateStockCapacity();
  }

  private validateStockCapacity(): void {
    if (!this.type || this.quantity < 1) {
      this.capacityError = '';
      return;
    }

    this.isCheckingCapacity = true;
    this.capacityError = '';

    this.shelfService.getShelvesByType(this.type).subscribe({
      next: shelves => {
        const totalSpace = shelves.reduce((sum, s) => sum + (s.maxQte - s.currentQte), 0);

        if (shelves.length === 0) {
          this.capacityError = `No shelves found for type "${this.type}". Please create a shelf first.`;
        } else if (totalSpace < this.quantity) {
          this.capacityError = `Insufficient storage! Available space for ${this.type}: ${totalSpace} units. (Requested: ${this.quantity})`;
        } else {
          this.capacityError = '';
        }
        this.isCheckingCapacity = false;
      },
      error: err => {
        console.error('Capacity check failed', err);
        this.capacityError = 'Could not verify stock capacity. Please try again.';
        this.isCheckingCapacity = false;
      }
    });
  }

  private rebuildUnits(): void {
    const n = Number(this.quantity) || 1;
    while (this.units.length < n) this.units.push(this.emptyUnit());
    this.units = this.units.slice(0, n);
  }

  private emptyUnit(): UnitRow {
    return {
      name: '', brand: '', model: '',
      specifications: {},
      serialNumber: '',
      purchaseDate: this.sharedPurchaseDate, supplierId: '', supplier: '', purchasePrice: 0, invoiceRef: '',
      warrantyEnd: '',
      selectedForInvoice: false, selectedForWarranty: false,
      selectedForGeneralSync: false, selectedForSpecSync: false,
      selectedForSN: false
    };
  }

  // ── Step 4 supplier ───────────────────────────────────────────────────
  onSharedSupplierChange(): void {
    const s = this.suppliers.find(x => x.id === this.sharedSupplierId);
    this.sharedSupplier = s?.companyName || '';
  }

  onUnitSupplierChange(unit: UnitRow): void {
    const s = this.suppliers.find(x => x.id === unit.supplierId);
    unit.supplier = s?.companyName || '';
  }

  // ── Step 3 Serial handles ─────────────────────────────────────────────
  isValidSerialNumber(sn: string): boolean {
    // Exactly 10 chars, alphanumeric, min 1 letter, min 1 digit
    const regex = /^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{10}$/;
    return regex.test(sn || '');
  }

  onSNChange(val: string, index?: number): void {
    const sn = (val || '').toUpperCase().trim();
    if (index !== undefined) {
      if (this.units[index]) this.units[index].serialNumber = sn;
    } else {
      this.sharedSerial = sn;
    }

    this.revalidateBatchSNs();
  }

  private revalidateBatchSNs(): void {
    const allSNs = this.quantity === 1 ? [this.sharedSerial] : this.units.map(u => u.serialNumber);

    allSNs.forEach((sn, idx) => {
      if (!sn || sn.length === 0) return;

      if (sn.length === 10 && this.isValidSerialNumber(sn)) {
        // Check for internal duplicates in current batch
        const count = allSNs.filter(s => s === sn).length;
        if (count > 1) {
          this.snStatusMap.set(sn, { checking: false, unique: false, error: 'Duplicate in current batch' });
        } else {
          // Internally unique. Always push to subject to let RxJS handle the state
          const status = this.snStatusMap.get(sn);
          // Only trigger if we don't already have a valid unique status or if it was an error
          if (!status || status.error || status.checking || !status.unique) {
            this.snStatusMap.set(sn, { checking: true, unique: true });
            this.snSubject.next({ sn, index: idx });
          }
        }
      } else {
        this.snStatusMap.delete(sn);
      }
    });

    // Final cleanup: remove status for SNs no longer present in allSNs
    const currentSNSet = new Set(allSNs);
    Array.from(this.snStatusMap.keys()).forEach(key => {
      if (!currentSNSet.has(key)) {
        this.snStatusMap.delete(key);
      }
    });
  }

  isSNChecking(sn: string): boolean {
    return this.snStatusMap.get(sn)?.checking || false;
  }

  isSNUnique(sn: string): boolean {
    const status = this.snStatusMap.get(sn);
    return status ? status.unique : true;
  }

  getSNError(sn: string): string | undefined {
    return this.snStatusMap.get(sn)?.error;
  }

  generateRandomSN(): string {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const digits = '0123456789';
    const all = letters + digits;
    let result = '';

    // Exactly 10 random alphanumeric chars
    for (let i = 0; i < 10; i++) {
      result += all.charAt(Math.floor(Math.random() * all.length));
    }

    // Ensure at least one letter and one digit
    if (!/\d/.test(result)) {
      result = result.substring(0, 9) + digits.charAt(Math.floor(Math.random() * digits.length));
    }
    if (!/[a-zA-Z]/.test(result)) {
      result = result.substring(0, 9) + letters.charAt(Math.floor(Math.random() * letters.length));
    }

    return result;
  }

  suggestSerials(): void {
    const generateUniqueSN = () => {
      let sn = this.generateRandomSN();
      // Simple loop to avoid internal batch collision (not fully robust but better than nothing)
      return sn;
    };

    if (this.currentStep === 3 && this.quantity === 1) {
      this.sharedSerial = generateUniqueSN();
      this.onSNChange(this.sharedSerial);
    } else {
      const isSpecStep = this.currentStep === 2;
      const selected = this.units.filter(u => isSpecStep ? u.selectedForSpecSync : u.selectedForSN);
      if (selected.length > 0) {
        selected.forEach(u => {
          u.serialNumber = generateUniqueSN();
          const idx = this.units.indexOf(u);
          this.onSNChange(u.serialNumber, idx);
          if (isSpecStep) {
            u.selectedForSpecSync = false;
          } else {
            u.selectedForSN = false;
          }
        });
      }
    }
  }

  hasSelectedSN(): boolean {
    return this.units.some(u => u.selectedForSN);
  }

  hasSelectedSpec(): boolean {
    return this.units.some(u => u.selectedForSpecSync);
  }

  hasSelectedGeneral(): boolean {
    return this.units.some(u => u.selectedForGeneralSync);
  }

  hasSelectedPurchase(): boolean {
    return this.units.some(u => u.selectedForInvoice);
  }

  hasSelectedWarranty(): boolean {
    return this.units.some(u => u.selectedForWarranty);
  }

  isAllPurchaseSelected(): boolean {
    return this.units && this.units.length > 0 && this.units.every(u => u.selectedForInvoice);
  }

  isAllWarrantySelected(): boolean {
    return this.units && this.units.length > 0 && this.units.every(u => u.selectedForWarranty);
  }

  onGeneralSelectAll(event: any): void {
    const checked = event.target.checked;
    this.units.forEach(u => u.selectedForGeneralSync = checked);
  }

  onSpecSelectAll(event: any): void {
    const checked = event.target.checked;
    this.units.forEach(u => u.selectedForSpecSync = checked);
  }

  onSNSelectAll(event: any): void {
    const checked = event.target.checked;
    this.units.forEach(u => u.selectedForSN = checked);
  }

  onPurchaseSelectAll(event: any): void {
    const checked = event.target.checked;
    this.units.forEach(u => u.selectedForInvoice = checked);
  }

  onWarrantySelectAll(event: any): void {
    const checked = event.target.checked;
    this.units.forEach(u => u.selectedForWarranty = checked);
  }

  clearSelectedSerials(): void {
    if (this.currentStep === 3 && this.quantity === 1) {
      this.sharedSerial = '';
    } else {
      const isStep2 = this.currentStep === 2;
      this.units.forEach(u => {
        if (isStep2 ? u.selectedForSpecSync : u.selectedForSN) {
          u.serialNumber = '';
        }
      });
    }
  }

  // ── Validation ────────────────────────────────────────────────────────
  isStepValid(): boolean {
    switch (this.currentStep) {
      case 0: return !!this.type && this.quantity >= 1 && !this.capacityError && !this.isCheckingCapacity;
      case 1: return this.configMode === 'same'
        ? !!this.sharedName && !!this.sharedBrand
        : this.units.every(u => !!u.name && !!u.brand);
      case 2:
        if (this.specMode === 'same' || this.quantity === 1) return true;
        return this.units.every(u => {
          const sn = u.serialNumber;
          return !!sn && this.isValidSerialNumber(sn) && this.isSNUnique(sn) && !this.isSNChecking(sn);
        });
      case 3:
        if (this.quantity === 1) return this.isValidSerialNumber(this.sharedSerial) && this.isSNUnique(this.sharedSerial) && !this.isSNChecking(this.sharedSerial);
        return this.units.length > 0 && this.units.every(u => this.isValidSerialNumber(u.serialNumber) && this.isSNUnique(u.serialNumber) && !this.isSNChecking(u.serialNumber));
      case 4: return this.purchaseMode === 'same'
        ? !!this.sharedSupplierId && !!this.sharedPurchaseDate
        : this.units.every(u => !!u.supplierId && !!u.purchaseDate);
      case 5: return true; // warranty optional
      case 6:
        return this.getTotalAssigned() === (Number(this.quantity) || 1);
      case 7: return true;
      default: return true;
    }
  }

  // ── Wizard navigation ─────────────────────────────────────────────────
  nextStep(): void {
    if (!this.isStepValid()) return;
    if (this.currentStep === 0) { this.rebuildUnits(); }

    if (this.currentStep === 1) {
      if (this.category === 'Consumable') {
        // We still skip step 2 & 3 for consumables? 
        // Actually, let's just let it go through step 2 for general specs.
      }
    }
    if (this.currentStep === 2) {
      if (this.specMode === 'different') {
        this.currentStep = 4; return;
      }
    }
    // Warranty is now accessible for all categories so we no longer jump from 4 to 6.
    if (this.currentStep < 7) {
      this.currentStep++;
      if (this.currentStep === 6) this.loadShelves();
    }
  }

  prevStep(): void {
    if (this.currentStep === 3) {
      if (this.category === 'Consumable') {
        // 
      }
    }
    if (this.currentStep === 4) {
      if (this.specMode === 'different') {
        this.currentStep = 2; return;
      }
    }
    // Backward navigation from 6 (Storage) goes naturally to 5 (Warranty) for all.
    if (this.currentStep > 0) this.currentStep--;
  }

  goToStep(targetStep: number): void {
    if (targetStep === this.currentStep) return;

    // Backward navigation: always allowed
    if (targetStep < this.currentStep) {
      this.currentStep = targetStep;
      return;
    }

    // Forward navigation: validate each intermediate step
    // Run units rebuild if we're going past step 0
    if (this.currentStep === 0) { this.rebuildUnits(); }

    for (let step = this.currentStep; step < targetStep; step++) {
      const savedStep = this.currentStep;
      this.currentStep = step;
      if (!this.isStepValid()) {
        // Stop here — can't skip this invalid step.
        // If we landed on the storage step, ensure shelves are loaded
        if (this.currentStep === 6) {
          this.loadShelves();
        }
        return;
      }
      this.currentStep = savedStep;
    }

    // All intermediate steps are valid — jump to target
    this.currentStep = targetStep;
    if (this.currentStep === 6) this.loadShelves();
  }

  // ── Step 6 helpers ────────────────────────────────────────────────────
  getTotalAssigned(): number {
    return this.shelfAssignments.reduce((sum, a) => sum + (a.assignCount || 0), 0);
  }

  loadShelves(): void {
    this.shelfService.getShelvesByType(this.type).subscribe({
      next: shelves => {
        this.availableShelves = shelves.filter(s => s.maxQte - s.currentQte > 0);
        const prev = new Map(this.shelfAssignments?.map(a => [a.shelf.id, a.assignCount]) || []);
        this.shelfAssignments = this.availableShelves.map(s => ({
          shelf: s,
          assignCount: prev.get(s.id) || 0
        }));
      },
      error: err => console.error('Failed to load shelves', err)
    });
  }

  decrementAssign(index: number): void {
    if (this.shelfAssignments[index].assignCount > 0) {
      this.shelfAssignments[index].assignCount--;
    }
  }

  incrementAssign(index: number): void {
    const assignment = this.shelfAssignments[index];
    const available = assignment.shelf.maxQte - assignment.shelf.currentQte;
    const currentTotal = this.getTotalAssigned();
    if (assignment.assignCount < available && currentTotal < this.quantity) {
      assignment.assignCount++;
    }
  }

  onlyNumbers(event: any): boolean {
    const charCode = (event.which) ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      event.preventDefault();
      return false;
    }
    return true;
  }

  onAssignCountInput(index: number, event: any): void {
    let val = parseInt(event.target.value, 10);
    if (isNaN(val) || val < 0) val = 0;

    const assignment = this.shelfAssignments[index];
    const available = assignment.shelf.maxQte - assignment.shelf.currentQte;

    // First, temporarily set it to 0 to calculate the current assigned excluding this shelf
    const otherAssignedTotal = this.getTotalAssigned() - assignment.assignCount;

    // Max we can assign to this shelf is the minimum of (available on shelf) and (remaining to be assigned)
    const remainingToAssignOverall = this.quantity - otherAssignedTotal;
    const maxPossibleOnThisShelf = Math.min(available, remainingToAssignOverall);

    if (val > maxPossibleOnThisShelf) {
      val = maxPossibleOnThisShelf;
      event.target.value = val;
    }

    assignment.assignCount = val;
  }

  close(): void {
    this.closeEvent.emit(false);
    this.reset();
  }

  // ── Build payloads ────────────────────────────────────────────────────
  private buildPayloads(): Equipment[] {
    const n = Number(this.quantity) || 1;
    const same = this.configMode === 'same';
    // Specs have their own independent mode — must NOT use configMode
    const specSame = this.specMode === 'same' || n === 1;

    const payloads = Array.from({ length: n }, (_, i) => {
      const u = this.units[i] || this.emptyUnit();

      const serial = n === 1 ? this.sharedSerial : u.serialNumber;

      const price = this.sharedPriceMode === 'total'
        ? (this.sharedPrice / n) : this.sharedPrice;

      return {
        equipmentName: same ? this.sharedName : u.name,
        brand: same ? this.sharedBrand : u.brand,
        model: same ? this.sharedModel : u.model,
        // Note is always shared for all units regardless of configMode
        note: this.sharedNotes,
        type: this.type,
        category: this.selectedCategoryName,
        qte: 1,
        serialNumber: serial,
        supplierId: this.purchaseMode === 'same' ? this.sharedSupplierId : u.supplierId,
        supplier: this.purchaseMode === 'same' ? this.sharedSupplier : u.supplier,
        purchaseDate: this.purchaseMode === 'same' ? this.sharedPurchaseDate : u.purchaseDate,
        purchasePrice: this.purchaseMode === 'same' ? price : u.purchasePrice,
        invoiceRef: this.purchaseMode === 'same' ? this.sharedInvoiceRef : u.invoiceRef,
        warrantyExpiration: this.warrantyMode === 'shared' ? this.sharedWarrantyEnd : u.warrantyEnd,
        invoiceFileName: this.purchaseMode === 'same' ? this.sharedInvoiceFileName : u.invoiceFileName,
        invoiceFileData: this.purchaseMode === 'same' ? this.sharedInvoiceFileData : u.invoiceFileData,
        warrantyFileName: this.warrantyMode === 'shared' ? this.sharedWarrantyFileName : u.warrantyFileName,
        warrantyFileData: this.warrantyMode === 'shared' ? this.sharedWarrantyFileData : u.warrantyFileData,
        // Specs use specMode, independently of configMode
        specifications: specSame ? { ...this.sharedSpecifications } : { ...u.specifications },
        department: 'stock',
        status: 'Available',
        shelfId: '' // assigned below
      } as Equipment;
    });

    let unitIndex = 0;
    for (const assignment of this.shelfAssignments) {
      for (let i = 0; i < assignment.assignCount; i++) {
        if (payloads[unitIndex]) {
          payloads[unitIndex].shelfId = assignment.shelf.id;
        }
        unitIndex++;
      }
    }

    return payloads;
  }

  // ── Submit ────────────────────────────────────────────────────────────
  submit(): void {
    this.isSaving = true;
    this.saveError = '';
    const payloads = this.buildPayloads();

    const doBulkSave = (finalPayloads: Equipment[]) => {
      this.equipmentService.createBulkEquipment(finalPayloads).subscribe({
        next: () => {
          this.isSaving = false;
          this.closeEvent.emit(true);
          this.reset();
        },
        error: err => {
          this.isSaving = false;
          this.saveError = 'Failed to save equipment batch. Please try again.';
          console.error(err);
        }
      });
    };

    if (this.typeRequiresQr) {
      // Generate QR codes for all payloads before saving
      const qrPromises = payloads.map((p, i) => {
        // Use a placeholder id for QR content (will be replaced by backend id if needed, 
        // but frontend usually generates based on name/serial)
        const tempId = `NEW-${Date.now()}-${i}`;
        return this.generateQRCodeDataUrl(p, tempId).then(url => {
          p.qrCode = url;
          return p;
        });
      });

      Promise.all(qrPromises).then(updatedPayloads => {
        doBulkSave(updatedPayloads);
      });
    } else {
      // No QR codes — explicit override
      payloads.forEach(p => { p.qrCode = 'NONE'; });
      doBulkSave(payloads);
    }
  }

  private reset(): void {
    this.currentStep = 0; this.quantity = 1; this.category = 'Asset';
    this.type = ''; this.configMode = 'same'; this.specMode = 'same';
    this.sharedName = ''; this.sharedBrand = ''; this.sharedModel = ''; this.sharedNotes = '';
    this.sharedSpecifications = {}; this.specKeys = [];
    this.sharedSerial = '';
    this.purchaseMode = 'same';
    this.sharedPurchaseDate = new Date().toISOString().split('T')[0];
    this.sharedSupplierId = ''; this.sharedSupplier = '';
    this.sharedPriceMode = 'per-unit'; this.sharedPrice = 0; this.sharedInvoiceRef = '';
    // Clear ALL invoice file state — including fileData to prevent stale icon on next open
    this.sharedInvoiceFile = null; this.sharedInvoiceFileName = ''; this.sharedInvoiceFileData = '';
    this.warrantyMode = 'shared'; this.sharedWarrantyEnd = '';
    // Clear ALL warranty file state — including fileData to prevent stale icon on next open
    this.sharedWarrantyFile = null; this.sharedWarrantyFileName = ''; this.sharedWarrantyFileData = '';
    this.availableShelves = []; this.shelfAssignments = [];
    this.units = []; this.isSaving = false; this.saveError = '';
    this.prefillData = null;
    this.reviewCurrentPage = 1;
  }

  // ── QR Code generation ────────────────────────────────────────────────
  private async generateQRCodeDataUrl(payload: Equipment, tempId: string): Promise<string> {
    const qrData = JSON.stringify({
      id: tempId,
      name: payload.equipmentName,
      serial: payload.serialNumber,
      shelfId: payload.shelfId
    });
    try {
      return await QRCode.toDataURL(qrData, { width: 240, margin: 1, color: { dark: '#1e293b', light: '#ffffff' } });
    } catch (err) {
      console.error('QR generation failed', err);
      return '';
    }
  }

  // ── Review helpers ────────────────────────────────────────────────────
  get reviewUnits(): { name: string; serial: string; brand: string; model: string; specs: string }[] {
    return Array.from({ length: this.quantity }, (_, i) => {
      const u = this.units[i];
      const serial = this.quantity === 1 ? (this.sharedSerial || '-') : (u?.serialNumber || '-');
      return {
        name: this.configMode === 'same' ? (this.sharedName || `Unit ${i + 1}`) : (u?.name || `Unit ${i + 1}`),
        serial: serial,
        brand: this.configMode === 'same' ? (this.sharedBrand || '-') : (u?.brand || '-'),
        model: this.configMode === 'same' ? (this.sharedModel || '-') : (u?.model || '-'),
        specs: this.specMode === 'same' 
          ? (Object.values(this.sharedSpecifications).filter(v => !!v).join(', ') || '-') 
          : (Object.values(u?.specifications || {}).filter(v => !!v).join(', ') || '-')
      };
    });
  }

  get pagedReviewUnits(): { name: string; serial: string; brand: string; model: string; specs: string }[] {
    const start = (this.reviewCurrentPage - 1) * this.reviewPageSize;
    return this.reviewUnits.slice(start, start + this.reviewPageSize);
  }

  get totalReviewPages(): number {
    return Math.ceil(this.quantity / this.reviewPageSize);
  }

  get reviewShowingStart(): number {
    return ((this.reviewCurrentPage - 1) * this.reviewPageSize) + 1;
  }

  get reviewShowingEnd(): number {
    const end = this.reviewCurrentPage * this.reviewPageSize;
    return end > this.quantity ? this.quantity : end;
  }

  get reviewPages(): (number | string)[] {
    const total = this.totalReviewPages;
    const current = this.reviewCurrentPage;
    const pages: (number | string)[] = [];

    if (total <= 7) {
      return Array.from({ length: total }, (_, i) => i + 1);
    }

    // Always show first two
    pages.push(1, 2);

    if (current > 4) {
      pages.push('...');
    }

    // Show current and neighbors
    const start = Math.max(3, current - 1);
    const end = Math.min(total - 2, current + 1);

    for (let i = start; i <= end; i++) {
      if (!pages.includes(i)) pages.push(i);
    }

    if (current < total - 3) {
      pages.push('...');
    }

    // Always show last two
    if (!pages.includes(total - 1)) pages.push(total - 1);
    if (!pages.includes(total)) pages.push(total);

    return pages;
  }

  changeReviewPage(page: number | string): void {
    if (typeof page === 'number' && page >= 1 && page <= this.totalReviewPages) {
      this.reviewCurrentPage = page;
    }
  }

  nextReviewPage(): void {
    if (this.reviewCurrentPage < this.totalReviewPages) {
      this.reviewCurrentPage++;
    }
  }

  prevReviewPage(): void {
    if (this.reviewCurrentPage > 1) {
      this.reviewCurrentPage--;
    }
  }

  // ── File Upload Handlers ────────────────────────────────────────────
  onInvoiceFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];

    // Set filenames immediately for UI feedback
    if (this.purchaseMode === 'same' || this.quantity === 1) {
      this.sharedInvoiceFile = file;
      this.sharedInvoiceFileName = file.name;
    } else {
      this.units.forEach(u => { if (u.selectedForInvoice) u.invoiceFileName = file.name; });
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (this.purchaseMode === 'same' || this.quantity === 1) {
        this.sharedInvoiceFileData = dataUrl;
      } else {
        let assigned = false;
        this.units.forEach(u => {
          if (u.selectedForInvoice) {
            u.invoiceFile = file;
            u.invoiceFileData = dataUrl;
            u.selectedForInvoice = false;
            assigned = true;
          }
        });
        if (!assigned) {
          this.units.forEach(u => {
            u.invoiceFile = file;
            u.invoiceFileName = file.name;
            u.invoiceFileData = dataUrl;
          });
        }
      }
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  onWarrantyFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;
    const file = input.files[0];

    // Set filenames immediately for UI feedback
    if (this.warrantyMode === 'shared' || this.quantity === 1) {
      this.sharedWarrantyFile = file;
      this.sharedWarrantyFileName = file.name;
    } else {
      this.units.forEach(u => { if (u.selectedForWarranty) u.warrantyFileName = file.name; });
    }

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      if (this.warrantyMode === 'shared' || this.quantity === 1) {
        this.sharedWarrantyFileData = dataUrl;
      } else {
        let assigned = false;
        this.units.forEach(u => {
          if (u.selectedForWarranty) {
            u.warrantyFile = file;
            u.warrantyFileData = dataUrl;
            u.selectedForWarranty = false;
            assigned = true;
          }
        });
        if (!assigned) {
          this.units.forEach(u => {
            u.warrantyFile = file;
            u.warrantyFileName = file.name;
            u.warrantyFileData = dataUrl;
          });
        }
      }
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  removeSharedInvoice(event?: Event): void {
    if (event) event.stopPropagation();
    this.sharedInvoiceFile = null;
    this.sharedInvoiceFileName = '';
    this.sharedInvoiceFileData = '';
  }

  removeUnitInvoice(unit: UnitRow, event?: Event): void {
    if (event) event.stopPropagation();
    unit.invoiceFile = undefined;
    unit.invoiceFileName = '';
    unit.invoiceFileData = '';
  }

  removeSharedWarranty(event?: Event): void {
    if (event) event.stopPropagation();
    this.sharedWarrantyFile = null;
    this.sharedWarrantyFileName = '';
    this.sharedWarrantyFileData = '';
  }

  removeUnitWarranty(unit: UnitRow, event?: Event): void {
    if (event) event.stopPropagation();
    unit.warrantyFile = undefined;
    unit.warrantyFileName = '';
    unit.warrantyFileData = '';
  }

  // ── Sync Logic ────────────────────────────────────────────────────────
  onUnitSyncCheck(index: number, type: 'general' | 'specs' | 'purchase' | 'warranty'): void {
    const unit = this.units[index];
    if (type === 'general') {
      if (unit.selectedForGeneralSync) {
        const sourceIndex = this.units.findIndex((u, i) => i !== index && u.selectedForGeneralSync);
        if (sourceIndex > -1) {
          const src = this.units[sourceIndex];
          unit.name = src.name;
          unit.brand = src.brand;
          unit.model = src.model;
        }
      }
    } else if (type === 'specs') {
      if (unit.selectedForSpecSync) {
        const sourceIndex = this.units.findIndex((u, i) => i !== index && u.selectedForSpecSync);
        if (sourceIndex > -1) {
          const src = this.units[sourceIndex];
          unit.specifications = { ...src.specifications };
        }
      }
    } else if (type === 'purchase') {
      if (unit.selectedForInvoice) {
        const sourceIndex = this.units.findIndex((u, i) => i !== index && u.selectedForInvoice);
        if (sourceIndex > -1) {
          const src = this.units[sourceIndex];
          unit.purchaseDate = src.purchaseDate;
          unit.supplierId = src.supplierId;
          unit.supplier = src.supplier;
          unit.purchasePrice = src.purchasePrice;
          unit.invoiceRef = src.invoiceRef;
          unit.invoiceFile = src.invoiceFile;
          unit.invoiceFileName = src.invoiceFileName;
        }
      }
    } else if (type === 'warranty') {
      if (unit.selectedForWarranty) {
        const sourceIndex = this.units.findIndex((u, i) => i !== index && u.selectedForWarranty);
        if (sourceIndex > -1) {
          const src = this.units[sourceIndex];
          unit.warrantyEnd = src.warrantyEnd;
          unit.warrantyFile = src.warrantyFile;
          unit.warrantyFileName = src.warrantyFileName;
        }
      }
    }
  }

  // ── Bulk Actions ────────────────────────────────────────────────────────
  clearSelectedRows(type: 'general' | 'specs' | 'purchase' | 'warranty'): void {
    this.units.forEach(u => {
      switch (type) {
        case 'general':
          if (u.selectedForGeneralSync) { u.name = ''; u.brand = ''; u.model = ''; }
          break;
        case 'specs':
          if (u.selectedForSpecSync) {
            u.specifications = {};
            this.specKeys.forEach(k => u.specifications[k] = '');
            u.serialNumber = '';
          }
          break;
        case 'purchase':
          if (u.selectedForInvoice) {
            u.purchaseDate = ''; u.supplierId = ''; u.supplier = ''; u.purchasePrice = 0; u.invoiceRef = '';
            u.invoiceFile = undefined; u.invoiceFileName = '';
          }
          break;
        case 'warranty':
          if (u.selectedForWarranty) {
            u.warrantyEnd = '';
            u.warrantyFile = undefined; u.warrantyFileName = '';
          }
          break;
      }
    });
  }

  previewFile(file?: File, fileData?: string, fileName?: string): void {
    if (file) {
      const url = URL.createObjectURL(file);
      window.open(url, '_blank');
    } else if (fileData) {
      try {
        if (fileData.startsWith('data:')) {
          const byteString = atob(fileData.split(',')[1]);
          const mimeString = fileData.split(',')[0].split(':')[1].split(';')[0];
          const ab = new ArrayBuffer(byteString.length);
          const ia = new Uint8Array(ab);
          for (let i = 0; i < byteString.length; i++) {
              ia[i] = byteString.charCodeAt(i);
          }
          const blob = new Blob([ab], {type: mimeString});
          const url = URL.createObjectURL(blob);
          window.open(url, '_blank');
        } else {
          window.open(fileData, '_blank');
        }
      } catch (e) {
        this.downloadDocument(fileData, fileName);
      }
    }
  }

  downloadDocument(fileData?: string, fileName?: string): void {
    if (!fileData || !fileName) return;
    const a = document.createElement('a');
    a.href = fileData;
    a.download = fileName;
    a.click();
  }
}
