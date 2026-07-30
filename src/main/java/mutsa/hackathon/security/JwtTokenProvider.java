package mutsa.hackathon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import mutsa.hackathon.domain.KakaoUserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${app.jwt.secret}") String secret, @Value("${app.jwt.access-token-expiration}") long accessTokenExpiration, @Value("${app.jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
    public String createAccessToken(KakaoUserProfile userProfile) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userProfile.id())).claim("provider", userProfile.provider()).claim("nickname", userProfile.nickname()).claim("type", "access").issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(accessTokenExpiration))).signWith(secretKey).compact();
    }
    public String createRefreshToken(KakaoUserProfile userProfile) {
        Instant now = Instant.now();
        return Jwts.builder().subject(String.valueOf(userProfile.id())).claim("type", "refresh").issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(refreshTokenExpiration))).signWith(secretKey).compact();
    }
    public boolean isValidToken(String token) {
        try { parseClaims(token); return true; } catch (Exception exception) { return false; }
    }
    public Long getUserId(String token) { return Long.valueOf(parseClaims(token).getSubject()); }
    public String getTokenType(String token) { return parseClaims(token).get("type", String.class); }
    public LocalDateTime getRefreshTokenExpiry(String token) { return LocalDateTime.ofInstant(parseClaims(token).getExpiration().toInstant(), ZoneId.systemDefault()); }
    public long getAccessTokenExpiration() { return accessTokenExpiration; }
    public long getRefreshTokenExpiration() { return refreshTokenExpiration; }
    private Claims parseClaims(String token) { return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload(); }
}