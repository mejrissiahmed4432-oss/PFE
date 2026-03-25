import { Component, OnInit, Input, Output, EventEmitter, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Equipment } from '../equipment.model';
import { EquipmentService } from '../equipment.service';
import { AuthService } from '../../auth.service';
import { SupplierService } from '../../supplier/supplier.service';
import { Supplier } from '../../supplier/supplier.model';
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
  @Output() closeEvent = new EventEmitter<boolean>();

  formData: Partial<Equipment> = {};
  isSaving: boolean = false;
  qrDataUrl: string = '';
  isUserName: string = '';
  isEditing: boolean = false;
  suppliers: Supplier[] = [];
  currentUserName: string = '';


  constructor(
    private equipmentService: EquipmentService,
    private authService: AuthService,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    const userData = this.authService.getCurrentUser();
    this.currentUserName = userData?.firstName
      ? `${userData.firstName} ${userData.lastName || ''}`.trim()
      : (userData?.email || 'Unknown');

    this.loadSuppliers();

    if (this.equipment) {
      this.formData = { ...this.equipment };
    } else {
      this.formData = {
        equipmentName: '',
        brand: '',
        model: '',
        serialNumber: '',
        category: '',
        supplier: '',
        location: '',
        department: '',
        note: '',
        purchasePrice: 0,
        purchaseDate: this.formatDate(new Date()),
        warrantyExpiration: '',
        createdBy: this.currentUserName,
        cpu: '',
        ram: '',
        storage: '',
        graphicsCard: '',
        operatingSystem: ''
      };
    }
  }

  loadSuppliers(): void {
    this.supplierService.getAllSuppliers().subscribe({
      next: (data) => this.suppliers = data,
      error: (err) => console.error('Error fetching suppliers', err)
    });
  }

  onSupplierChange(): void {
    const selectedSupplier = this.suppliers.find(s => s.id === this.formData.supplierId);
    if (selectedSupplier) {
      this.formData.supplier = selectedSupplier.name;
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
      location: this.equipment.location
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
        <p>Location: ${this.equipment?.location || 'N/A'}</p>
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
      this.isEditing = false;
    } else {
      this.closeEvent.emit(false);
    }
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
