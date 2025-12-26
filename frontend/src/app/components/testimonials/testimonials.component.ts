import { Component, ElementRef, QueryList, ViewChildren, AfterViewInit, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Testimonial {
    quote: string;
    author: string;
    initials: string;
    role: string;
    featured?: boolean;
}

@Component({
    selector: 'app-testimonials',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './testimonials.component.html',
    encapsulation: ViewEncapsulation.None
})
export class TestimonialsComponent implements AfterViewInit {
    @ViewChildren('testimonialCard') testimonialCards!: QueryList<ElementRef>;

    testimonials: Testimonial[] = [
        {
            quote: 'This platform helped me find the exact blood type I needed for my mother\'s surgery within hours. The process was seamless and the staff was incredibly supportive.',
            author: 'Sarah K.',
            initials: 'SK',
            role: 'Blood Recipient\'s Family'
        },
        {
            quote: 'As a regular donor, I love how easy it is to track where my donations help. Knowing I\'ve contributed to saving lives gives me immense joy. This platform makes giving back effortless.',
            author: 'Michael J.',
            initials: 'MJ',
            role: 'Regular Donor',
            featured: true
        },
        {
            quote: 'Our hospital has partnered with BloodBank, and it has transformed how we manage emergency blood requirements. Highly recommended for all healthcare facilities.',
            author: 'Dr. Rachel M.',
            initials: 'DR',
            role: 'Hospital Administrator'
        }
    ];

    ngAfterViewInit() {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('animate-in');
                        observer.unobserve(entry.target);
                    }
                });
            },
            { threshold: 0.1, rootMargin: '0px 0px -50px 0px' }
        );

        this.testimonialCards.forEach((card) => {
            observer.observe(card.nativeElement);
        });
    }
}
