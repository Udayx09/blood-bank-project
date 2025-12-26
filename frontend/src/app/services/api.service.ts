import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

// Interfaces
export interface BloodBank {
    id: number;
    name: string;
    address: string;
    city: string;
    phone: string;
    email: string;
    rating: number;
    isOpen: boolean;
    bloodTypes: string[];
    availableUnits: { [key: string]: number };
    unitsAvailable?: number;
    distance?: string;
    location: {
        lat: number;
        lng: number;
    };
}

export interface Reservation {
    id?: number;
    patientName: string;
    whatsappNumber: string;
    bloodType: string;
    unitsNeeded: number;
    urgencyLevel: 'normal' | 'urgent' | 'emergency';
    additionalNotes?: string;
    bloodBankId: number;
    bloodBankName?: string;
    status?: string;
    createdAt?: string;
    expiresAt?: string;
    prescriptionPath?: string;
    referringDoctor?: string;
}

interface ApiResponse<T> {
    success: boolean;
    data: T;
    count?: number;
    bloodType?: string;
    message?: string;
    error?: string;
}

@Injectable({
    providedIn: 'root'
})
export class ApiService {
    private baseUrl = 'https://bloodbank-backend-701641288198.asia-south1.run.app/api';

    constructor(private http: HttpClient) { }

    // Blood Banks
    getBloodBanks(): Observable<BloodBank[]> {
        return this.http.get<ApiResponse<BloodBank[]>>(`${this.baseUrl}/blood-banks`)
            .pipe(map(response => response.data));
    }

    searchByBloodType(bloodType: string): Observable<BloodBank[]> {
        const encodedType = encodeURIComponent(bloodType);
        return this.http.get<ApiResponse<BloodBank[]>>(`${this.baseUrl}/blood-banks/search?bloodType=${encodedType}`)
            .pipe(map(response => response.data));
    }

    getBloodBank(id: number): Observable<BloodBank> {
        return this.http.get<ApiResponse<BloodBank>>(`${this.baseUrl}/blood-banks/${id}`)
            .pipe(map(response => response.data));
    }

    createBloodBank(bloodBank: Partial<BloodBank>): Observable<any> {
        return this.http.post<ApiResponse<any>>(`${this.baseUrl}/blood-banks`, bloodBank);
    }

    updateBloodBank(id: number, bloodBank: Partial<BloodBank>): Observable<any> {
        return this.http.put<ApiResponse<any>>(`${this.baseUrl}/blood-banks/${id}`, bloodBank);
    }

    toggleBloodBankStatus(id: number): Observable<any> {
        return this.http.put<ApiResponse<any>>(`${this.baseUrl}/blood-banks/${id}/toggle`, {});
    }

