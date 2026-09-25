import { Distribution, Instance } from './models';

export function distribution(rows: Instance[], key: (row: Instance) => string | undefined): Distribution[] {
    const counts = new Map<string, number>();
    for (const row of rows) {
        const label = key(row)?.trim() || 'Not reported';
        counts.set(label, (counts.get(label) ?? 0) + 1);
    }
    return ranked(counts, rows.length);
}
export function moduleDistribution(rows: Instance[]): Distribution[] {
    const counts = new Map<string, number>();
    for (const row of rows) {
        const features = row.latestStartup.moduleFeatures;
        const labels = features == null ? ['Not reported'] : features.length ? [...new Set(features)] : ['None enabled'];
        for (const label of labels) counts.set(label, (counts.get(label) ?? 0) + 1);
    }
    return ranked(counts, rows.length);
}
function ranked(counts: Map<string, number>, total: number): Distribution[] {
    return [...counts]
        .map(([label, count]) => ({ label, count, percent: total ? (count * 100) / total : 0 }))
        .sort((a, b) => b.count - a.count || a.label.localeCompare(b.label));
}
export function filterInstances(rows: Instance[], search: string, environment: string, days: number, now: number): Instance[] {
    const query = search.trim().toLocaleLowerCase();
    return rows.filter((row) => {
        const s = row.latestStartup;
        const matchesEnvironment =
            environment === 'all' || (environment === 'production' ? s.isProductionInstance === true : s.isProductionInstance !== true);
        const matchesDate = !days || Date.parse(row.lastSeen) >= now - days * 86400000;
        const matchesSearch = [row.serverUrl, s.universityName, s.operator, s.adminName, s.contact, s.version].some((v) =>
            v?.toLocaleLowerCase().includes(query),
        );
        return matchesEnvironment && matchesDate && matchesSearch;
    });
}
export function safeUrl(value: string): string | null {
    try {
        const url = new URL(value);
        return ['https:', 'http:'].includes(url.protocol) && !url.username && !url.password ? url.href : null;
    } catch {
        return null;
    }
}
export function contactUrl(value: string | undefined): string | null {
    return value && /^[^\s@?&#:]+@[^\s@?&#:]+\.[^\s@?&#:]+$/.test(value) ? `mailto:${encodeURIComponent(value)}` : null;
}
