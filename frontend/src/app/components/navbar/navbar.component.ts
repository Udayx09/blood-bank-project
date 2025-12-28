import { Component, HostListener, ViewEncapsulation, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ThemeService } from '../../services/theme.service';

@Component({
    selector: 'app-navbar',
    standalone: true,
    imports: [CommonModule, RouterLink],
    templateUrl: './navbar.component.html',
    encapsulation: ViewEncapsulation.None
})
export class NavbarComponent {
    themeService = inject(ThemeService);
    router = inject(Router);
    isScrolled = false;
    isMobileMenuOpen = false;

    navLinks = [
        { label: 'Home', href: '#home' },
        { label: 'About', href: '#about' },
        { label: 'Bank Portal', route: '/bank-login' },
        { label: 'Donor Portal', route: '/donor/login' },
        { label: 'Admin', route: '/admin-login' }
    ];

    @HostListener('window:scroll', [])
    onWindowScroll() {
        this.isScrolled = window.pageYOffset > 50;
    }

    toggleMobileMenu() {
        this.isMobileMenuOpen = !this.isMobileMenuOpen;
    }

    toggleTheme() {
        this.themeService.toggleTheme();
    }

    navigateTo(link: any) {
        if (link.route) {
            this.router.navigate([link.route]);
        } else if (link.href) {
            const element = document.querySelector(link.href);
            if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
        this.isMobileMenuOpen = false;
    }

    goToSearch() {
        this.router.navigate(['/search']);
    }
}