    deleteBloodBank(id: number): Observable<any> {
        return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/blood-banks/${id}`);
    }

    // Reservations
    createReservation(reservation: Reservation): Observable<Reservation> {
        return this.http.post<ApiResponse<Reservation>>(`${this.baseUrl}/reservations`, reservation)
            .pipe(map(response => response.data));
    }

    // Upload prescription file
    uploadPrescription(formData: FormData): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/upload/prescription`, formData);
    }

    getReservations(): Observable<Reservation[]> {
        return this.http.get<ApiResponse<Reservation[]>>(`${this.baseUrl}/reservations`)
            .pipe(map(response => response.data));
    }

    getReservation(id: number): Observable<Reservation> {
        return this.http.get<ApiResponse<Reservation>>(`${this.baseUrl}/reservations/${id}`)
            .pipe(map(response => response.data));
    }

    updateReservationStatus(id: number, status: string): Observable<any> {
        return this.http.put<ApiResponse<any>>(`${this.baseUrl}/reservations/${id}/status`, { status });
    }

    cancelReservation(id: number): Observable<any> {
        return this.http.delete<ApiResponse<any>>(`${this.baseUrl}/reservations/${id}`);
    }

    // Admin Stats
    getAdminStats(): Observable<any> {
        return this.http.get<ApiResponse<any>>(`${this.baseUrl}/admin/stats`)
            .pipe(map(response => response.data));
    }

    // Admin Inventory
    getAdminInventory(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/admin/inventory`)
            .pipe(map(response => response.data));
    }

    updateAdminInventory(bloodBankId: number, bloodType: string, units: number): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/admin/inventory`, { bloodBankId, bloodType, units });
    }

    // Admin Donors
    getAdminDonors(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/admin/donors`);
    }

    // WhatsApp
    getWhatsAppStatus(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/whatsapp/status`);
    }

    // Health check
    checkHealth(): Observable<any> {
        return this.http.get(`${this.baseUrl}/health`);
    }

    // Bank Auth
    bankLogin(phone: string, password: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/auth/bank/login`, { phone, password });
    }

    bankRegister(phone: string, password: string, bloodBankId: number): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/auth/bank/register`, { phone, password, bloodBankId });
    }

    getBankMe(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/auth/bank/me`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Bank Portal APIs
    getBankStats(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/stats`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        }).pipe(map(response => response.data));
    }

    getBankReservations(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/reservations`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        }).pipe(map(response => response.data));
    }

    updateBankReservationStatus(id: number, status: string): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/bank/reservations/${id}/status`, { status }, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    getBankInventory(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/inventory`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        }).pipe(map(response => response.data));
    }

    updateBankInventory(bloodType: string, units: number): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/bank/inventory`, { bloodType, units }, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    getExpiringBlood(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/expiring`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        }).pipe(map(response => response.data));
    }

    // ==================== DONOR APIs ====================

    // Send OTP to donor phone
    sendDonorOtp(phone: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/send-otp`, { phone });
    }

    // Verify donor OTP
    verifyDonorOtp(phone: string, otp: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/verify-otp`, { phone, otp });
    }

    // Register new donor
    registerDonor(donor: DonorRegistration): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/register`, donor);
    }

    // Get donor profile by phone
    getDonorProfile(phone: string): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/profile?phone=${encodeURIComponent(phone)}`);
    }

    // Get donor profile by ID
    getDonorProfileById(id: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/profile/${id}`);
    }

    // Update donor profile
    updateDonorProfile(id: number, donor: Partial<DonorRegistration>): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/donor/profile/${id}`, donor);
    }

    // Get donation history
    getDonationHistory(donorId: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/history/${donorId}`);
    }

    // Record a donation
    recordDonation(donorId: number, bloodBankId?: number, donationDate?: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/record-donation`, {
            donorId,
            bloodBankId,
            donationDate
        });
    }

    // Find available donors (for blood banks)
    findAvailableDonors(bloodType: string, city: string): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/available?bloodType=${encodeURIComponent(bloodType)}&city=${encodeURIComponent(city)}`);
    }

    // Get incoming donation requests for a donor
    getDonorRequests(donorId: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/${donorId}/requests`);
    }

    // Respond to a donation request (accept or decline)
    respondToRequest(requestId: number, donorId: number, accept: boolean): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/donor/requests/${requestId}/respond`, { donorId, accept });
    }

    // Login with phone and password
    loginWithPassword(phone: string, password: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/login`, { phone, password });
    }

    // Set password for donor (after OTP verification)
    setPassword(donorId: number, password: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/${donorId}/set-password`, { password });
    }

    // Check if donor has password set
    hasPassword(donorId: number): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/donor/${donorId}/has-password`);
    }

    // ========== BANK PORTAL DONOR SEARCH ==========

    // Search for donors available for contact
    searchDonors(bloodType?: string): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        let url = `${this.baseUrl}/bank/donors`;
        if (bloodType && bloodType !== 'ALL') {
            url += `?bloodType=${encodeURIComponent(bloodType)}`;
        }
        return this.http.get<any>(url, { headers });
    }

    // Send donation request to a donor
    sendDonationRequest(donorId: number): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.post<any>(`${this.baseUrl}/bank/donors/${donorId}/request`, {}, { headers });
    }

    // Get bank's donation request history
    getDonorRequestHistory(): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/bank/donor-requests`, { headers });
    }

    // Update donor contact availability (opt-in/opt-out)
    updateContactAvailability(donorId: number, available: boolean): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/donor/${donorId}/contact-availability`, { available });
    }

    // ==================== ANALYTICS API ====================

    // Bank Analytics - Summary
    getBankAnalyticsSummary(): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/bank/summary`, { headers });
    }

    // Bank Analytics - Inventory Distribution
    getBankInventoryDistribution(): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/bank/inventory-distribution`, { headers });
    }

    // Bank Analytics - Donations Trend
    getBankDonationsTrend(): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/bank/donations-trend`, { headers });
    }

    // Admin Analytics - Summary
    getAdminAnalyticsSummary(): Observable<any> {
        const token = localStorage.getItem('adminToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/admin/summary`, { headers });
    }

    // Admin Analytics - Blood Type Distribution
    getAdminBloodTypeDistribution(): Observable<any> {
        const token = localStorage.getItem('adminToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/admin/blood-type-distribution`, { headers });
    }

    // Admin Analytics - Donations Trend
    getAdminDonationsTrend(): Observable<any> {
        const token = localStorage.getItem('adminToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/admin/donations-trend`, { headers });
    }

    // Bank Analytics - Reservations Stats
    getBankReservationsStats(): Observable<any> {
        const token = localStorage.getItem('bankToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/bank/reservations-stats`, { headers });
    }

    // Admin Analytics - Registrations Trend
    getAdminRegistrationsTrend(): Observable<any> {
        const token = localStorage.getItem('adminToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/analytics/admin/registrations-trend`, { headers });
    }

    // ===================== DONOR AUTH =====================

    // Donor login with phone and password
    donorLogin(phone: string, password: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/login`, { phone, password });
    }

    // Donor register with password
    donorRegister(data: DonorRegistrationWithPassword): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/auth/register`, data);
    }

    // Set password for existing donor
    donorSetPassword(donorId: number | null, phone: string | null, password: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/auth/set-password`, { donorId, phone, password });
    }

    // Get authenticated donor profile
    getDonorAuthProfile(): Observable<any> {
        const token = localStorage.getItem('donorToken');
        const headers = { 'Authorization': `Bearer ${token}` };
        return this.http.get<any>(`${this.baseUrl}/donor/auth/profile`, { headers });
    }

    // Forgot Password - send OTP
    donorForgotPassword(phone: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/auth/forgot-password`, { phone });
    }

    // Reset Password with OTP
    donorResetPassword(phone: string, otp: string, newPassword: string): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/donor/auth/reset-password`, { phone, otp, newPassword });
    }

    // Upload profile photo
    uploadProfilePhoto(file: File): Observable<any> {
        const token = localStorage.getItem('donorToken');
        if (!token) {
            throw new Error('Not authenticated');
        }
        const headers = { 'Authorization': `Bearer ${token}` };
        const formData = new FormData();
        formData.append('file', file);
        return this.http.post<any>(`${this.baseUrl}/upload/profile-photo`, formData, { headers });
    }

    // ===================== BLOOD UNITS (Component Tracking) =====================

    // Get all blood components
    getBloodComponents(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/units/components`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Record a blood donation with multiple components (blood units) - supports walk-in donor registration
    recordBloodDonation(donation: {
        bloodType: string,
        collectionDate: string,
        components: string[],
        donorId?: number,
        donorName?: string,
        donorPhone?: string,
        donorDateOfBirth?: string
    }): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/bank/units/record-donation`, donation, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // ========== TWO-STEP DONATION WORKFLOW ==========

    // Lookup donor by phone
    lookupDonor(phone: string): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/units/lookup-donor?phone=${encodeURIComponent(phone)}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Step 1: Record donation (donor + date only)
    recordDonationStep1(data: {
        phone: string,
        donationDate: string,
        bloodType?: string,
        donorName?: string,
        donorDateOfBirth?: string
    }): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/bank/units/record-donation-step1`, data, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Step 2: Add components to pending donation
    addComponents(donationId: number, components: string[]): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/bank/units/add-components/${donationId}`, { components }, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Get pending donations (no components added yet)
    getPendingDonations(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/units/pending-donations`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Get blood units for bank
    getBloodUnits(status?: string, bloodType?: string, component?: string): Observable<any> {
        let params = new URLSearchParams();
        if (status) params.append('status', status);
        if (bloodType) params.append('bloodType', bloodType);
        if (component) params.append('component', component);
        const query = params.toString() ? `?${params.toString()}` : '';
        return this.http.get<any>(`${this.baseUrl}/bank/units${query}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Get expiring units
    getExpiringUnits(days: number = 7): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/units/expiring?days=${days}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Get expiry summary
    getExpirySummary(): Observable<any> {
        return this.http.get<any>(`${this.baseUrl}/bank/units/expiry-summary`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Add blood unit
    addBloodUnit(unit: { bloodType: string, component: string, collectionDate: string, unitNumber?: string, donorId?: number }): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/bank/units`, unit, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Update unit status
    updateBloodUnitStatus(id: number, status: string): Observable<any> {
        return this.http.put<any>(`${this.baseUrl}/bank/units/${id}/status`, { status }, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Delete blood unit
    deleteBloodUnit(id: number): Observable<any> {
        return this.http.delete<any>(`${this.baseUrl}/bank/units/${id}`, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }

    // Mark expired units
    markExpiredUnits(): Observable<any> {
        return this.http.post<any>(`${this.baseUrl}/bank/units/mark-expired`, {}, {
            headers: { Authorization: `Bearer ${localStorage.getItem('bankToken')}` }
        });
    }
}

// Donor interfaces
export interface DonorRegistrationWithPassword {
    name: string;
    phone: string;
    password: string;
    bloodType: string;
    dateOfBirth: string;
    city: string;
    weight: number;
}

// Donor interfaces
export interface DonorRegistration {
    name: string;
    phone: string;
    bloodType: string;
    dateOfBirth: string;
    city: string;
    weight: number;
    lastDonationDate?: string;
}

export interface Donor {
    id: number;
    name: string;
    phone: string;
    bloodType: string;
    dateOfBirth: string;
    city: string;
    weight: number;
    lastDonationDate?: string;
    isVerified: boolean;
    isAvailableForContact?: boolean;
    eligible: boolean;
    daysUntilEligible: number;
    age: number;
    maskedPhone?: string;
    profilePhoto?: string;
}

export interface DonationHistory {
    id: number;
    donationDate: string;
    bloodBankName: string;
    units: number;
}
