# Artemis Telemetry

## Platform
web

## Stack
Angular 22.2.0 client, existing Spring Boot collector, same Docker container and origin.

## Purpose
Give Artemis maintainers a private overview of installations, common configurations, and university contacts. Exactly one configured account; no registration or user administration.

## Data and constraints
Use the latest startup per canonical instance URL for distributions, not all historical reports. Keep startup history accessible separately. Test servers are excluded. Missing metadata and counts remain unknown, never fabricated. Contacts must come from the latest report, respecting sender opt-out.

## Assumptions
Confirmed: the default includes all known instances, with a visible last-reported date and an optional recency filter. The environment filter initially includes all environments; test servers remain excluded. The dashboard is used mainly on a desktop, with a usable mobile view.
