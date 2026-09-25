import { describe, it, expect } from 'vitest';
import { distribution, moduleDistribution, filterInstances, safeUrl, contactUrl } from './data';
import { Instance } from './models';
const instance = (id: number, fields: object = {}): Instance => ({
    id,
    serverUrl: `https://instance${id}.example`,
    firstSeen: '2026-01-01T00:00:00Z',
    lastSeen: '2026-09-25T00:00:00Z',
    latestStartup: { id, serverUrl: `https://instance${id}.example`, isProductionInstance: true, ...fields },
});
describe('instance overview', () => {
    it('counts each latest snapshot once and distinguishes unknown from zero', () => {
        expect(
            distribution([instance(1, { buildAgentCount: 0 }), instance(2)], (i) => i.latestStartup.buildAgentCount?.toString()),
        ).toEqual([
            { label: '0', count: 1, percent: 50 },
            { label: 'Not reported', count: 1, percent: 50 },
        ]);
    });
    it('counts module adoption per instance and distinguishes none from unreported', () => {
        expect(
            moduleDistribution([instance(1, { moduleFeatures: ['iris', 'iris'] }), instance(2, { moduleFeatures: [] }), instance(3)]),
        ).toEqual([
            { label: 'iris', count: 1, percent: 100 / 3 },
            { label: 'None enabled', count: 1, percent: 100 / 3 },
            { label: 'Not reported', count: 1, percent: 100 / 3 },
        ]);
    });
    it('applies search, environment and recency to the same population', () => {
        const rows = [
            instance(1, { universityName: 'München' }),
            instance(2, { isProductionInstance: false }),
            { ...instance(3), lastSeen: '2025-01-01T00:00:00Z' },
        ];
        expect(filterInstances(rows, 'MÜNCH', 'production', 90, Date.parse('2026-09-25'))).toHaveLength(1);
        expect(filterInstances(rows, '', 'all', 0, Date.now())).toHaveLength(3);
        expect(filterInstances(rows, '', 'production', 90, Date.parse('2026-09-25'))).toHaveLength(1);
    });
    it('only links safe server URLs and email addresses', () => {
        expect(safeUrl('javascript:alert(1)')).toBeNull();
        expect(safeUrl('https://example.org')).toBe('https://example.org/');
        expect(contactUrl('admin@example.org')).toBe('mailto:admin%40example.org');
        expect(contactUrl('javascript:alert(1)')).toBeNull();
        expect(contactUrl('admin@example.org?bcc=someone@example.org')).toBeNull();
    });
});
