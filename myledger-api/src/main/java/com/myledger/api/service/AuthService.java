package com.myledger.api.service;

import com.github.nfwork.dbfound.starter.ModelExecutor;
import com.myledger.api.model.dto.request.LoginRequest;
import com.myledger.api.model.dto.response.AuthUserDto;
import com.myledger.api.model.entity.MlUser;
import com.nfwork.dbfound.core.Context;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpSession;

@Service
public class AuthService {

    private final ModelExecutor modelExecutor;

    public AuthService(ModelExecutor modelExecutor) {
        this.modelExecutor = modelExecutor;
    }

    /**
     * 校验账号密码，写入 Session 并返回用户信息。
     */
    public AuthUserDto login(LoginRequest body, HttpSession session) {
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
        if (mlUser == null || mlUser.getUser_id() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        AuthUserDto user = AuthUserDto.fromMlUser(mlUser);

        session.setAttribute("user_id", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("nickname", user.getNickname());
        return user;
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    /**
     * 当前登录用户；未登录则抛出 401。
     */
    public AuthUserDto requireCurrentUser(HttpSession session) {
        Object raw = session.getAttribute("user_id");
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return AuthUserDto.fromSession(session);
    }
}
