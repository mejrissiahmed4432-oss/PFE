import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../auth.service';

@Component({
    selector: 'app-login',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './login.component.html',
    styleUrl: './login.component.css'
})
export class LoginComponent implements OnInit {
    email: string = '';
    password: string = '';
    newPassword: string = '';
    confirmPassword: string = '';
    showPassword: boolean = false;
    showNewPassword: boolean = false;
    showConfirmPassword: boolean = false;
    isLoading: boolean = false;
    isForgotPassword: boolean = false;
    isResetPassword: boolean = false;
    resetToken: string | null = null;
    errorMessage: string = '';
    successMessage: string = '';
    currentYear: number = new Date().getFullYear();

    constructor(
        private router: Router,
        private route: ActivatedRoute,
        private authService: AuthService
    ) { }

    ngOnInit(): void {
        // Check for reset token in URL
        this.route.queryParams.subscribe(params => {
            if (params['token']) {
                this.resetToken = params['token'];
                this.isResetPassword = true;
            }
        });
    }

    togglePasswordVisibility(): void {
        this.showPassword = !this.showPassword;
    }

    toggleNewPasswordVisibility(): void {
        this.showNewPassword = !this.showNewPassword;
    }

    toggleConfirmPasswordVisibility(): void {
        this.showConfirmPassword = !this.showConfirmPassword;
    }

    onForgotPassword(): void {
        this.isForgotPassword = true;
        this.errorMessage = '';
        this.successMessage = '';
    }

    onBackToLogin(): void {
        this.isForgotPassword = false;
        this.errorMessage = '';
        this.successMessage = '';
    }

    onSendResetLink(): void {
        if (!this.email) {
            this.errorMessage = 'Please enter your email address.';
            return;
        }

        this.errorMessage = '';
        this.successMessage = '';
        this.isLoading = true;

        this.authService.forgotPassword(this.email).subscribe({
            next: (response: any) => {
                this.isLoading = false;
                this.successMessage = response.message || 'A reset link has been sent to your email.';
            },
            error: (err) => {
                this.isLoading = false;
                console.error('Password reset request failed:', err);
                this.errorMessage = err.error?.message || 'An error occurred. Please try again later.';
            }
        });
    }

    onResetPassword(): void {
        if (this.newPassword !== this.confirmPassword) {
            this.errorMessage = 'Passwords do not match.';
            return;
        }

        this.errorMessage = '';
        this.successMessage = '';
        this.isLoading = true;

        this.authService.resetPassword({
            token: this.resetToken!,
            newPassword: this.newPassword
        }).subscribe({
            next: (response: any) => {
                this.isLoading = false;
                this.successMessage = 'Password updated successfully! Redirecting to login...';
                setTimeout(() => {
                    this.isResetPassword = false;
                    this.router.navigate(['/login'], { queryParams: {} });
                }, 3000);
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMessage = err.error?.message || err.error || 'Failed to update password. Link may be expired.';
            }
        });
    }

    onSubmit(): void {
        // Basic check for empty fields
        if (!this.email || !this.password) {
            this.errorMessage = 'Please fill in all fields.';
            return;
        }

        this.errorMessage = '';
        this.isLoading = true;

        this.authService.login({ email: this.email, password: this.password }).subscribe({
            next: (response) => {
                this.isLoading = false;
                console.log('Login successful', response);
                this.router.navigate(['/board']);
            },
            error: (err) => {
                this.isLoading = false;
                console.error('Login failed', err);
                if (err.status === 401) {
                    this.errorMessage = 'Invalid email or password.';
                } else {
                    this.errorMessage = 'A server error occurred. Please try again later.';
                }
            }
        });
    }
}
