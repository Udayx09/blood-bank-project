import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { ApiService } from '../../services/api.service';

@Component({
    selector: 'app-bank-login',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    templateUrl: './bank-login.component.html',
    styleUrl: './bank-login.component.css'
})
export class BankLoginComponent {
    isLogin = true;
    phone = '';
    password = '';
    bloodBankId: number | null = null;
    bloodBanks: any[] = [];
    loading = false;
    error = '';

    constructor(private apiService: ApiService, private router: Router) {
        // Check if already logged in - but verify the session is still valid
        if (localStorage.getItem('bankToken')) {
            // Verify session by calling stats
            this.apiService.getBankStats().subscribe({
                next: () => {
                    // Session valid, redirect to portal
                    this.router.navigate(['/bank-portal']);
                },
                error: () => {
                    // Session invalid (blood bank deleted or token expired)
                    localStorage.removeItem('bankToken');
                    localStorage.removeItem('bankInfo');
                    // Stay on login page
                }
            });
        }
        // Load blood banks for registration
        this.loadBloodBanks();
    }

    loadBloodBanks() {
        this.apiService.getBloodBanks().subscribe({
            next: (banks) => {
                // Filter to show only banks that don't have an account yet
                this.bloodBanks = (banks || []).filter((bank: any) => !bank.hasAccount);
                console.log('Available blood banks for registration:', this.bloodBanks.length);
            },
            error: (err) => {
                console.error('Failed to load blood banks:', err);
            }
        });
    }

    toggleMode() {
        this.isLogin = !this.isLogin;
        this.error = '';
    }

    submit() {
        if (!this.phone || !this.password) {
            this.error = 'Please fill in all fields';
            return;
        }

        if (!this.isLogin && !this.bloodBankId) {
            this.error = 'Please select your blood bank';
            return;
        }

        this.loading = true;
        this.error = '';

        if (this.isLogin) {
            this.apiService.bankLogin(this.phone, this.password).subscribe({
                next: (res) => {
                    localStorage.setItem('bankToken', res.token);
                    localStorage.setItem('bankInfo', JSON.stringify(res.bank));
                    this.router.navigate(['/bank-portal']);
                },
                error: (err) => {
                    this.error = err.error?.error || 'Login failed';
                    this.loading = false;
                }
            });
        } else {
            this.apiService.bankRegister(this.phone, this.password, this.bloodBankId!).subscribe({
                next: (res) => {
                    localStorage.setItem('bankToken', res.token);
                    localStorage.setItem('bankInfo', JSON.stringify(res.bank));
                    this.router.navigate(['/bank-portal']);
                },
                error: (err) => {
                    this.error = err.error?.error || 'Registration failed';
                    this.loading = false;
                }
            });
        }
    }
}
