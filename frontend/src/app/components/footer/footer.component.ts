import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';

interface FooterLink {
    label: string;
    href: string;
}

@Component({
    selector: 'app-footer',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './footer.component.html',
    encapsulation: ViewEncapsulation.None
})
export class FooterComponent {
    currentYear = new Date().getFullYear();

    quickLinks: FooterLink[] = [
        { label: 'Home', href: '#home' },
        { label: 'About Us', href: '#about' },
        { label: 'Find Blood', href: '#' },
        { label: 'Become a Donor', href: '#' }
    ];

    resourceLinks: FooterLink[] = [
        { label: 'FAQs', href: '#' },
        { label: 'Blood Types', href: '#' },
        { label: 'Donation Process', href: '#' },
        { label: 'Health Guidelines', href: '#' }
    ];

    contactInfo = {
        email: 'admin@bloodbankproject.com',
        phone: '+1-234-567-8900',
        address: '123 Donation Lane, Bloodville'
    };

    scrollTo(href: string) {
        if (href.startsWith('#')) {
            const element = document.querySelector(href);
            if (element) {
                element.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }
    }
}
