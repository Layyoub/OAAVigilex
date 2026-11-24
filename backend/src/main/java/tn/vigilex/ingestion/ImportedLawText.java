/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.ingestion;

import java.time.LocalDate;

public record ImportedLawText(String reference, String type, String titleFr, String titleAr,
                              String authority, LocalDate publishedOn, String officialGazetteRef,
                              String status, String sourceUrl, String summary, String fullText) { }
