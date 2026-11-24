/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import tn.vigilex.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHashAndRevokedAtIsNull(String tokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RefreshToken r where r.tokenHash = :tokenHash and r.revokedAt is null")
    Optional<RefreshToken> findActiveForUpdate(@Param("tokenHash") String tokenHash);
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(UUID userId);
    void revokeAllByUserIdAndRevokedAtIsNull(UUID userId);
}
