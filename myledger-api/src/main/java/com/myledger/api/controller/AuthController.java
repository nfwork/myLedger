package com.myledger.api.controller;

import com.myledger.api.model.dto.request.LoginRequest;
import com.myledger.api.model.dto.response.ApiResponse;
import com.myledger.api.model.dto.response.AuthUserDto;
import com.myledger.api.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthUserDto> login(@RequestBody LoginRequest body, HttpSession session) {
        return ApiResponse.ok(authService.login(body, session));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        authService.logout(session);
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<AuthUserDto> me(HttpSession session) {
        return ApiResponse.ok(authService.requireCurrentUser(session));
    }
}
