import {
  Component, OnInit, Input, Output, EventEmitter, OnChanges, SimpleChanges
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin, Observable } from 'rxjs';
import { Equipment } from '../equipment.model';
import { EquipmentService } from '../equipment.service';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ShelfService } from '../../shelf/shelf.service';
import { Shelf } from '../../shelf/shelf.model';

// ─── Interfaces ───────────────────────────────────────────────────────────
export interface UnitRow {
  name: string;
  brand: string;
  model: string;
  cpu: string;
  ram: string;
  storage: string;
  graphicsCard: string;
  operatingSystem: string;
  networkInterface: string;
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

  // ── External data ──────────────────────────────────────────────────────
  suppliers: Supplier[] = [];

  // ── Step 0: Setup ─────────────────────────────────────────────────────
  quantity: number = 1;
  category: 'Asset' | 'Consumable' = 'Asset';
  type: string = '';
  configMode: 'same' | 'different' = 'same';

  // ── Step 1: General Info ──────────────────────────────────────────────
  sharedName: string = '';
  sharedBrand: string = '';
  sharedModel: string = '';
  sharedNotes: string = '';

  // ── Step 2: Specifications ────────────────────────────────────────────
  specMode: 'same' | 'different' = 'same';
  sharedCpu: string = '';
  sharedRam: string = '';
  sharedStorage: string = '';
  sharedGpu: string = '';
  sharedOs: string = '';
  sharedNetInterface: string = '';
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

  constructor(
    private equipmentService: EquipmentService,
    private supplierService: SupplierService,
    private shelfService: ShelfService
  ) { }

