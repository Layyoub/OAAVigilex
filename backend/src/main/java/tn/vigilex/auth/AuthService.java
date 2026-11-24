/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.vigilex.common.ApiException;
import tn.vigilex.domain.AppUser;
import tn.vigilex.domain.PasswordResetToken;
import tn.vigilex.domain.RefreshToken;
import tn.vigilex.domain.Tenant;
import tn.vigilex.repository.AppUserRepository;
import tn.vigilex.repository.PasswordResetTokenRepository;
import tn.vigilex.repository.RefreshTokenRepository;
import tn.vigilex.repository.TenantRepository;
import tn.vigilex.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final TenantRepository tenants;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordEncoder passwords;
    private final JwtService jwt;
    private final JavaMailSender mail;
    private final Duration refreshTtl;
    private final String resetUrl;
    private final String dummyPasswordHash;
    private final SecureRandom random = new SecureRandom();

    public AuthService(AppUserRepository users, TenantRepository tenants, RefreshTokenRepository refreshTokens,
                      PasswordResetTokenRepository resetTokens, PasswordEncoder passwords, JwtService jwt,
                      JavaMailSender mail, @Value("${vigilex.security.refresh-ttl:P30D}") Duration refreshTtl,
                      @Value("${vigilex.reset-url:http://localhost:4200/reset-password}") String resetUrl) {
        this.users = users;
        this.tenants = tenants;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.passwords = passwords;
        this.jwt = jwt;
        this.mail = mail;
        this.refreshTtl = refreshTtl;
        this.resetUrl = resetUrl;
        this.dummyPasswordHash = passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Session login(String email, String rawPassword) {
        AppUser user = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        if (user == null) {
            passwords.matches(rawPassword, dummyPasswordHash);
            throw invalidCredentials();
        }
        Instant now = Instant.now();
        if (!user.isActive() || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(now))) {
            throw invalidCredentials();
        }
        if (!passwords.matches(rawPassword, user.getPasswordHash())) {
            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            if (attempts >= 5) user.setLockedUntil(now.plus(Duration.ofMinutes(15)));
            users.save(user);
            throw invalidCredentials();
        }
        if (user.getTenantId() != null) {
            Tenant tenant = tenants.findById(user.getTenantId()).orElseThrow(() -> invalidCredentials());
            if (!"ACTIVE".equals(tenant.getStatus())) throw invalidCredentials();
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        users.save(user);
        return createSession(user);
    }

    @Transactional
    public Session refresh(String rawToken) {
        RefreshToken oldToken = refreshTokens.findActiveForUpdate(hash(rawToken))
                .filter(token -> token.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(this::invalidCredentials);
        oldToken.setRevokedAt(Instant.now());
        refreshTokens.save(oldToken);
        AppUser user = users.findById(oldToken.getUserId()).filter(AppUser::isActive)
                .orElseThrow(this::invalidCredentials);
        if (user.getTenantId() != null && tenants.findById(user.getTenantId())
                .filter(tenant -> "ACTIVE".equals(tenant.getStatus())).isEmpty())
            throw invalidCredentials();
        return createSession(user);
    }

    @Transactional
    public void logout(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        refreshTokens.findByTokenHashAndRevokedAtIsNull(hash(rawToken)).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokens.save(token);
        });
    }

    @Transactional
    public void requestPasswordReset(String email) {
        users.findByEmailIgnoreCase(email.trim()).filter(AppUser::isActive).ifPresent(user -> {
            resetTokens.findAllByUserIdAndUsedAtIsNull(user.getId()).forEach(previous -> {
                previous.setUsedAt(Instant.now());
                resetTokens.save(previous);
            });
            byte[] tokenBytes = new byte[32];
            random.nextBytes(tokenBytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
            PasswordResetToken token = new PasswordResetToken();
            token.setUserId(user.getId());
            token.setTokenHash(hash(raw));
            token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
            resetTokens.save(token);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setSubject("Réinitialisation de votre mot de passe Vigilex");
            message.setText("Utilisez ce lien avant expiration : " + resetUrl + "?token=" + raw);
            mail.send(message);
        });
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findActiveForUpdate(hash(rawToken))
                .filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
        AppUser user = users.findById(token.getUserId()).orElseThrow(() -> ApiException.badRequest("Invalid reset token"));
        user.setPasswordHash(passwords.encode(newPassword));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        token.setUsedAt(Instant.now());
        users.save(user);
        resetTokens.save(token);
        refreshTokens.findAllByUserIdAndRevokedAtIsNull(user.getId()).forEach(r -> {
            r.setRevokedAt(Instant.now());
            refreshTokens.save(r);
        });
    }

    private Session createSession(AppUser user) {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken token = new RefreshToken();
        token.setUserId(user.getId());
        token.setTokenHash(hash(refresh));
        token.setExpiresAt(Instant.now().plus(refreshTtl));
        refreshTokens.save(token);
        JwtService.AppPrincipalData principal = new JwtService.AppPrincipalData(
                user.getId(), user.getTenantId(), user.getRole(), user.getEmail());
        return new Session(jwt.issueAccessToken(principal), refresh, user.getId(), user.getTenantId(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials or inactive account");
    }

    public record Session(String accessToken, String refreshToken, UUID userId, UUID tenantId,
                          String email, String fullName, String role) { }
}
