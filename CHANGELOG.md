# Changelog

## Unreleased
### Added
- Disease and gene search match synonyms: MONDO exact and related synonyms for diseases, and HGNC full names, alias and previous symbols and names for genes. The ETL writes them to new FTS5 tables, `disease_fts` and `gene_fts`.
- Disease search by MONDO id returns the OMIM and Orphanet diseases mapped to it.

### Changed
- Search results are ranked in SQL: exact id, exact name, exact synonym, name prefix, other word matches by relevance, id substring, then name substring. Words match in any order, and the last word matches as a prefix.
- Search results are paged in SQL instead of loading every match per request. The response format is unchanged, and every result 1.1.0 returned is still returned.
- Upgraded to Micronaut 4.10.24 and Java 21 (build, CI and the `eclipse-temurin:21-jre` Docker image).
- Annotation processors and libraries (JUnit, Mockito, Hamcrest, Swagger, picocli, logback, snakeyaml) take their versions from the Micronaut BOM instead of local pins.
- Maven plugins updated (compiler 3.13, surefire 3.5 and the remaining lifecycle plugins).
- The generated OpenAPI spec no longer marks the `page` and `limit` search parameters as required; both have defaults.

### Removed
- Unused `javax.*` and `neo4j-harness` test dependencies.
- REST Assured and the serde annotation processor are no longer bundled into the oan-rest jar.
- Snapshot versioning: `snapshot-version.sh`, the Sonatype snapshots plugin repository and `--update-snapshots` in the PR build.

## 1.1.0 - 2026-11-18
### Changed
- Replaced Neo4j with an embedded SQLite database in both oan-etl and oan-rest. The API is unchanged.
- One combined build-and-deploy pipeline replaces the two separate release paths.
- oan-rest tuned for Cloud Run: connection pool, blocking executor, JVM and instance sizing.
- ETL matches diseases to MONDO through an xref index built once per load, and logs the time taken by each stage.
- GitHub Actions upgraded: actions/checkout v7, actions/setup-java v6, google-github-actions/auth and setup-gcloud v3.
- SQA deploy workflow defaults to `main`.

### Fixed
- SQLite disease and gene search treat spaces and hyphens in names as wildcards, matching the Neo4j search ("loeys dietz" finds Loeys-Dietz syndrome, "x linked" finds X-linked diseases).
- Search ranks exact and prefix matches first, ignoring spaces and hyphens, and returns nothing for a query made only of separators.
- ETL skips malformed MONDO xrefs instead of failing.
- Orphanet gene associations are fetched from orphadata.com (orphadata.org no longer resolves).
- ETL tolerates duplicate NCBI Gene IDs in HGNC's complete set.

### Added
- SQA deployment, k6 load test, and a prod cutover/rollback runbook.
