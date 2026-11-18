# Changelog

## 1.1.0 - 2026-11-18
### Changed
- Replaced Neo4j with an embedded SQLite database in both oan-etl and oan-rest. The API is unchanged.
- One combined build-and-deploy pipeline replaces the two separate release paths.
- oan-rest tuned for Cloud Run: connection pool, blocking executor, JVM and instance sizing.

### Fixed
- SQLite disease and gene search treat spaces and hyphens as wildcards, matching the Neo4j search ("loeys dietz" finds Loeys-Dietz syndrome, "x linked" finds X-linked diseases).
- Orphanet gene associations are fetched from orphadata.com (orphadata.org no longer resolves).
- ETL tolerates duplicate NCBI Gene IDs in HGNC's complete set.

### Added
- SQA deployment, k6 load test, and a prod cutover/rollback runbook.
