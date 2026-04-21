package com.myledger.api.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.myledger.api.model.entity.MlUser;

/**
 * 登录 / 刷新令牌接口返回：用户信息 + access / refresh JWT 材料。
 */
public class AuthTokenBundleDto {

    private final Long userId;
    private final String username;
    private final String nickname;
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;

    public AuthTokenBundleDto(MlUser user, String accessToken, String refreshToken, long expiresInSeconds) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.nickname = user.getNickname();
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresInSeconds;
    }

    @JsonProperty("user_id")
    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    @JsonProperty("access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @JsonProperty("refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    @JsonProperty("token_type")
    public String getTokenType() {
        return tokenType;
    }

    @JsonProperty("expires_in")
    public long getExpiresIn() {
        return expiresIn;
    }
}
