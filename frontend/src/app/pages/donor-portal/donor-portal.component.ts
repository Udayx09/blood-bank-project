import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, Donor } from '../../services/api.service';

@Component({
    selector: 'app-donor-portal',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './donor-portal.component.html',
    styleUrls: ['./donor-portal.component.css']
})
export class DonorPortalComponent implements OnInit {
    donor: Donor | null = null;
    donationHistory: any[] = [];
    donationRequests: any[] = [];
    isLoading = true;
    isEditing = false;
    isUploadingPhoto = false;
    errorMessage = '';
    successMessage = '';

    // Edit form data
    editForm = {
        name: '',
        bloodType: '',
        city: '',
        weight: 50
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
        { value: 'UNKNOWN', label: "Unknown" }
    ];

    cities = [
        { value: 'solapur', label: 'Solapur, Maharashtra' }
    ];

    constructor(
        private apiService: ApiService,
        private router: Router
    ) { }

    ngOnInit() {
        this.loadDonorData();
    }

    loadDonorData() {
        const donorToken = localStorage.getItem('donorToken');
        const donorData = localStorage.getItem('donorData');

        if (!donorToken) {
            // No donor session, redirect to login
            this.router.navigate(['/donor/login']);
            return;
        }

        // If we have cached data, use it first
        if (donorData) {
            this.donor = JSON.parse(donorData);
            this.isLoading = false;
            this.loadDonationHistory();
        }

        // Refresh from server using JWT auth
        this.apiService.getDonorAuthProfile().subscribe({
            next: (response) => {
                if (response.success && response.donor) {
                    this.donor = response.donor;
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.loadDonationHistory();
                }
                this.isLoading = false;
            },
            error: () => {
                this.isLoading = false;
                // Token might be invalid, redirect to login
                if (!this.donor) {
                    localStorage.removeItem('donorToken');
                    localStorage.removeItem('donorData');
                    this.router.navigate(['/donor/login']);
                }
            }
        });
    }

    loadDonationHistory() {
        if (!this.donor?.id) return;

        this.apiService.getDonationHistory(this.donor.id).subscribe({
            next: (response) => {
                if (response.success) {
                    this.donationHistory = response.donations || [];
                }
            },
            error: () => {
                // Silent fail for history
            }
        });

        // Also load donation requests
        this.loadDonorRequests();
    }

    loadDonorRequests() {
        if (!this.donor?.id) return;

        this.apiService.getDonorRequests(this.donor.id).subscribe({
            next: (response) => {
                if (response.success) {
                    this.donationRequests = response.requests || [];
                }
            },
            error: () => {
                // Silent fail
            }
        });
    }

    acceptRequest(requestId: number) {
        if (!this.donor?.id) return;

        this.apiService.respondToRequest(requestId, this.donor.id, true).subscribe({
            next: (response) => {
                if (response.success) {
                    this.successMessage = response.message;
                    this.loadDonorRequests();
                } else {
                    this.errorMessage = response.error;
                }
            },
            error: () => {
                this.errorMessage = 'Failed to accept request';
            }
        });
    }

    declineRequest(requestId: number) {
        if (!this.donor?.id) return;

        this.apiService.respondToRequest(requestId, this.donor.id, false).subscribe({
            next: (response) => {
                if (response.success) {
                    this.successMessage = response.message;
                    this.loadDonorRequests();
                } else {
                    this.errorMessage = response.error;
                }
            },
            error: () => {
                this.errorMessage = 'Failed to decline request';
            }
        });
    }

    startEditing() {
        if (!this.donor) return;
        this.editForm = {
            name: this.donor.name,
            bloodType: this.donor.bloodType,
            city: this.donor.city,
            weight: this.donor.weight
        };
        this.isEditing = true;
        this.errorMessage = '';
        this.successMessage = '';
    }

    cancelEditing() {
        this.isEditing = false;
        this.errorMessage = '';
    }

