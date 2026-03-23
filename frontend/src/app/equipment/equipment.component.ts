import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { EquipmentListComponent } from './equipment-list/equipment-list.component';
import { EquipmentFormComponent } from './equipment-form/equipment-form.component';
import { Equipment } from './equipment.model';

@Component({
  selector: 'app-equipment',
  standalone: true,
  imports: [CommonModule, EquipmentListComponent, EquipmentFormComponent],
  templateUrl: './equipment.component.html',
  styleUrl: './equipment.component.css'
})
export class EquipmentComponent implements OnInit {
  mode: 'list' | 'form' = 'list';
  equipmentToEdit: Equipment | null = null;
  formViewOnly: boolean = false;
  refreshFlag: number = 0;

  constructor() {}
  ngOnInit(): void {}

  openAdd(): void {
    this.equipmentToEdit = null;
    this.formViewOnly = false;
    this.mode = 'form';
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

  closeForm(saved: boolean): void {
    this.mode = 'list';
    this.equipmentToEdit = null;
    this.formViewOnly = false;
    if (saved) {
      this.refreshFlag++;
    }
  }
}
