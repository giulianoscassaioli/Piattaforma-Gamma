package com.gamma.firma.security;

import com.gamma.firma.tenant.TenantContext;
import com.gamma.firma.tenant.TenantInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String tenantId = request.getHeader("X-Tenant-Id");
            String userId = request.getHeader("X-User-Id");
            String rolesHeader = request.getHeader("X-Roles");
            if (tenantId != null && !tenantId.isBlank() && userId != null && !userId.isBlank()) {
                settaContestoTenantDallHeader(tenantId, userId, rolesHeader);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private static void settaContestoTenantDallHeader(String tenantId, String userId, String rolesHeader) {
        TenantInfo info = TenantInfo.builder()
                .tenantId(tenantId)
                .userId(userId)
                .build();
        TenantContext.set(info);
        List<SimpleGrantedAuthority> authorities = List.of();
        if (rolesHeader != null && !rolesHeader.isBlank()) {
            authorities = aggiungiAuthorities(rolesHeader);
        }
        var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("TenantContext impostato: tenantId={}, userId={}", tenantId, userId);
    }

    private static @NonNull List<SimpleGrantedAuthority> aggiungiAuthorities(String rolesHeader) {
        List<SimpleGrantedAuthority> authorities;
        authorities = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return authorities;
    }
}
