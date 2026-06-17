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
import { CategoryService } from '../../category-manager/category.service';
import { CategoryType, EquipmentCategory } from '../../category-manager/category.model';
import { ToastService } from '../../shared/toast.service';
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
  @Input() hideEdit: boolean = false;
  @Input() isAddSimilar: boolean = false;
  @Output() closeEvent = new EventEmitter<boolean>();
  @Output() viewOtherEvent = new EventEmitter<string>();

  formData: Partial<Equipment> = {};
  isSaving: boolean = false;
  qrDataUrl: string = '';
  isUserName: string = '';
  isEditing: boolean = false;
  suppliers: Supplier[] = [];
  availableShelves: Shelf[] = [];
  availableTypes: CategoryType[] = [];
  allShelves: Shelf[] = [];
  categories: EquipmentCategory[] = [];
  currentUserName: string = '';
  currentUserRole: string = '';
  activeTab: string = 'overview';
  isSNAvailable: boolean = true;
  isCheckingSN: boolean = false;
  originalSN: string = '';
  formGenerateQr: boolean = false;  // QR code checkbox for edit form
  installedParts: Equipment[] = [];
  showInstallModal: boolean = false;
  isLoadingPartsForInstall: boolean = false;
  availablePartsForInstall: Equipment[] = [];
  selectedPartToInstall: Equipment | null = null;
  selectedPartId: string = '';
  installSpecKey: string = '';
  installFilterCategory: string = '';
  installFilterType: string = '';
  installSearchQuery: string = '';
  installAvailableTypes: any[] = [];
  filteredPartsForInstall: Equipment[] = [];
  private snSubject = new Subject<string>();

  equipmentTypes = [
    'pc', 'laptop', 'server', 'monitor', 'printer', 'scanner',
    'projector', 'router', 'switch', 'ups', 'tablet', 'phone',
    'ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'
  ];
  consumables = ['ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'];
  statusOptions = ['Available', 'Broken', 'Maintenance', 'Out of Stock', 'Installed', 'Unrepairable'];


  constructor(
    private equipmentService: EquipmentService,
    private authService: AuthService,
    private supplierService: SupplierService,
    private shelfService: ShelfService,
    private categoryService: CategoryService,
    private toastService: ToastService
  ) { }

  ngOnInit(): void {
    const userData = this.authService.getCurrentUser();
    this.currentUserRole = userData?.role || '';
    this.currentUserName = userData?.firstName
      ? `${userData.firstName} ${userData.lastName || ''}`.trim()
      : (userData?.email || 'Unknown');

    this.supplierService.getAllSuppliers().subscribe(d => this.suppliers = d);
    this.shelfService.getAllShelves().subscribe(d => this.allShelves = d);

    // Setup debounced SN uniqueness check
    this.snSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(sn => {
      this.performSNUniquenessCheck(sn);
    });

    // Load categories then handle the equipment data
    this.categoryService.getAllCategories().subscribe({
      next: (cats) => {
        this.categories = cats;

        if (this.equipment && this.equipment.id) {
          this.equipmentService.getEquipmentById(this.equipment.id).subscribe({
            next: (fullEq) => {
              this.formData = { ...fullEq };

              if (this.formData.purchaseDate) {
                this.formData.purchaseDate = this.formatDate(this.formData.purchaseDate);
              }
              if (this.formData.warrantyExpiration) {
                this.formData.warrantyExpiration = this.formatDate(this.formData.warrantyExpiration);
              }

              if (this.viewOnly) {
                this.equipment = { ...fullEq }; // Sync for Details view
              }
              this.originalSN = !this.isAddSimilar ? (fullEq.serialNumber || '') : '';
              // Initialise QR checkbox based on whether the equipment already has a QR code
              this.formGenerateQr = !!(fullEq.qrCode);

              // Find the category for this equipment based on its type (case-insensitive)
              const currentType = (this.formData.type || '').toLowerCase().trim();
              const foundCat = this.categories.find(c =>
                c.types?.some(t => t.name.toLowerCase().trim() === currentType)
              );

              if (foundCat) {
                this.formData.category = foundCat.name;
                this.availableTypes = foundCat.types || [];

                // Ensure formData.type matches exactly one of the types in the list for the select to work
                const exactTypeMatch = this.availableTypes.find(t => t.name.toLowerCase().trim() === currentType);
                if (exactTypeMatch) {
                  this.formData.type = exactTypeMatch.name;
                  this.syncSpecificationsWithSchema();
                }
              } else {
                // Fallback for types not in any functional category
                this.availableTypes = this.equipmentTypes.map(name => ({ name, requiresQrCode: false }));
              }

              if (this.formData.type) {
                this.loadAvailableShelves();
              }
              this.loadInstalledParts();
            },
            error: (err) => {
              console.error('Error fetching full equipment detail', err);
              this.formData = { ...this.equipment! };

              if (this.formData.purchaseDate) {
                this.formData.purchaseDate = this.formatDate(this.formData.purchaseDate);
              }
              if (this.formData.warrantyExpiration) {
                this.formData.warrantyExpiration = this.formatDate(this.formData.warrantyExpiration);
              }

            }
          });
        }
      },
      error: (err) => console.error('Error fetching categories', err)
    });

    if (!this.equipment || !this.equipment.id) {
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
        specifications: {},
        invoiceRef: '',
        status: 'Available'
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
    if (!this.formData.type) return;

    // Update specifications based on type schema
    const typeObj = this.availableTypes.find(t => t.name === this.formData.type);
    if (typeObj && typeObj.specificationFields) {
      const currentSpecs = { ...(this.formData.specifications || {}) };
      const newSpecs: Record<string, string> = {};
      typeObj.specificationFields.forEach(field => {
        newSpecs[field] = currentSpecs[field] || '';
      });
      this.formData.specifications = newSpecs;
    }

    this.loadAvailableShelves();
  }

  syncSpecificationsWithSchema(): void {
    if (!this.formData.type) return;

    const typeObj = this.availableTypes.find(t => t.name === this.formData.type);
    if (typeObj && typeObj.specificationFields) {
      const currentSpecs = { ...(this.formData.specifications || {}) };
      const newSpecs: Record<string, string> = { ...currentSpecs };

      // Ensure all fields from the current schema exist in the specifications object
      typeObj.specificationFields.forEach(field => {
        if (!(field in newSpecs)) {
          newSpecs[field] = '';
        }
      });

      this.formData.specifications = newSpecs;
    }
  }

  onCategoryChange(): void {
    const foundCat = this.categories.find(c => c.name === this.formData.category);
    if (foundCat) {
      this.availableTypes = foundCat.types || [];
    } else {
      this.availableTypes = [];
    }

    // If the currently selected type isn't in the new category's types, clear it
    if (this.formData.type && !this.availableTypes.some(t => t.name.toLowerCase() === this.formData.type?.toLowerCase())) {
      this.formData.type = '';
      this.availableShelves = [];
      this.formData.shelfId = '';
    }
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;

        // If we already have a category (from ngOnInit init), populate availableTypes
        if (this.formData.category) {
          this.onCategoryChange();
        }
      },
      error: (err) => console.error('Error fetching categories', err)
    });
  }

  onStatusChange(): void {
    const status = this.formData.status;
    
    // Clear PC assignment fields if status is no longer 'Installed'
    if (status !== 'Installed') {
      this.formData.assignedToEquipmentId = undefined;
      this.formData.assignedToEquipmentName = undefined;
    }

    if (status === 'Maintenance') {
      this.formData.shelfId = 'MAINTENANCE_AREA';
    } else if (status === 'Broken' || status === 'Unrepairable') {
      this.formData.shelfId = 'SCRAP_YARD';
    } else if (status === 'Out of Stock') {
      this.formData.shelfId = 'OUT_OF_STOCK';
    } else if (status === 'Installed') {
      this.formData.shelfId = '';
    } else if (status === 'Available' || status === 'In Stock') {
      // If switching back to Available from a virtual area, reset shelf
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

    // Generate exactly 15 random alphanumeric chars
    const chars = letters + digits;
    for (let i = 0; i < 15; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length));
    }

    // Ensure it's never just letters or just digits (force at least one of each)
    if (!/\d/.test(result)) {
      result = result.substring(0, 14) + digits.charAt(Math.floor(Math.random() * digits.length));
    }
    if (!/[a-zA-Z]/.test(result)) {
      result = result.substring(0, 14) + letters.charAt(Math.floor(Math.random() * letters.length));
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
    if (!sn || sn.length < 5) return;

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
    return sn.length > 0 && sn.length < 5;
  }

  get isSNInvalidFormat(): boolean {
    const sn = this.formData.serialNumber || '';
    if (sn.length === 0) return false;
    return !/^[a-zA-Z0-9]+$/.test(sn);
  }

  get isSNMissingChars(): boolean {
    const sn = this.formData.serialNumber || '';
    if (sn.length < 5 || this.isSNInvalidFormat) return false;
    return !/[a-zA-Z]/.test(sn) || !/\d/.test(sn);
  }

  isSerialNumberValid(): boolean {
    const sn = this.formData.serialNumber || '';
    if (!sn) return false;

    const hasMinLength = sn.length >= 5;
    const onlyAlphanumeric = /^[a-zA-Z0-9]+$/.test(sn);
    const hasLetter = /[a-zA-Z]/.test(sn);
    const hasDigit = /\d/.test(sn);

    return hasMinLength && onlyAlphanumeric && hasLetter && hasDigit;
  }

  get isFormInvalid(): boolean {
    const isBasicInfoMissing = !this.formData.equipmentName || !this.formData.brand || !this.isSerialNumberValid();
    const isShelfMissingForInStock = (this.formData.status === 'Available' || this.formData.status === 'In Stock') && (!this.formData.shelfId || this.formData.shelfId === '');
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
    const isUpdate = !!this.equipment?.id && !this.isAddSimilar;
    const originalShelfId = isUpdate ? this.equipment?.shelfId : null;

    // Filter shelves that have enough capacity and are not full
    let validShelves = this.availableShelves.filter(s => {
      if (s.status === 'FULL') {
        // If it's the current shelf, it's at max capacity but we are ALREADY in it.
        // So objectively there is space for us (we are it).
        return isUpdate && s.id === originalShelfId;
      }

      // Calculate effective current quantity by excluding ourselves if we are already there
      const effectiveCurrentQte = (isUpdate && s.id === originalShelfId) ? (s.currentQte - 1) : s.currentQte;
      return (effectiveCurrentQte + qteNeeded <= s.maxQte);
    });

    this.availableShelves = validShelves;

    // Auto select if only one is available
    if (this.availableShelves.length === 1 && !this.formData.shelfId) {
      this.formData.shelfId = this.availableShelves[0].id;
    } else if (this.availableShelves.length === 0 && (this.formData.status === 'Available' || this.formData.status === 'In Stock')) {
      // Only reset shelf if we really found nothing AND we are supposed to be available
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
    const payload = { ...this.formData } as Equipment;
    // Only force qte = 1 for NEW items. For edits, preserve the current quantity (which might be 0).
    if (!this.equipment?.id || this.isAddSimilar) {
      payload.qte = 1;
    }

    // Performance Optimization: Prevent sending unaltered 5MB base64 strings
    if (this.equipment && this.equipment.id) {
      if (payload.invoiceFileName === this.equipment.invoiceFileName) {
        delete payload.invoiceFileName;
        delete payload.invoiceFileData;
      }
      if (payload.warrantyFileName === this.equipment.warrantyFileName) {
        delete payload.warrantyFileName;
        delete payload.warrantyFileData;
      }
    }

    const doSave = (finalPayload: Equipment) => {
      if (this.equipment && this.equipment.id) {
        this.equipmentService.updateEquipment(this.equipment.id, finalPayload).subscribe({
          next: () => { 
            this.toastService.success(`${finalPayload.equipmentName} updated successfully.`);
            this.isSaving = false; 
            this.closeEvent.emit(true); 
          },
          error: (err) => { console.error(err); this.isSaving = false; }
        });
      } else {
        this.equipmentService.createEquipment(finalPayload).subscribe({
          next: () => { 
            this.toastService.success(`${finalPayload.equipmentName} added to inventory.`);
            this.isSaving = false; 
            this.closeEvent.emit(true); 
          },
          error: (err) => { console.error(err); this.isSaving = false; }
        });
      }
    };

    if (this.formGenerateQr) {
      // Generate QR code data URL then save
      this.generateQRCodeForSave(payload).then(url => {
        payload.qrCode = url;
        doSave(payload);
      });
    } else {
      // Remove QR code
      payload.qrCode = 'NONE';
      doSave(payload);
    }
  }

  private async generateQRCodeForSave(payload: Equipment): Promise<string> {
    const qrData = JSON.stringify({
      id: this.equipment?.id || 'NEW',
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

  enableEdit(): void {
    this.isEditing = true;
  }

  cancel(): void {
    if (this.viewOnly && this.isEditing) {
      // reset to original data and go back to view-only
      this.formData = { ...this.equipment };

      if (this.formData.purchaseDate) {
        this.formData.purchaseDate = this.formatDate(this.formData.purchaseDate);
      }
      if (this.formData.warrantyExpiration) {
        this.formData.warrantyExpiration = this.formatDate(this.formData.warrantyExpiration);
      }

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

  removeInvoiceFile(event: MouseEvent): void {
    event.stopPropagation();
    this.formData.invoiceFileName = "DELETE";
    this.formData.invoiceFileData = null as any;
  }

  removeWarrantyFile(event: MouseEvent): void {
    event.stopPropagation();
    this.formData.warrantyFileName = "DELETE";
    this.formData.warrantyFileData = null as any;
  }

  downloadDocument(fileType: 'invoice' | 'warranty', fileName?: string): void {
    if (!this.equipment?.id || !fileName) return;

    if (fileType === 'invoice') {
      // Check if we already have it in formData (user just uploaded it during edit)
      if (this.formData.invoiceFileData) {
        this.executeDownload(this.formData.invoiceFileData, fileName);
        return;
      }
      this.equipmentService.getInvoiceFile(this.equipment.id).subscribe({
        next: (base64) => this.executeDownload(base64, fileName),
        error: (err) => console.error('Failed to download invoice', err)
      });
    } else {
      if (this.formData.warrantyFileData) {
        this.executeDownload(this.formData.warrantyFileData, fileName);
        return;
      }
      this.equipmentService.getWarrantyFile(this.equipment.id).subscribe({
        next: (base64) => this.executeDownload(base64, fileName),
        error: (err) => console.error('Failed to download warranty', err)
      });
    }
  }

  private executeDownload(fileData: string, fileName: string): void {
    const a = document.createElement('a');
    a.href = fileData;
    a.download = fileName;
    a.click();
  }

  getShelfLocation(shelfId?: string, status?: string): string {
    if (!shelfId || shelfId === '') {
      if (status === 'Allocated') return 'Allocated (Not on Shelf)';
      if (status === 'Assigned' || status === 'Installed') return 'Installed (Not on Shelf)';
      return 'Unassigned';
    }
    if (shelfId === 'MAINTENANCE_AREA') return 'Maintenance Area';
    if (shelfId === 'SCRAP_YARD') return 'Scrap Yard';
    if (shelfId === 'OUT_OF_STOCK') return 'Out of Stock';

    const s = this.allShelves.find(x => x.id === shelfId);
    return s ? `Shelf ${s.nb}` : shelfId;
  }

  getDisplayDepartment(eq: Equipment | null): string {
    if (!eq) return '—';
    if (eq.allocatedToTechnicianName) return 'Technician';
    if (eq.assignedToEquipmentName) return 'Deployed';
    return eq.department || '—';
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
    // Show specs tab if there are dynamic specifications defined
    if (this.formData.specifications && Object.keys(this.formData.specifications).length > 0) {
      return true;
    }
    const type = this.formData.type || '';
    const category = this.formData.category?.toUpperCase() || '';
    return category === 'DEVICE' || category === 'SERVER' || ['pc', 'laptop', 'server', 'tablet', 'phone'].includes(type);
  }

  isWarrantyExpired(): boolean {
    if (!this.equipment?.warrantyExpiration) return false;
    return new Date(this.equipment.warrantyExpiration) < new Date();
  }


  private formatDate(dateObjOrString: Date | string): string {
    const d = typeof dateObjOrString === 'string' ? new Date(dateObjOrString) : dateObjOrString;
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;

  }

  typeRequiresQr(): boolean {
    if (!this.formData.category || !this.formData.type) {
      // If we have existing equipment, check its QR code field
      return !!(this.equipment && this.equipment.qrCode);
    }
    const cat = this.categories.find(c => c.name === this.formData.category);
    if (!cat) return true;
    const typeObj = cat.types?.find(t => t.name === this.formData.type);
    return typeObj ? typeObj.requiresQrCode : true;
  }

  isLegacyType(): boolean {
    if (!this.formData.type || !this.availableTypes) return false;
    return !this.availableTypes.some(t => t.name === this.formData.type);
  }

  loadInstalledParts(): void {
    if (!this.equipment || !this.equipment.id) return;
    const parentId = this.equipment.id;
    const parentName = this.equipment.equipmentName;
    this.equipmentService.getAllEquipment().subscribe({
      next: (allEq) => {
        this.installedParts = allEq.filter(e => {
          const status = (e.status || '').toLowerCase();
          const isInstalled = status === 'installed' || status === 'assigned';
          const matchesParent =
            e.assignedToEquipmentId === parentId ||
            (!!parentName && e.assignedToEquipmentName === parentName);
          return isInstalled && matchesParent;
        });
      },
      error: (err) => console.error('Error loading installed parts', err)
    });
  }

  uninstallPart(part: Equipment): void {
    if (!part.id) return;
    this.equipmentService.returnPart(part.id).subscribe({
      next: () => {
        this.toastService.success(`Part ${part.equipmentName} uninstalled and returned to stock.`);
        this.loadInstalledParts();
        this.refreshParentEquipment();
      },
      error: (err: any) => {
        console.error('Error uninstalling part', err);
        this.toastService.error(`Failed to uninstall part: ${err.error?.message || err.message || 'Unknown error'}`);
      }
    });
  }

  isAsset(): boolean {
    if (!this.equipment || !this.equipment.type) return false;
    const currentType = this.equipment.type.toLowerCase().trim();
    for (const cat of this.categories) {
      const foundType = cat.types?.find(t => t.name.toLowerCase().trim() === currentType);
      if (foundType) {
        return foundType.nature === 'Asset';
      }
    }
    // Fallback standard assets
    const assetTypes = ['pc', 'laptop', 'server'];
    return assetTypes.includes(currentType);
  }

  onInstallCategoryChange(): void {
    this.installFilterType = '';
    this.selectedPartToInstall = null;
    this.selectedPartId = '';
    
    if (!this.installFilterCategory) {
      this.installAvailableTypes = [];
    } else {
      const cat = this.categories.find(c => c.name === this.installFilterCategory);
      this.installAvailableTypes = cat ? (cat.types || []) : [];
    }
    this.applyInstallFilters();
  }

  onInstallTypeChange(): void {
    this.selectedPartToInstall = null;
    this.selectedPartId = '';
    this.applyInstallFilters();
  }

  applyInstallFilters(): void {
    let parts = [...this.availablePartsForInstall];

    // Filter by category
    if (this.installFilterCategory) {
      parts = parts.filter(p => p.category === this.installFilterCategory);
    }

    // Filter by type
    if (this.installFilterType) {
      parts = parts.filter(p => p.type?.toLowerCase().trim() === this.installFilterType.toLowerCase().trim());
    }

    // Search query filter (Brand, Model, Serial, Name)
    if (this.installSearchQuery) {
      const q = this.installSearchQuery.toLowerCase().trim();
      parts = parts.filter(p => 
        (p.equipmentName || '').toLowerCase().includes(q) ||
        (p.brand || '').toLowerCase().includes(q) ||
        (p.model || '').toLowerCase().includes(q) ||
        (p.serialNumber || '').toLowerCase().includes(q)
      );
    }

    this.filteredPartsForInstall = parts;
  }

  openInstallModal(): void {
    this.showInstallModal = true;
    this.isLoadingPartsForInstall = true;
    this.availablePartsForInstall = [];
    this.filteredPartsForInstall = [];
    this.selectedPartToInstall = null;
    this.selectedPartId = '';
    this.installSpecKey = '';

    // Reset filters
    this.installFilterCategory = '';
    this.installFilterType = '';
    this.installSearchQuery = '';
    this.installAvailableTypes = [];

    // Ensure categories are loaded
    if (!this.categories || this.categories.length === 0) {
      this.categoryService.getAllCategories().subscribe(cats => {
        this.categories = cats;
      });
    }

    this.equipmentService.getAllEquipment().subscribe({
      next: (allEq) => {
        this.isLoadingPartsForInstall = false;
        
        // Filter parts
        const filtered = allEq.filter(e => 
          (e.status?.toLowerCase() === 'available' || !e.assignedToEquipmentId) && 
          e.id !== this.equipment?.id
        );

        // Deduplicate by Serial Number (if serialNumber exists, otherwise fallback to ID)
        const seenSerials = new Set<string>();
        const seenIds = new Set<string>();
        const uniqueParts: Equipment[] = [];
        for (const p of filtered) {
          if (!p.id || seenIds.has(p.id)) continue;
          
          const sn = p.serialNumber ? p.serialNumber.trim().toLowerCase() : '';
          if (sn && seenSerials.has(sn)) {
            continue; // Skip duplicates with same serial number
          }
          
          seenIds.add(p.id);
          if (sn) {
            seenSerials.add(sn);
          }
          uniqueParts.push(p);
        }

        this.availablePartsForInstall = uniqueParts;
        this.filteredPartsForInstall = [...uniqueParts];
      },
      error: (err) => {
        this.isLoadingPartsForInstall = false;
        console.error('Error loading available parts', err);
        this.toastService.error('Failed to load parts from stock.');
      }
    });
  }

  onPartIdSelected(partId: string): void {
    this.selectedPartId = partId;
    const part = this.availablePartsForInstall.find(p => p.id === partId);
    if (part) {
      this.selectedPartToInstall = part;
      this.installSpecKey = this.resolveInstallSpecKey(part);
    } else {
      this.selectedPartToInstall = null;
      this.installSpecKey = '';
    }
  }

  onPartSelected(part: Equipment): void {
    this.selectedPartToInstall = part;
    if (part) {
      this.selectedPartId = part.id || '';
      this.installSpecKey = this.resolveInstallSpecKey(part);
    } else {
      this.selectedPartId = '';
      this.installSpecKey = '';
    }
  }

  resolveInstallSpecKey(part: Equipment): string {
    const type = (part.type || '').toLowerCase().trim();
    const parentSpecs = this.equipment?.specifications || {};
    const existingKey = Object.keys(parentSpecs).find(k => k.toLowerCase().trim() === type);
    return existingKey || part.type || 'Part';
  }

  formatPartSpecifications(part: Equipment): string {
    if (!part.specifications || Object.keys(part.specifications).length === 0) {
      return '—';
    }
    return Object.entries(part.specifications)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ');
  }

  getPartSpecificationEntries(part: Equipment): { key: string; value: string }[] {
    if (!part.specifications) return [];
    return Object.entries(part.specifications).map(([key, value]) => ({ key, value }));
  }

  refreshParentEquipment(): void {
    if (!this.equipment?.id) return;
    this.equipmentService.getEquipmentById(this.equipment.id).subscribe({
      next: (fullEq) => {
        this.equipment = fullEq;
        this.formData = { ...fullEq };
        if (this.formData.purchaseDate) {
          this.formData.purchaseDate = this.formatDate(this.formData.purchaseDate);
        }
        if (this.formData.warrantyExpiration) {
          this.formData.warrantyExpiration = this.formatDate(this.formData.warrantyExpiration);
        }
      },
      error: (err) => console.error('Error refreshing equipment', err)
    });
  }

  confirmInstallPart(): void {
    if (!this.selectedPartToInstall || !this.selectedPartToInstall.id || !this.equipment || !this.equipment.id) return;

    const part = this.selectedPartToInstall;
    const parent = this.equipment;
    const specKey = (this.installSpecKey || this.resolveInstallSpecKey(part)).trim();
    const specification = this.formatPartSpecifications(part);
    const actor = this.currentUserName || 'Stock Manager';

    this.equipmentService.installPartFromMaintenance(part.id!, parent.id!, {
      replacesSpecKey: specKey,
      actionType: 'Install',
      specification: specification !== '—' ? specification : undefined,
      brand: part.brand,
      actor
    }).subscribe({
      next: () => {
        this.toastService.success(`Part ${part.equipmentName} successfully installed inside ${parent.equipmentName}.`);
        this.showInstallModal = false;
        this.refreshParentEquipment();
        this.loadInstalledParts();
      },
      error: (err) => {
        console.error('Error installing part', err);
        this.toastService.error(`Failed to install part: ${err.error?.message || err.message || 'Unknown error'}`);
      }
    });
  }

  trackByKey(index: number, item: any): string {
    return item.key;
  }
}
