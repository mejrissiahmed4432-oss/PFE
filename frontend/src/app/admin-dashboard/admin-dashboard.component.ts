import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DepartmentService } from './department.service';
import { EmployeeService } from '../employee/employee.service';
import { Department } from './department.model';
import { Employee } from '../employee/employee.model';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.css'
})
export class AdminDashboardComponent implements OnInit {

  // ── Sub-tab ──────────────────────────────────────────────
  activeSubTab: 'departments' | 'employees' = 'departments';

  // ── Departments ──────────────────────────────────────────
  departments: Department[] = [];
  filteredDepartments: Department[] = [];
  deptSearch = '';
  isDeptLoading = true;

  // Department detail panel
  selectedDept: Department | null = null;
  deptEmployees: Employee[] = [];
  isDeptDetailOpen = false;

  // Department form modal
  showDeptForm = false;
  editingDept: Department | null = null;
  deptForm: Department = this.emptyDeptForm();
  isSavingDept = false;
  deptFormError = '';

  // Department delete
  showDeptDelete = false;
  deptToDelete: Department | null = null;

  // ── Employees ────────────────────────────────────────────
  employees: Employee[] = [];
  filteredEmployees: Employee[] = [];
  empSearch = '';
  empFilterStatus = '';
  empFilterDept = '';
  isEmpLoading = true;

  // Employee stats
  totalActive = 0;
  totalOnLeave = 0;
  totalTerminated = 0;
  totalEmployees = 0;

  // Employee pagination
  readonly EMP_PAGE_SIZE = 7;
  empCurrentPage = 1;

  // Employee form
  showEmpForm = false;
  editingEmp: Employee | null = null;
  empForm: Employee = this.emptyEmpForm();
  isSavingEmp = false;
  empFormError = '';
  isEmpViewOnly = false;

  // Employee delete
  showEmpDelete = false;
  empToDelete: Employee | null = null;

  constructor(
    private deptService: DepartmentService,
    private empService: EmployeeService
  ) {}

  ngOnInit(): void {
    this.loadDepartments();
    this.loadEmployees();
  }

  // ═══════════════════════════════════════════════════════
  //  DEPARTMENTS
  // ═══════════════════════════════════════════════════════

  loadDepartments(): void {
    this.isDeptLoading = true;
    this.deptService.getAll().subscribe({
      next: data => {
        this.departments = data;
        this.filterDepts();
        this.isDeptLoading = false;
      },
      error: err => { console.error(err); this.isDeptLoading = false; }
    });
  }

  filterDepts(): void {
    const term = this.deptSearch.toLowerCase().trim();
    this.filteredDepartments = this.departments.filter(d =>
      !term ||
      d.name.toLowerCase().includes(term) ||
      (d.description || '').toLowerCase().includes(term) ||
      (d.headOfDepartment || '').toLowerCase().includes(term)
    );
  }

  openDeptDetail(dept: Department): void {
    this.selectedDept = dept;
    this.isDeptDetailOpen = true;
    this.deptEmployees = [];
    // Load employees for this department
    this.empService.getAllEmployees().subscribe({
      next: emps => {
        this.deptEmployees = emps.filter(e => e.department === dept.name);
      }
    });
  }

  closeDeptDetail(): void {
    this.isDeptDetailOpen = false;
    this.selectedDept = null;
  }

  openAddDeptForm(): void {
    this.editingDept = null;
    this.deptForm = this.emptyDeptForm();
    this.deptFormError = '';
    this.showDeptForm = true;
  }

  openEditDeptForm(dept: Department): void {
    this.editingDept = dept;
    this.deptForm = { ...dept };
    this.deptFormError = '';
    this.showDeptForm = true;
  }

  saveDept(): void {
    if (!this.deptForm.name?.trim()) {
      this.deptFormError = 'Department name is required.';
      return;
    }
    this.isSavingDept = true;
    this.deptFormError = '';
    if (this.editingDept?.id) {
      this.deptService.update(this.editingDept.id, this.deptForm).subscribe({
        next: () => { this.showDeptForm = false; this.isSavingDept = false; this.loadDepartments(); },
        error: () => { this.deptFormError = 'Failed to update department.'; this.isSavingDept = false; }
      });
    } else {
      this.deptService.create(this.deptForm).subscribe({
        next: () => { this.showDeptForm = false; this.isSavingDept = false; this.loadDepartments(); },
        error: () => { this.deptFormError = 'Failed to create department.'; this.isSavingDept = false; }
      });
    }
  }

  confirmDeleteDept(dept: Department): void {
    this.deptToDelete = dept;
    this.showDeptDelete = true;
  }

  executeDeleteDept(): void {
    if (!this.deptToDelete?.id) return;
    this.deptService.delete(this.deptToDelete.id).subscribe({
      next: () => { this.showDeptDelete = false; this.deptToDelete = null; this.loadDepartments(); },
      error: err => console.error(err)
    });
  }

  getDeptColor(name: string): string {
    const colors = [
      '#6366f1','#8b5cf6','#ec4899','#0ea5e9','#10b981',
      '#f59e0b','#ef4444','#14b8a6','#f97316','#64748b'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash);
    return colors[Math.abs(hash) % colors.length];
  }

