/*
 * © 2026 Ayoub — Tous droits réservés
 */
package tn.vigilex.ingestion;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tn.vigilex.common.ApiException;
import java.net.URI;
import java.time.LocalDate;
import java.util.*;

@Component
public class WebSourceAdapter implements SourceAdapter {
    private final boolean configuredEnabled;
    private final String sourceUrl;
    private final Set<String> allowedHosts;

    public WebSourceAdapter(@Value("${vigilex.ingestion.web.enabled:false}") boolean enabled,
                            @Value("${vigilex.ingestion.web.url:}") String sourceUrl,
                            @Value("${vigilex.ingestion.web.allowed-hosts:}") String allowedHosts) {
        this.configuredEnabled = enabled;
        this.sourceUrl = sourceUrl;
        this.allowedHosts = Arrays.stream(allowedHosts.split(",")).map(String::trim)
                .filter(host -> !host.isBlank()).map(String::toLowerCase).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    @Override public String name() { return "configured-authorized-web-source"; }
    @Override public boolean enabled() { return configuredEnabled && validAllowedUrl(); }

    @Override
    public List<ImportedLawText> fetch() {
        if (!enabled()) return List.of();
        try {
            Document document = Jsoup.connect(sourceUrl).userAgent("Vigilex/1.0 (authorized regulatory source ingestion)")
                    .timeout(5000).maxBodySize(5 * 1024 * 1024).get();
            List<ImportedLawText> results = new ArrayList<>();
            for (Element article : document.select("article[data-vigilex-reference]")) {
                String reference = article.attr("data-vigilex-reference").trim();
                String title = article.attr("data-vigilex-title-fr").trim();
                if (reference.isBlank() || title.isBlank()) continue;
                String date = article.attr("data-vigilex-published-on");
                results.add(new ImportedLawText(reference, article.attr("data-vigilex-type").isBlank()
                        ? "LAW" : article.attr("data-vigilex-type"), title,
                        article.attr("data-vigilex-title-ar"), article.attr("data-vigilex-authority"),
                        date.isBlank() ? null : LocalDate.parse(date),
                        article.attr("data-vigilex-gazette-ref"), "IN_FORCE",
                        article.absUrl("data-vigilex-source-url"), article.selectFirst("[data-vigilex-summary]") == null
                        ? null : article.selectFirst("[data-vigilex-summary]").text(),
                        article.selectFirst("[data-vigilex-full-text]") == null
                        ? null : article.selectFirst("[data-vigilex-full-text]").text()));
            }
            return results;
        } catch (Exception e) {
            throw new ApiException(org.springframework.http.HttpStatus.BAD_GATEWAY, "Authorized source ingestion failed");
        }
    }

    private boolean validAllowedUrl() {
        try {
            URI uri = URI.create(sourceUrl);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && allowedHosts.contains(uri.getHost().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
