import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';

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

  // Editable copy of user fields
  form = {
    firstName: '',
    lastName: '',
    email: '',
    phone: '',
    department: '',
    bio: ''
  };

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser() || {};
    this.resetForm();
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
      phone: this.user?.phone || '',
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

  saveProfile(): void {
    this.isSaving = true;
    // Simulate API call – in production call authService.updateProfile(this.form)
    setTimeout(() => {
      // Persist in localStorage optimistically
      const updated = { ...this.user, ...this.form };
      localStorage.setItem('user_data', JSON.stringify(updated));
      this.user = updated;
      this.isSaving = false;
      this.isEditing = false;
      this.showNotification('Profile updated successfully!', 'success');
    }, 900);
  }

  private showNotification(msg: string, type: 'success' | 'error'): void {
    this.toastMessage = msg;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 3500);
  }
}
