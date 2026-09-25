package de.tum.cit.aet.artemis.telemetry.service;

import java.net.URI;
import java.util.Locale;

/** Canonical installation identity, shared with the historical-data migration. */
public final class ServerUrl {
    private ServerUrl() { }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Server URL must not be empty");
        }
        URI uri;
        try {
            uri = URI.create(value.strip());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Server URL must be an absolute HTTP(S) URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if ((!scheme.equals("http") && !scheme.equals("https")) || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null || uri.getPort() > 65535 || uri.getPort() == 0) {
            throw new IllegalArgumentException("Server URL must be HTTP(S), without credentials, query or fragment");
        }
        int port = uri.getPort();
        boolean defaultPort = port == -1 || scheme.equals("https") && port == 443 || scheme.equals("http") && port == 80;
        String path = uri.getRawPath();
        // Strip terminal slashes consistently so canonicalization is idempotent.
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        String result = scheme + "://" + uri.getHost().toLowerCase(Locale.ROOT) + (defaultPort ? "" : ":" + port) + path;
        if (result.length() > 255) {
            throw new IllegalArgumentException("Server URL must not exceed 255 characters");
        }
        return result;
    }
}
