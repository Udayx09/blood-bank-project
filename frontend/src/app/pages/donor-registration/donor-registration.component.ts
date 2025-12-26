import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, DonorRegistration } from '../../services/api.service';

type RegistrationStep = 'phone' | 'otp' | 'form' | 'success';

@Component({
    selector: 'app-donor-registration',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './donor-registration.component.html',
    styleUrls: ['./donor-registration.component.css']
})
export class DonorRegistrationComponent {
    currentStep: RegistrationStep = 'phone';

    // Phone step
    phoneNumber = '';
    isLoading = false;
    errorMessage = '';

    // OTP step
    otp = '';
    otpSent = false;
    resendTimer = 0;

    // Form step
    formData: DonorRegistration = {
        name: '',
        phone: '',
        bloodType: '',
        dateOfBirth: '',
        city: 'solapur',
        weight: 50,
        lastDonationDate: undefined
    };
    neverDonated = true;

    // Options
    bloodTypes = [
        { value: 'A+', label: 'A+' },
        { value: 'A-', label: 'A-' },
        { value: 'B+', label: 'B+' },
        { value: 'B-', label: 'B-' },
        { value: 'AB+', label: 'AB+' },
        { value: 'AB-', label: 'AB-' },
        { value: 'O+', label: 'O+' },
        { value: 'O-', label: 'O-' },
        { value: 'UNKNOWN', label: "I don't know / Unsure" }
    ];

    cities = [
        { value: 'solapur', label: 'Solapur, Maharashtra' }
    ];

    // Success step
    registeredDonor: any = null;

    constructor(
        private apiService: ApiService,
        private router: Router
    ) { }

    // Phone step
    sendOtp() {
        if (!this.phoneNumber || this.phoneNumber.length < 10) {
            this.errorMessage = 'Please enter a valid 10-digit phone number';
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';

        this.apiService.sendDonorOtp(this.phoneNumber).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    this.formData.phone = this.phoneNumber;
                    this.otpSent = true;
                    this.currentStep = 'otp';
                    this.startResendTimer();
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

    // OTP step
    verifyOtp() {
        if (!this.otp || this.otp.length !== 6) {
            this.errorMessage = 'Please enter the 6-digit OTP';
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';

        this.apiService.verifyDonorOtp(this.phoneNumber, this.otp).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success && response.verified) {
                    if (response.isExistingDonor) {
                        // Redirect to portal for existing donors
                        localStorage.setItem('donorPhone', this.phoneNumber);
                        localStorage.setItem('donorData', JSON.stringify(response.donor));
                        this.router.navigate(['/donor/portal']);
                    } else {
                        // New donor - go to registration form
                        this.currentStep = 'form';
                    }
                } else {
                    this.errorMessage = response.error || 'Invalid OTP';
                }
            },
            error: (error) => {
                this.isLoading = false;
                this.errorMessage = error.error?.error || 'Verification failed. Please try again.';
            }
        });
    }

    resendOtp() {
        if (this.resendTimer > 0) return;
        this.otp = '';
        this.sendOtp();
    }

    startResendTimer() {
        this.resendTimer = 60;
        const interval = setInterval(() => {
            this.resendTimer--;
            if (this.resendTimer <= 0) {
                clearInterval(interval);
            }
        }, 1000);
    }

    // Form step
    submitRegistration() {
        // Validate form
        if (!this.formData.name?.trim()) {
            this.errorMessage = 'Please enter your name';
            return;
        }
        if (!this.formData.bloodType) {
            this.errorMessage = 'Please select your blood type';
            return;
        }
        if (!this.formData.dateOfBirth) {
            this.errorMessage = 'Please enter your date of birth';
            return;
        }
        if (!this.formData.weight || this.formData.weight < 50) {
            this.errorMessage = 'Weight must be at least 50 kg';
            return;
        }

        // Validate age
        const age = this.calculateAge(this.formData.dateOfBirth);
        if (age < 18 || age > 65) {
            this.errorMessage = 'You must be between 18 and 65 years old to register';
            return;
        }

        // Set last donation date
        if (this.neverDonated) {
            this.formData.lastDonationDate = undefined;
        }

        this.isLoading = true;
        this.errorMessage = '';

        this.apiService.registerDonor(this.formData).subscribe({
            next: (response) => {
                this.isLoading = false;
                if (response.success) {
                    this.registeredDonor = response.donor;
                    localStorage.setItem('donorPhone', this.phoneNumber);
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.currentStep = 'success';
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

    calculateAge(dateOfBirth: string): number {
        const today = new Date();
        const birthDate = new Date(dateOfBirth);
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }
        return age;
    }

    goToPortal() {
        this.router.navigate(['/donor/portal']);
    }

    goBack() {
        if (this.currentStep === 'otp') {
            this.currentStep = 'phone';
            this.otp = '';
        } else if (this.currentStep === 'form') {
            this.currentStep = 'otp';
        }
        this.errorMessage = '';
    }

    goHome() {
        this.router.navigate(['/']);
    }
}
