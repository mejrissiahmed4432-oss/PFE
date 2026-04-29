import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OsService } from '../os.service';
import { OperatingSystem } from '../os.model';

@Component({
  selector: 'app-os-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './os-wizard.component.html',
  styleUrl: './os-wizard.component.css'
})
export class OsWizardComponent implements OnInit {
  @Input() visible: boolean = false;
  @Input() selectedOS: OperatingSystem | null = null;
  @Output() closeWizard = new EventEmitter<boolean>();

  osData: OperatingSystem = {
    name: '',
    version: '',
    edition: '',
    architecture: 'x64',
    licenseType: 'Volume',
    licenseKey: '',
    totalLicenses: 1,
    isoPath: '',
    size: '',
    requiredRam: 4,
    requiredStorage: 64,
    status: 'Active'
  };

  currentStep: number = 1;
  isSubmitting: boolean = false;

  constructor(private osService: OsService) {}

  ngOnInit(): void {
    if (this.selectedOS) {
      this.osData = { ...this.selectedOS };
    }
  }

  close(): void {
    this.closeWizard.emit(false);
  }

  nextStep(): void {
    if (this.currentStep === 1) {
      if (!this.osData.name || !this.osData.version || !this.osData.edition) {
        alert('Please fill in the required fields (Name, Version, Edition).');
        return;
      }
      this.currentStep = 2;
    }
  }

  prevStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  submitOS(): void {
    this.isSubmitting = true;
    
    if (this.osData.id) {
      // Update
      this.osService.updateOperatingSystem(this.osData.id, this.osData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.closeWizard.emit(true);
        },
        error: (err) => {
          console.error(err);
          this.isSubmitting = false;
          alert('Failed to update OS');
        }
      });
    } else {
      // Add
      this.osService.addOperatingSystem(this.osData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.closeWizard.emit(true);
        },
        error: (err) => {
          console.error(err);
          this.isSubmitting = false;
          alert('Failed to add OS');
        }
      });
    }
  }
}
