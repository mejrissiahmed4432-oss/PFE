import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareService } from '../software.service';
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
export class LicenseAssignmentModalComponent {
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

  constructor(private softwareService: SoftwareService) {}

  submit() {
    if (!this.pool || !this.assignment.assignedTargetName) return;

    this.isSubmitting = true;
    
    const payload: SoftwareAssignment = {
      licensePoolId: this.pool.id!,
      softwareId: this.pool.softwareId!,
      assignedToType: this.assignment.assignedToType!,
      assignedTargetId: this.assignment.assignedTargetId || 'TMP-' + Date.now(), // In real app, select from dropdown
      assignedTargetName: this.assignment.assignedTargetName,
      status: AssignmentStatus.ACTIVE,
      assignedBy: 'Admin_User', // Mocked user
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
