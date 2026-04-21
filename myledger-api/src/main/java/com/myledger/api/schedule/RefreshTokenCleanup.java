package com.myledger.api.schedule;

import com.myledger.api.service.RefreshTokenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCleanup {

    private final RefreshTokenService refreshTokenService;

    public RefreshTokenCleanup(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    /** 每天凌晨清理已过期的刷新令牌行 */
    @Scheduled(cron = "0 0 4 * * ?")
    public void purgeExpired() {
        refreshTokenService.deleteExpired();
    }
}
