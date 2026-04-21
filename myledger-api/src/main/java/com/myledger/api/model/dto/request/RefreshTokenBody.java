package com.myledger.api.model.dto.request;

/**
 * JSON：{@code refresh_token}（由全局 snake_case 映射到 {@code refreshToken}）。
 */
public class RefreshTokenBody {

    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
