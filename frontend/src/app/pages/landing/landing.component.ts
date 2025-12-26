import { Component, ViewEncapsulation } from '@angular/core';
import { NavbarComponent } from '../../components/navbar/navbar.component';
import { HeroComponent } from '../../components/hero/hero.component';
import { TestimonialsComponent } from '../../components/testimonials/testimonials.component';
import { FooterComponent } from '../../components/footer/footer.component';
import { ChatWidgetComponent } from '../../components/chat-widget/chat-widget.component';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [NavbarComponent, HeroComponent, TestimonialsComponent, FooterComponent, ChatWidgetComponent],
  template: `
    <app-navbar></app-navbar>
    <app-hero></app-hero>
    <app-testimonials></app-testimonials>
    <app-footer></app-footer>
    <app-chat-widget></app-chat-widget>
  `,
  encapsulation: ViewEncapsulation.None
})
export class LandingComponent { }
