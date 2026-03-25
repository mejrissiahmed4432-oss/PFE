import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SupplierListComponent } from './supplier-list/supplier-list.component';
import { SupplierFormComponent } from './supplier-form/supplier-form.component';
import { Supplier } from './supplier.model';

@Component({
  selector: 'app-supplier',
  standalone: true,
  imports: [CommonModule, SupplierListComponent, SupplierFormComponent],
  templateUrl: './supplier.component.html',
  styleUrl: './supplier.component.css'
})
export class SupplierComponent implements OnInit {
  mode: 'list' | 'form' = 'list';
  supplierToEdit: Supplier | null = null;
  formViewOnly: boolean = false;
  refreshFlag: number = 0;

  constructor() {}
  ngOnInit(): void {}

  openAdd(): void {
    this.supplierToEdit = null;
    this.formViewOnly = false;
    this.mode = 'form';
  }

  openEdit(supplier: Supplier): void {
    this.supplierToEdit = { ...supplier };
    this.formViewOnly = false;
    this.mode = 'form';
  }

  openView(supplier: Supplier): void {
    this.supplierToEdit = { ...supplier };
    this.formViewOnly = true;
    this.mode = 'form';
  }

  closeForm(saved: boolean): void {
    this.mode = 'list';
    this.supplierToEdit = null;
    this.formViewOnly = false;
    if (saved) {
      this.refreshFlag++;
    }
  }
}
