# Changelog

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
