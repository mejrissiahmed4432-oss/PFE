import { Component, EventEmitter, Input, Output, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareService } from '../software.service';
import { AuthService } from '../../auth.service';
import { 
  LicensePool, 
  SoftwareAssignment, 
  AssignedToType, 
  AssignmentStatus 
} from '../software.model';

@Component({
  selector: 'app-license-assignment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './license-assignment-modal.component.html',
  styleUrls: ['./license-assignment-modal.component.css']
})
export class LicenseAssignmentModalComponent implements OnInit {
  @Input() pool: LicensePool | null = null;
  @Output() closeModal = new EventEmitter<boolean>();

  assignment: Partial<SoftwareAssignment> = {
    assignedToType: AssignedToType.USER,
    assignedTargetId: '',
    assignedTargetName: '',
    licenseKeyUsed: ''
  };

  assignedToTypes = Object.values(AssignedToType);
  isSubmitting = false;

  // User Selection State
  employees: any[] = [];
  filteredEmployees: any[] = [];
  uniqueRoles: string[] = [];
  searchQuery: string = '';
  selectedRoleFilter: string = '';
  selectedEmployee: any | null = null;
  isLoadingUsers: boolean = false;

  constructor(
    private softwareService: SoftwareService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.fetchEmployees();
  }

  fetchEmployees() {
    this.isLoadingUsers = true;
    this.authService.getAllUsers().subscribe({
      next: (data) => {
        this.employees = data || [];
        this.filteredEmployees = data || [];
        
        // Extract unique roles for the dropdown (from .role field)
        const roles = this.employees.map(user => user.role).filter(r => !!r);
        this.uniqueRoles = Array.from(new Set(roles)).sort();
        
        this.isLoadingUsers = false;
      },
      error: (err) => {
        console.error('Failed to load users', err);
        this.isLoadingUsers = false;
      }
    });
  }

  onSearchInput() {
    if (this.selectedEmployee && this.searchQuery !== `${this.selectedEmployee.firstName} ${this.selectedEmployee.lastName}`) {
      this.selectedEmployee = null;
      this.assignment.assignedTargetId = '';
      this.assignment.assignedTargetName = '';
    }
    
    this.filterUsers();
  }
  
  onRoleFilterChange() {
    this.filterUsers();
  }

  filterUsers() {
    const q = this.searchQuery.toLowerCase().trim();
    const r = this.selectedRoleFilter;

    this.filteredEmployees = this.employees.filter(emp => {
      const fName = emp.firstName || emp.name || '';
      const lName = emp.lastName || '';
      const name = `${fName} ${lName}`.trim().toLowerCase();
      const email = (emp.email || '').toLowerCase();
      
      const matchesText = !q || name.includes(q) || email.includes(q);
      const matchesRole = !r || emp.role === r;
      
      return matchesText && matchesRole;
    });
  }

  selectEmployee(emp: any) {
    this.selectedEmployee = emp;
    this.assignment.assignedTargetId = emp.id;
    const fName = emp.firstName || emp.name || '';
    const lName = emp.lastName || '';
    this.assignment.assignedTargetName = `${fName} ${lName}`.trim();
  }

  submit() {
    if (!this.pool) return;

    // Validation
    if (this.assignment.assignedToType === AssignedToType.USER && !this.selectedEmployee) {
        alert('Please select a user from the list.');
        return;
    }
    if (this.assignment.assignedToType === AssignedToType.DEVICE && !this.assignment.assignedTargetName) {
        alert('Please enter a device name.');
        return;
    }

    this.isSubmitting = true;
    
    const payload: SoftwareAssignment = {
      licensePoolId: this.pool.id!,
      softwareId: this.pool.softwareId!,
      assignedToType: this.assignment.assignedToType!,
      assignedTargetId: this.assignment.assignedToType === AssignedToType.USER ? this.selectedEmployee!.id! : 'DEV-' + Date.now(),
      assignedTargetName: this.assignment.assignedToType === AssignedToType.USER ? this.assignment.assignedTargetName! : this.assignment.assignedTargetName!,
      status: AssignmentStatus.ACTIVE,
      assignedBy: 'IT_Manager', // Mocked user
      licenseKeyUsed: this.assignment.licenseKeyUsed
    };

    this.softwareService.assignLicense(payload).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.closeModal.emit(true);
      },
      error: (err) => {
        alert('Failed to assign license: ' + err.message);
        this.isSubmitting = false;
      }
    });
  }

  cancel() {
    this.closeModal.emit(false);
  }
}
