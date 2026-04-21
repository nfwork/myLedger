package com.myledger.api.model.dto.response;

import com.myledger.api.model.entity.MlUser;

/**
 * 登录成功及 {@code GET /api/auth/me} 返回的用户信息（JSON 字段名与既有前端约定一致）。
 * 昵称来自库表（登录包体与 {@code /me} 均由 {@link #fromMlUser} 构造），不嵌入 access JWT。
 */
public class AuthUserDto {

    private final Long userId;
    private final String username;
    private final String nickname;

    public AuthUserDto(Long userId, String username, String nickname) {
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
    }

    public static AuthUserDto fromMlUser(MlUser mlUser) {
        if (mlUser == null) {
            return null;
        }
        return new AuthUserDto(mlUser.getUserId(), mlUser.getUsername(), mlUser.getNickname());
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }
}
