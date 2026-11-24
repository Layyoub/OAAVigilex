/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(@Value("${vigilex.security.jwt-secret}") String secret,
                      @Value("${vigilex.security.access-ttl:PT15M}") Duration accessTtl) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT signing secret must contain at least 32 bytes");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = accessTtl;
    }

    public String issueAccessToken(AppPrincipalData user) {
        Instant now = Instant.now();
        return Jwts.builder().subject(user.id().toString()).claim("email", user.email())
                .claim("role", user.role()).claim("tenant", user.tenantId() == null ? null : user.tenantId().toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plus(accessTtl)))
                .signWith(key).compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public record AppPrincipalData(UUID id, UUID tenantId, String role, String email) { }
}
