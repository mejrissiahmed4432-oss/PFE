import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Supplier } from '../supplier.model';
import { SupplierService } from '../supplier.service';

@Component({
  selector: 'app-supplier-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-form.component.html',
  styleUrl: './supplier-form.component.css'
})
export class SupplierFormComponent implements OnInit {
  @Input() visible: boolean = false;
  @Input() supplier: Supplier | null = null;
  @Input() viewOnly: boolean = false;
  @Output() closeEvent = new EventEmitter<boolean>();

  supplierForm!: FormGroup;
  isSaving: boolean = false;
  isEditing: boolean = false;
  
  // For website/URL validation
  urlPattern = /^(https?:\/\/)?([\da-z.-]+)\.([a-z.]{2,6})([/\w .-]*)?(\/.*)?$/;
  phonePattern = /^\+?[0-9]*$/;

  constructor(
    private fb: FormBuilder,
    private supplierService: SupplierService
  ) {}

  ngOnInit(): void {
    this.initForm();
    if (this.supplier) {
      this.supplierForm.patchValue(this.supplier);
    }
  }

  initForm(): void {
    this.supplierForm = this.fb.group({
      companyName: ['', [Validators.required, Validators.minLength(2)]],
      contactPerson: ['', Validators.required],
      phoneNumber: ['', [Validators.required, Validators.pattern(this.phonePattern)]],
      email: ['', [Validators.required, Validators.email]],
      website: ['', [Validators.required, Validators.pattern(this.urlPattern)]],
      address: ['', Validators.required],
      category: ['', Validators.required],
      rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
      note: ['']
    });

    if (this.viewOnly && !this.isEditing) {
      this.supplierForm.disable();
    }
  }

  setRating(rating: number): void {
    if (!this.viewOnly || this.isEditing) {
      this.supplierForm.patchValue({ rating });
    }
  }

  get f() { return this.supplierForm.controls; }

  saveSupplier(): void {
    if (this.supplierForm.invalid) {
      this.supplierForm.markAllAsTouched();
      return;
    }

    this.isSaving = true;
    const payload = this.supplierForm.getRawValue();

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
    this.supplierForm.enable();
  }

  cancel(): void {
    if (this.viewOnly && this.isEditing) {
      this.supplierForm.patchValue(this.supplier!);
      this.supplierForm.disable();
      this.isEditing = false;
    } else {
      this.closeEvent.emit(false);
    }
  }
}
