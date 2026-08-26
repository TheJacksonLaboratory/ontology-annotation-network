# HPO-160/161/162/164/166: oan-rest, Neo4j → SQLite

**Branch:** `feature/hpo-164-sqlite-repo-fixtures` (based on `main`, not on
`feature/hpo-158-sqlite-etl` — see the merge-order note in
`docs/sqlite-migration-overview.md`) · **Status:** complete, verified, not yet merged

## What changed

All three repositories (`DiseaseRepository`, `GeneRepository`, `PhenotypeRepository`) are
rewritten from Cypher/`Driver` to plain JDBC SQL against a new `SqliteConnectionProvider`
(a Micronaut singleton wrapping a `Connection` to a configured `sqlite.path`). Public method
signatures are unchanged — services needed no changes, matching each ticket's acceptance
criteria. `DriverSessionChecker` (the Neo4j connectivity startup check) is deleted,
`micronaut-neo4j-bolt`/`neo4j-harness` are dropped from `oan-rest/pom.xml`, and `neo4j.*`
config is replaced with `sqlite.path` across all three environments (`dev` → `./oan.db`,
`test` → per-test isolated files, `prod` → `/app/oan.db`).

Root `pom.xml` is untouched — on this branch, `oan-etl` still depends on `neo4j-harness`
(this branch doesn't have HPO-158's changes), so removing it from dependency management
here would break `oan-etl`'s build. That happens once both branches converge on `main`.

## HPO-162: recursive CTE instead of a materialized closure

`findDiseasesByTerm`/`findGenesByTerm` needed to replace Neo4j's `HAS_CHILD *0..` traversal.
Rather than building the `phenotype_descendant` closure table HPO-159 called for, both use a
`WITH RECURSIVE` query over `phenotype_child` (already present from HPO-158), with
`CROSS JOIN` forcing the join order:

```sql
WITH RECURSIVE descendants(id) AS (
    SELECT ? UNION SELECT pc.child_id FROM phenotype_child pc JOIN descendants d ON pc.parent_id = d.id
)
SELECT DISTINCT d.id, d.name, d.mondo_id
FROM descendants desc
CROSS JOIN disease_phenotype dp ON dp.phenotype_id = desc.id
JOIN disease d ON d.id = dp.disease_id
```

Without `CROSS JOIN`, SQLite's planner defaults to a full scan of `disease_phenotype`
(285k rows) instead of seeking per descendant — about 700x slower. With it: ~0.3ms averaged
over 50 real phenotype terms, full detail and benchmark numbers on HPO-159's Jira comment.
No closure table exists anywhere as a result.

`findAssaysByTerm` no longer re-prefixes assay ids with `LOINC:` at read time — they're
normalized at ETL time now (HPO-158), an intentional, documented difference.

## Bugs found and fixed while porting (not preserved)

Two pre-existing issues surfaced during the port that were fixed rather than faithfully
reproduced, since they represented dead/never-actually-run code rather than deliberate
behavior:

- **`PhenotypeRepositoryTest`'s assay fixture never worked.** The original Cypher created
  an `Assay` node with id `'03923'` but matched against `'LOINC:03923'` when wiring the
  `MEASURES` relationship — they never matched, so the relationship was never created. The
  test's assertion direction (`expected.containsAll(assays)`, not the reverse) passed
  vacuously even on an empty result. Fixed the fixture and the assertion direction; also
  added a real two-level phenotype hierarchy (disease/gene attached only to a child term)
  so the recursive-CTE closure logic is actually exercised, and added the first-ever test
  for `findMedicalActionsByTerm` (previously completely untested).
- **`oan-rest`'s test suite never declared `mockito-junit-jupiter`.** It was only reachable
  as an accidental transitive dependency of `neo4j-harness`; removing Neo4j broke
  compilation for 7 test files. Added the dependency explicitly (version already managed in
  the root `pom.xml`).

Everything else preserves the exact original behavior, including one harmless-but-odd
pre-existing quirk: the original `DiseaseRepositoryTest` fixture set a disease's `mondoId` to
`'MONDO:000001'` (5 digits) while asserting against `'MONDO:0000001'` (7 digits) — a typo
that was never caught because `Disease.equals()` only compares id and name, inherited from
`BaseOntologyClass`. Ported as-is; not a business rule, just not this migration's problem
to fix.

## Verification

Full `oan-rest` suite: 46/46 passing, zero `org.neo4j` references anywhere in `oan-rest/src`
or `oan-rest/pom.xml`. Module test time dropped from ~30s to ~10s with the embedded Neo4j
harness gone. Manually verified live against the real 63MB production `.db` file built on
`feature/hpo-158-sqlite-etl` — API responses match.

## Explicitly not in scope here

- **HPO-163** (FTS) — `findDiseases`/`findGenes` use a plain SQL `LIKE` for now, not proper
  full-text search. Documented as an accepted, temporary difference.
- **HPO-165** (parallel deploy + live diff) — a deployment task, not code.
- **HPO-166's infrastructure half** (decommissioning the `graph-data` disk, the loader VM,
  the GCS bucket) — explicitly gated in the ticket on a successful release through HPO-167.
  Only the code/config half (this branch) is done.
