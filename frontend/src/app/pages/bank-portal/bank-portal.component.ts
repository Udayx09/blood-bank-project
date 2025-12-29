import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

@Component({
    selector: 'app-bank-portal',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule, BaseChartDirective],
    templateUrl: './bank-portal.component.html',
    styleUrl: './bank-portal.component.css'
})
export class BankPortalComponent implements OnInit {
    bankInfo: any = null;
    stats: any = null;
    reservations: any[] = [];
    inventory: any[] = [];
    expiringBlood: any = null;
    loading = true;
    activeView = 'dashboard';

    // Toast notifications
    toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    // Inline form error (shown inside modals)
    formError: string | null = null;

    // Add Inventory Form
    showAddInventoryForm = false;
    allBloodTypes = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
    newInventory = {
        bloodType: '',
        units: 0
    };

    // Donor Search
    donors: any[] = [];
    donorRequests: any[] = [];
    searchBloodType = 'ALL';
    loadingDonors = false;
    sendingRequest: number | null = null;

    // Blood Units (Component Tracking)
    bloodUnits: any[] = [];
    bloodComponents: any[] = [];
    expirySummary: any = null;
    loadingUnits = false;
    showAddUnitForm = false;
    showRecordDonationForm = false;
    newUnit = {
        bloodType: '',
        component: '',
        collectionDate: '',
        unitNumber: ''
    };
    newDonation = {
        phone: '',
        donationDate: '',
        bloodType: '',
        // For new donors only
        donorName: '',
        donorDateOfBirth: '',
        // Phone lookup result
        donorFound: false,
        donorId: null as number | null,
        foundDonorName: '',
        foundBloodType: '',
        isEligible: true,
        daysUntilEligible: 0
    };
    // For adding components to pending donation
    pendingDonations: any[] = [];
    selectedPendingDonation: any = null;
    selectedComponents: string[] = [];
    showAddComponentsModal = false;

    // Prescription viewer
    viewingPrescription: string | null = null;
    viewedPrescriptions: Set<number> = new Set(
        JSON.parse(localStorage.getItem('viewedPrescriptions') || '[]')
    ); // Track which reservations had prescription viewed - persisted

    unitFilter = {
        status: 'AVAILABLE',
        bloodType: '',
        component: '',
        expiryStatus: ''
    };

    // Analytics Charts
    analyticsLoaded = false;

