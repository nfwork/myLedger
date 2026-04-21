package com.myledger.api.service;

import com.github.nfwork.dbfound.starter.ModelExecutor;
import com.myledger.api.filter.BearerAuthFilter;
import com.myledger.api.model.dto.request.LoginRequest;
import com.myledger.api.model.dto.request.RefreshTokenBody;
import com.myledger.api.model.dto.response.AuthTokenBundleDto;
import com.myledger.api.model.dto.response.AuthUserDto;
import com.myledger.api.model.entity.MlUser;
import com.nfwork.dbfound.core.Context;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ModelExecutor modelExecutor;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(ModelExecutor modelExecutor, JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.modelExecutor = modelExecutor;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 校验账号密码，签发 access JWT 与 refresh（入库为哈希），同一用户旧 refresh 全部作废。
     */
    public AuthTokenBundleDto login(LoginRequest body) {
        MlUser mlUser = authenticateCredentials(body);
        refreshTokenService.deleteAllForUser(mlUser.getUserId());
        return issueTokenBundle(mlUser);
    }

    public AuthTokenBundleDto refresh(RefreshTokenBody body) {
        if (body == null || !StringUtils.hasText(body.getRefreshToken())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 refresh_token");
        }
        Long userId = refreshTokenService.consumeByPlaintext(body.getRefreshToken().trim());
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "刷新令牌无效或已过期");
        }
        MlUser user = findUserByIdForAuth(userId);
        if (user == null || user.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }
        return issueTokenBundle(user);
    }

    public void logout(String refreshTokenPlain) {
        if (StringUtils.hasText(refreshTokenPlain)) {
            refreshTokenService.revokeByPlaintext(refreshTokenPlain.trim());
        }
    }

    public AuthUserDto requireCurrentUser(HttpServletRequest request) {
        Object raw = request.getAttribute(BearerAuthFilter.ATTR_USER_ID);
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return AuthUserDto.fromRequest(request);
    }

    private MlUser authenticateCredentials(LoginRequest body) {
        if (body == null || !StringUtils.hasText(body.getUsername()) || !StringUtils.hasText(body.getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 username 或 password");
        }
        String username = body.getUsername().trim();
        if (username.length() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名至少4个字符");
        }
        Context ctx = new Context()
                .withParam("username", username)
                .withParam("password", body.getPassword());
        MlUser mlUser = modelExecutor.queryOne(ctx, "user/user", "login", MlUser.class);
        if (mlUser == null || mlUser.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        return mlUser;
    }

    private MlUser findUserByIdForAuth(long userId) {
        return modelExecutor.queryOne(new Context().withParam("user_id", userId), "user/user", "findByIdForAuth", MlUser.class);
    }

    private AuthTokenBundleDto issueTokenBundle(MlUser user) {
        String access = jwtService.issueAccessToken(user.getUserId(), user.getUsername(), user.getNickname());
        String refreshRaw = newRefreshTokenRaw();
        Instant exp = Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds());
        refreshTokenService.insert(user.getUserId(), JwtService.sha256Hex(refreshRaw), exp.getEpochSecond());
        return new AuthTokenBundleDto(user, access, refreshRaw, jwtService.getAccessTokenTtlSeconds());
    }

    private static String newRefreshTokenRaw() {
        byte[] b = new byte[32];
        RANDOM.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
