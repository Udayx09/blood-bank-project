import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';

interface Stat {
    number: string;
    label: string;
}

@Component({
    selector: 'app-hero',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './hero.component.html',
    styleUrls: ['./hero.component.css'],
    encapsulation: ViewEncapsulation.None
})
export class HeroComponent {
    stats: Stat[] = [
        { number: '10K+', label: 'Lives Saved' },
        { number: '500+', label: 'Blood Banks' },
        { number: '24/7', label: 'Support' }
    ];

    constructor(private router: Router) { }

    goToSearch() {
        this.router.navigate(['/search']);
    }

    goToDonorRegistration() {
        this.router.navigate(['/donor/register']);
    }
}
