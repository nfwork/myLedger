package com.myledger.api.service;

import com.github.nfwork.dbfound.starter.ModelExecutor;
import com.myledger.api.model.entity.RefreshTokenUserIdRow;
import com.myledger.api.security.TokenHasher;
import com.nfwork.dbfound.core.Context;
import com.nfwork.dbfound.dto.ResponseObject;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

    private static final String MODEL = "auth/refresh_token";

    private final ModelExecutor modelExecutor;

    public RefreshTokenService(ModelExecutor modelExecutor) {
        this.modelExecutor = modelExecutor;
    }

    public void insert(long userId, String tokenHash, long expiresAtEpochSeconds) {
        Context ctx = new Context()
                .withParam("user_id", userId)
                .withParam("token_hash", tokenHash)
                .withParam("expires_at", expiresAtEpochSeconds);
        requireSuccess(modelExecutor.execute(ctx, MODEL, "insert"), "insert refresh token");
    }

    public Long consumeByPlaintext(String rawToken) {
        String hash = TokenHasher.sha256Hex(rawToken);
        Context q = new Context().withParam("token_hash", hash);
        RefreshTokenUserIdRow row = modelExecutor.queryOne(q, MODEL, "findValidUserIdByHash", RefreshTokenUserIdRow.class);
        if (row == null || row.getUser_id() == null) {
            return null;
        }
        requireSuccess(modelExecutor.execute(new Context().withParam("token_hash", hash), MODEL, "deleteByHash"), "delete refresh token");
        return row.getUser_id();
    }

    public void revokeByPlaintext(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = TokenHasher.sha256Hex(rawToken.trim());
        modelExecutor.execute(new Context().withParam("token_hash", hash), MODEL, "deleteByHash");
    }

    public void deleteAllForUser(long userId) {
        requireSuccess(modelExecutor.execute(new Context().withParam("user_id", userId), MODEL, "deleteAllForUser"), "delete refresh tokens for user");
    }

    public void deleteExpired() {
        modelExecutor.execute(new Context(), MODEL, "deleteExpired");
    }

    private static void requireSuccess(ResponseObject ro, String action) {
        if (ro == null || !ro.isSuccess()) {
            String msg = ro != null ? ro.getMessage() : "null response";
            throw new IllegalStateException("dbfound " + action + " failed: " + msg);
        }
    }
}
