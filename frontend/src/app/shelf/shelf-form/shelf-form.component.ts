import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ShelfService } from '../shelf.service';
import { Shelf } from '../shelf.model';

@Component({
  selector: 'app-shelf-form',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule],
  templateUrl: './shelf-form.component.html',
  styleUrl: './shelf-form.component.css'
})
export class ShelfFormComponent implements OnInit {
  @Input() shelf: Shelf | null = null;
  @Output() close = new EventEmitter<void>();

  shelfForm: FormGroup;
  isSubmitting = false;
  equipmentTypes = [
    'pc', 'laptop', 'server', 'monitor', 'printer', 'scanner', 
    'projector', 'router', 'switch', 'ups', 'tablet', 'phone', 
    'ram', 'hard drive', 'ssd', 'cables', 'keyboard', 'mouse', 'headset'
  ];

  existingShelves: string[] = [];

  constructor(
    private fb: FormBuilder,
    private shelfService: ShelfService
  ) {
    this.shelfForm = this.fb.group({
      nb: ['', Validators.required],
      equipmentType: ['', Validators.required],
      maxQte: [10, [Validators.required, Validators.min(1)]],
      minQte: [2, [Validators.required, Validators.min(0)]],
      currentQte: [0, [Validators.min(0)]],
      status: ['EMPTY']
    }, { validators: this.minMaxValidator });
  }

  minMaxValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
    const min = group.get('minQte')?.value;
    const max = group.get('maxQte')?.value;
    return min !== null && max !== null && min >= max ? { minGreaterEqualMax: true } : null;
  };

  ngOnInit(): void {
    // Fetch existing shelves for uniqueness validation
    this.shelfService.getAllShelves().subscribe(shelves => {
      const currentNb = this.shelf ? this.shelf.nb?.toLowerCase() : '';
      this.existingShelves = shelves
        .map(s => s.nb?.toLowerCase())
        .filter(nb => nb && nb !== currentNb);
      this.shelfForm.get('nb')?.updateValueAndValidity();
    });

    // Dynamic validation for 'nb' based on selected 'equipmentType'
    this.shelfForm.get('equipmentType')?.valueChanges.subscribe(type => {
      this.updateNbValidator(type);
    });

    if (this.shelf) {
      this.shelfForm.patchValue(this.shelf);
    }

    if (this.shelfForm.get('equipmentType')?.value) {
      this.updateNbValidator(this.shelfForm.get('equipmentType')?.value);
    }
  }

  updateNbValidator(type: string): void {
    const ctrl = this.shelfForm.get('nb');
    if (!ctrl) return;
    if (!type) {
      ctrl.setValidators([Validators.required, this.uniqueNbValidator()]);
    } else {
      // e.g. if type is 'pc', pattern is '^pc-[0-9]+$' (case insensitive)
      const safeType = type.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      const pattern = new RegExp(`^${safeType}-[0-9]+$`, 'i');
      ctrl.setValidators([Validators.required, Validators.pattern(pattern), this.uniqueNbValidator()]);
    }
    ctrl.updateValueAndValidity();
  }

  uniqueNbValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      const val = control.value.toLowerCase();
      if (this.existingShelves.includes(val)) {
        return { notUnique: true };
      }
      return null;
    };
  }

  onSubmit(): void {
    if (this.shelfForm.invalid) {
      this.markFormGroupTouched(this.shelfForm);
      return;
    }

    this.isSubmitting = true;
    const shelfData = this.shelfForm.value;
    
    // Force the identifier back to lowercase before saving (e.g. PC-8 -> pc-8)
    if (shelfData.nb) {
      shelfData.nb = shelfData.nb.toLowerCase();
    }

    if (this.shelf && this.shelf.id) {
      // Update existing
      this.shelfService.updateShelf(this.shelf.id, shelfData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.closeForm();
        },
        error: (error) => {
          console.error('Error updating shelf', error);
          this.isSubmitting = false;
        }
      });
    } else {
      // Create new
      this.shelfService.createShelf(shelfData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.closeForm();
        },
        error: (error) => {
          console.error('Error creating shelf', error);
          this.isSubmitting = false;
        }
      });
    }
  }

  closeForm(): void {
    this.close.emit();
  }

  private markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if (control instanceof FormGroup) {
        this.markFormGroupTouched(control);
      }
    });
  }
}
