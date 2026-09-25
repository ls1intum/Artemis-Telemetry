import { ChangeDetectionStrategy, Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { Api } from './api';
import { ChartComponent } from './chart';
import { compareVersions, contactUrl, distribution, filterInstances, includeInDirectory, moduleDistribution, safeUrl } from './data';
import { Instance, Page, Startup } from './models';

@Component({
    selector: 'app-root',
    imports: [FormsModule, DatePipe, DecimalPipe, ChartComponent],
    templateUrl: './app.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppComponent implements OnInit {
    private api = inject(Api);
    private generation = 0;
    private historyRequest = 0;
    user = signal<string | null>(null);
    initializing = signal(true);
    busy = signal(false);
    loading = signal(false);
    error = signal('');
    loginError = signal('');
    rows = signal<Instance[]>([]);
    loadedAt = signal<Date | null>(null);
    search = signal('');
    environment = signal('all');
    days = signal(0);
    hideUnidentified = signal(true);
    page = signal(0);
    selected = signal<Instance | null>(null);
    history = signal<Page<Startup> | null>(null);
    historyLoading = signal(false);
    historyError = signal('');
    username = '';
    password = '';
    safeUrl = safeUrl;
    contactUrl = contactUrl;
    readonly pageSize = 15;
    filtered = computed(() =>
        filterInstances(this.rows(), this.search(), this.environment(), this.days(), this.loadedAt()?.getTime() ?? Date.now()),
    );
    directory = computed(() =>
        this.filtered()
            .filter((row) => !this.hideUnidentified() || includeInDirectory(row))
            .sort((a, b) => this.institution(a).localeCompare(this.institution(b)) || a.serverUrl.localeCompare(b.serverUrl)),
    );
    pageCount = computed(() => Math.ceil(this.directory().length / this.pageSize));
    visibleRows = computed(() => this.directory().slice(this.page() * this.pageSize, (this.page() + 1) * this.pageSize));
    universities = computed(
        () =>
            new Set(
                this.filtered()
                    .map((i) => i.latestStartup.universityName?.trim().toLocaleLowerCase())
                    .filter(Boolean),
            ).size,
    );
    contacts = computed(() => this.filtered().filter((i) => i.latestStartup.contact?.trim()).length);
    versions = computed(() =>
        distribution(this.filtered(), (i) => i.latestStartup.version).sort((a, b) => compareVersions(a.label, b.label)),
    );
    modules = computed(() => moduleDistribution(this.filtered()));
    databases = computed(() => distribution(this.filtered(), (i) => i.latestStartup.dataSource));
    nodes = computed(() =>
        distribution(this.filtered(), (i) =>
            i.latestStartup.numberOfNodes == null
                ? undefined
                : `${i.latestStartup.numberOfNodes} node${i.latestStartup.numberOfNodes === 1 ? '' : 's'}`,
        ),
    );

    async ngOnInit() {
        try {
            const session = await this.api.session();
            this.user.set(session.authenticated ? session.username : null);
            if (session.authenticated) await this.refresh();
        } catch {
            this.loginError.set('Could not connect to the server. Try signing in again.');
        } finally {
            this.initializing.set(false);
        }
    }
    async login() {
        if (this.busy()) return;
        this.busy.set(true);
        this.loginError.set('');
        try {
            const session = await this.api.login(this.username.trim(), this.password);
            this.password = '';
            this.user.set(session.username);
            await this.refresh();
        } catch (error) {
            this.password = '';
            this.loginError.set(
                error instanceof HttpErrorResponse && error.status === 401
                    ? 'The username or password is incorrect.'
                    : 'Could not sign in. Check your connection and try again.',
            );
        } finally {
            this.busy.set(false);
        }
    }
    async logout() {
        this.busy.set(true);
        try {
            await this.api.logout();
            this.clearSession();
        } catch (error) {
            if (this.expired(error)) return;
            this.error.set('Could not sign out. Check your connection and try again.');
        } finally {
            this.busy.set(false);
        }
    }
    private clearSession() {
        this.generation++;
        this.historyRequest++;
        this.user.set(null);
        this.rows.set([]);
        this.selected.set(null);
        this.history.set(null);
        this.loadedAt.set(null);
        this.error.set('');
        this.loading.set(false);
    }
    private expired(error: unknown): boolean {
        if (error instanceof HttpErrorResponse && error.status === 401) {
            this.clearSession();
            this.loginError.set('Your session has expired. Sign in again to continue.');
            return true;
        }
        return false;
    }
    async refresh() {
        const generation = ++this.generation;
        this.loading.set(true);
        this.error.set('');
        try {
            const rows = await this.api.dashboard();
            if (generation !== this.generation) return;
            this.rows.set(rows);
            this.loadedAt.set(new Date());
            this.page.set(0);
        } catch (error) {
            if (generation === this.generation && !this.expired(error))
                this.error.set('Could not load telemetry. Use Refresh to try again. Previously loaded data may be out of date.');
        } finally {
            if (generation === this.generation) this.loading.set(false);
        }
    }
    setSearch(value: string) {
        this.search.set(value);
        this.page.set(0);
    }
    setEnvironment(value: string) {
        this.environment.set(value);
        this.page.set(0);
    }
    setDays(value: number) {
        this.days.set(Number(value));
        this.page.set(0);
    }
    setHideUnidentified(value: boolean) {
        this.hideUnidentified.set(value);
        this.page.set(0);
    }
    resetFilters() {
        this.search.set('');
        this.environment.set('all');
        this.days.set(0);
        this.page.set(0);
    }
    institution(row: Instance) {
        return row.latestStartup.universityName?.trim() || 'University not reported';
    }
    async openHistory(row: Instance, page = 0) {
        const request = ++this.historyRequest;
        this.selected.set(row);
        setTimeout(() => document.getElementById('startup-history')?.focus(), 0);
        this.historyLoading.set(true);
        this.historyError.set('');
        try {
            const history = await this.api.history(row.id, page);
            if (request === this.historyRequest) this.history.set(history);
        } catch (error) {
            if (request === this.historyRequest && !this.expired(error))
                this.historyError.set('Could not load startup history. Try again.');
        } finally {
            if (request === this.historyRequest) this.historyLoading.set(false);
        }
    }
    closeHistory() {
        const id = this.selected()?.id;
        this.historyRequest++;
        this.selected.set(null);
        this.history.set(null);
        this.historyError.set('');
        setTimeout(() => (document.getElementById(`history-${id}`) ?? document.getElementById('search'))?.focus(), 0);
    }
}
