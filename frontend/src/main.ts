import { bootstrapApplication } from '@angular/platform-browser';
import { AppComponent } from './app/app.component';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { routes } from './app/app.routes';

// Register Chart.js components globally
import { Chart, registerables } from 'chart.js';
Chart.register(...registerables);

// Import ng2-charts provider
import { provideCharts, withDefaultRegisterables } from 'ng2-charts';

bootstrapApplication(AppComponent, {
    providers: [
        provideAnimations(),
        provideRouter(routes),
        provideHttpClient(),
        provideCharts(withDefaultRegisterables())
    ]
}).catch((err) => console.error(err));

