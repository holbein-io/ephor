package io.holbein.ephor.api.enrichment;

import io.holbein.ephor.api.repositories.VulnerabilityRepository;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EnrichmentServiceTest {

    private final VulnerabilityRepository repository = mock(VulnerabilityRepository.class);
    private final EnrichmentService service =
            new EnrichmentService(new EnrichmentProperties(), repository, null);

    @Test
    void parsesTrackedRows_andReturnsMatchCount() {
        byte[] feed = gzip("""
                #model_version:v2025,score_date:2026-06-23
                cve,epss,percentile
                CVE-2023-44487,0.97123,0.99988
                CVE-2024-21626,0.17280,0.96700
                """);

        int matched = service.applyEpss(feed, Set.of("CVE-2023-44487", "CVE-2024-21626"));

        assertEquals(2, matched);
        verify(repository).updateEpss("CVE-2023-44487", 0.97123, 0.99988);
        verify(repository).updateEpss("CVE-2024-21626", 0.17280, 0.96700);
    }

    @Test
    void skipsCommentAndHeaderLines() {
        byte[] feed = gzip("""
                #model_version:v2025,score_date:2026-06-23
                cve,epss,percentile
                CVE-2023-44487,0.5,0.9
                """);

        int matched = service.applyEpss(feed, Set.of("CVE-2023-44487"));

        assertEquals(1, matched);
        // The "#..." comment and "cve,epss,percentile" header must never reach updateEpss.
        verify(repository, never()).updateEpss("cve", 0.0, 0.0);
    }

    @Test
    void skipsUntrackedCves() {
        byte[] feed = gzip("""
                cve,epss,percentile
                CVE-9999-0001,0.80000,0.99000
                CVE-2023-44487,0.50000,0.90000
                """);

        int matched = service.applyEpss(feed, Set.of("CVE-2023-44487"));

        assertEquals(1, matched);
        verify(repository).updateEpss("CVE-2023-44487", 0.50000, 0.90000);
        verify(repository, never()).updateEpss("CVE-9999-0001", 0.80000, 0.99000);
    }

    @Test
    void skipsMalformedRow_withoutAbortingTheFeed() {
        // A non-numeric score and a truncated row sit between two good rows; both must be
        // skipped while the surrounding valid rows still get written.
        byte[] feed = gzip("""
                cve,epss,percentile
                CVE-2023-44487,0.50000,0.90000
                CVE-2024-99999,notanumber,0.5
                CVE-2024-88888,0.1
                CVE-2024-21626,0.17280,0.96700
                """);

        int matched = service.applyEpss(feed,
                Set.of("CVE-2023-44487", "CVE-2024-99999", "CVE-2024-88888", "CVE-2024-21626"));

        assertEquals(2, matched);
        verify(repository).updateEpss("CVE-2023-44487", 0.50000, 0.90000);
        verify(repository).updateEpss("CVE-2024-21626", 0.17280, 0.96700);
    }

    @Test
    void throwsOnUnreadableGzip() {
        // refreshEpss() wraps applyEpss in a try/catch -> last-known-good; this is the boundary
        // it relies on: a corrupt payload surfaces as an exception rather than silent success.
        byte[] garbage = "not gzip".getBytes(StandardCharsets.UTF_8);

        assertThrows(IllegalStateException.class,
                () -> service.applyEpss(garbage, Set.of("CVE-2023-44487")));
    }

    private static byte[] gzip(String csv) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(csv.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return out.toByteArray();
    }
}
