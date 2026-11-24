/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.ingestion;

import java.util.List;

public interface SourceAdapter {
    String name();
    boolean enabled();
    List<ImportedLawText> fetch();
}
