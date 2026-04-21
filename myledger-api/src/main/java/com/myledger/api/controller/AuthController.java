package com.myledger.api.controller;

import com.myledger.api.model.dto.request.LoginRequest;
import com.myledger.api.model.dto.request.RefreshTokenBody;
import com.myledger.api.model.dto.response.ApiResponse;
import com.myledger.api.model.dto.response.AuthTokenBundleDto;
import com.myledger.api.model.dto.response.AuthUserDto;
import com.myledger.api.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokenBundleDto> login(@RequestBody LoginRequest body) {
        return ApiResponse.ok(authService.login(body));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokenBundleDto> refresh(@RequestBody(required = false) RefreshTokenBody body) {
        return ApiResponse.ok(authService.refresh(body));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenBody body) {
        String rt = body != null ? body.getRefreshToken() : null;
        authService.logout(rt);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> me(HttpServletRequest request) {
        return ApiResponse.ok(authService.requireCurrentUser(request));
    }
}
