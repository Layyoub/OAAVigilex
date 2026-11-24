/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.evidence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileSystemStorageService implements StorageService {
    private final Path root;
    public FileSystemStorageService(@Value("${vigilex.storage.directory:./data/evidence}") String directory) {
        this.root = Path.of(directory).toAbsolutePath().normalize();
    }

    @Override
    public String store(UUID tenantId, InputStream content) throws IOException {
        Path tenantFolder = root.resolve(tenantId.toString()).normalize();
        if (!tenantFolder.startsWith(root)) throw new IOException("Invalid tenant storage path");
        Files.createDirectories(tenantFolder);
        String key = tenantId + "/" + UUID.randomUUID();
        Path target = resolve(key);
        Path staging = Files.createTempFile(tenantFolder, "upload-", ".part");
        try {
            Files.copy(content, staging, StandardCopyOption.REPLACE_EXISTING);
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
            return key;
        } finally {
            Files.deleteIfExists(staging);
        }
    }

    @Override
    public Resource load(String storageKey) throws IOException {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) throw new IOException("Evidence file not found");
        return new FileSystemResource(path);
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    private Path resolve(String key) throws IOException {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IOException("Invalid storage key");
        return path;
    }
}
