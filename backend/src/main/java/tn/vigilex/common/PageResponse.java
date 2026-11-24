/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.common;

import org.springframework.data.domain.Page;
import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, long total) {
    public static <T> PageResponse<T> from(Page<T> result) {
        return new PageResponse<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }
}
