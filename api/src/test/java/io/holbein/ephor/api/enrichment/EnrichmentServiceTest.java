package io.holbein.ephor.api.enrichment;

import io.holbein.ephor.api.enrichment.EnrichmentService.EpssScore;
import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class EnrichmentServiceTest {

    private final VulnerabilityRepository repository = mock(VulnerabilityRepository.class);
    private final EnrichmentService service =
            new EnrichmentService(new EnrichmentProperties(), repository, null, null);

    @Test
    void parsesTrackedRows_andReturnsMatchCount() {
        InputStream feed = gzip("""
                #model_version:v2025,score_date:2026-06-23
                cve,epss,percentile
                CVE-2023-44487,0.97123,0.99988
                CVE-2024-21626,0.17280,0.96700
                """);

        Map<String, EpssScore> scores = service.parseEpss(feed, Set.of("CVE-2023-44487", "CVE-2024-21626"));

        assertEquals(Map.of(
                "CVE-2023-44487", new EpssScore(0.97123, 0.99988),
                "CVE-2024-21626", new EpssScore(0.17280, 0.96700)), scores);
    }

    @Test
    void skipsCommentAndHeaderLines() {
        InputStream feed = gzip("""
                #model_version:v2025,score_date:2026-06-23
                cve,epss,percentile
                CVE-2023-44487,0.5,0.9
                """);

        Map<String, EpssScore> scores = service.parseEpss(feed, Set.of("CVE-2023-44487"));

        // The "#..." comment and "cve,epss,percentile" header must never be parsed as scores.
        assertEquals(Map.of("CVE-2023-44487", new EpssScore(0.5, 0.9)), scores);
    }

    @Test
    void skipsUntrackedCves() {
        InputStream feed = gzip("""
                cve,epss,percentile
                CVE-9999-0001,0.80000,0.99000
                CVE-2023-44487,0.50000,0.90000
                """);

        Map<String, EpssScore> scores = service.parseEpss(feed, Set.of("CVE-2023-44487"));

        assertEquals(Map.of("CVE-2023-44487", new EpssScore(0.50000, 0.90000)), scores);
    }

    @Test
    void skipsMalformedRow_withoutAbortingTheFeed() {
        // A non-numeric score and a truncated row sit between two good rows; both must be
        // skipped while the surrounding valid rows still get scored.
        InputStream feed = gzip("""
                cve,epss,percentile
                CVE-2023-44487,0.50000,0.90000
                CVE-2024-99999,notanumber,0.5
                CVE-2024-88888,0.1
                CVE-2024-21626,0.17280,0.96700
                """);

        Map<String, EpssScore> scores = service.parseEpss(feed,
                Set.of("CVE-2023-44487", "CVE-2024-99999", "CVE-2024-88888", "CVE-2024-21626"));

        assertEquals(Map.of(
                "CVE-2023-44487", new EpssScore(0.50000, 0.90000),
                "CVE-2024-21626", new EpssScore(0.17280, 0.96700)), scores);
    }

    @Test
    void throwsOnUnreadableGzip() {
        // refreshEpss() wraps parseEpss in a try/catch -> last-known-good; this is the boundary
        // it relies on: a corrupt payload surfaces as an exception rather than silent success.
        InputStream garbage = new ByteArrayInputStream("not gzip".getBytes(StandardCharsets.UTF_8));

        assertThrows(IllegalStateException.class,
                () -> service.parseEpss(garbage, Set.of("CVE-2023-44487")));
    }

    private static InputStream gzip(String csv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(csv.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}
