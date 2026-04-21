package com.myledger.api.model.entity;

/**
 * {@code auth/refresh_token.query!findValidUserIdByHash} 单行投影。
 */
public class RefreshTokenUserIdRow {

    private Long userId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
