import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ApiService, Reservation } from '../../services/api.service';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';

interface DashboardStats {
    reservations: {
        total: number;
        pending: number;
        confirmed: number;
        completed: number;
        cancelled: number;
    };
    bloodBanks: {
        total: number;
    };
    bloodUnits: {
        total: number;
        byType: { type: string; units: number }[];
    };
    whatsapp: {
        connected: boolean;
        hasQR: boolean;
    };
    lowStockAlerts: {
        bloodBankId: number;
        bloodBankName: string;
        bloodType: string;
        unitsAvailable: number;
        threshold: number;
        isCritical: boolean;
    }[];
    lowStockThreshold: number;
    recentActivity: {
        id: number;
        type: string;
        patientName: string;
        bloodType: string;
        status: string;
        bloodBankName: string;
        createdAt: string;
    }[];
}

@Component({
    selector: 'app-admin',
    standalone: true,
    imports: [CommonModule, RouterModule, FormsModule, BaseChartDirective],
    templateUrl: './admin.component.html',
    styleUrl: './admin.component.css'
})
export class AdminComponent implements OnInit {
    stats: DashboardStats | null = null;
    loading = true;
    error = '';

    // Animated counters
    animatedStats = {
        totalReservations: 0,
        pendingReservations: 0,
        bloodBanks: 0,
        bloodUnits: 0
    };

    // Toast notifications
    toast: { message: string; type: 'success' | 'error' | 'info' } | null = null;

    // Reservations
    reservations: Reservation[] = [];
    filteredReservations: Reservation[] = [];
    reservationsLoading = false;
    activeFilter = 'all';
    searchQuery = '';

    // Blood Banks
    bloodBanks: any[] = [];
    bloodBanksLoading = false;
    showBloodBankForm = false;
    editingBloodBank: any = null;
    bloodBankForm = {
        name: '',
        address: '',
        city: '',
        phone: '',
        email: '',
        isOpen: true
    };

    // Delete Confirmation Modal
    showDeleteConfirm = false;
    deleteTarget: any = null;

    // Inventory
    inventoryData: any[] = [];
    inventoryLoading = false;
    private inventoryRefreshInterval: any;

    // Donors
    donors: any[] = [];
    donorsLoading = false;

    get eligibleDonorsCount(): number {
        return this.donors.filter(d => d.eligible).length;
    }

    get verifiedDonorsCount(): number {
        return this.donors.filter(d => d.isVerified).length;
    }

    // Active view
    activeView: 'dashboard' | 'reservations' | 'bloodbanks' | 'inventory' | 'analytics' | 'donors' = 'dashboard';

    // Analytics Charts
    analyticsLoaded = false;

    // Blood Type Distribution (Doughnut)
    bloodTypeChartData: ChartData<'doughnut'> = {
        labels: [],
        datasets: [{
            data: [],
            backgroundColor: ['#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'],
            borderColor: 'rgba(255,255,255,0.2)',
            borderWidth: 2
        }]
    };
    bloodTypeChartOptions: ChartConfiguration<'doughnut'>['options'] = {
        responsive: true,
        maintainAspectRatio: false,
        animation: { duration: 1500, easing: 'easeOutQuart' },
        plugins: {
            legend: { position: 'bottom', labels: { color: '#fff', padding: 15 } }
        }
    };

