# Minimal telemetry dashboard

Implement a same-origin Angular 22.2.0 SPA served by Spring Boot. Reuse telemetry.user / telemetry.password (TELEMETRY_USER / TELEMETRY_PASSWORD) for a single in-memory account. Browser login uses a server session with HttpOnly cookies, CSRF-protected login/logout, no stored browser credentials. Preserve HTTP Basic for existing read clients and anonymous telemetry ingestion.

Provide an authenticated latest-instance snapshot endpoint using two bulk queries. Client filters all snapshot rows together, then derives charts and a paginated, searchable contact directory. Charts: Artemis versions, enabled modules (unknown and none distinguished), databases, node counts. Display per-instance history on demand through the existing paginated endpoint. Show data age, no-data, unavailable-field, loading, session-expired and error states.

Use a restrained research-report layout: dark blue header, warm white canvas, compact navy text, blue horizontal bars, flat tables and clear dividing rules. Native form controls and accessible chart labels. No component framework or chart dependency is needed.

Sequence: test authentication/data access; implement session and dashboard APIs; create Angular client and aggregation tests; integrate production build/Docker/CI; run backend/client/security/browser checks; independent review; create PR. Do not deploy production without a separate deployment request.
