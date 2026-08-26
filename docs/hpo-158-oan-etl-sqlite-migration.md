# HPO-158: oan-etl, Neo4j → SQLite

**Branch:** `feature/hpo-158-sqlite-etl` · **Status:** complete, verified, not yet merged
**Spec:** `docs/schema-design.md` (HPO-157) · **Overview:** `docs/sqlite-migration-overview.md`

## What changed

`oan-etl` no longer talks to Neo4j at all. `GraphCommand` now takes `-o/--output` (a SQLite
file path) instead of an injected `Driver`; `-t/--truncate` deletes any existing file at that
path before writing a fresh one. `HpoOntologyAnnotationLoader`'s nine write methods build
batched SQL rows via a new `SqliteWriter`/`SqliteSchema` instead of Cypher `Query` objects.
`GraphWriter`, `GraphDatabaseWriter`, `GraphOperations`, `GraphDatabaseOperations`, and
`SessionAware` are deleted outright — the ticket's own framing was that there's no seam to
reimplement, since Cypher was inline across the loader, not behind an abstraction.

Schema is the subset of `docs/schema-design.md` in scope for this ticket: all 12 entity/
association tables, **excluding** `phenotype_descendant` (HPO-159) and the `disease_fts`/
`gene_fts` virtual tables (HPO-163). Indexes are created *after* all inserts, not before —
the correct order for SQLite bulk loads, and a deliberate change from the old Neo4j path.

## Why the migration order isn't what the ticket implies

The ticket's method list reads top-to-bottom, but methods couldn't be migrated in that
order. `phenotypes()`, `diseases()`, `genes()` are read by every relationship method's Neo4j
`MATCH` clause (e.g. `diseaseToPhenotype`'s Cypher needs a `Disease` and a `Phenotype` node to
already exist). Migrating an entity method to SQLite-only before its dependents were migrated
meant those dependents' Neo4j-based tests started silently returning zero rows — Cypher
`MATCH` doesn't error on an empty match, it just finds nothing.

Fix: migrate all **relationship** methods first (their new SQL logic reads from in-memory
phenol data structures, not from Neo4j, so they have no such dependency), then the three
**entity** methods last, once nothing on the Neo4j side still needs their output. The final
cutover (deleting the Neo4j classes, dropping the dependency, moving index creation to the
end of `load()`) happened only once every method was already SQL-based.

## Decisions made along the way

- **`owl:Thing` exclusion.** phenol's ontology loader synthesizes a non-HPO "Artificial root
  term" alongside real HP terms. It has no category (`phenotypeToCategory` only categorizes
  HP-prefixed terms), which the old `category TEXT NOT NULL` — sorry, which the new NOT NULL
  column can't silently accept the way Neo4j silently omitted a null property. `phenotypes()`
  filters to HP-prefixed terms only; `phenotypeToPhenotype()` filters consistently so
  `phenotype_child` never references a phenotype that was never inserted.
- **`medicalAction()`'s mondoId resolution** happens in Java now instead of Cypher's
  `MATCH (d:Disease {mondoId}) WHERE d.id contains 'OMIM'`. It resolves a mondoId for *every*
  disease (matching how `diseases()` itself works), then gates only the OMIM-only
  restriction at insertion into the lookup map — this is what makes the ORPHA/DECIPHER
  exclusion a real, tested behavior (an ORPHA disease that *does* resolve a mondoId but is
  still excluded) rather than a coincidental "no match."
- **Assay ids** are normalized to `LOINC:<id>` at ETL time, replacing the old read-time
  `String.format`. An intentional, visible change from the Neo4j build (per the schema doc).
- **HGNC duplicate Entrez Gene IDs.** Discovered by running against real production data:
  phenol's `GeneIdentifiers.geneIdById` builds a strict, no-duplicates map and crashes hard
  on any collision. Hit a live one — two genuinely distinct HGNC records
  (`ALDH1L1-AS1`, stable since 2023; `SLC41A3-AS1`, modified days before this was found)
  currently claim the same NCBI Gene ID, confirmed via NCBI's own record as a stale
  cross-reference on HGNC's side. `deduplicateHgncEntrezIds()` keeps the first row seen per
  `entrez_id`, drops the rest, and logs what was dropped. Unrelated to this migration — the
  old Neo4j loader would crash identically against today's data — but it's the only way to
  actually run the ETL against current real data, so it's included here.

## Verified against real production data

Ran the full ETL against the real ~187MB of input files (not just test fixtures), producing
a 63.26MB SQLite file:

| Table | Rows |
|---|---|
| phenotype | 19,836 |
| disease | 12,909 |
| gene | 44,403 |
| disease_phenotype | 285,270 |
| gene_phenotype | 272,799 |
| disease_gene | 16,095 |
| assay | 3,118 |
| assay_phenotype | 5,704 |
| medical_action | 165 |
| medical_action_target | 406 |
| medical_action_annotation | 424 |
| phenotype_child | 24,378 |

Confirmed idempotent (two runs with `-t` produce identical row counts) and confirmed no
database server involved at any point.

## Explicitly not in scope here

- **HPO-159** (descendant closure table) — superseded. See the overview doc: a recursive
  CTE over `phenotype_child` matches materialized-closure performance with zero extra
  storage, so no closure table is built at all, in `oan-etl` or otherwise.
- **HPO-163** (FTS) — `disease_fts`/`gene_fts` virtual tables are not created.
- **HPO-166** (oan-rest cleanup) — separate branch, see
  `docs/hpo-160-166-oan-rest-sqlite-migration.md`.
