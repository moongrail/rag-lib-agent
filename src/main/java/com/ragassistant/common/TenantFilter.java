package com.ragassistant.common;

import com.ragassistant.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final AppProperties appProperties;

    public TenantFilter(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String headerName = appProperties.getTenant().getHeaderName();
        String tenant = request.getHeader(headerName);
        if (tenant == null || tenant.isBlank()) {
            tenant = appProperties.getTenant().getDefaultTenant();
        }
        TenantContext.set(tenant);
        MDC.put("tenant", tenant);
        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenant");
        }
    }
}
