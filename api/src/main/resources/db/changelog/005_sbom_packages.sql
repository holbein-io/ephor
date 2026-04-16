-- Package index extracted from SBOM documents for cross-fleet search
CREATE TABLE sbom_packages (
    id              UUID PRIMARY KEY,
    sbom_id         UUID NOT NULL REFERENCES sbom_documents(id) ON DELETE CASCADE,
    image_reference TEXT NOT NULL,
    purl            TEXT,
    name            TEXT NOT NULL,
    version         TEXT NOT NULL,
    type            TEXT,
    license         TEXT,
    supplier        TEXT
);

CREATE INDEX idx_sbom_packages_sbom_id ON sbom_packages(sbom_id);
CREATE INDEX idx_sbom_packages_name ON sbom_packages(name);
CREATE INDEX idx_sbom_packages_purl ON sbom_packages(purl);
CREATE INDEX idx_sbom_packages_image ON sbom_packages(image_reference);
CREATE INDEX idx_sbom_packages_license ON sbom_packages(license);
CREATE INDEX idx_sbom_packages_name_version ON sbom_packages(name, version);
