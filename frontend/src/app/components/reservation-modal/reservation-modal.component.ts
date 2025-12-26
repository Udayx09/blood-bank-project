import { Component, EventEmitter, Input, Output, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, BloodBank, Reservation } from '../../services/api.service';

@Component({
    selector: 'app-reservation-modal',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './reservation-modal.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ReservationModalComponent {
    @Input() isOpen = false;
    @Input() bloodType: string = '';
    @Input() bloodBank: BloodBank | null = null;
    @Output() close = new EventEmitter<void>();

    form = {
        patientName: '',
        whatsappNumber: '',
        bloodType: '',
        unitsNeeded: 1,
        urgencyLevel: 'normal' as 'normal' | 'urgent' | 'emergency',
        additionalNotes: '',
        referringDoctor: '',
        isConfirmed: false
    };

    // Prescription upload
    prescriptionFile: File | null = null;
    prescriptionPreview: string | null = null;
    prescriptionPath: string | null = null;
    isUploading = false;

    isSubmitting = false;
    isSuccess = false;
    errorMessage: string | null = null;

    constructor(private apiService: ApiService) { }

    ngOnChanges() {
        if (this.bloodType) {
            this.form.bloodType = this.bloodType;
        }
    }

    decrementUnits() {
        if (this.form.unitsNeeded > 1) {
            this.form.unitsNeeded--;
        }
    }

    incrementUnits() {
        if (this.form.unitsNeeded < 10) {
            this.form.unitsNeeded++;
        }
    }

    closeModal() {
        this.resetForm();
        this.close.emit();
    }

    onSubmit() {
        if (!this.validateForm()) return;
        if (!this.bloodBank) return;

        this.isSubmitting = true;
        this.errorMessage = null;

        const reservation: Reservation = {
            patientName: this.form.patientName,
            whatsappNumber: this.form.whatsappNumber,
            bloodType: this.form.bloodType,
            unitsNeeded: this.form.unitsNeeded,
            urgencyLevel: this.form.urgencyLevel,
            additionalNotes: this.form.additionalNotes,
            bloodBankId: this.bloodBank.id,
            prescriptionPath: this.prescriptionPath!,
            referringDoctor: this.form.referringDoctor
        };

        this.apiService.createReservation(reservation).subscribe({
            next: (result) => {
                console.log('Reservation created:', result);
                this.isSubmitting = false;
                this.isSuccess = true;

                // Auto close after 3 seconds
                setTimeout(() => {
                    this.closeModal();
                }, 3000);
            },
            error: (err) => {
                console.error('Reservation error:', err);
                this.isSubmitting = false;
                // Extract actual error message from API response
                if (err.error && err.error.error) {
                    this.errorMessage = err.error.error;
                } else if (err.error && err.error.message) {
                    this.errorMessage = err.error.message;
                } else if (err.message) {
                    this.errorMessage = err.message;
                } else {
                    this.errorMessage = 'Failed to create reservation. Please try again.';
                }
                console.log('Error message set to:', this.errorMessage);
                alert('Error: ' + this.errorMessage);
            }
        });
    }

    validateForm(): boolean {
        if (!this.form.patientName.trim()) {
            alert('Please enter patient name');
            return false;
        }
        if (!this.form.whatsappNumber.trim() || this.form.whatsappNumber.length < 10) {
            alert('Please enter a valid WhatsApp number');
            return false;
        }
        if (this.form.unitsNeeded < 1 || this.form.unitsNeeded > 10) {
            alert('Units needed must be between 1 and 10');
            return false;
        }
        if (!this.form.referringDoctor.trim()) {
            alert('Please enter the referring doctor name');
            return false;
        }
        if (!this.prescriptionPath) {
            alert('Please upload doctor prescription');
            return false;
        }
        if (!this.form.isConfirmed) {
            alert('Please confirm that this request is genuine');
            return false;
        }
        return true;
    }

    resetForm() {
        this.form = {
            patientName: '',
            whatsappNumber: '',
            bloodType: this.bloodType,
            unitsNeeded: 1,
            urgencyLevel: 'normal',
            additionalNotes: '',
            referringDoctor: '',
            isConfirmed: false
        };
        this.isSuccess = false;
        this.isSubmitting = false;
        this.errorMessage = null;
        this.prescriptionFile = null;
        this.prescriptionPreview = null;
        this.prescriptionPath = null;
    }

    // Prescription upload methods
    onPrescriptionSelect(event: Event) {
        const input = event.target as HTMLInputElement;
        if (input.files && input.files[0]) {
            const file = input.files[0];

            // Validate file type
            if (!file.type.startsWith('image/') && file.type !== 'application/pdf') {
                alert('Please select an image or PDF file');
                return;
            }

            // Validate file size (5MB)
            if (file.size > 5 * 1024 * 1024) {
                alert('File must be less than 5MB');
                return;
            }

            this.prescriptionFile = file;

            // Create preview for images
            if (file.type.startsWith('image/')) {
                const reader = new FileReader();
                reader.onload = (e) => {
                    this.prescriptionPreview = e.target?.result as string;
                };
                reader.readAsDataURL(file);
            } else {
                this.prescriptionPreview = 'PDF';
            }

            // Upload file
            this.uploadPrescription();
        }
    }

    uploadPrescription() {
        if (!this.prescriptionFile) return;

        this.isUploading = true;

        const formData = new FormData();
        formData.append('file', this.prescriptionFile);

        this.apiService.uploadPrescription(formData).subscribe({
            next: (response: any) => {
                this.isUploading = false;
                if (response.success) {
                    this.prescriptionPath = response.url;
                }
            },
            error: () => {
                this.isUploading = false;
                alert('Failed to upload prescription');
            }
        });
    }

    removePrescription() {
        this.prescriptionFile = null;
        this.prescriptionPreview = null;
        this.prescriptionPath = null;
    }

    onBackdropClick(event: MouseEvent) {
        if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
            this.closeModal();
        }
    }
}
