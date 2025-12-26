import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';

// Admin Guard - checks for adminToken
export const adminGuard: CanActivateFn = () => {
    const router = inject(Router);
    const token = localStorage.getItem('adminToken');

    if (token) {
        return true;
    }

    router.navigate(['/admin-login']);
    return false;
};

// Bank Guard - checks for bankToken
export const bankGuard: CanActivateFn = () => {
    const router = inject(Router);
    const token = localStorage.getItem('bankToken');

    if (token) {
        return true;
    }

    router.navigate(['/bank-login']);
    return false;
};

// Donor Guard - checks for donorToken
export const donorGuard: CanActivateFn = () => {
    const router = inject(Router);
    const token = localStorage.getItem('donorToken');

    if (token) {
        return true;
    }

    router.navigate(['/donor/login']);
    return false;
};
