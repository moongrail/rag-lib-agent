package com.ragassistant.common;

import com.ragassistant.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantFilterTest {

    private final AppProperties props = new AppProperties();
    private final TenantFilter filter = new TenantFilter(props);

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        MDC.remove("tenant");
    }

    private String captureTenant(String headerValue) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader(props.getTenant().getHeaderName())).thenReturn(headerValue);

        AtomicReference<String> captured = new AtomicReference<>();
        FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            captured.set(TenantContext.get());
            return null;
        }).when(chain).doFilter(any(), any());

        filter.doFilter(request, response, chain);
        return captured.get();
    }

    @Test
    void usesHeaderWhenPresent() throws Exception {
        assertThat(captureTenant("acme")).isEqualTo("acme");
        assertThat(MDC.get("tenant")).isNull();
    }

    @Test
    void fallsBackToDefaultTenant() throws Exception {
        assertThat(captureTenant(null)).isEqualTo(props.getTenant().getDefaultTenant());
    }

    @Test
    void blankHeaderFallsBackToDefault() throws Exception {
        assertThat(captureTenant("   ")).isEqualTo(props.getTenant().getDefaultTenant());
    }
}
