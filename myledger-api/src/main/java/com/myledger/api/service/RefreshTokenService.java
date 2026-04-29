package com.myledger.api.service;

import com.github.nfwork.dbfound.starter.ModelExecutor;
import com.myledger.api.model.entity.RefreshTokenUserIdRow;
import com.nfwork.dbfound.core.Context;
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
        modelExecutor.execute(ctx, MODEL, "insert");
    }

    public Long consumeByPlaintext(String rawToken) {
        String hash = JwtService.sha256Hex(rawToken);
        Context q = new Context().withParam("token_hash", hash);
        RefreshTokenUserIdRow row = modelExecutor.queryOne(q, MODEL, "findValidUserIdByHash", RefreshTokenUserIdRow.class);
        if (row == null || row.getUserId() == null) {
            return null;
        }
        modelExecutor.execute(new Context().withParam("token_hash", hash), MODEL, "deleteByHash");
        return row.getUserId();
    }

    public void revokeByPlaintext(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        String hash = JwtService.sha256Hex(rawToken.trim());
        modelExecutor.execute(new Context().withParam("token_hash", hash), MODEL, "deleteByHash");
    }

    public void deleteExpired() {
        modelExecutor.execute(new Context(), MODEL, "deleteExpired");
    }
}
