import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './settings.component.html',
  styleUrl: './settings.component.css'
})
export class SettingsComponent implements OnInit {
  // Password change
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  isSavingPassword = false;
  showCurrent = false;
  showNew = false;
  showConfirm = false;

  // Email change
  newEmail = '';
  confirmEmail = '';
  isSavingEmail = false;

  // Email Preferences
  emailNotifications = true;
  securityAlerts = true;
  marketingEmails = false;

  // Toast
  toastMessage = '';
  toastType: 'success' | 'error' = 'success';
  showToast = false;

  get passwordStrength(): number {
    const p = this.newPassword;
    if (!p) return 0;
    let score = 0;
    if (p.length >= 8) score++;
    if (/[A-Z]/.test(p)) score++;
    if (/[0-9]/.test(p)) score++;
    if (/[^A-Za-z0-9]/.test(p)) score++;
    return score; // 0-4
  }

  get strengthLabel(): string {
    const labels = ['', 'Weak', 'Fair', 'Good', 'Strong'];
    return labels[this.passwordStrength] || '';
  }

  get strengthClass(): string {
    const classes = ['', 'weak', 'fair', 'good', 'strong'];
    return classes[this.passwordStrength] || '';
  }

  constructor(private authService: AuthService, private http: HttpClient) {}

  ngOnInit(): void {}

  changePassword(): void {
    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.showNotification('Please fill in all password fields.', 'error');
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.showNotification('New passwords do not match.', 'error');
      return;
    }
    if (this.newPassword.length < 8) {
      this.showNotification('Password must be at least 8 characters.', 'error');
      return;
    }

    this.isSavingPassword = true;
    const token = localStorage.getItem('auth_token');
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.post('/api/users/change-password', {
      currentPassword: this.currentPassword,
      newPassword: this.newPassword
    }, { headers }).subscribe({
      next: () => {
        this.isSavingPassword = false;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.showNotification('Password updated successfully!', 'success');
      },
      error: () => {
        this.isSavingPassword = false;
        this.showNotification('Failed to update password. Please check your current password.', 'error');
      }
    });
  }

  changeEmail(): void {
    if (!this.newEmail || !this.confirmEmail) {
      this.showNotification('Please fill in both email fields.', 'error');
      return;
    }
    if (this.newEmail !== this.confirmEmail) {
      this.showNotification('Emails do not match.', 'error');
      return;
    }
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(this.newEmail)) {
      this.showNotification('Please enter a valid email address.', 'error');
      return;
    }

    this.isSavingEmail = true;
    const token = localStorage.getItem('auth_token');
    const headers = new HttpHeaders({ Authorization: `Bearer ${token}` });

    this.http.post('/api/users/change-email', {
      newEmail: this.newEmail
    }, { headers }).subscribe({
      next: () => {
        this.isSavingEmail = false;
        // Update local user data
        const userData = localStorage.getItem('user_data');
        if (userData) {
          const user = JSON.parse(userData);
          user.email = this.newEmail;
          localStorage.setItem('user_data', JSON.stringify(user));
        }
        this.newEmail = '';
        this.confirmEmail = '';
        this.showNotification('Email updated successfully!', 'success');
      },
      error: () => {
        this.isSavingEmail = false;
        this.showNotification('Failed to update email. Please try again.', 'error');
      }
    });
  }

  saveEmailPreferences(): void {
    this.showNotification('Email preferences saved!', 'success');
  }

  private showNotification(msg: string, type: 'success' | 'error'): void {
    this.toastMessage = msg;
    this.toastType = type;
    this.showToast = true;
    setTimeout(() => { this.showToast = false; }, 3500);
  }
}
