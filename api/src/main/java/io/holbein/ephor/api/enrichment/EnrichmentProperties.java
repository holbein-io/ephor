package io.holbein.ephor.api.enrichment;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ephor.enrichment")
public class EnrichmentProperties {

    private boolean enabled = true;
    private String kevUrl = "https://www.cisa.gov/sites/default/files/feeds/known_exploited_vulnerabilities.json";
    private String epssUrl = "https://epss.cyentia.com/epss_scores-current.csv.gz";

    // A CVE is "exploitable" at or above this EPSS probability (KEV is always exploitable).
    private double epssThreshold = 0.10;
}
