import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EquipmentListComponent } from './equipment-list/equipment-list.component';
import { EquipmentFormComponent } from './equipment-form/equipment-form.component';
import { EquipmentWizardComponent } from './equipment-wizard/equipment-wizard.component';
import { Equipment } from './equipment.model';
import { EquipmentService } from './equipment.service';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, EquipmentListComponent, EquipmentFormComponent, EquipmentWizardComponent],
  templateUrl: './equipment.component.html',
  styleUrl: './equipment.component.css'
})
export class EquipmentComponent implements OnInit {
  mode: 'list' | 'form' = 'list';
  equipmentToEdit: Equipment | null = null;
  formViewOnly: boolean = false;
  refreshFlag: number = 0;
  showWizard: boolean = false;
  wizardPrefillData: Equipment | null = null;
  isAddSimilar: boolean = false;
  /** True while fetching file data for Add Similar — prevents wizard from opening prematurely */
  isLoadingAddSimilar: boolean = false;

  constructor(private equipmentService: EquipmentService) {}
  ngOnInit(): void {}

  openAdd(): void {
    this.showWizard = true;
  }

  onWizardClose(saved: boolean): void {
    this.showWizard = false;
    this.wizardPrefillData = null;
    if (saved) this.refreshFlag++;
  }

  openEdit(equipment: Equipment): void {
    this.equipmentToEdit = { ...equipment };
    this.formViewOnly = false;
    this.mode = 'form';
  }

  openView(equipment: Equipment): void {
    this.equipmentToEdit = { ...equipment };
    this.formViewOnly = true;
    this.mode = 'form';
  }

  openAddSimilar(equipment: Equipment): void {
    if (!equipment.id) {
      // No id — open directly without file fetching
      this.wizardPrefillData = { ...equipment };
      this.showWizard = true;
      return;
    }

    const hasInvoice = !!equipment.invoiceFileName;
    const hasWarranty = !!equipment.warrantyFileName;

    // No documents at all — open wizard immediately
    if (!hasInvoice && !hasWarranty) {
      this.wizardPrefillData = { ...equipment };
      this.showWizard = true;
      return;
    }

    // Fetch file data before opening wizard so doc fields are pre-filled
    this.isLoadingAddSimilar = true;

    const invoice$ = hasInvoice
      ? this.equipmentService.getInvoiceFile(equipment.id).pipe(catchError(() => of('')))
      : of('');

    const warranty$ = hasWarranty
      ? this.equipmentService.getWarrantyFile(equipment.id).pipe(catchError(() => of('')))
      : of('');

    forkJoin([invoice$, warranty$]).subscribe({
      next: ([invoiceData, warrantyData]) => {
        this.wizardPrefillData = {
          ...equipment,
          invoiceFileData: invoiceData || '',
          warrantyFileData: warrantyData || ''
        };
        this.isLoadingAddSimilar = false;
        this.showWizard = true;
      },
      error: () => {
        // Fallback: open wizard without file data
        this.wizardPrefillData = { ...equipment };
        this.isLoadingAddSimilar = false;
        this.showWizard = true;
      }
    });
  }

  closeForm(saved: boolean): void {
    this.mode = 'list';
    this.equipmentToEdit = null;
    this.formViewOnly = false;
    this.isAddSimilar = false;
    if (saved) {
      this.refreshFlag++;
    }
  }
}

