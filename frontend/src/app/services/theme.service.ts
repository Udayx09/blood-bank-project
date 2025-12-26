import { Injectable, signal, effect } from '@angular/core';

@Injectable({
    providedIn: 'root'
})
export class ThemeService {
    private readonly THEME_KEY = 'blood-bank-theme';

    // Signal to track current theme
    isDarkMode = signal<boolean>(false);

    constructor() {
        // Initialize theme from localStorage or system preference
        this.initTheme();

        // Effect to apply theme changes to DOM
        effect(() => {
            this.applyTheme(this.isDarkMode());
        });
    }

    private initTheme() {
        const savedTheme = localStorage.getItem(this.THEME_KEY);

        if (savedTheme) {
            this.isDarkMode.set(savedTheme === 'dark');
        } else {
            // Check system preference
            const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
            this.isDarkMode.set(prefersDark);
        }
    }

    private applyTheme(isDark: boolean) {
        const html = document.documentElement;

        if (isDark) {
            html.setAttribute('data-theme', 'dark');
            html.classList.add('dark-mode');
            html.classList.remove('light-mode');
        } else {
            html.setAttribute('data-theme', 'light');
            html.classList.add('light-mode');
            html.classList.remove('dark-mode');
        }

        // Save preference
        localStorage.setItem(this.THEME_KEY, isDark ? 'dark' : 'light');
    }

    toggleTheme() {
        this.isDarkMode.update(current => !current);
    }

    setDarkMode(isDark: boolean) {
        this.isDarkMode.set(isDark);
    }
}
