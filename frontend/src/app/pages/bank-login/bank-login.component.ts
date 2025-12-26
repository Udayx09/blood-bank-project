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
    phone = '';
    password = '';
    loading = false;
    error = '';

    constructor(private apiService: ApiService, private router: Router) {
        // Check if already logged in
        if (localStorage.getItem('bankToken')) {
            this.router.navigate(['/bank-portal']);
        }
    }

    submit() {
        if (!this.phone || !this.password) {
            this.error = 'Please fill in all fields';
            return;
        }

        this.loading = true;
        this.error = '';

        this.apiService.bankLogin(this.phone, this.password).subscribe({
            next: (res) => {
                localStorage.setItem('bankToken', res.token);
                localStorage.setItem('bankInfo', JSON.stringify(res.bank));
                this.router.navigate(['/bank-portal']);
            },
            error: (err) => {
                this.error = err.error?.error || 'Login failed. Please check your credentials.';
                this.loading = false;
            }
        });
    }
}
