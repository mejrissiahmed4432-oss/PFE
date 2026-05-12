import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Employee } from '../employee.model';
import { EmployeeService } from '../employee.service';

@Component({
  selector: 'app-employee-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './employee-form.component.html',
  styleUrl: './employee-form.component.css'
})
export class EmployeeFormComponent implements OnInit {
  @Input() employee: Employee | null = null;
  @Input() isViewOnly: boolean = false;
  @Output() closeEvent = new EventEmitter<boolean>();

  formData: Employee = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    jobTitle: '',
    department: '',
    employmentStatus: 'Active',
    hireDate: '',
    cin: ''
  };

  isSaving = false;
  isEditing = false;

  departments = ['HR', 'IT', 'Finance', 'Engineering', 'Marketing', 'Sales', 'Operations'];
  statuses = ['Active', 'On Leave', 'Terminated'];

  constructor(private employeeService: EmployeeService) {}

  ngOnInit(): void {
    if (this.employee) {
      this.formData = { ...this.employee };
    } else {
      const today = new Date();
      this.formData.hireDate = today.toISOString().split('T')[0];
    }
    if (!this.isViewOnly && this.employee) {
      this.isEditing = true;
    }
  }

  getPreviewInitials(): string {
    const f = this.formData.firstName?.charAt(0) || '';
    const l = this.formData.lastName?.charAt(0) || '';
    return (f + l).toUpperCase();
  }

  numberOnly(event: any): boolean {
    const charCode = (event.which) ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57)) {
      return false;
    }
    return true;
  }

  onOverlayClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (target.classList.contains('modal-overlay')) {
      this.cancel();
    }
  }

  enableEdit(): void {
    this.isViewOnly = false;
    this.isEditing = true;
  }

  cancel(): void {
    if (this.isViewOnly) {
      this.closeEvent.emit(false);
    } else if (this.isEditing && this.employee) {
      this.formData = { ...this.employee };
      this.isViewOnly = true;
      this.isEditing = false;
    } else {
      this.closeEvent.emit(false);
    }
  }

  save(): void {
    this.isSaving = true;

    if (this.employee && this.employee.id) {
      this.employeeService.updateEmployee(this.employee.id, this.formData).subscribe({
        next: () => { this.isSaving = false; this.closeEvent.emit(true); },
        error: (err) => { console.error('Error updating employee', err); this.isSaving = false; }
      });
    } else {
      this.employeeService.createEmployee(this.formData).subscribe({
        next: () => { this.isSaving = false; this.closeEvent.emit(true); },
        error: (err) => { console.error('Error creating employee', err); this.isSaving = false; }
      });
    }
  }
}
