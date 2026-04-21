package com.myledger.api.model.dto.response;

import com.myledger.api.filter.BearerAuthFilter;
import com.myledger.api.model.entity.MlUser;
import jakarta.servlet.http.HttpServletRequest;

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
        return new AuthUserDto(mlUser.getUserId(), mlUser.getUsername(), mlUser.getNickname());
    }

    public static AuthUserDto fromRequest(HttpServletRequest request) {
        Long userId = parseUserId(request.getAttribute(BearerAuthFilter.ATTR_USER_ID));
        String username = (String) request.getAttribute(BearerAuthFilter.ATTR_USERNAME);
        String nickname = (String) request.getAttribute(BearerAuthFilter.ATTR_NICKNAME);
        return new AuthUserDto(userId, username, nickname);
    }

    private static Long parseUserId(Object raw) {
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
