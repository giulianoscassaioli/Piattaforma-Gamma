package com.gamma.pec.security;

import com.gamma.pec.tenant.TenantContext;
import com.gamma.pec.tenant.TenantInfo;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(1)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwtToken) {
                Jwt jwt = jwtToken.getToken();
                TenantInfo info = TenantInfo.builder()
                        .tenantId(jwt.getClaimAsString("tenant_id"))
                        .userId(jwt.getSubject())
                        .userEmail(jwt.getClaimAsString("email"))
                        .build();
                TenantContext.set(info);
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
