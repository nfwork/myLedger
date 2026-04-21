package com.myledger.api.model.entity;

/**
 * {@code auth/refresh_token.query!findValidUserIdByHash} 单行投影。
 */
public class RefreshTokenUserIdRow {

    private Long user_id;

    public Long getUser_id() {
        return user_id;
    }

    public void setUser_id(Long user_id) {
        this.user_id = user_id;
    }
}
