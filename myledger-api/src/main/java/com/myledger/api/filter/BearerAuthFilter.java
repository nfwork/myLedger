package com.myledger.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myledger.api.model.dto.response.ApiResponse;
import com.myledger.api.security.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

/**
 * 校验 {@code Authorization: Bearer &lt;access_jwt&gt;}，将 {@code user_id}、{@code username}、{@code nickname}
 * 写入 request attribute，供 dbfound {@code scope="request"} 使用。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class BearerAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_USER_ID = "user_id";
    public static final String ATTR_USERNAME = "username";
    public static final String ATTR_NICKNAME = "nickname";

    private static final Set<String> ANONYMOUS_PATHS = Set.of(
            "/api/health",
            "/api/auth/login",
            "/api/auth/logout",
            "/api/auth/refresh",
            "/user/user.execute!register"
    );

    private final ObjectMapper objectMapper;
    private final JwtService jwtService;

    public BearerAuthFilter(ObjectMapper objectMapper, JwtService jwtService) {
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();
        if (!requiresAuth(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String bearer = readBearerToken(request);
        Optional<JwtService.JwtAccessPrincipal> principal = jwtService.parseAccessToken(bearer);
        if (principal.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail("未登录或访问令牌已过期")));
            return;
        }

        JwtService.JwtAccessPrincipal p = principal.get();
        request.setAttribute(ATTR_USER_ID, p.userId());
        request.setAttribute(ATTR_USERNAME, p.username() != null ? p.username() : "");
        request.setAttribute(ATTR_NICKNAME, p.nickname() != null ? p.nickname() : "");

        filterChain.doFilter(request, response);
    }

    private static boolean requiresAuth(String path) {
        if (ANONYMOUS_PATHS.contains(path)) {
            return false;
        }
        if (path.startsWith("/api/")) {
            return true;
        }
        return path.contains(".query") || path.contains(".execute");
    }

    static String readBearerToken(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null) {
            return null;
        }
        String prefix = "Bearer ";
        if (h.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return h.substring(prefix.length()).trim();
        }
        return null;
    }
}
