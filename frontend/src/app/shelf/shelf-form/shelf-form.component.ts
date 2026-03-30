import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
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

  constructor(
    private fb: FormBuilder,
    private shelfService: ShelfService
  ) {
    this.shelfForm = this.fb.group({
      nb: ['', Validators.required],
      equipmentType: ['', Validators.required],
      maxQte: [10, [Validators.required, Validators.min(1)]],
      currentQte: [0, [Validators.min(0)]],
      status: ['EMPTY']
    });
  }

  ngOnInit(): void {
    if (this.shelf) {
      this.shelfForm.patchValue(this.shelf);
    }
  }

  onSubmit(): void {
    if (this.shelfForm.invalid) {
      this.markFormGroupTouched(this.shelfForm);
      return;
    }

    this.isSubmitting = true;
    const shelfData = this.shelfForm.value;

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
