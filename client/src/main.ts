import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { AppComponent } from './app/app';
bootstrapApplication(AppComponent, {
    providers: [
        provideHttpClient(
            withInterceptors([(request, next) => next(request.clone({ setHeaders: { 'X-Requested-With': 'XMLHttpRequest' } }))]),
        ),
    ],
}).catch(console.error);
