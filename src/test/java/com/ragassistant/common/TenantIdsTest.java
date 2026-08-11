package com.ragassistant.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIdsTest {

    @Test
    void sanitize_keepsValidIds() {
        assertThat(TenantIds.sanitize("acme")).isEqualTo("acme");
        assertThat(TenantIds.sanitize("cli")).isEqualTo("cli");
        assertThat(TenantIds.sanitize("tenantA")).isEqualTo("tenantA");
        assertThat(TenantIds.sanitize("111")).isEqualTo("111");
        assertThat(TenantIds.sanitize("team-1_x")).isEqualTo("team-1_x");
    }

    @Test
    void sanitize_rejectsTraversalAndSeparators() {
        assertThat(TenantIds.sanitize("../../etc")).isEqualTo("default");
        assertThat(TenantIds.sanitize("a/b")).isEqualTo("default");
        assertThat(TenantIds.sanitize("a..b")).isEqualTo("default");
        assertThat(TenantIds.sanitize("acme!")).isEqualTo("default");
    }

    @Test
    void sanitize_fallsBackOnBlankOrNullOrTooLong() {
        assertThat(TenantIds.sanitize(null)).isEqualTo("default");
        assertThat(TenantIds.sanitize("   ")).isEqualTo("default");
        assertThat(TenantIds.sanitize("x".repeat(65))).isEqualTo("default");
        assertThat(TenantIds.sanitize(null, "cli")).isEqualTo("cli");
        assertThat(TenantIds.sanitize("../../etc", "cli")).isEqualTo("cli");
    }
}