    // Donor Registrations Trend (Line)
    registrationsChartData: ChartData<'line'> = {
        labels: [],
        datasets: [{
            data: [],
            label: 'New Donors',
            fill: true,
            borderColor: '#8b5cf6',
            backgroundColor: 'rgba(139, 92, 246, 0.3)',
            tension: 0.4,
            pointBackgroundColor: '#8b5cf6',
            pointBorderColor: '#fff',
            pointRadius: 6,
            pointHoverRadius: 10
        }]
    };
    registrationsChartOptions: ChartConfiguration<'line'>['options'] = {
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

    // Donations Trend (Line)
    adminDonationsChartData: ChartData<'line'> = {
        labels: [],
        datasets: [{
            data: [],
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
    adminDonationsChartOptions: ChartConfiguration<'line'>['options'] = {
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

    constructor(private apiService: ApiService, private router: Router) { }

    ngOnInit() {
        this.loadStats();
        this.loadReservations();
        // Removed auto-refresh - use manual refresh button instead
    }

    loadStats() {
        this.apiService.getAdminStats().subscribe({
            next: (data) => {
                const isFirstLoad = !this.stats;
                this.stats = data;
                this.loading = false;

                // Animate counters on first load
                if (isFirstLoad) {
                    this.animateCounter('totalReservations', data.reservations.total);
                    this.animateCounter('pendingReservations', data.reservations.pending);
                    this.animateCounter('bloodBanks', data.bloodBanks.total);
                    this.animateCounter('bloodUnits', data.bloodUnits.total);
                } else {
                    // Just update values for refreshes
                    this.animatedStats.totalReservations = data.reservations.total;
                    this.animatedStats.pendingReservations = data.reservations.pending;
                    this.animatedStats.bloodBanks = data.bloodBanks.total;
                    this.animatedStats.bloodUnits = data.bloodUnits.total;
                }
            },
            error: (err) => {
                this.error = 'Failed to load dashboard stats';
                this.loading = false;
                console.error(err);
            }
        });
    }

    // Counter animation helper
    animateCounter(key: keyof typeof this.animatedStats, target: number) {
        const duration = 1000; // 1 second
        const steps = 30;
        const increment = target / steps;
        let current = 0;

        const timer = setInterval(() => {
            current += increment;
            if (current >= target) {
                this.animatedStats[key] = target;
                clearInterval(timer);
            } else {
                this.animatedStats[key] = Math.floor(current);
            }
        }, duration / steps);
    }

    // Toast notification
    showToast(message: string, type: 'success' | 'error' | 'info' = 'success') {
        this.toast = { message, type };
        setTimeout(() => {
            this.toast = null;
        }, 3000);
    }


    loadReservations() {
        this.reservationsLoading = true;
        this.apiService.getReservations().subscribe({
            next: (data) => {
                this.reservations = data;
                this.applyFilter();
                this.reservationsLoading = false;
            },
            error: (err) => {
                console.error('Failed to load reservations:', err);
                this.reservationsLoading = false;
            }
        });
    }

    setActiveView(view: 'dashboard' | 'reservations' | 'bloodbanks' | 'inventory' | 'analytics' | 'donors') {
        this.activeView = view;

        // Clear inventory refresh interval when leaving inventory view
        if (this.inventoryRefreshInterval) {
            clearInterval(this.inventoryRefreshInterval);
            this.inventoryRefreshInterval = null;
        }

        if (view === 'reservations') {
            this.loadReservations();
        } else if (view === 'bloodbanks') {
            this.loadBloodBanks();
        } else if (view === 'inventory') {
            this.loadInventory();
        } else if (view === 'analytics') {
            this.loadAnalytics();
        } else if (view === 'donors') {
            this.loadDonors();
        }
    }

    // Analytics Methods
    loadAnalytics() {
        if (this.analyticsLoaded) return;
        this.analyticsLoaded = true;

        // Load blood type distribution
        this.apiService.getAdminBloodTypeDistribution().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.bloodTypeChartData = {
                    labels: data.map((d: any) => d.type),
                    datasets: [{
                        data: data.map((d: any) => d.count || 0),
                        backgroundColor: ['#ef4444', '#f97316', '#eab308', '#22c55e', '#06b6d4', '#3b82f6', '#8b5cf6', '#ec4899'],
                        borderColor: 'rgba(255,255,255,0.2)',
                        borderWidth: 2,
                        hoverOffset: 8
                    }]
                };
            },
            error: (err) => console.error('Failed to load blood type chart:', err)
        });

        // Load registrations trend
        this.apiService.getAdminRegistrationsTrend().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.registrationsChartData = {
                    labels: data.map((d: any) => d.week),
                    datasets: [{
                        data: data.map((d: any) => d.count || 0),
                        label: 'New Donors',
                        fill: true,
                        borderColor: '#8b5cf6',
                        backgroundColor: 'rgba(139, 92, 246, 0.3)',
                        tension: 0.4,
                        pointBackgroundColor: '#8b5cf6',
                        pointBorderColor: '#fff',
                        pointRadius: 6,
                        pointHoverRadius: 10
                    }]
                };
            },
            error: (err) => console.error('Failed to load registrations chart:', err)
        });

