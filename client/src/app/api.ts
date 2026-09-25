import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Instance, Page, Session, Startup } from './models';

@Injectable({ providedIn: 'root' })
export class Api {
    private http = inject(HttpClient);
    private csrf = '';
    async session(): Promise<Session> {
        const session = await firstValueFrom(this.http.get<Session>('/api/auth/session'));
        this.csrf = session.csrfToken;
        return session;
    }
    async login(username: string, password: string): Promise<Session> {
        await this.session();
        const body = new HttpParams().set('username', username).set('password', password);
        await firstValueFrom(this.http.post('/api/auth/login', body, { headers: { 'X-CSRF-TOKEN': this.csrf } }));
        return this.session();
    }
    async logout(): Promise<void> {
        // Another tab may have signed in again and rotated the shared session's CSRF token.
        await this.session();
        await firstValueFrom(this.http.post('/api/auth/logout', {}, { headers: { 'X-CSRF-TOKEN': this.csrf } }));
        this.csrf = '';
    }
    dashboard(): Promise<Instance[]> {
        return firstValueFrom(this.http.get<Instance[]>('/api/dashboard'));
    }
    history(id: number, page: number): Promise<Page<Startup>> {
        return firstValueFrom(this.http.get<Page<Startup>>(`/api/telemetry/instances/${id}/startups`, { params: { page, size: 10 } }));
    }
}
