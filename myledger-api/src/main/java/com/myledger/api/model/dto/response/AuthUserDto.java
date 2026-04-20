package com.myledger.api.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.myledger.api.model.entity.MlUser;
import jakarta.servlet.http.HttpSession;

/**
 * 登录成功及 /me 返回的用户信息（JSON 字段名与既有前端约定一致）。
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
        return new AuthUserDto(mlUser.getUser_id(), mlUser.getUsername(), mlUser.getNickname());
    }

    public static AuthUserDto fromSession(HttpSession session) {
        Long userId = parseSessionUserId(session.getAttribute("user_id"));
        String username = (String) session.getAttribute("username");
        String nickname = (String) session.getAttribute("nickname");
        return new AuthUserDto(userId, username, nickname);
    }

    private static Long parseSessionUserId(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        if (raw instanceof String s && !s.isBlank()) {
            return Long.parseLong(s.trim());
        }
        throw new IllegalArgumentException("无法解析为 Long: " + raw);
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
}
