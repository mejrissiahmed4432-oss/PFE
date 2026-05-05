import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EmployeeService } from '../employee.service';
import { Employee } from '../employee.model';
import { EmployeeFormComponent } from '../employee-form/employee-form.component';

@Component({
  selector: 'app-employee-list',
  standalone: true,
  imports: [CommonModule, FormsModule, EmployeeFormComponent],
  templateUrl: './employee-list.component.html',
  styleUrl: './employee-list.component.css'
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  filteredEmployees: Employee[] = [];
  isLoading = true;

  // Search & Filters
  searchTerm = '';
  filterStatus = '';
  filterDepartment = '';
  filterHireDateFrom = '';
  filterHireDateTo = '';

  readonly allDepartments = ['HR', 'IT', 'Finance', 'Engineering', 'Marketing', 'Sales', 'Operations'];

  // Pagination
  readonly PAGE_SIZE = 5;
  currentPage = 1;

  // Modal state
  showFormModal = false;
  selectedEmployee: Employee | null = null;
  isViewOnly = false;

  // Delete confirmation
  showDeleteModal = false;
  employeeToDelete: string | undefined = undefined;

  constructor(private employeeService: EmployeeService) {}

  ngOnInit(): void {
    this.loadEmployees();
  }

  loadEmployees(): void {
    this.isLoading = true;
    this.employeeService.getAllEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        this.filterEmployees();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Failed to load employees', err);
        this.isLoading = false;
      }
    });
  }

  filterEmployees(): void {
    this.currentPage = 1;
    const term = this.searchTerm.toLowerCase().trim();

    this.filteredEmployees = this.employees.filter(e => {
      // Text search across all fields
      const matchesSearch = !term || [
        e.firstName, e.lastName, e.email,
        e.phone || '', e.jobTitle, e.department,
        e.employmentStatus, e.hireDate || ''
      ].some(val => val.toLowerCase().includes(term));

      // Status filter
      const matchesStatus = !this.filterStatus || e.employmentStatus === this.filterStatus;

      // Department filter
      const matchesDept = !this.filterDepartment || e.department === this.filterDepartment;

      // Hire date range filter
      const empDate = e.hireDate || '';
      const matchesFrom = !this.filterHireDateFrom || empDate >= this.filterHireDateFrom;
      const matchesTo   = !this.filterHireDateTo   || empDate <= this.filterHireDateTo;

      return matchesSearch && matchesStatus && matchesDept && matchesFrom && matchesTo;
    });
  }

  hasActiveFilters(): boolean {
    return !!(this.filterStatus || this.filterDepartment || this.filterHireDateFrom || this.filterHireDateTo);
  }

  resetFilters(): void {
    this.filterStatus = '';
    this.filterDepartment = '';
    this.filterHireDateFrom = '';
    this.filterHireDateTo = '';
    this.filterEmployees();
  }

  // Pagination
  get totalPages(): number {
    return Math.ceil(this.filteredEmployees.length / this.PAGE_SIZE);
  }

  get pagedEmployees(): Employee[] {
    const start = (this.currentPage - 1) * this.PAGE_SIZE;
    return this.filteredEmployees.slice(start, start + this.PAGE_SIZE);
  }

  get pageNumbers(): number[] {
    return Array.from({ length: this.totalPages }, (_, i) => i + 1);
  }

  goToPage(page: number): void {
    if (page >= 1 && page <= this.totalPages) this.currentPage = page;
  }

  // Avatar - fixed blue gradient, initials only
  getInitials(emp: Employee): string {
    return `${emp.firstName.charAt(0)}${emp.lastName.charAt(0)}`.toUpperCase();
  }

  // Modal actions
  openAddForm(): void {
    this.selectedEmployee = null;
    this.isViewOnly = false;
    this.showFormModal = true;
  }

  openEditForm(employee: Employee): void {
    this.selectedEmployee = employee;
    this.isViewOnly = false;
    this.showFormModal = true;
  }

  openViewForm(employee: Employee): void {
    this.selectedEmployee = employee;
    this.isViewOnly = true;
    this.showFormModal = true;
  }

  confirmDelete(id: string | undefined): void {
    this.employeeToDelete = id;
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.employeeToDelete = undefined;
  }

  executeDelete(): void {
    if (!this.employeeToDelete) return;
    this.employeeService.deleteEmployee(this.employeeToDelete).subscribe({
      next: () => {
        this.showDeleteModal = false;
        this.employeeToDelete = undefined;
        this.loadEmployees();
      },
      error: (err) => console.error('Failed to delete employee', err)
    });
  }

  handleFormClose(refresh: boolean): void {
    this.showFormModal = false;
    if (refresh) this.loadEmployees();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Active':     return 'status-active';
      case 'On Leave':   return 'status-leave';
      case 'Terminated': return 'status-terminated';
      default:           return 'status-default';
    }
  }

  minVal(a: number, b: number): number {
    return Math.min(a, b);
  }
}
