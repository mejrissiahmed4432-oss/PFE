import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent implements OnInit {
  user: any = {};
  isEditing = false;
  isSaving = false;
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  showToast = false;
  private userSub: Subscription | undefined;

  // Editable copy of user fields
  form = {
    firstName: '',
    lastName: '',
    email: '',
    phoneNumber: '',
    department: '',
    bio: ''
  };

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.userSub = this.authService.user$.subscribe(user => {
      this.user = user || {};
      if (!this.isEditing) {
        this.resetForm();
      }
    });
  }

  ngOnDestroy(): void {
    if (this.userSub) {
      this.userSub.unsubscribe();
    }
  }

  get initials(): string {
    const f = this.form.firstName?.charAt(0) || this.user?.firstName?.charAt(0) || 'U';
    const l = this.form.lastName?.charAt(0) || this.user?.lastName?.charAt(0) || '';
    return (f + l).toUpperCase();
  }

  get fullName(): string {
    const first = this.form.firstName || this.user?.firstName || '';
    const last = this.form.lastName || this.user?.lastName || '';
    return `${first} ${last}`.trim() || 'User';
  }

  get role(): string {
    return this.user?.role || 'Stock Manager';
  }

  resetForm(): void {
    this.form = {
      firstName: this.user?.firstName || '',
      lastName: this.user?.lastName || '',
      email: this.user?.email || '',
      phoneNumber: this.user?.phoneNumber || '',
      department: this.user?.department || 'IT Management',
      bio: this.user?.bio || ''
    };
  }

  startEdit(): void {
    this.isEditing = true;
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.resetForm();
  }

  phoneError: string = '';

  validatePhone(): boolean {
    if (!this.form.phoneNumber) {
      this.phoneError = 'Phone number is required';
      return false;
    }
    if (this.form.phoneNumber.length !== 8) {
      this.phoneError = 'Phone number must be 8 digits';
      return false;
    }
    this.phoneError = '';
    return true;
  }

  onPhoneKeypress(event: KeyboardEvent) {
    if (event.key < '0' || event.key > '9') {
      event.preventDefault();
    }
  }

  onPhoneInput(event: Event) {
    const input = event.target as HTMLInputElement;
    input.value = input.value.replace(/[^0-9]/g, '');
    this.form.phoneNumber = input.value;
    this.validatePhone();
  }

  get isFormInvalid(): boolean {
    return !this.form.firstName?.trim() || 
           !this.form.lastName?.trim() || 
           !this.form.phoneNumber || 
           this.form.phoneNumber.length !== 8;
  }

  get firstNameError(): string {
    return !this.form.firstName?.trim() ? 'First name is required' : '';
  }

  get lastNameError(): string {
    return !this.form.lastName?.trim() ? 'Last name is required' : '';
  }

  saveProfile(): void {
    if (!this.validatePhone()) {
      return;
    }
    this.isSaving = true;
    
    this.authService.updateProfile(this.form).subscribe({
      next: (response) => {
        this.user = { ...this.user, ...this.form };
        this.isSaving = false;
        this.isEditing = false;
        this.showNotification('Profile updated successfully!', 'success');
      },
      error: (err) => {
        this.isSaving = false;
        this.showNotification(err.error?.message || 'Error updating profile', 'error');
      }
    });
  }

  private showNotification(msg: string, type: 'success' | 'error'): void {
    this.toastMessage = msg;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 3500);
  }
}
