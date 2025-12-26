import { Component, ViewEncapsulation, OnInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ReservationModalComponent } from '../../components/reservation-modal/reservation-modal.component';
import { ApiService, BloodBank } from '../../services/api.service';

@Component({
    selector: 'app-blood-search',
    standalone: true,
    imports: [CommonModule, ReservationModalComponent],
    templateUrl: './blood-search.component.html',
    encapsulation: ViewEncapsulation.None
})
export class BloodSearchComponent implements OnInit {
    @ViewChild('resultsSection') resultsSection!: ElementRef;

    bloodTypes: string[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
    selectedBloodType: string | null = null;
    searchResults: BloodBank[] = [];
    isSearching = false;
    isLoading = true;
    error: string | null = null;

    // Modal state
    isModalOpen = false;
    selectedBank: BloodBank | null = null;

    constructor(
        private router: Router,
        private apiService: ApiService
    ) { }

    ngOnInit() {
        // Check API health on load
        this.apiService.checkHealth().subscribe({
            next: () => {
                this.isLoading = false;
                console.log('✅ Connected to API');
            },
            error: (err) => {
                this.isLoading = false;
                this.error = 'Cannot connect to server. Please make sure the backend is running.';
                console.error('❌ API connection failed:', err);
            }
        });
    }

    selectBloodType(type: string) {
        this.selectedBloodType = type;
        this.searchForBlood();
    }

    searchForBlood() {
        if (!this.selectedBloodType) return;

        this.isSearching = true;
        this.error = null;

        this.apiService.searchByBloodType(this.selectedBloodType).subscribe({
            next: (results) => {
                this.searchResults = results;
                this.isSearching = false;
                console.log(`Found ${results.length} blood banks with ${this.selectedBloodType}`);
                // Auto-scroll to results after search completes
                this.scrollToResults();
            },
            error: (err) => {
                this.isSearching = false;
                this.error = 'Failed to search. Please try again.';
                console.error('Search error:', err);
                // Still scroll to show error message
                this.scrollToResults();
            }
        });
    }

    scrollToResults() {
        // Wait for the DOM to update before scrolling
        setTimeout(() => {
            if (this.resultsSection) {
                this.resultsSection.nativeElement.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        }, 100);
    }

    getAvailableUnits(bank: BloodBank): number {
        if (bank.unitsAvailable !== undefined) {
            return bank.unitsAvailable;
        }
        return bank.availableUnits?.[this.selectedBloodType!] || 0;
    }

    getUnitsColorClass(bank: BloodBank): string {
        const units = this.getAvailableUnits(bank);
        if (units < 5) {
            return 'units-low';      // Red - Critical
        } else if (units < 15) {
            return 'units-medium';   // Yellow - Medium
        } else {
            return 'units-high';     // Green - Good
        }
    }

    goBack() {
        this.router.navigate(['/']);
    }

    contactBank(bank: BloodBank) {
        alert(`Contact ${bank.name}\nPhone: ${bank.phone}\nEmail: ${bank.email}`);
    }

    openReservationModal(bank: BloodBank) {
        this.selectedBank = bank;
        this.isModalOpen = true;
    }

    closeModal() {
        this.isModalOpen = false;
        this.selectedBank = null;
    }
}