    // Inventory Doughnut Chart
    inventoryChartData: ChartData<'doughnut'> = {
        labels: [],
        datasets: [{
            data: [],
            backgroundColor: ['#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'],
            borderColor: 'rgba(255,255,255,0.1)',
            borderWidth: 2
        }]
    };
    inventoryChartOptions: ChartConfiguration<'doughnut'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 1500, easing: 'easeOutQuart' },
        plugins: {
            legend: { position: 'bottom', labels: { color: '#fff', padding: 15 } }
        }
    };

    // Donations Line Chart
    donationsChartData: ChartData<'line'> = {
        labels: [],
        datasets: [{
            data: [],
            label: 'Donations',
            fill: true,
            borderColor: '#2ed573',
            backgroundColor: 'rgba(46, 213, 115, 0.2)',
            tension: 0.4,
            pointBackgroundColor: '#2ed573',
            pointBorderColor: '#fff',
            pointRadius: 6,
            pointHoverRadius: 8
        }]
    };
    donationsChartOptions: ChartConfiguration<'line'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 1500, easing: 'easeOutQuart' },
        scales: {
            x: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.1)' } },
            y: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.1)' }, beginAtZero: true }
        },
        plugins: {
            legend: { display: false }
        }
    };

    // Reservations Bar Chart
    reservationsChartData: ChartData<'bar'> = {
        labels: ['Pending', 'Confirmed', 'Completed', 'Cancelled'],
        datasets: [{
            data: [],
            label: 'Reservations',
            backgroundColor: ['#f59e0b', '#3b82f6', '#22c55e', '#ef4444'],
            borderRadius: 8
        }]
    };
    reservationsChartOptions: ChartConfiguration<'bar'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 1500, easing: 'easeOutQuart' },
        scales: {
            x: { ticks: { color: '#94a3b8' }, grid: { display: false } },
            y: { ticks: { color: '#94a3b8' }, grid: { color: 'rgba(255,255,255,0.1)' }, beginAtZero: true }
        },
        plugins: {
            legend: { display: false }
        }
    };

    constructor(private apiService: ApiService, private router: Router) {
        // Check if logged in
        const token = localStorage.getItem('bankToken');
        if (!token) {
            this.router.navigate(['/bank-login']);
            return;
        }
        this.bankInfo = JSON.parse(localStorage.getItem('bankInfo') || '{}');
    }


    ngOnInit() {
        this.loadStats();
        this.loadReservations();
        this.loadInventory();
        this.loadExpiringBlood();
        this.loadPendingDonations();
    }

    loadStats() {
        this.apiService.getBankStats().subscribe({
            next: (data) => {
                this.stats = data;
                this.loading = false;
            },
            error: (err) => {
                console.error('Failed to load stats:', err);
                // If 401 (unauthorized) or 404 (blood bank deleted), logout
                if (err.status === 401 || err.status === 404) {
                    this.showToast('Blood bank session expired or was deleted. Please login again.', 'error');
                    setTimeout(() => this.logout(), 2000);
                }
                this.loading = false;
            }
        });
    }

    loadReservations() {
        this.apiService.getBankReservations().subscribe({
            next: (data) => {
                this.reservations = data;
            },
            error: (err) => console.error('Failed to load reservations:', err)
        });
    }

    loadInventory() {
        this.apiService.getBankInventory().subscribe({
            next: (data) => {
                this.inventory = data;
            },
            error: (err) => console.error('Failed to load inventory:', err)
        });
    }

    loadExpiringBlood() {
        this.apiService.getExpiringBlood().subscribe({
            next: (data) => {
                this.expiringBlood = data;
            },
            error: (err) => console.error('Failed to load expiring blood:', err)
        });
    }

    setActiveView(view: string) {
        this.activeView = view;
        if (view === 'units') {
            this.loadBloodUnits();
            this.loadBloodComponents();
            this.loadExpirySummary();
        }
        if (view === 'donors') {
            this.searchDonors();
        }
        if (view === 'analytics') {
            this.loadAnalytics();
        }
    }

    // ===================== BLOOD UNITS METHODS =====================

    loadBloodComponents() {
        this.apiService.getBloodComponents().subscribe({
            next: (response) => {
                if (response.success) {
                    this.bloodComponents = response.data;
                }
            },
            error: (err) => console.error('Failed to load components:', err)
        });
    }

    loadBloodUnits() {
        this.loadingUnits = true;
        this.apiService.getBloodUnits(
            this.unitFilter.status,
            this.unitFilter.bloodType,
            this.unitFilter.component
        ).subscribe({
            next: (response) => {
                this.loadingUnits = false;
                if (response.success) {
                    let units = response.data;
                    // Client-side expiry status filtering (cumulative)
                    if (this.unitFilter.expiryStatus) {
                        units = units.filter((u: any) => {
                            const days = u.daysUntilExpiry;
                            switch (this.unitFilter.expiryStatus) {
                                case 'critical': return days <= 3 && days >= 0;
                                case 'warning': return days <= 7 && days >= 0;
                                case 'expired': return days < 0;
                                case 'good': return days > 7;
                                default: return true;
                            }
                        });
                    }
                    this.bloodUnits = units;
                }
            },
            error: (err) => {
                this.loadingUnits = false;
                console.error('Failed to load blood units:', err);
            }
        });
    }

    loadExpirySummary() {
        this.apiService.getExpirySummary().subscribe({
            next: (response) => {
                if (response.success) {
                    this.expirySummary = response.data;
                }
            },
            error: (err) => console.error('Failed to load expiry summary:', err)
        });
    }

    filterByExpiry(status: string) {
        // Reset status filter to show all units when filtering by expiry
        this.unitFilter.status = '';
        this.unitFilter.bloodType = '';
        this.unitFilter.expiryStatus = status;
        console.log('Filtering by expiry:', status);
        console.log('Current units:', this.bloodUnits.map(u => ({ id: u.id, expiryStatus: u.expiryStatus, daysLeft: u.daysUntilExpiry })));
        this.applyUnitFilter();
    }

    openAddUnitForm() {
        this.showAddUnitForm = true;
        this.formError = null;
        this.newUnit = {
            bloodType: '',
            component: '',
            collectionDate: new Date().toISOString().split('T')[0],
            unitNumber: ''
        };
    }

    closeAddUnitForm() {
        this.showAddUnitForm = false;
        this.formError = null;
    }

    addBloodUnit() {
        if (!this.newUnit.bloodType || !this.newUnit.component || !this.newUnit.collectionDate) {
            this.formError = 'Please fill all required fields';
            return;
        }
        this.formError = null;

        this.apiService.addBloodUnit({
            bloodType: this.newUnit.bloodType,
            component: this.newUnit.component,
            collectionDate: this.newUnit.collectionDate,
            unitNumber: this.newUnit.unitNumber || undefined
        }).subscribe({
            next: (response) => {
                if (response.success) {
                    this.showToast('Blood unit added successfully', 'success');
                    this.closeAddUnitForm();
                    this.loadBloodUnits();
                    this.loadExpirySummary();
                    this.loadInventory();
                } else {
                    this.formError = response.error || 'Failed to add unit';
                }
            },
            error: (err) => {
                this.formError = err.error?.error || 'Failed to add unit';
            }
        });
    }

    // Record Donation Methods - TWO-STEP WORKFLOW
    openRecordDonationForm() {
        this.showRecordDonationForm = true;
        this.formError = null;
        this.newDonation = {
            phone: '',
            donationDate: new Date().toISOString().split('T')[0],
            bloodType: '',
            donorName: '',
            donorDateOfBirth: '',
            donorFound: false,
            donorId: null,
            foundDonorName: '',
            foundBloodType: '',
            isEligible: true,
            daysUntilEligible: 0
        };
    }

    closeRecordDonationForm() {
        this.showRecordDonationForm = false;
        this.formError = null;
    }

    // Phone lookup for donor
    lookupDonor() {
        if (!this.newDonation.phone || this.newDonation.phone.length < 10) {
            this.formError = 'Please enter a valid phone number';
            return;
        }
        this.formError = null;

        this.apiService.lookupDonor(this.newDonation.phone).subscribe({
            next: (response) => {
                if (response.success && response.found) {
                    const donor = response.donor;
                    this.newDonation.donorFound = true;
                    this.newDonation.donorId = donor.id;
                    this.newDonation.foundDonorName = donor.name;
                    this.newDonation.foundBloodType = donor.bloodType;
                    this.newDonation.bloodType = donor.bloodType;
                    this.newDonation.isEligible = donor.eligible;
                    this.newDonation.daysUntilEligible = donor.daysUntilEligible;
                    this.showToast(`Found: ${donor.name} (${donor.bloodType})`, 'success');
                } else {
                    this.newDonation.donorFound = false;
                    this.newDonation.donorId = null;
                    this.showToast('Donor not found - Please enter details for new donor', 'info');
                }
            },
            error: () => {
                this.showToast('Error looking up donor', 'error');
            }
        });
    }

    toggleComponent(code: string) {
        const index = this.selectedComponents.indexOf(code);
        if (index > -1) {
            this.selectedComponents.splice(index, 1);
        } else {
            this.selectedComponents.push(code);
        }
    }

    isComponentSelected(code: string): boolean {
        return this.selectedComponents.includes(code);
    }

    selectAllComponents() {
        if (this.areAllComponentsSelected()) {
            this.selectedComponents = [];
        } else {
            this.selectedComponents = this.bloodComponents.map(c => c.code);
        }
    }

    areAllComponentsSelected(): boolean {
        return this.bloodComponents.length > 0 &&
            this.selectedComponents.length === this.bloodComponents.length;
    }

    // Step 1: Record donation (donor + date only)
    recordDonation() {
        if (!this.newDonation.phone) {
            this.formError = 'Please enter phone number';
            return;
        }
        if (!this.newDonation.donationDate) {
            this.formError = 'Please enter donation date';
            return;
        }

        // If new donor, validate name and DOB
        if (!this.newDonation.donorFound) {
            if (!this.newDonation.donorName || !this.newDonation.donorDateOfBirth || !this.newDonation.bloodType) {
                this.formError = 'New donor requires name, date of birth, and blood type';
                return;
            }
        }
        this.formError = null;

        this.apiService.recordDonationStep1({
            phone: this.newDonation.phone,
            donationDate: this.newDonation.donationDate,
            bloodType: this.newDonation.bloodType,
            donorName: this.newDonation.donorName,
            donorDateOfBirth: this.newDonation.donorDateOfBirth
        }).subscribe({
            next: (response) => {
                if (response.success) {
                    this.showToast(response.message, 'success');
                    this.closeRecordDonationForm();
                    this.loadPendingDonations();
                } else {
                    this.formError = response.error || 'Failed to record donation';
                }
            },
            error: (err) => {
                this.formError = err.error?.error || 'Failed to record donation';
            }
        });
    }

    // Load pending donations (no components added yet)
    loadPendingDonations() {
        this.apiService.getPendingDonations().subscribe({
            next: (response) => {
                if (response.success) {
                    this.pendingDonations = response.donations || [];
                }
            },
            error: () => {
                // Silent fail
            }
        });
    }

    // Open add components modal
    openAddComponentsModal(donation: any) {
        this.selectedPendingDonation = donation;
        this.selectedComponents = [];
        this.formError = null;
        this.showAddComponentsModal = true;
    }

    closeAddComponentsModal() {
        this.showAddComponentsModal = false;
        this.selectedPendingDonation = null;
        this.selectedComponents = [];
        this.formError = null;
    }

    // Step 2: Add components to pending donation
    addComponentsToDonation() {
        if (!this.selectedPendingDonation || this.selectedComponents.length === 0) {
            this.formError = 'Please select at least one component';
            return;
        }
        this.formError = null;

        this.apiService.addComponents(this.selectedPendingDonation.id, this.selectedComponents).subscribe({
            next: (response) => {
                if (response.success) {
                    this.showToast(response.message, 'success');
                    this.closeAddComponentsModal();
                    this.loadPendingDonations();
                    this.loadBloodUnits();
                    this.loadExpirySummary();
                    this.loadInventory();
                } else {
                    this.formError = response.error || 'Failed to add components';
                }
            },
            error: (err) => {
                this.formError = err.error?.error || 'Failed to add components';
            }
        });
    }

    updateUnitStatus(unit: any, status: string) {
        this.apiService.updateBloodUnitStatus(unit.id, status).subscribe({
            next: (response) => {
                if (response.success) {
                    unit.status = status;
                    this.showToast(`Unit marked as ${status}`, 'success');
                    this.loadExpirySummary();
                    this.loadInventory();
                    this.loadBloodUnits();
                } else {
                    this.showToast(response.error || 'Failed to update status', 'error');
                    // Update unit data if returned (e.g., when expired unit auto-updated)
                    if (response.data) {
                        unit.status = response.data.status;
                    }
                }
            },
            error: (err) => {
                this.showToast(err.error?.error || 'Failed to update status', 'error');
            }
        });
    }

    deleteUnit(unit: any) {
        if (!confirm('Are you sure you want to delete this unit?')) return;

        this.apiService.deleteBloodUnit(unit.id).subscribe({
            next: (response) => {
                if (response.success) {
                    this.bloodUnits = this.bloodUnits.filter(u => u.id !== unit.id);
                    this.showToast('Unit deleted', 'success');
                    this.loadExpirySummary();
                    this.loadInventory();
                }
            },
            error: (err) => {
                this.showToast('Failed to delete unit', 'error');
            }
        });
    }

    getExpiryStatusClass(status: string): string {
        switch (status) {
            case 'critical': return 'status-critical';
            case 'warning': return 'status-warning';
            case 'caution': return 'status-caution';
            case 'expired': return 'status-expired';
            default: return 'status-good';
        }
    }

    applyUnitFilter() {
        this.loadBloodUnits();
    }

    updateStatus(reservation: any, status: string) {
        this.apiService.updateBankReservationStatus(reservation.id, status).subscribe({
            next: () => {
                reservation.status = status;
                this.loadStats();
                this.showToast(`Reservation ${status}`, 'success');
            },
            error: (err) => {
                console.error('Failed to update status:', err);
                this.showToast('Failed to update status', 'error');
            }
        });
    }

    confirmReservation(reservation: any) {
        // Check if prescription exists and hasn't been viewed
        if (reservation.prescriptionPath && !this.hasPrescriptionViewed(reservation.id)) {
            alert('Please view the prescription before confirming the reservation.');
            return;
        }
        // If viewed or no prescription, proceed to complete
        this.updateStatus(reservation, 'completed');
    }

    updateInventory(bloodType: string, units: number, item?: any) {
        // Optimistic update
        if (item) {
            item.units_available = units;
        }

        this.apiService.updateBankInventory(bloodType, units).subscribe({
            next: () => {
                this.loadStats();
            },
            error: (err) => {
                console.error('Failed to update inventory:', err);
                this.loadInventory();
                this.showToast('Failed to update inventory', 'error');
            }
        });
    }

    // Toast notification
    showToast(message: string, type: 'success' | 'error' | 'info' = 'success') {
        this.toast = { message, type };
        setTimeout(() => {
            this.toast = null;
        }, 3000);
    }

    updateInventoryWithAnimation(bloodType: string, units: number, direction: 'increase' | 'decrease', item: any) {
        // Set animation state
        item.animating = direction;

        // Update the inventory
        this.updateInventory(bloodType, units, item);

        // Clear animation after it completes
        setTimeout(() => {
            item.animating = null;
        }, 300);
    }

    // Add Inventory Methods
    openAddInventoryForm() {
        this.newInventory = { bloodType: '', units: 0 };
        this.formError = null;
        this.showAddInventoryForm = true;
    }

    closeAddInventoryForm() {
        this.showAddInventoryForm = false;
        this.formError = null;
    }

    getAvailableBloodTypes(): string[] {
        const existingTypes = this.inventory.map(i => i.blood_type);
        return this.allBloodTypes.filter(t => !existingTypes.includes(t));
    }

    addInventory() {
        if (!this.newInventory.bloodType || this.newInventory.units <= 0) {
            this.formError = 'Please select blood type and enter valid units';
            return;
        }
        this.formError = null;

        this.apiService.updateBankInventory(this.newInventory.bloodType, this.newInventory.units).subscribe({
            next: () => {
                this.loadInventory();
                this.loadStats();
                this.closeAddInventoryForm();
                this.showToast(`Added ${this.newInventory.units} units of ${this.newInventory.bloodType}`, 'success');
            },
            error: (err) => {
                console.error('Failed to add inventory:', err);
                this.formError = 'Failed to add inventory';
            }
        });
    }

    logout() {
        localStorage.removeItem('bankToken');
        localStorage.removeItem('bankInfo');
        this.router.navigate(['/bank-login']);
    }

    formatDate(dateString: string): string {
        if (!dateString) return '';
        // Ensure the date is parsed as UTC if it doesn't have timezone info
        let date = new Date(dateString);
        if (!dateString.endsWith('Z') && !dateString.includes('+')) {
            date = new Date(dateString + 'Z');
        }
        return date.toLocaleString('en-IN', {
            day: 'numeric',
            month: 'short',
            hour: '2-digit',
            minute: '2-digit',
            hour12: true,
            timeZone: 'Asia/Kolkata'
        });
    }

    getStatusClass(status: string): string {
        switch (status) {
            case 'pending': return 'status-pending';
            case 'confirmed': return 'status-confirmed';
            case 'completed': return 'status-completed';
            case 'cancelled': return 'status-cancelled';
            default: return '';
        }
    }

    // ==================== DONOR SEARCH METHODS ====================

    searchDonors() {
        this.loadingDonors = true;
        this.apiService.searchDonors(this.searchBloodType).subscribe({
            next: (data: any) => {
                this.donors = data.donors || [];
                this.loadingDonors = false;
            },
            error: (err) => {
                console.error('Failed to search donors:', err);
                this.showToast('Failed to load donors', 'error');
                this.loadingDonors = false;
            }
        });
    }

    sendDonationRequest(donorId: number) {
        this.sendingRequest = donorId;
        this.apiService.sendDonationRequest(donorId).subscribe({
            next: (data: any) => {
                if (data.success) {
                    this.showToast('Donation request sent!', 'success');
                    // Remove donor from list (they're now contacted)
                    this.donors = this.donors.filter(d => d.id !== donorId);
                    this.loadDonorRequests();
                } else {
                    this.showToast(data.error || 'Failed to send request', 'error');
                }
                this.sendingRequest = null;
            },
            error: (err) => {
                console.error('Failed to send request:', err);
                this.showToast(err.error?.error || 'Failed to send request', 'error');
                this.sendingRequest = null;
            }
        });
    }

    loadDonorRequests() {
        this.apiService.getDonorRequestHistory().subscribe({
            next: (data: any) => {
                this.donorRequests = data.requests || [];
            },
            error: (err) => console.error('Failed to load donor requests:', err)
        });
    }

    getRequestStatusClass(status: string): string {
        switch (status) {
            case 'PENDING': return 'status-pending';
            case 'ACCEPTED': return 'status-confirmed';
            case 'DECLINED': return 'status-cancelled';
            case 'EXPIRED': return 'status-expired';
            default: return '';
        }
    }

    loadAnalytics() {
        if (this.analyticsLoaded) return;
        this.analyticsLoaded = true;

        // Load inventory distribution chart
        this.apiService.getBankInventoryDistribution().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.inventoryChartData = {
                    labels: data.map((d: any) => d.type),
                    datasets: [{
                        data: data.map((d: any) => d.units || 0),
                        backgroundColor: ['#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'],
                        borderColor: 'rgba(255,255,255,0.2)',
                        borderWidth: 2,
                        hoverOffset: 8
                    }]
                };
            },
            error: (err) => console.error('Failed to load inventory chart:', err)
        });

        // Load donations trend chart
        this.apiService.getBankDonationsTrend().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.donationsChartData = {
                    labels: data.map((d: any) => {
                        const date = new Date(d.date);
                        return date.toLocaleDateString('en-US', { weekday: 'short' });
                    }),
                    datasets: [{
                        data: data.map((d: any) => d.count || 0),
                        label: 'Donations',
                        fill: true,
                        borderColor: '#2ed573',
                        backgroundColor: 'rgba(46, 213, 115, 0.3)',
                        tension: 0.4,
                        pointBackgroundColor: '#2ed573',
                        pointBorderColor: '#fff',
                        pointRadius: 6,
                        pointHoverRadius: 10
                    }]
                };
            },
            error: (err) => console.error('Failed to load donations chart:', err)
        });

        // Load reservations stats chart
        this.apiService.getBankReservationsStats().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.reservationsChartData = {
                    labels: data.map((d: any) => d.status.charAt(0).toUpperCase() + d.status.slice(1)),
                    datasets: [{
                        data: data.map((d: any) => d.count || 0),
                        label: 'Reservations',
                        backgroundColor: ['#f59e0b', '#3b82f6', '#22c55e', '#ef4444'],
                        borderRadius: 8
                    }]
                };
            },
            error: (err) => console.error('Failed to load reservations chart:', err)
        });
    }

    // Prescription Viewer
    currentViewingReservationId: number | null = null;

    viewPrescription(path: string, reservationId: number) {
        this.viewingPrescription = path;
        this.currentViewingReservationId = reservationId;
    }

    closePrescriptionViewer() {
        // Mark as viewed when closing
        if (this.currentViewingReservationId !== null) {
            this.viewedPrescriptions.add(this.currentViewingReservationId);
            // Save to localStorage for persistence
            localStorage.setItem('viewedPrescriptions', JSON.stringify([...this.viewedPrescriptions]));
        }
        this.viewingPrescription = null;
        this.currentViewingReservationId = null;
    }

    hasPrescriptionViewed(reservationId: number): boolean {
        return this.viewedPrescriptions.has(reservationId);
    }

    getPrescriptionUrl() {
        if (!this.viewingPrescription) return '';
        return 'https://bloodbank-backend-701641288198.asia-south1.run.app' + this.viewingPrescription;
    }
}
