import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

@Component({
    selector: 'app-admin-login',
    standalone: true,
    imports: [CommonModule, FormsModule],
    template: `
        <div class="login-container">
            <div class="login-card">
                <h1>🔐 Admin Login</h1>
                <p class="subtitle">Enter admin credentials to continue</p>
                
                <form (ngSubmit)="login()">
                    <div class="form-group">
                        <label>Password</label>
                        <input type="password" [(ngModel)]="password" name="password" 
                               placeholder="Enter admin password" required>
                    </div>
                    
                    <p class="error" *ngIf="error">{{ error }}</p>
                    
                    <button type="submit" class="login-btn">Login</button>
                </form>
                
                <a class="back-link" routerLink="/">← Back to Home</a>
            </div>
        </div>
    `,
    styles: [`
        .login-container {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            background: linear-gradient(135deg, #0f0f1a 0%, #1a1a2e 100%);
            padding: 2rem;
        }
        .login-card {
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 20px;
            padding: 3rem;
            width: 100%;
            max-width: 400px;
            text-align: center;
        }
        h1 { color: #fff; margin-bottom: 0.5rem; font-size: 2rem; }
        .subtitle { color: #94a3b8; margin-bottom: 2rem; }
        .form-group { margin-bottom: 1.5rem; text-align: left; }
        label { display: block; color: #94a3b8; margin-bottom: 0.5rem; font-size: 0.9rem; }
        input {
            width: 100%;
            padding: 1rem;
            background: rgba(255,255,255,0.05);
            border: 1px solid rgba(255,255,255,0.1);
            border-radius: 10px;
            color: #fff;
            font-size: 1rem;
        }
        input:focus { outline: none; border-color: #4fc3f7; }
        .error { color: #ef5350; margin-bottom: 1rem; }
        .login-btn {
            width: 100%;
            padding: 1rem;
            background: linear-gradient(135deg, #4fc3f7, #29b6f6);
            border: none;
            border-radius: 10px;
            color: #fff;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s;
        }
        .login-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 20px rgba(79,195,247,0.4); }
        .back-link { display: block; margin-top: 1.5rem; color: #4fc3f7; text-decoration: none; }
    `]
})
export class AdminLoginComponent {
    password = '';
    error = '';

    constructor(private router: Router) { }

    login() {
        // Simple password check - in production, use proper backend authentication
        if (this.password === 'udaysproject18') {
            localStorage.setItem('adminToken', 'admin-authenticated');
            this.router.navigate(['/admin']);
        } else {
            this.error = 'Invalid password';
        }
    }
}
