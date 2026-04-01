import { Component, OnInit, Input, Output, EventEmitter, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Equipment } from '../equipment.model';
import { EquipmentService } from '../equipment.service';
import { AuthService } from '../../auth.service';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
import { ShelfService } from '../../shelf/shelf.service';
import { Shelf } from '../../shelf/shelf.model';
import * as QRCode from 'qrcode';

@Component({
  selector: 'app-equipment-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-form.component.html',
  styleUrl: './equipment-form.component.css'
})
export class EquipmentFormComponent implements OnInit, AfterViewInit {
  @Input() equipment: Equipment | null = null;
  @Input() viewOnly: boolean = false;
  @Input() isAddSimilar: boolean = false;
  @Output() closeEvent = new EventEmitter<boolean>();

  formData: Partial<Equipment> = {};
  isSaving: boolean = false;
  qrDataUrl: string = '';
  isUserName: string = '';
  isEditing: boolean = false;
  suppliers: Supplier[] = [];
  availableShelves: Shelf[] = [];
  allShelves: Shelf[] = [];
  currentUserName: string = '';
  activeTab: string = 'overview';
  isSNAvailable: boolean = true;
  isCheckingSN: boolean = false;
  originalSN: string = '';
  private snSubject = new Subject<string>();

  equipmentTypes = [
    'pc', 'laptop', 'server', 'monitor', 'printer', 'scanner', 
    'projector', 'router', 'switch', 'ups', 'tablet', 'phone', 
    'ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'
  ];
  consumables = ['ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'];
  statusOptions = ['In Stock', 'Broken', 'Maintenance', 'Out of Stock'];


  constructor(
    private equipmentService: EquipmentService,
    private authService: AuthService,
    private supplierService: SupplierService,
    private shelfService: ShelfService
  ) {}

  ngOnInit(): void {
    const userData = this.authService.getCurrentUser();
    this.currentUserName = userData?.firstName
      ? `${userData.firstName} ${userData.lastName || ''}`.trim()
      : (userData?.email || 'Unknown');

    this.loadSuppliers();
    this.loadAllShelves();

    // Setup debounced SN uniqueness check
    this.snSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(sn => {
      this.performSNUniquenessCheck(sn);
    });

    if (this.equipment && this.equipment.id) {
      this.equipmentService.getEquipmentById(this.equipment.id).subscribe({
        next: (fullEq) => {
          this.formData = { ...fullEq };
          // If editing existing equipment, remember original SN to skip uniqueness check if unchanged
          // If adding similar, we ignore original SN as we need a NEW unique one
          this.originalSN = !this.isAddSimilar ? (fullEq.serialNumber || '') : '';
          
          if (this.formData.type) {
            this.loadAvailableShelves();
          }
        },
        error: (err) => {
          console.error('Error fetching full equipment detail', err);
          this.formData = { ...this.equipment! };
        }
      });
    } else {
      this.formData = {
        equipmentName: '',
        brand: '',
        model: '',
        serialNumber: '',
        type: '',
        category: '',
        qte: 1,
        supplier: '',
        supplierId: '',
        shelfId: '',
        department: 'stock',
        note: '',
        purchasePrice: 0,
        purchaseDate: this.formatDate(new Date()),
        warrantyExpiration: '',
        createdBy: this.currentUserName,
        cpu: '',
        ram: '',
        storage: '',
        graphicsCard: '',
        operatingSystem: '',
        status: 'In Stock'
      };
    }
  }

  loadSuppliers(): void {
    this.supplierService.getAllSuppliers().subscribe({
      next: (data) => this.suppliers = data,
      error: (err) => console.error('Error fetching suppliers', err)
    });
  }

  onTypeChange(): void {
    // Auto-set category based on type
    if (this.formData.type) {
      this.formData.category = this.consumables.includes(this.formData.type) ? 'Consumable' : 'Asset';
      this.loadAvailableShelves();
    }
  }

  onStatusChange(): void {
    const status = this.formData.status;
    if (status === 'Maintenance') {
      this.formData.shelfId = 'MAINTENANCE_AREA';
    } else if (status === 'Broken') {
      this.formData.shelfId = 'SCRAP_YARD';
    } else if (status === 'Out of Stock') {
      this.formData.shelfId = 'OUT_OF_STOCK';
    } else if (status === 'In Stock') {
      // If switching back to In Stock from a virtual area, reset shelf
      if (this.formData.shelfId === 'MAINTENANCE_AREA' || 
          this.formData.shelfId === 'SCRAP_YARD' || 
          this.formData.shelfId === 'OUT_OF_STOCK') {
        this.formData.shelfId = '';
        this.loadAvailableShelves(); // Refresh available options
      }
    }
  }


  loadAvailableShelves(): void {
    if (!this.formData.type) return;
    this.shelfService.getShelvesByType(this.formData.type).subscribe({
      next: (shelves) => {
        this.availableShelves = shelves;
        this.filterShelvesByQte();
      },
      error: (err) => console.error('Error fetching shelves', err)
    });
  }

