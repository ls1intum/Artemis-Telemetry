import '@angular/compiler';
import { Injector, runInInjectionContext } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Api } from './api';
import { AppComponent } from './app';
import { Instance, Page, Startup } from './models';

function deferred<T>() {
    let resolve!: (value: T) => void;
    let reject!: (error: unknown) => void;
    const promise = new Promise<T>((accept, fail) => { resolve = accept; reject = fail; });
    return { promise, resolve, reject };
}

const instance = (id: number): Instance => ({
    id, serverUrl: `https://instance${id}.example`, firstSeen: '2026-01-01T00:00:00Z', lastSeen: '2026-09-25T00:00:00Z',
    latestStartup: { id, serverUrl: `https://instance${id}.example` },
});
const history = (row: Instance): Page<Startup> => ({ content: [row.latestStartup], totalElements: 1, totalPages: 1, number: 0 });

describe('dashboard session and asynchronous responses', () => {
    let app: AppComponent;
    let api: { dashboard: ReturnType<typeof vi.fn>; history: ReturnType<typeof vi.fn>; logout: ReturnType<typeof vi.fn> };

    beforeEach(() => {
        // Focus callbacks are exercised in browser checks, not in this DOM-free state test.
        vi.useFakeTimers();
        api = { dashboard: vi.fn(), history: vi.fn(), logout: vi.fn().mockResolvedValue(undefined) };
        app = runInInjectionContext(Injector.create({ providers: [{ provide: Api, useValue: api }] }), () => new AppComponent());
        app.user.set('admin');
    });

    afterEach(() => { vi.clearAllTimers(); vi.useRealTimers(); });

    it('keeps the newest refresh when responses arrive out of order', async () => {
        const older = deferred<Instance[]>();
        api.dashboard.mockReturnValueOnce(older.promise).mockResolvedValueOnce([instance(2)]);
        const pending = app.refresh();
        await app.refresh();
        older.resolve([instance(1)]);
        await pending;
        expect(app.rows()).toEqual([instance(2)]);
        expect(app.loading()).toBe(false);
    });

    it('does not restore dashboard or history data after signing out', async () => {
        const dashboard = deferred<Instance[]>();
        const startups = deferred<Page<Startup>>();
        api.dashboard.mockReturnValue(dashboard.promise);
        api.history.mockReturnValue(startups.promise);
        const pending = [app.refresh(), app.openHistory(instance(1))];
        await app.logout();
        dashboard.resolve([instance(1)]);
        startups.resolve(history(instance(1)));
        await Promise.all(pending);
        expect(app.user()).toBeNull();
        expect(app.rows()).toEqual([]);
        expect(app.selected()).toBeNull();
        expect(app.history()).toBeNull();
        expect(app.loadedAt()).toBeNull();
    });

    it('ignores a superseded history failure rather than expiring the current session', async () => {
        const older = deferred<Page<Startup>>();
        api.history.mockReturnValueOnce(older.promise).mockResolvedValueOnce(history(instance(2)));
        const pending = app.openHistory(instance(1));
        await app.openHistory(instance(2));
        older.reject(new HttpErrorResponse({ status: 401 }));
        await pending;
        expect(app.user()).toBe('admin');
        expect(app.selected()?.id).toBe(2);
        expect(app.history()).toEqual(history(instance(2)));
        expect(app.historyError()).toBe('');
    });

    it('clears private data when the active request reports an expired session', async () => {
        app.rows.set([instance(1)]);
        app.selected.set(instance(1));
        app.history.set(history(instance(1)));
        api.dashboard.mockRejectedValue(new HttpErrorResponse({ status: 401 }));
        await app.refresh();
        expect(app.user()).toBeNull();
        expect(app.rows()).toEqual([]);
        expect(app.history()).toBeNull();
        expect(app.loginError()).toContain('session has expired');
    });

    it('preserves the snapshot and reports a temporary refresh failure', async () => {
        app.rows.set([instance(1)]);
        const retrieved = new Date('2026-09-25T00:00:00Z');
        app.loadedAt.set(retrieved);
        api.dashboard.mockRejectedValue(new HttpErrorResponse({ status: 503 }));
        await app.refresh();
        expect(app.user()).toBe('admin');
        expect(app.rows()).toEqual([instance(1)]);
        expect(app.loadedAt()).toEqual(retrieved);
        expect(app.error()).toContain('Could not load telemetry');
        expect(app.loading()).toBe(false);
    });

    it('retains the session when sign-out fails so the user can retry', async () => {
        api.logout.mockRejectedValue(new HttpErrorResponse({ status: 403 }));
        await app.logout();
        expect(app.user()).toBe('admin');
        expect(app.error()).toContain('Could not sign out');
        expect(app.busy()).toBe(false);
    });
});
