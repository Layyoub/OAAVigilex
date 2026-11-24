/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService auth;
    private final LoginRateLimiter limiter;
    private final Duration refreshTtl;
    private final boolean secureCookie;

    public AuthController(AuthService auth, LoginRateLimiter limiter,
                          @Value("${vigilex.security.refresh-ttl:P30D}") Duration refreshTtl,
                          @Value("${vigilex.security.cookie-secure:false}") boolean secureCookie) {
        this.auth = auth;
        this.limiter = limiter;
        this.refreshTtl = refreshTtl;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String key = servletRequest.getRemoteAddr();
        if (!limiter.allow(key)) throw new tn.vigilex.common.ApiException(HttpStatus.TOO_MANY_REQUESTS, "Too many login attempts");
        AuthService.Session session = auth.login(request.email(), request.password());
        return sessionResponse(session);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "vigilex_refresh", required = false) String token) {
        if (token == null || token.isBlank()) return ResponseEntity.status(401).build();
        return sessionResponse(auth.refresh(token));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = "vigilex_refresh", required = false) String token) {
        auth.logout(token);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, clearCookie().toString()).build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotRequest request) {
        auth.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(Map.of("message", "If the account exists, reset instructions were sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetRequest request) {
        auth.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> sessionResponse(AuthService.Session session) {
        ResponseCookie cookie = ResponseCookie.from("vigilex_refresh", session.refreshToken())
                .httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/v1/auth")
                .maxAge(refreshTtl).build();
        AuthResponse response = new AuthResponse(session.accessToken(), session.userId(), session.tenantId(),
                session.email(), session.fullName(), session.role());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie.toString()).body(response);
    }

    private ResponseCookie clearCookie() {
        return ResponseCookie.from("vigilex_refresh", "").httpOnly(true).secure(secureCookie)
                .sameSite("Strict").path("/api/v1/auth").maxAge(Duration.ZERO).build();
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank @Size(max = 200) String password) { }
    public record ForgotRequest(@Email @NotBlank String email) { }
    public record ResetRequest(@NotBlank String token, @NotBlank @Size(min = 12, max = 200) String password) { }
    public record AuthResponse(String accessToken, java.util.UUID userId, java.util.UUID tenantId,
                               String email, String fullName, String role) { }
}
