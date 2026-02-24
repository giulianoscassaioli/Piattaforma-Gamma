package com.gamma.gateway.proxy;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class ProxyController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gateway.pec-service-url:http://localhost:8081}")
    private String pecServiceUrl;

    @Value("${gateway.firma-service-url:http://localhost:8082}")
    private String firmaServiceUrl;

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> proxy(
            HttpMethod method,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body,
            JwtAuthenticationToken jwtAuth) {

        String path = request.getRequestURI();
        String query = request.getQueryString();
        String baseUrl = path.startsWith("/api/firma") ? firmaServiceUrl : pecServiceUrl;
        String targetUrl = baseUrl + path + (query != null ? "?" + query : "");

        HttpHeaders headers = estraiHeaders(request, jwtAuth.getToken());

        log.debug("Proxy {} {} → {}", method, path, targetUrl);

        try {
            return restTemplate.exchange(targetUrl, method, new HttpEntity<>(body, headers), byte[].class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .headers(e.getResponseHeaders())
                    .body(e.getResponseBodyAsByteArray());
        }
    }

    private HttpHeaders estraiHeaders(HttpServletRequest request, Jwt jwt) {
        String tenantId = jwt.getClaimAsString("tenant_id");
        String userId = jwt.getSubject();
        String roles = estraiRuoli(jwt);
        HttpHeaders headers = new HttpHeaders();
        String contentType = request.getContentType();
        if (contentType != null) {
            headers.set(HttpHeaders.CONTENT_TYPE, contentType);
        }
        headers.set("X-Tenant-Id", tenantId != null ? tenantId : "");
        headers.set("X-User-Id", userId != null ? userId : "");
        headers.set("X-Roles", roles);
        return headers;
    }

    private String estraiRuoli(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) return "";
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return "";
        return String.join(",", roles);
    }
}
