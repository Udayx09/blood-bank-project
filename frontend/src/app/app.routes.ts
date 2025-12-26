import { Routes } from '@angular/router';
import { LandingComponent } from './pages/landing/landing.component';
import { BloodSearchComponent } from './pages/blood-search/blood-search.component';
import { AdminComponent } from './pages/admin/admin.component';
import { AdminLoginComponent } from './pages/admin-login/admin-login.component';
import { BankLoginComponent } from './pages/bank-login/bank-login.component';
import { BankPortalComponent } from './pages/bank-portal/bank-portal.component';
import { DonorLoginComponent } from './pages/donor-login/donor-login.component';
import { DonorRegistrationComponent } from './pages/donor-registration/donor-registration.component';
import { DonorPortalComponent } from './pages/donor-portal/donor-portal.component';
import { adminGuard, bankGuard, donorGuard } from './guards/auth.guard';

export const routes: Routes = [
    { path: '', component: LandingComponent },
    { path: 'search', component: BloodSearchComponent },
    { path: 'admin-login', component: AdminLoginComponent },
    { path: 'admin', component: AdminComponent, canActivate: [adminGuard] },
    { path: 'bank-login', component: BankLoginComponent },
    { path: 'bank-portal', component: BankPortalComponent, canActivate: [bankGuard] },
    { path: 'donor/login', component: DonorLoginComponent },
    { path: 'donor/register', component: DonorRegistrationComponent },
    { path: 'donor/portal', component: DonorPortalComponent, canActivate: [donorGuard] },
    { path: '**', redirectTo: '' }
];
