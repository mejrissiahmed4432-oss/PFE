import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';

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
  isChangingPassword = false;

  // Email change
  currentEmailAddress = '';
  newEmail = '';
  confirmEmail = '';
  emailPassword = '';
  showEmailPwd = false;
  isSavingEmail = false;
  isChangingEmail = false;

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

  constructor(
    private authService: AuthService, 
    private http: HttpClient,
    private router: Router
  ) {}

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    if (user) {
      this.currentEmailAddress = user.email || 'user&#64;example.com';
    }
  }

  togglePasswordChange(): void {
    this.isChangingPassword = !this.isChangingPassword;
    if (this.isChangingPassword) {
      this.isChangingEmail = false;
      this.cancelEmailChange();
    } else {
      this.cancelPasswordChange();
    }
  }

  toggleEmailChange(): void {
    this.isChangingEmail = !this.isChangingEmail;
    if (this.isChangingEmail) {
      this.isChangingPassword = false;
      this.cancelPasswordChange();
    } else {
      this.cancelEmailChange();
    }
  }

  cancelPasswordChange(): void {
    this.isChangingPassword = false;
    this.currentPassword = '';
    this.newPassword = '';
    this.confirmPassword = '';
  }

  cancelEmailChange(): void {
    this.isChangingEmail = false;
    this.newEmail = '';
    this.confirmEmail = '';
    this.emailPassword = '';
  }

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
      next: (res: any) => {
        if (res && res.success === false) {
          this.isSavingPassword = false;
          this.showNotification(res.message || 'Failed to update password.', 'error');
          return;
        }
        this.isSavingPassword = false;
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.showNotification('Password updated successfully! logging you out...', 'success');
        
        // Log out after a short delay
        setTimeout(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isSavingPassword = false;
        const errMsg = err.error?.message || 'Failed to update password. Please check your connection.';
        this.showNotification(errMsg, 'error');
      }
    });
  }

  changeEmail(): void {
    if (!this.newEmail || !this.confirmEmail || !this.emailPassword) {
      this.showNotification('Please fill in all email change fields, including your password.', 'error');
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
      newEmail: this.newEmail,
      password: this.emailPassword
    }, { headers }).subscribe({
      next: (res: any) => {
        if (res && res.success === false) {
          this.isSavingEmail = false;
          this.showNotification(res.message || 'Failed to update email.', 'error');
          return;
        }
        
        this.isSavingEmail = false;
        this.showNotification('Email updated successfully! logging you out...', 'success');
        
        // Log out after a short delay
        setTimeout(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        }, 2000);
      },
      error: (err) => {
        this.isSavingEmail = false;
        const errMsg = err.error?.message || 'Failed to update email. Please check your connection.';
        this.showNotification(errMsg, 'error');
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
