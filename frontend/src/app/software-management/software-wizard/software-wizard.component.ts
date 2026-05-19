import { Component, EventEmitter, Output, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SoftwareService } from '../software.service';
import { 
  Software, 
  LicensePool, 
  LicenseModel, 
  ActivationMethod, 
  RenewalType 
} from '../software.model';

@Component({
  selector: 'app-software-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './software-wizard.component.html',
  styleUrls: ['./software-wizard.component.css']
})
export class SoftwareWizardComponent implements OnInit {
  @Input() visible = false;
  @Input() softwareToEdit: Software | null = null;
  @Output() closeWizard = new EventEmitter<boolean>();

  editMode = false;
  currentStep = 1;
  totalSteps = 4;
  stepLabels = ['General', 'Licensing', 'Keys', 'Purchase'];
  isSubmitting = false;

  // Step 1: Info
  software: Software = {
    name: '',
    type: 'Application',
    vendor: '',
    version: '',
    website: '',
    status: 'Active'
  };

  // Step 2 & 3 & 4 & 5: Pool
  pool: LicensePool = {
    licenseModel: LicenseModel.SUBSCRIPTION,
    activationMethod: ActivationMethod.USER_LOGIN,
    totalSeats: 0,
    renewalType: RenewalType.AUTO_RENEW,
    rawKeys: []
  };

  keysInput: string = '';

  // Static options for UI (initial)
  types = ['Application', 'OS', 'Tool', 'Service'];
  vendors = ['Microsoft', 'Adobe', 'Google', 'JetBrains', 'Slack'];
  
  // Dropdown states
  isVendorDropdownOpen = false;

  licenseModels = Object.values(LicenseModel);
  activationMethods = Object.values(ActivationMethod);
  renewalTypes = Object.values(RenewalType);

  constructor(private softwareService: SoftwareService) {}

  ngOnInit() {
    if (this.softwareToEdit) {
      this.editMode = true;
      this.software = { ...this.softwareToEdit };
      // For now, we only allow editing general info in the wizard if editing software
      // or we can fetch the first pool to edit it too.
      // But let's keep it simple: editMode only edits software info (Step 1)
      this.totalSteps = 1; 
      this.stepLabels = ['General Info'];
    }
  }

  get filteredVendors() {
    const search = (this.software.vendor || '').toLowerCase();
    return this.vendors.filter(v => v.toLowerCase().includes(search));
  }

  selectVendor(ven: string) {
    this.software.vendor = ven;
    this.isVendorDropdownOpen = false;
  }

  addNewVendor() {
    if (this.software.vendor && !this.vendors.includes(this.software.vendor)) {
      this.vendors.push(this.software.vendor);
    }
    this.isVendorDropdownOpen = false;
  }

  closeDropdowns() {
    setTimeout(() => {
      this.isVendorDropdownOpen = false;
    }, 200); // Delay allows click event on dropdown items to fire
  }

  autoSuggestActivation() {
    if (this.pool.licenseModel === LicenseModel.SUBSCRIPTION) {
      this.pool.activationMethod = ActivationMethod.USER_LOGIN;
      this.pool.renewalType = RenewalType.AUTO_RENEW;
    } else if (this.pool.licenseModel === LicenseModel.OEM) {
      this.pool.activationMethod = ActivationMethod.DEVICE_BOUND;
      this.pool.renewalType = RenewalType.NONE;
    } else if (this.pool.licenseModel === LicenseModel.VOLUME || this.pool.licenseModel === LicenseModel.PERPETUAL) {
      this.pool.activationMethod = ActivationMethod.KEY_BASED;
      this.pool.renewalType = RenewalType.MANUAL;
    }
  }

  nextStep() {
    if (this.currentStep === 2) {
      this.autoSuggestActivation();
    }
    if (this.currentStep < this.totalSteps) {
      this.currentStep++;
    }
  }

  prevStep() {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  goToStep(step: number) {
    if (step >= 1 && step <= this.totalSteps && step <= this.currentStep) {
      this.currentStep = step;
    }
  }

  submit() {
    this.isSubmitting = true;

    if (this.editMode && this.software.id) {
      this.softwareService.updateSoftware(this.software.id, this.software).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.closeWizard.emit(true);
        },
        error: (err) => {
          alert('Error updating software: ' + err.message);
          this.isSubmitting = false;
        }
      });
      return;
    }

    // Parse keys if key based
    if (this.pool.activationMethod === ActivationMethod.KEY_BASED && this.keysInput) {
      this.pool.rawKeys = this.keysInput.split('\n').map(k => k.trim()).filter(k => k);
    }

    this.softwareService.createSoftware(this.software).subscribe({
      next: (savedSw) => {
        this.softwareService.createLicensePool(savedSw.id!, this.pool).subscribe({
          next: () => {
            this.isSubmitting = false;
            this.closeWizard.emit(true);
          },
          error: (err) => {
            alert('Error creating license pool: ' + err.message);
            this.isSubmitting = false;
          }
        });
      },
      error: (err) => {
        alert('Error creating software: ' + err.message);
        this.isSubmitting = false;
      }
    });
  }

  cancel() {
    this.closeWizard.emit(false);
  }
}
