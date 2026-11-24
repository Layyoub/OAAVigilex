/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.ingestion;

import org.springframework.stereotype.Component;
import tn.vigilex.common.ApiException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Component
public class ManualCsvSourceAdapter implements SourceAdapter {
    @Override public String name() { return "manual-csv"; }
    @Override public boolean enabled() { return false; }
    @Override public List<ImportedLawText> fetch() { return List.of(); }

    public List<ImportedLawText> parse(String csv) {
        CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).build();
        try (CSVParser parser = CSVParser.parse(csv, format)) {
            if (!parser.getHeaderMap().keySet().containsAll(List.of("reference", "type", "titleFr")))
                throw ApiException.badRequest("CSV must include reference,type,titleFr columns");
            List<ImportedLawText> records = new ArrayList<>();
            for (CSVRecord record : parser) {
                if (record.size() != parser.getHeaderMap().size())
                    throw ApiException.badRequest("CSV row " + record.getRecordNumber() + " has an invalid number of fields");
                Map<String, String> values = new HashMap<>();
                parser.getHeaderMap().keySet().forEach(header -> values.put(header, record.isMapped(header) ? record.get(header) : ""));
                try {
                    String date = values.getOrDefault("publishedOn", "");
                    records.add(new ImportedLawText(required(values, "reference"), required(values, "type"),
                            required(values, "titleFr"), values.get("titleAr"), values.get("authority"),
                            date.isBlank() ? null : LocalDate.parse(date), values.get("officialGazetteRef"),
                            values.get("status"), values.get("sourceUrl"), values.get("summary"), values.get("fullText")));
                } catch (DateTimeParseException e) {
                    throw ApiException.badRequest("CSV row " + record.getRecordNumber() + " has an invalid publishedOn date");
                }
            }
            if (records.isEmpty() || records.size() > 5000) throw ApiException.badRequest("CSV must contain between 1 and 5000 text rows");
            return records;
        } catch (java.io.IOException | IllegalArgumentException e) {
            if (e instanceof ApiException api) throw api;
            throw ApiException.badRequest("Could not read CSV content");
        }
    }

    private String required(Map<String, String> values, String name) {
        String value = values.get(name);
        if (value == null || value.isBlank()) throw ApiException.badRequest("CSV field " + name + " is required");
        return value.trim();
    }

}
