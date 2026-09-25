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

/** Descending numeric versions, with releases before prereleases and unknown labels last. */
export function compareVersions(a: string, b: string): number {
    const pattern = /^v?(\d+(?:\.\d+)*)(?:-([\w.-]+))?(?:\+[\w.-]+)?$/i;
    const left = pattern.exec(a);
    const right = pattern.exec(b);
    if (!left || !right) {
        if (left) return -1;
        if (right) return 1;
        if (a === b) return 0;
        if (a === 'Not reported') return 1;
        if (b === 'Not reported') return -1;
        return a.localeCompare(b);
    }
    const leftNumbers = left[1].split('.').map(BigInt);
    const rightNumbers = right[1].split('.').map(BigInt);
    for (let i = 0; i < Math.max(leftNumbers.length, rightNumbers.length); i++) {
        const l = leftNumbers[i] ?? 0n;
        const r = rightNumbers[i] ?? 0n;
        if (l !== r) return l > r ? -1 : 1;
    }
    if (!left[2] || !right[2]) return left[2] ? 1 : right[2] ? -1 : 0;
    const leftPre = left[2].split('.');
    const rightPre = right[2].split('.');
    for (let i = 0; i < Math.max(leftPre.length, rightPre.length); i++) {
        const l = leftPre[i];
        const r = rightPre[i];
        if (l === r) continue;
        if (l === undefined) return 1;
        if (r === undefined) return -1;
        const lNumeric = /^\d+$/.test(l);
        const rNumeric = /^\d+$/.test(r);
        if (lNumeric && rNumeric) {
            if (BigInt(l) !== BigInt(r)) return BigInt(l) > BigInt(r) ? -1 : 1;
        } else if (lNumeric !== rNumeric) {
            return lNumeric ? 1 : -1;
        } else {
            return l > r ? -1 : 1;
        }
    }
    return 0;
}

export function includeInDirectory(row: Instance): boolean {
    const startup = row.latestStartup;
    const identity = [startup.adminName, startup.contact, startup.universityName];
    const hasIdentity = identity.some((value) => value?.trim());
    const hasExcludedText = [row.serverUrl, startup.operator, ...identity].some((value) => /test|staging/i.test(value ?? ''));
    return hasIdentity && !hasExcludedText;
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
