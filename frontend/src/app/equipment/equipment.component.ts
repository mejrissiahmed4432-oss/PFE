import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EquipmentListComponent } from './equipment-list/equipment-list.component';
import { EquipmentFormComponent } from './equipment-form/equipment-form.component';
import { EquipmentWizardComponent } from './equipment-wizard/equipment-wizard.component';
import { Equipment } from './equipment.model';

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

  constructor() {}
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
    this.wizardPrefillData = { ...equipment };
    this.showWizard = true;
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
