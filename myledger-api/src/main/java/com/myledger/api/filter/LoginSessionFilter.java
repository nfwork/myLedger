package com.myledger.api.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myledger.api.model.dto.response.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 未登录拦截：依赖 HttpSession 中的 {@code user_id}（由 {@code /api/auth/login} 写入）。
 * dbfound 各 model 通过 {@code scope="session"} 读取同一 {@code user_id}。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class LoginSessionFilter extends OncePerRequestFilter {

    private static final Set<String> ANONYMOUS_PATHS = Set.of(
            "/api/health",
            "/api/auth/login",
            "/api/auth/logout",
            "/user/user.query!login",
            "/user/user.execute!register"
    );

    private final ObjectMapper objectMapper;

    public LoginSessionFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("user_id") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.fail("未登录或会话已过期")));
            return;
        }

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
}
