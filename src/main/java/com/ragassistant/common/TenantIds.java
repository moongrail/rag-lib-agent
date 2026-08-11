package com.ragassistant.common;

import java.util.regex.Pattern;

/**
 * Central tenant-id sanitization.
 *
 * <p>Why: tenant id flows into JPA filters, pgvector metadata filters,
 * MDC logs and — critically — filesystem paths
 * ({@code DocumentStorageService}). Accepting arbitrary header values
 * allows path traversal ({@code ../../etc}) and cross-tenant probing.
 * Whitelist keeps backwards compatibility with existing tenants
 * ({@code acme}, {@code cli}, {@code tenantA}, numeric chat ids).</p>
 */
public final class TenantIds {

    private static final Pattern SAFE = Pattern.compile("[a-zA-Z0-9_-]{1,64}");
    private static final String FALLBACK = "default";

    private TenantIds() {
    }

    public static String sanitize(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback != null ? fallback : FALLBACK;
        }
        String t = raw.trim();
        if (SAFE.matcher(t).matches()) {
            return t;
        }
        return fallback != null ? fallback : FALLBACK;
    }

    public static String sanitize(String raw) {
        return sanitize(raw, FALLBACK);
    }

    public static String fallback() {
        return FALLBACK;
    }
}
