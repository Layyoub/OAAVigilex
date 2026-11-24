/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.evidence;

import org.springframework.core.io.Resource;
import java.io.InputStream;
import java.io.IOException;
import java.util.UUID;

public interface StorageService {
    String store(UUID tenantId, InputStream content) throws IOException;
    Resource load(String storageKey) throws IOException;
    void delete(String storageKey) throws IOException;
}