        // Load donations trend
        this.apiService.getAdminDonationsTrend().subscribe({
            next: (response: any) => {
                const data = response.data || [];
                this.adminDonationsChartData = {
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
    }

    // Inventory Methods
    loadInventory() {
        this.inventoryLoading = true;
        this.apiService.getAdminInventory().subscribe({
            next: (data) => {
                this.inventoryData = data;
                this.inventoryLoading = false;
            },
            error: (err) => {
                console.error('Failed to load inventory:', err);
                this.inventoryLoading = false;
            }
        });
    }

    // Donors Methods
    loadDonors() {
        this.donorsLoading = true;
        this.apiService.getAdminDonors().subscribe({
            next: (response: any) => {
                console.log('Donors response:', response);
                this.donors = response.data || [];
                console.log('Donors loaded:', this.donors);
                this.donorsLoading = false;
            },
            error: (err: any) => {
                console.error('Failed to load donors:', err);
                this.donorsLoading = false;
            }
        });
    }

    updateInventory(bloodBankId: number, bloodType: string, units: number) {
        // Optimistic update - update locally first to prevent blinking
        const bank = this.inventoryData.find(b => b.id === bloodBankId);
        if (bank) {
            const item = bank.inventory.find((i: any) => i.bloodType === bloodType);
            if (item) {
                item.units = units;
            }
        }

        // Then sync with server
        this.apiService.updateAdminInventory(bloodBankId, bloodType, units).subscribe({
            next: () => {
                // Silently refresh stats (for low stock alerts)
                this.loadStats();
            },
            error: (err) => {
                console.error('Failed to update inventory:', err);
                // Revert on error by reloading
                this.loadInventory();
                alert('Failed to update inventory');
            }
        });
    }

    updateInventoryWithAnimation(bloodBankId: number, bloodType: string, units: number, direction: 'increase' | 'decrease', item: any) {
        // Set animation state
        item.animating = direction;

        // Update the inventory
        this.updateInventory(bloodBankId, bloodType, units);

        // Clear animation after it completes
        setTimeout(() => {
            item.animating = null;
        }, 300);
    }

    // Blood Banks Methods
    loadBloodBanks() {
        this.bloodBanksLoading = true;
        this.apiService.getBloodBanks().subscribe({
            next: (data) => {
                this.bloodBanks = data;
                this.bloodBanksLoading = false;
            },
            error: (err) => {
                console.error('Failed to load blood banks:', err);
                this.bloodBanksLoading = false;
            }
        });
    }

    openAddBloodBankForm() {
        this.editingBloodBank = null;
        this.bloodBankForm = {
            name: '',
            address: '',
            city: '',
            phone: '',
            email: '',
            isOpen: true
        };
        this.showBloodBankForm = true;
    }

    openEditBloodBankForm(bloodBank: any) {
        this.editingBloodBank = bloodBank;
        this.bloodBankForm = {
            name: bloodBank.name,
            address: bloodBank.address,
            city: bloodBank.city,
            phone: bloodBank.phone,
            email: bloodBank.email || '',
            isOpen: bloodBank.isOpen
        };
        this.showBloodBankForm = true;
    }

    closeBloodBankForm() {
        this.showBloodBankForm = false;
        this.editingBloodBank = null;
    }

    saveBloodBank() {
        if (!this.bloodBankForm.name || !this.bloodBankForm.address || !this.bloodBankForm.city || !this.bloodBankForm.phone) {
            alert('Please fill in all required fields');
            return;
        }

        if (this.editingBloodBank) {
            this.apiService.updateBloodBank(this.editingBloodBank.id, this.bloodBankForm).subscribe({
                next: () => {
                    this.loadBloodBanks();
                    this.loadStats();
                    this.closeBloodBankForm();
                    this.showToast('Blood bank updated successfully!', 'success');
                },
                error: (err) => {
                    console.error('Failed to update blood bank:', err);
                    this.showToast('Failed to update blood bank', 'error');
                }
            });
        } else {
            this.apiService.createBloodBank(this.bloodBankForm).subscribe({
                next: () => {
                    this.loadBloodBanks();
                    this.loadStats();
                    this.closeBloodBankForm();
                    this.showToast('Blood bank created successfully!', 'success');
                },
                error: (err) => {
                    console.error('Failed to create blood bank:', err);
                    this.showToast('Failed to create blood bank', 'error');
                }
            });
        }
    }

    toggleBloodBankStatus(bloodBank: any) {
        this.apiService.toggleBloodBankStatus(bloodBank.id).subscribe({
            next: () => {
                bloodBank.isOpen = !bloodBank.isOpen;
                this.showToast(`Blood bank ${bloodBank.isOpen ? 'opened' : 'closed'}`, 'info');
            },
            error: (err) => {
                console.error('Failed to toggle status:', err);
                this.showToast('Failed to toggle status', 'error');
            }
        });
    }

    deleteBloodBank(bloodBank: any) {
        // Show custom confirmation modal instead of native confirm()
        this.deleteTarget = bloodBank;
        this.showDeleteConfirm = true;
    }

    confirmDelete() {
        if (!this.deleteTarget) return;

        console.log('Deleting blood bank:', this.deleteTarget.id);
        this.apiService.deleteBloodBank(this.deleteTarget.id).subscribe({
            next: () => {
                this.loadBloodBanks();
                this.loadStats();
                this.showToast('Blood bank deleted', 'success');
                this.cancelDelete();
            },
            error: (err) => {
                console.error('Failed to delete blood bank:', err);
                this.showToast('Failed to delete blood bank', 'error');
                this.cancelDelete();
            }
        });
    }

    cancelDelete() {
        this.showDeleteConfirm = false;
        this.deleteTarget = null;
    }

    // Reservations Methods
    filterReservations(status: string) {
        this.activeFilter = status;
        this.applyFilter();
    }

    applyFilter() {
        let filtered = [...this.reservations];

        if (this.activeFilter !== 'all') {
            filtered = filtered.filter(r => r.status === this.activeFilter);
        }

        if (this.searchQuery.trim()) {
            const query = this.searchQuery.toLowerCase();
            filtered = filtered.filter(r =>
                r.patientName.toLowerCase().includes(query) ||
                r.bloodType.toLowerCase().includes(query) ||
                r.bloodBankName?.toLowerCase().includes(query)
            );
        }

        this.filteredReservations = filtered;
    }

    onSearchChange() {
        this.applyFilter();
    }

    updateStatus(reservation: Reservation, newStatus: string) {
        if (!reservation.id) return;

        this.apiService.updateReservationStatus(reservation.id, newStatus).subscribe({
            next: (response) => {
                reservation.status = newStatus;
                this.loadStats();
                this.applyFilter();
                const whatsappSent = response.whatsappNotification === 'sent';
                console.log(`Status updated to ${newStatus}. WhatsApp: ${whatsappSent ? 'Sent' : 'Not sent'}`);
            },
            error: (err) => {
                console.error('Failed to update status:', err);
                alert('Failed to update status');
            }
        });
    }

    getStatusClass(status: string | undefined): string {
        switch (status) {
            case 'pending': return 'status-pending';
            case 'confirmed': return 'status-confirmed';
            case 'completed': return 'status-completed';
            case 'cancelled': return 'status-cancelled';
            default: return '';
        }
    }

    formatDate(dateString: string | undefined): string {
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

    getFilterCount(status: string): number {
        if (status === 'all') return this.reservations.length;
        return this.reservations.filter(r => r.status === status).length;
    }

    logout() {
        localStorage.removeItem('adminToken');
        this.router.navigate(['/admin-login']);
    }
}