  getDeptInitial(name: string): string {
    return name?.charAt(0)?.toUpperCase() || '?';
  }

  private emptyDeptForm(): Department {
    return { name: '', description: '', headOfDepartment: '' };
  }

  // ═══════════════════════════════════════════════════════
  //  EMPLOYEES
  // ═══════════════════════════════════════════════════════

  loadEmployees(): void {
    this.isEmpLoading = true;
    this.empService.getAllEmployees().subscribe({
      next: data => {
        this.employees = data;
        this.computeStats(data);
        this.filterEmployees();
        this.isEmpLoading = false;
      },
      error: err => { console.error(err); this.isEmpLoading = false; }
    });
  }

  computeStats(data: Employee[]): void {
    this.totalEmployees = data.length;
    this.totalActive = data.filter(e => e.employmentStatus === 'Active').length;
    this.totalOnLeave = data.filter(e => e.employmentStatus === 'On Leave').length;
    this.totalTerminated = data.filter(e => e.employmentStatus === 'Terminated').length;
  }

  filterEmployees(): void {
    this.empCurrentPage = 1;
    const term = this.empSearch.toLowerCase().trim();
    this.filteredEmployees = this.employees.filter(e => {
      const matchSearch = !term || [
        e.firstName, e.lastName, e.email, e.phone || '',
        e.jobTitle, e.department, e.employmentStatus
      ].some(v => v.toLowerCase().includes(term));
      const matchStatus = !this.empFilterStatus || e.employmentStatus === this.empFilterStatus;
      const matchDept = !this.empFilterDept || e.department === this.empFilterDept;
      return matchSearch && matchStatus && matchDept;
    });
  }

  get totalEmpPages(): number {
    return Math.ceil(this.filteredEmployees.length / this.EMP_PAGE_SIZE);
  }

  get pagedEmployees(): Employee[] {
    const start = (this.empCurrentPage - 1) * this.EMP_PAGE_SIZE;
    return this.filteredEmployees.slice(start, start + this.EMP_PAGE_SIZE);
  }

  get empPageNumbers(): number[] {
    return Array.from({ length: this.totalEmpPages }, (_, i) => i + 1);
  }

  goToEmpPage(p: number): void {
    if (p >= 1 && p <= this.totalEmpPages) this.empCurrentPage = p;
  }

  getEmpInitials(e: Employee): string {
    return `${e.firstName?.charAt(0) || ''}${e.lastName?.charAt(0) || ''}`.toUpperCase();
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'Active': return 'status-active';
      case 'On Leave': return 'status-leave';
      case 'Terminated': return 'status-terminated';
      default: return 'status-default';
    }
  }

  get allDepartmentNames(): string[] {
    return [...new Set(this.employees.map(e => e.department))].filter(Boolean).sort();
  }

  openAddEmpForm(): void {
    this.editingEmp = null;
    this.empForm = this.emptyEmpForm();
    this.empFormError = '';
    this.isEmpViewOnly = false;
    this.showEmpForm = true;
  }

  openEditEmpForm(emp: Employee): void {
    this.editingEmp = emp;
    this.empForm = { ...emp };
    this.empFormError = '';
    this.isEmpViewOnly = false;
    this.showEmpForm = true;
  }

  openViewEmp(emp: Employee): void {
    this.editingEmp = emp;
    this.empForm = { ...emp };
    this.isEmpViewOnly = true;
    this.showEmpForm = true;
  }

  saveEmp(): void {
    if (!this.empForm.firstName?.trim() || !this.empForm.lastName?.trim() || !this.empForm.email?.trim()) {
      this.empFormError = 'First name, last name and email are required.';
      return;
    }
    this.isSavingEmp = true;
    this.empFormError = '';
    if (this.editingEmp?.id) {
      this.empService.updateEmployee(this.editingEmp.id, this.empForm).subscribe({
        next: () => { this.showEmpForm = false; this.isSavingEmp = false; this.loadEmployees(); },
        error: () => { this.empFormError = 'Failed to update employee.'; this.isSavingEmp = false; }
      });
    } else {
      this.empService.createEmployee(this.empForm).subscribe({
        next: () => { this.showEmpForm = false; this.isSavingEmp = false; this.loadEmployees(); },
        error: () => { this.empFormError = 'Failed to create employee.'; this.isSavingEmp = false; }
      });
    }
  }

  confirmDeleteEmp(emp: Employee): void {
    this.empToDelete = emp;
    this.showEmpDelete = true;
  }

  executeDeleteEmp(): void {
    if (!this.empToDelete?.id) return;
    this.empService.deleteEmployee(this.empToDelete.id).subscribe({
      next: () => { this.showEmpDelete = false; this.empToDelete = null; this.loadEmployees(); },
      error: err => console.error(err)
    });
  }

  private emptyEmpForm(): Employee {
    return {
      firstName: '', lastName: '', email: '', phone: '',
      jobTitle: '', department: '', employmentStatus: 'Active', cin: ''
    };
  }

  minVal(a: number, b: number): number { return Math.min(a, b); }
}
