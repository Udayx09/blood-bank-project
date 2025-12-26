import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService, DonorRegistrationWithPassword } from '../../services/api.service';

type ViewMode = 'login' | 'register' | 'set-password' | 'forgot-password' | 'reset-password';

@Component({
    selector: 'app-donor-login',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './donor-login.component.html',
    styleUrls: ['./donor-login.component.css']
})
export class DonorLoginComponent {
    viewMode: ViewMode = 'login';
    isLoading = false;
    errorMessage = '';
    successMessage = '';

    // Login form
    loginData = {
        phone: '',
        password: '',
        rememberMe: false
    };

    // Registration form
    registerData: DonorRegistrationWithPassword = {
        name: '',
        phone: '',
        password: '',
        bloodType: '',
        dateOfBirth: '',
        city: 'solapur',
        weight: 50
    };
    confirmPassword = '';

    // Set password (for existing donors)
    setPasswordData = {
        donorId: null as number | null,
        phone: '',
        password: '',
        confirmPassword: ''
    };

    // Forgot password
    forgotPasswordData = {
        phone: ''
    };

    // Reset password
    resetPasswordData = {
        phone: '',
        otp: '',
        newPassword: '',
        confirmPassword: ''
    };

    bloodTypes = [
        { value: 'A+', label: 'A+' },
        { value: 'A-', label: 'A-' },
        { value: 'B+', label: 'B+' },
        { value: 'B-', label: 'B-' },
        { value: 'AB+', label: 'AB+' },
        { value: 'AB-', label: 'AB-' },
        { value: 'O+', label: 'O+' },
        { value: 'O-', label: 'O-' },
        { value: 'UNKNOWN', label: "I don't know" }
    ];

    constructor(
        private apiService: ApiService,
        private router: Router
    ) {
        // Check if already logged in
        if (localStorage.getItem('donorToken')) {
            this.router.navigate(['/donor/portal']);
        }
    }

    login() {
        this.errorMessage = '';

        if (!this.loginData.phone || this.loginData.phone.length < 10) {
            this.errorMessage = 'Please enter a valid phone number';
            return;
        }

        this.isLoading = true;
        this.apiService.donorLogin(this.loginData.phone, this.loginData.password).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    localStorage.setItem('donorToken', response.token);
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    if (this.loginData.rememberMe) {
                        localStorage.setItem('donorRememberMe', 'true');
                    }
                    this.router.navigate(['/donor/portal']);
                } else if (response.requiresPasswordSetup) {
                    this.setPasswordData.donorId = response.donorId;
                    this.setPasswordData.phone = this.loginData.phone;
                    this.viewMode = 'set-password';
                    this.successMessage = response.message;
                    this.errorMessage = '';
                } else {
                    this.errorMessage = response.error || 'Login failed';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Login failed. Please try again.';
            }
        });
    }

    register() {
        this.errorMessage = '';

        if (!this.registerData.name?.trim()) {
            this.errorMessage = 'Please enter your name';
            return;
        }
        if (!this.registerData.phone || this.registerData.phone.length < 10) {
            this.errorMessage = 'Please enter a valid phone number';
            return;
        }
        if (!this.registerData.password || this.registerData.password.length < 6) {
            this.errorMessage = 'Password must be at least 6 characters';
            return;
        }
        if (this.registerData.password !== this.confirmPassword) {
            this.errorMessage = 'Passwords do not match';
            return;
        }
        if (!this.registerData.bloodType) {
            this.errorMessage = 'Please select your blood type';
            return;
        }
        if (!this.registerData.dateOfBirth) {
            this.errorMessage = 'Please enter your date of birth';
            return;
        }

        this.isLoading = true;
        this.apiService.donorRegister(this.registerData).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    localStorage.setItem('donorToken', response.token);
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.router.navigate(['/donor/portal']);
                } else {
                    this.errorMessage = response.error || 'Registration failed';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Registration failed. Please try again.';
            }
        });
    }

    setPassword() {
        this.errorMessage = '';

        if (!this.setPasswordData.password || this.setPasswordData.password.length < 6) {
            this.errorMessage = 'Password must be at least 6 characters';
            return;
        }
        if (this.setPasswordData.password !== this.setPasswordData.confirmPassword) {
            this.errorMessage = 'Passwords do not match';
            return;
        }

        this.isLoading = true;
        this.apiService.donorSetPassword(
            this.setPasswordData.donorId,
            this.setPasswordData.phone,
            this.setPasswordData.password
        ).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    localStorage.setItem('donorToken', response.token);
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.router.navigate(['/donor/portal']);
                } else {
                    this.errorMessage = response.error || 'Failed to set password';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Failed to set password. Please try again.';
            }
        });
    }

    // ===================== FORGOT PASSWORD =====================

    sendResetOtp() {
        this.errorMessage = '';

        if (!this.forgotPasswordData.phone || this.forgotPasswordData.phone.length < 10) {
            this.errorMessage = 'Please enter a valid phone number';
            return;
        }

        this.isLoading = true;
        this.apiService.donorForgotPassword(this.forgotPasswordData.phone).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    this.resetPasswordData.phone = response.phone || this.forgotPasswordData.phone;
                    this.viewMode = 'reset-password';
                    this.successMessage = 'OTP sent to your phone!';
                    this.errorMessage = '';
                } else {
                    this.errorMessage = response.error || 'Failed to send OTP';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Failed to send OTP. Please try again.';
            }
        });
    }

    resetPassword() {
        this.errorMessage = '';

        if (!this.resetPasswordData.otp || this.resetPasswordData.otp.length < 4) {
            this.errorMessage = 'Please enter the OTP';
            return;
        }
        if (!this.resetPasswordData.newPassword || this.resetPasswordData.newPassword.length < 6) {
            this.errorMessage = 'Password must be at least 6 characters';
            return;
        }
        if (this.resetPasswordData.newPassword !== this.resetPasswordData.confirmPassword) {
            this.errorMessage = 'Passwords do not match';
            return;
        }

        this.isLoading = true;
        this.apiService.donorResetPassword(
            this.resetPasswordData.phone,
            this.resetPasswordData.otp,
            this.resetPasswordData.newPassword
        ).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    localStorage.setItem('donorToken', response.token);
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.router.navigate(['/donor/portal']);
                } else {
                    this.errorMessage = response.error || 'Failed to reset password';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Failed to reset password. Please try again.';
            }
        });
    }

    // ===================== NAVIGATION =====================

    switchToRegister() {
        this.viewMode = 'register';
        this.errorMessage = '';
        this.successMessage = '';
    }

    switchToLogin() {
        this.viewMode = 'login';
        this.errorMessage = '';
        this.successMessage = '';
    }

    switchToForgotPassword() {
        this.viewMode = 'forgot-password';
        this.forgotPasswordData.phone = this.loginData.phone;
        this.errorMessage = '';
        this.successMessage = '';
    }

    goHome() {
        this.router.navigate(['/']);
    }
}
