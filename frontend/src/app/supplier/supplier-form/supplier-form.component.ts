import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Supplier } from '../supplier.model';
import { SupplierService } from '../supplier.service';

@Component({
  selector: 'app-supplier-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier-form.component.html',
  styleUrl: './supplier-form.component.css'
})
export class SupplierFormComponent implements OnInit {
  @Input() supplier: Supplier | null = null;
  @Input() viewOnly: boolean = false;
  @Output() closeEvent = new EventEmitter<boolean>();

  formData: Partial<Supplier> = {};
  isSaving: boolean = false;
  isEditing: boolean = false;

  constructor(private supplierService: SupplierService) {}

  ngOnInit(): void {
    if (this.supplier) {
      this.formData = { ...this.supplier };
    } else {
      this.formData = {
        name: '',
        companyName: '',
        address: '',
        phoneNumber: '',
        email: '',
        website: '',
        category: '',
        contactPerson: '',
        note: ''
      };
    }
  }

  saveSupplier(): void {
    this.isSaving = true;
    const payload = this.formData as Supplier;

    if (this.supplier && this.supplier.id) {
      this.supplierService.updateSupplier(this.supplier.id, payload).subscribe({
        next: () => { this.isSaving = false; this.closeEvent.emit(true); },
        error: (err) => { console.error(err); this.isSaving = false; }
      });
    } else {
      this.supplierService.createSupplier(payload).subscribe({
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
      this.formData = { ...this.supplier };
      this.isEditing = false;
    } else {
      this.closeEvent.emit(false);
    }
  }
}
