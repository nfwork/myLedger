package com.myledger.api.service;

import com.myledger.api.config.JwtSecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class JwtService {

    public static final String CLAIM_USERNAME = "username";

    private final JwtSecurityProperties props;
    private final SecretKey key;

    public JwtService(JwtSecurityProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** refresh 明文入库前 SHA-256 hex（与 dbfound 中校验一致） */
    public static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public String issueAccessToken(long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(props.getAccessTokenTtlSeconds());
        String u = username != null ? username : "";
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim(CLAIM_USERNAME, u)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public Optional<JwtAccessPrincipal> parseAccessToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            long userId = Long.parseLong(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            return Optional.of(new JwtAccessPrincipal(userId, username));
        } catch (ExpiredJwtException e) {
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public int getAccessTokenTtlSeconds() {
        return props.getAccessTokenTtlSeconds();
    }

    public int getRefreshTokenTtlSeconds() {
        return props.getRefreshTokenTtlSeconds();
    }

    public record JwtAccessPrincipal(long userId, String username) {
    }
}