    saveProfile() {
        if (!this.donor?.id) return;

        if (!this.editForm.name?.trim()) {
            this.errorMessage = 'Name is required';
            return;
        }
        if (!this.editForm.weight || this.editForm.weight < 50) {
            this.errorMessage = 'Weight must be at least 50 kg';
            return;
        }

        this.apiService.updateDonorProfile(this.donor.id, this.editForm).subscribe({
            next: (response) => {
                if (response.success) {
                    this.donor = response.donor;
                    localStorage.setItem('donorData', JSON.stringify(response.donor));
                    this.isEditing = false;
                    this.successMessage = 'Profile updated successfully!';
                    setTimeout(() => this.successMessage = '', 3000);
                } else {
                    this.errorMessage = response.error || 'Failed to update profile';
                }
            },
            error: (error) => {
                this.errorMessage = error.error?.error || 'Failed to update profile';
            }
        });
    }

    getEligibilityProgress(): number {
        if (!this.donor) return 0;
        if (this.donor.eligible) return 100;
        const daysWaited = 90 - this.donor.daysUntilEligible;
        return Math.round((daysWaited / 90) * 100);
    }

    formatDate(dateString: string): string {
        if (!dateString) return 'N/A';
        const date = new Date(dateString);
        return date.toLocaleDateString('en-IN', {
            day: 'numeric',
            month: 'short',
            year: 'numeric'
        });
    }

    logout() {
        localStorage.removeItem('donorToken');
        localStorage.removeItem('donorData');
        this.router.navigate(['/donor/login']);
    }

    goHome() {
        this.router.navigate(['/']);
    }

    toggleContactAvailability() {
        if (!this.donor?.id) return;

        const newValue = !this.donor.isAvailableForContact;

        this.apiService.updateContactAvailability(this.donor.id, newValue).subscribe({
            next: (response) => {
                if (response.success) {
                    this.donor!.isAvailableForContact = response.isAvailableForContact;
                    localStorage.setItem('donorData', JSON.stringify(this.donor));
                    this.successMessage = response.message;
                    setTimeout(() => this.successMessage = '', 3000);
                } else {
                    this.errorMessage = response.error || 'Failed to update preference';
                }
            },
            error: (error) => {
                this.errorMessage = error.error?.error || 'Failed to update preference';
            }
        });
    }

    // ===================== PROFILE PHOTO =====================

    getPhotoUrl(): string {
        if (this.donor?.profilePhoto) {
            // Handle both absolute and relative URLs
            if (this.donor.profilePhoto.startsWith('http')) {
                return this.donor.profilePhoto;
            }
            return 'https://bloodbank-backend-701641288198.asia-south1.run.app' + this.donor.profilePhoto;
        }
        return '';
    }

    onPhotoSelected(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files[0]) {
            this.uploadPhoto(input.files[0]);
        }
    }

    uploadPhoto(file: File) {
        if (!file) return;

        // Validate file size (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            this.errorMessage = 'Photo too large. Max 5MB allowed.';
            return;
        }

        // Validate file type
        if (!file.type.startsWith('image/')) {
            this.errorMessage = 'Only image files are allowed.';
            return;
        }

        this.isUploadingPhoto = true;
        this.errorMessage = '';

        this.apiService.uploadProfilePhoto(file).subscribe({
            next: (response) => {
                this.isUploadingPhoto = false;
                if (response.success) {
                    this.donor!.profilePhoto = response.photoUrl;
                    localStorage.setItem('donorData', JSON.stringify(this.donor));
                    this.successMessage = 'Profile photo updated!';
                    setTimeout(() => this.successMessage = '', 3000);
                } else {
                    this.errorMessage = response.error || 'Failed to upload photo';
                }
            },
            error: (error) => {
                this.isUploadingPhoto = false;
                this.errorMessage = error.error?.error || 'Failed to upload photo';
            }
        });
    }
}