  ngOnInit(): void {
    this.supplierService.getAllSuppliers().subscribe({ next: d => this.suppliers = d });
    this.sharedPurchaseDate = new Date().toISOString().split('T')[0];
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['prefillData'] && this.prefillData) {
      this.populateFromPrefill(this.prefillData);
    }
  }

  private populateFromPrefill(data: Equipment): void {
    this.type = data.type || '';
    this.category = data.category as 'Asset' | 'Consumable';
    this.sharedName = data.equipmentName || '';
    this.sharedBrand = data.brand || '';
    this.sharedModel = data.model || '';
    this.sharedNotes = data.note || '';
    
    // Specs
    this.sharedCpu = data.cpu || '';
    this.sharedRam = data.ram || '';
    this.sharedStorage = data.storage || '';
    this.sharedGpu = data.graphicsCard || '';
    this.sharedOs = data.operatingSystem || '';
    
    // Purchase
    this.sharedPurchaseDate = data.purchaseDate ? data.purchaseDate.toString().split('T')[0] : '';
    this.sharedSupplierId = data.supplierId || '';
    this.sharedSupplier = data.supplier || '';
    this.sharedPrice = data.purchasePrice || 0;
    this.sharedPriceMode = 'per-unit';
    this.sharedInvoiceFileName = data.invoiceFileName || '';
    this.sharedInvoiceFileData = data.invoiceFileData || '';
    
    // Warranty
    this.sharedWarrantyEnd = data.warrantyExpiration ? data.warrantyExpiration.toString().split('T')[0] : '';
    this.sharedWarrantyFileName = data.warrantyFileName || '';
    this.sharedWarrantyFileData = data.warrantyFileData || '';

    // Quantity & Serial (User requested empty serial)
    this.quantity = 1;
    this.sharedSerial = '';
    this.configMode = 'same';
    this.specMode = 'same';
    this.purchaseMode = 'same';
    this.warrantyMode = 'shared';
    this.currentStep = 0;
  }

  // ── Computed helpers ──────────────────────────────────────────────────
  get isComputerType(): boolean { return this.computerTypes.includes(this.type); }
  get isConsumable(): boolean { return this.consumableTypes.includes(this.type); }
  get totalSteps(): number { return 8; }   // 0‑7
  get progressPct(): number { return Math.round((this.currentStep / 7) * 100); }
  get perUnitPrice(): number {
    if (this.sharedPriceMode === 'per-unit') return this.sharedPrice;
    return this.quantity ? this.sharedPrice / this.quantity : 0;
  }

  // ── Step 0 handlers ───────────────────────────────────────────────────
  onTypeChange(): void {
    if (this.consumableTypes.includes(this.type)) {
      this.category = 'Consumable';
    } else {
      this.category = 'Asset';
    }
  }

  onQuantityChange(): void {
    this.rebuildUnits();
  }

  private rebuildUnits(): void {
    const n = Number(this.quantity) || 1;
    while (this.units.length < n) this.units.push(this.emptyUnit());
    this.units = this.units.slice(0, n);
  }

  private emptyUnit(): UnitRow {
    return {
      name: '', brand: '', model: '',
      cpu: '', ram: '', storage: '', graphicsCard: '', operatingSystem: '', networkInterface: '',
      serialNumber: '',
      purchaseDate: this.sharedPurchaseDate, supplierId: '', supplier: '', purchasePrice: 0, invoiceRef: '',
      warrantyEnd: '',
      selectedForInvoice: false, selectedForWarranty: false,
      selectedForGeneralSync: false, selectedForSpecSync: false
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

  // ── Validation ────────────────────────────────────────────────────────
  isStepValid(): boolean {
    switch (this.currentStep) {
      case 0: return !!this.type && this.quantity >= 1;
      case 1: return this.configMode === 'same'
        ? !!this.sharedName && !!this.sharedBrand
        : this.units.every(u => !!u.name && !!u.brand);
      case 2:
        return true;
      case 3:
        if (this.category === 'Consumable') return true;
        if (this.quantity === 1) return !!this.sharedSerial;
        return this.units.length > 0 && this.units.every(u => !!u.serialNumber);
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
      if (this.category === 'Consumable' || !this.isComputerType) {
        this.currentStep = 3; return;
      }
    }
    if (this.currentStep === 2) {
      if (this.specMode === 'different') {
        this.currentStep = 4; return;
      }
    }
    if (this.currentStep === 4) {
      if (this.category === 'Consumable') {
        this.currentStep = 6;
        this.loadShelves();
        return;
      }
    }
    if (this.currentStep < 7) {
      this.currentStep++;
      if (this.currentStep === 6) this.loadShelves();
    }
  }

  prevStep(): void {
    if (this.currentStep === 3) {
      if (this.category === 'Consumable' || !this.isComputerType) {
        this.currentStep = 1; return;
      }
    }
    if (this.currentStep === 4) {
      if (this.isComputerType && this.specMode === 'different') {
        this.currentStep = 2; return;
      }
    }
    if (this.currentStep === 6) {
      if (this.category === 'Consumable') {
        this.currentStep = 4; return;
      }
    }
    if (this.currentStep > 0) this.currentStep--;
  }

  // ── Step 6 helpers ────────────────────────────────────────────────────
  getTotalAssigned(): number {
    return this.shelfAssignments.reduce((sum, a) => sum + (a.assignCount || 0), 0);
  }

  loadShelves(): void {
    this.shelfService.getShelvesByType(this.type).subscribe({
      next: shelves => {
        this.availableShelves = shelves.filter(s => s.maxQte - s.currentQte > 0);
        this.shelfAssignments = this.availableShelves.map(s => ({ shelf: s, assignCount: 0 }));
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

  close(): void {
    this.closeEvent.emit(false);
    this.reset();
  }

  // ── Build payloads ────────────────────────────────────────────────────
  private buildPayloads(): Equipment[] {
    const n = Number(this.quantity) || 1;
    const same = this.configMode === 'same';

    const payloads = Array.from({ length: n }, (_, i) => {
      const u = this.units[i] || this.emptyUnit();

      const serial = this.isConsumable ? this.sharedSerial
        : (n === 1 ? this.sharedSerial : u.serialNumber);

      const price = this.sharedPriceMode === 'total'
        ? (this.sharedPrice / n) : this.sharedPrice;

      return {
        equipmentName: same ? this.sharedName : u.name,
        brand: same ? this.sharedBrand : u.brand,
        model: same ? this.sharedModel : u.model,
        note: same ? this.sharedNotes : '',
        type: this.type,
        category: this.category,
        qte: 1,
        serialNumber: serial,
        supplierId: this.purchaseMode === 'same' ? this.sharedSupplierId : u.supplierId,
        supplier: this.purchaseMode === 'same' ? this.sharedSupplier : u.supplier,
        purchaseDate: this.purchaseMode === 'same' ? this.sharedPurchaseDate : u.purchaseDate,
        purchasePrice: this.purchaseMode === 'same' ? price : u.purchasePrice,
        warrantyExpiration: this.warrantyMode === 'shared' ? this.sharedWarrantyEnd : u.warrantyEnd,
        invoiceFileName: this.purchaseMode === 'same' ? this.sharedInvoiceFileName : u.invoiceFileName,
        invoiceFileData: this.purchaseMode === 'same' ? this.sharedInvoiceFileData : u.invoiceFileData,
        warrantyFileName: this.warrantyMode === 'shared' ? this.sharedWarrantyFileName : u.warrantyFileName,
        warrantyFileData: this.warrantyMode === 'shared' ? this.sharedWarrantyFileData : u.warrantyFileData,
        cpu: same ? this.sharedCpu : u.cpu,
        ram: same ? this.sharedRam : u.ram,
        storage: same ? this.sharedStorage : u.storage,
        graphicsCard: same ? this.sharedGpu : u.graphicsCard,
        operatingSystem: same ? this.sharedOs : u.operatingSystem,
        department: 'stock',
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
    const requests: Observable<Equipment>[] = payloads.map(p => this.equipmentService.createEquipment(p));

    forkJoin(requests).subscribe({
      next: () => {
        this.isSaving = false;
        this.closeEvent.emit(true);
        this.reset();
      },
      error: err => {
        this.isSaving = false;
        this.saveError = 'Failed to save some equipment. Please try again.';
        console.error(err);
      }
    });
  }

  private reset(): void {
    this.currentStep = 0; this.quantity = 1; this.category = 'Asset';
    this.type = ''; this.configMode = 'same'; this.specMode = 'same';
    this.sharedName = ''; this.sharedBrand = ''; this.sharedModel = ''; this.sharedNotes = '';
    this.sharedCpu = ''; this.sharedRam = ''; this.sharedStorage = '';
    this.sharedGpu = ''; this.sharedOs = ''; this.sharedNetInterface = '';
    this.sharedSerial = '';
    this.purchaseMode = 'same';
    this.sharedPurchaseDate = new Date().toISOString().split('T')[0];
    this.sharedSupplierId = ''; this.sharedSupplier = '';
    this.sharedPriceMode = 'per-unit'; this.sharedPrice = 0; this.sharedInvoiceRef = '';
    this.sharedInvoiceFile = null; this.sharedInvoiceFileName = '';
    this.warrantyMode = 'shared'; this.sharedWarrantyEnd = '';
    this.sharedWarrantyFile = null; this.sharedWarrantyFileName = '';
    this.availableShelves = []; this.shelfAssignments = [];
    this.units = []; this.isSaving = false; this.saveError = '';
    this.prefillData = null;
  }

  // ── Review helpers ────────────────────────────────────────────────────
  get reviewUnits(): { name: string; serial: string }[] {
    return Array.from({ length: this.quantity }, (_, i) => {
      const u = this.units[i];
      const serial = this.isConsumable ? this.sharedSerial
        : (this.quantity === 1 ? this.sharedSerial : (u?.serialNumber || '—'));
      return {
        name: this.configMode === 'same' ? this.sharedName : (u?.name || `Unit ${i + 1}`),
        serial: serial
      };
    });
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
          unit.cpu = src.cpu;
          unit.ram = src.ram;
          unit.storage = src.storage;
          unit.graphicsCard = src.graphicsCard;
          unit.operatingSystem = src.operatingSystem;
          unit.networkInterface = src.networkInterface;
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
            u.cpu = ''; u.ram = ''; u.storage = '';
            u.graphicsCard = ''; u.operatingSystem = ''; u.networkInterface = '';
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

  previewFile(file?: File): void {
    if (!file) return;
    const url = URL.createObjectURL(file);
    window.open(url, '_blank');
  }

  downloadDocument(fileData?: string, fileName?: string): void {
    if (!fileData || !fileName) return;
    const a = document.createElement('a');
    a.href = fileData;
    a.download = fileName;
    a.click();
  }
}
