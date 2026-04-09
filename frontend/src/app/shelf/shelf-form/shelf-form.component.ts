import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';
import { ShelfService } from '../shelf.service';
import { Shelf } from '../shelf.model';
import { CategoryService } from '../../category-manager/category.service';
import { EquipmentCategory, CategoryType } from '../../category-manager/category.model';

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
  categories: EquipmentCategory[] = [];
  availableTypes: CategoryType[] = [];
  existingShelves: string[] = [];

  constructor(
    private fb: FormBuilder,
    private shelfService: ShelfService,
    private categoryService: CategoryService
  ) {
    this.shelfForm = this.fb.group({
      category: ['', Validators.required],
      nbSuffix: ['', [Validators.required, Validators.pattern('^[0-9]+$'), this.uniqueNbValidator()]],
      equipmentType: [{ value: '', disabled: true }, Validators.required],
      maxQte: [10, [Validators.required, Validators.min(1)]],
      minQte: [2, [Validators.required, Validators.min(0)]],
      currentQte: [0, [Validators.min(0)]],
      status: ['EMPTY']
    }, { validators: this.minMaxValidator });
  }

  minMaxValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
    const min = group.get('minQte')?.value;
    const max = group.get('maxQte')?.value;
    const current = group.get('currentQte')?.value;
    
    if (min !== null && max !== null && min >= max) {
      return { minGreaterEqualMax: true };
    }
    if (current !== null && max !== null && max < current) {
      return { maxLessThanCurrent: true };
    }
    return null;
  };

  ngOnInit(): void {
    // Fetch categories and map existing category if editing
    this.categoryService.getAllCategories().subscribe(data => {
      this.categories = data;
      
      // If editing, find which category owns the shelf's equipmentType
      if (this.shelf && this.shelf.equipmentType) {
        const matchingCategory = this.categories.find(c => 
          c.types?.some(t => t.name.toLowerCase() === this.shelf!.equipmentType.toLowerCase())
        );
        if (matchingCategory && matchingCategory.name) {
          this.shelfForm.get('category')?.setValue(matchingCategory.name, { emitEvent: true });
        }
      }
    });

    // When category changes, update available types
    this.shelfForm.get('category')?.valueChanges.subscribe(catName => {
      const selectedCat = this.categories.find(c => c.name === catName);
      if (selectedCat && selectedCat.types) {
        this.availableTypes = selectedCat.types;
      } else {
        this.availableTypes = [];
      }
      
      
      // Reset equipment type if current one isn't in the new list
      const eqTypeCtrl = this.shelfForm.get('equipmentType');
      if (eqTypeCtrl) {
        if (!catName) {
           eqTypeCtrl.disable({ emitEvent: false });
        } else {
           if (!this.shelf || !this.shelf.currentQte || this.shelf.currentQte === 0) {
             eqTypeCtrl.enable({ emitEvent: false });
           }
        }
        const currentType = eqTypeCtrl.value;
        if (currentType && !this.availableTypes.some(t => t.name.toLowerCase() === currentType.toLowerCase())) {
           eqTypeCtrl.setValue('');
        }
      }
    });

    // Fetch existing shelves for uniqueness validation
    this.shelfService.getAllShelves().subscribe(shelves => {
      const currentNb = this.shelf ? this.shelf.nb?.toLowerCase() : '';
      this.existingShelves = shelves
        .map(s => s.nb?.toLowerCase())
        .filter(nb => nb && nb !== currentNb);
      this.shelfForm.get('nbSuffix')?.updateValueAndValidity();
    });

    // Re-validate unique id when equipmentType changes
    this.shelfForm.get('equipmentType')?.valueChanges.subscribe(type => {
      this.shelfForm.get('nbSuffix')?.updateValueAndValidity();
    });

    if (this.shelf) {
      const formVal: any = { ...this.shelf };
      
      // Extract the suffix from the existing nb
      if (this.shelf.nb && this.shelf.equipmentType) {
        const prefix = this.shelf.equipmentType + '-';
        if (this.shelf.nb.toLowerCase().startsWith(prefix.toLowerCase())) {
          formVal['nbSuffix'] = this.shelf.nb.substring(prefix.length);
        } else {
          formVal['nbSuffix'] = this.shelf.nb;
        }
      }

      this.shelfForm.patchValue(formVal);
      
      if (this.shelf.equipmentType && (!this.shelf.currentQte || this.shelf.currentQte === 0)) {
         this.shelfForm.get('equipmentType')?.enable({ emitEvent: false });
      }

      if (this.shelf.currentQte && this.shelf.currentQte > 0) {
        this.shelfForm.get('equipmentType')?.disable({ emitEvent: false });
        this.shelfForm.get('category')?.disable({ emitEvent: false });
      }
    }
  }

  uniqueNbValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      if (!control.value) return null;
      if (!this.shelfForm) return null;
      
      const type = this.shelfForm.get('equipmentType')?.value;
      if (!type) return null;

      const val = `${type}-${control.value}`.toLowerCase();
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
    const shelfData = this.shelfForm.getRawValue();
    
    // Remove transient properties before saving
    delete shelfData.category;
    
    // Construct the full nb identifier
    if (shelfData.equipmentType && shelfData.nbSuffix) {
      shelfData.nb = `${shelfData.equipmentType}-${shelfData.nbSuffix}`.toLowerCase();
    }
    delete shelfData.nbSuffix;

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