  suggestSerialNumber(): void {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const digits = '0123456789';
    let result = '';
    
    // Generate exactly 10 random alphanumeric chars
    const chars = letters + digits;
    for (let i = 0; i < 10; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    
    // Ensure it's never just letters or just digits (force at least one of each)
    if (!/\d/.test(result)) {
      result = result.substring(0, 9) + digits.charAt(Math.floor(Math.random() * digits.length));
    }
    if (!/[a-zA-Z]/.test(result)) {
      result = result.substring(0, 9) + letters.charAt(Math.floor(Math.random() * letters.length));
    }
    
    this.formData.serialNumber = result;
    this.onSerialNumberChange(); // Trigger uniqueness check for suggested SN
  }

  onSerialNumberChange(): void {
    if (this.formData.serialNumber) {
      // Force uppercase and trim
      this.formData.serialNumber = this.formData.serialNumber.toUpperCase().trim();
      
      const currentSN = this.formData.serialNumber;

      // If it's the same as the original, it's valid by default
      if (currentSN === this.originalSN) {
        this.isSNAvailable = true;
        this.isCheckingSN = false;
        return;
      }

      // Reset availability while checking
      if (this.isSerialNumberValid()) {
        this.snSubject.next(currentSN);
      } else {
        this.isSNAvailable = true; // Don't show "not unique" if it's already invalid for other reasons
      }
    }
  }

  private performSNUniquenessCheck(sn: string): void {
    if (!sn || sn.length !== 10) return;

    this.isCheckingSN = true;
    const excludeId = this.equipment?.id || undefined;

    this.equipmentService.checkSerialNumberUnique(sn, excludeId).subscribe({
      next: (isUnique) => {
        this.isSNAvailable = isUnique;
        this.isCheckingSN = false;
      },
      error: (err) => {
        console.error('Error checking SN uniqueness', err);
        this.isCheckingSN = false;
        this.isSNAvailable = true; // Assume available on error to not block user
      }
    });
  }

  get isSNInvalidLength(): boolean {
    const sn = this.formData.serialNumber || '';
    return sn.length > 0 && sn.length !== 10;
  }

  get isSNInvalidFormat(): boolean {
    const sn = this.formData.serialNumber || '';
    if (sn.length === 0) return false;
    return !/^[a-zA-Z0-9]+$/.test(sn);
  }

  get isSNMissingChars(): boolean {
    const sn = this.formData.serialNumber || '';
    if (sn.length !== 10 || this.isSNInvalidFormat) return false;
    return !/[a-zA-Z]/.test(sn) || !/\d/.test(sn);
  }

  isSerialNumberValid(): boolean {
    const sn = this.formData.serialNumber || '';
    if (!sn) return false;
    
    const hasExactLength = sn.length === 10;
    const onlyAlphanumeric = /^[a-zA-Z0-9]+$/.test(sn);
    const hasLetter = /[a-zA-Z]/.test(sn);
    const hasDigit = /\d/.test(sn);
    
    return hasExactLength && onlyAlphanumeric && hasLetter && hasDigit;
  }

  get isFormInvalid(): boolean {
    const isBasicInfoMissing = !this.formData.equipmentName || !this.formData.brand || !this.isSerialNumberValid();
    const isShelfMissingForInStock = this.formData.status === 'In Stock' && (!this.formData.shelfId || this.formData.shelfId === '');
    return isBasicInfoMissing || isShelfMissingForInStock || !this.isSNAvailable || this.isCheckingSN;
  }

  loadAllShelves(): void {
    this.shelfService.getAllShelves().subscribe({
      next: (data) => this.allShelves = data,
      error: (err) => console.error('Error fetching all shelves', err)
    });
  }

  filterShelvesByQte(): void {
    const qteNeeded = this.formData.qte || 1;
    // Filter shelves that have enough capacity and are not full
    let validShelves = this.availableShelves.filter(s => 
      s.status !== 'FULL' && (s.currentQte + qteNeeded <= s.maxQte)
    );
    
    this.availableShelves = validShelves;

    // Auto select if only one is available
    if (this.availableShelves.length === 1 && !this.formData.shelfId) {
      this.formData.shelfId = this.availableShelves[0].id;
    } else if (this.availableShelves.length === 0) {
      this.formData.shelfId = '';
    }
  }

  onSupplierChange(): void {
    const selectedSupplier = this.suppliers.find(s => s.id === this.formData.supplierId);
    if (selectedSupplier) {
      this.formData.supplier = selectedSupplier.companyName;
    }
  }

  ngAfterViewInit(): void {
    if (this.equipment?.id) {
      this.generateQRCode();
    }
  }

  generateQRCode(): void {
    if (!this.equipment?.id) return;
    const qrData = JSON.stringify({
      id: this.equipment.id,
      name: this.equipment.equipmentName,
      serial: this.equipment.serialNumber,
      shelfId: this.equipment.shelfId
    });
    QRCode.toDataURL(qrData, { width: 160, margin: 1, color: { dark: '#1e293b', light: '#ffffff' } })
      .then((url: string) => { this.qrDataUrl = url; })
      .catch((err: any) => console.error('QR generation failed', err));
  }

  downloadQR(): void {
    if (!this.qrDataUrl) return;
    const a = document.createElement('a');
    a.href = this.qrDataUrl;
    a.download = `QR_${this.equipment?.equipmentName || 'equipment'}.png`;
    a.click();
  }

  printQR(): void {
    if (!this.qrDataUrl) return;
    const win = window.open('', '_blank');
    if (!win) return;
    win.document.write(`
      <html><head><title>QR Label - ${this.equipment?.equipmentName}</title>
      <style>
        body { font-family: Arial, sans-serif; display: flex; flex-direction: column; align-items: center; padding: 20px; }
        h2 { margin: 10px 0 4px; font-size: 16px; }
        p { margin: 2px 0; font-size: 12px; color: #64748b; }
        img { border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px; }
      </style></head><body>
        <img src="${this.qrDataUrl}" width="160" height="160" />
        <h2>${this.equipment?.equipmentName}</h2>
        <p>ID: ${this.equipment?.id}</p>
        <p>S/N: ${this.equipment?.serialNumber || 'N/A'}</p>
        <p>Shelf ID: ${this.equipment?.shelfId || 'N/A'}</p>
      </body></html>
    `);
    win.document.close();
    win.focus();
    win.print();
    win.close();
  }

  saveEquipment(): void {
    this.isSaving = true;
    const payload = this.formData as Equipment;
    payload.qte = 1; // Enforce single unit management

    if (this.equipment && this.equipment.id) {
      this.equipmentService.updateEquipment(this.equipment.id, payload).subscribe({
        next: () => { this.isSaving = false; this.closeEvent.emit(true); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    } else {
      this.equipmentService.createEquipment(payload).subscribe({
        next: () => { this.isSaving = false; this.closeEvent.emit(true); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    }
  }

  enableEdit(): void {
    this.isEditing = true;
  }

  cancel(): void {
    if (this.viewOnly && this.isEditing) {
      // reset to original data and go back to view-only
      this.formData = { ...this.equipment };
      if (this.formData.type) {
        this.loadAvailableShelves();
      }
      this.isEditing = false;
    } else {
      this.closeEvent.emit(false);
    }
  }

  onInvoiceFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.formData.invoiceFileName = file.name;
    
    const reader = new FileReader();
    reader.onload = () => {
      this.formData.invoiceFileData = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  onWarrantyFileUpload(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) return;
    const file = input.files[0];
    this.formData.warrantyFileName = file.name;
    
    const reader = new FileReader();
    reader.onload = () => {
      this.formData.warrantyFileData = reader.result as string;
    };
    reader.readAsDataURL(file);
  }

  downloadDocument(fileData?: string, fileName?: string): void {
    if (!fileData || !fileName) return;
    const a = document.createElement('a');
    a.href = fileData;
    a.download = fileName;
    a.click();
  }

  getShelfLocation(shelfId?: string): string {
    if (!shelfId || shelfId === '') return 'Unassigned';
    if (shelfId === 'MAINTENANCE_AREA') return 'Maintenance Area';
    if (shelfId === 'SCRAP_YARD') return 'Scrap Yard';
    if (shelfId === 'OUT_OF_STOCK') return 'Out of Stock';
    
    const s = this.allShelves.find(x => x.id === shelfId);
    return s ? `Shelf ${s.nb}` : shelfId;
  }

  onDetailInvoiceUpdate(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.equipment?.id) return;
    const file = input.files[0];
    
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const updateData: any = { 
        ...this.equipment,
        invoiceFileName: file.name,
        invoiceFileData: dataUrl
      };
      this.equipmentService.updateEquipment(this.equipment!.id!, updateData).subscribe({
        next: (res) => {
          this.equipment = res;
          this.formData = { ...res };
          input.value = '';
        },
        error: (err) => console.error('Error updating invoice from detail', err)
      });
    };
    reader.readAsDataURL(file);
  }

  onDetailWarrantyUpdate(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0 || !this.equipment?.id) return;
    const file = input.files[0];
    
    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const updateData: any = { 
        ...this.equipment,
        warrantyFileName: file.name,
        warrantyFileData: dataUrl
      };
      this.equipmentService.updateEquipment(this.equipment!.id!, updateData).subscribe({
        next: (res) => {
          this.equipment = res;
          this.formData = { ...res };
          input.value = '';
        },
        error: (err) => console.error('Error updating warranty from detail', err)
      });
    };
    reader.readAsDataURL(file);
  }

  isComputerCategory(): boolean {
    const type = this.formData.type || '';
    return ['pc', 'laptop', 'server', 'tablet', 'phone'].includes(type);
  }

  isWarrantyExpired(): boolean {
    if (!this.equipment?.warrantyExpiration) return false;
    return new Date(this.equipment.warrantyExpiration) < new Date();
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
