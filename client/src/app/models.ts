export interface Startup {
    id: number;
    version?: string;
    serverUrl: string;
    operator?: string;
    universityName?: string;
    adminName?: string;
    contact?: string;
    profiles?: string[];
    moduleFeatures?: string[];
    numberOfNodes?: number;
    buildAgentCount?: number;
    dataSource?: string;
    isProductionInstance?: boolean;
    isMultiNode?: boolean;
    startedAt?: string;
    timestamp?: string;
}
export interface Instance {
    id: number;
    serverUrl: string;
    firstSeen: string;
    lastSeen: string;
    latestStartup: Startup;
}
export interface Page<T> {
    content: T[];
    totalElements: number;
    totalPages: number;
    number: number;
}
export interface Session {
    authenticated: boolean;
    username: string | null;
    csrfToken: string;
}
export interface Distribution {
    label: string;
    count: number;
    percent: number;
}
