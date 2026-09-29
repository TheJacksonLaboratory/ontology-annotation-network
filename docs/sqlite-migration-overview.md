# OAN off Neo4j: overview and status

The Jackson Laboratory · Ontology Annotation Network

## Why

`docs/schema-design.md` (HPO-157) made the call: OAN moves from a live Neo4j graph to a
SQLite file built in CI and baked into the `oan-rest` container image. The deciding
evidence was the ~4-minute data load — even loading straight into memory rather than over
Bolt, that cost cannot become a Cloud Run cold start, since Cloud Run scales to zero. The
release path being replaced is a throwaway Neo4j container, a GCS backup rotation, and a
disk-swap dance between two VMs synchronized by a `sleep 60` — documented in full in that
file. This set of branches carries out that decision.

## Status

| Ticket | What | Branch | Status |
|---|---|---|---|
| HPO-158 | `oan-etl` emits SQLite, Neo4j deleted | `feature/hpo-158-sqlite-etl` | Done, not merged |
| HPO-159 | Precompute phenotype descendant closure | — | **Superseded**, see below |
| HPO-160 | Port `DiseaseRepository` | `feature/hpo-164-sqlite-repo-fixtures` | Done, not merged |
| HPO-161 | Port `GeneRepository` | `feature/hpo-164-sqlite-repo-fixtures` | Done, not merged |
| HPO-162 | Port `PhenotypeRepository` (incl. closure lookup) | `feature/hpo-164-sqlite-repo-fixtures` | Done, not merged |
| HPO-163 | FTS replacing regex name search | — | Not started (plain `LIKE` in place as a placeholder) |
| HPO-164 | Port repository tests to a fixture artifact | `feature/hpo-164-sqlite-repo-fixtures` | Done, not merged |
| HPO-165 | Deploy SQLite build at a 2nd URL, live-diff vs. Neo4j | — | Not started (deployment task, not code) |
| HPO-166 | Remove residual Neo4j (oan-rest code/config half) | `feature/hpo-164-sqlite-repo-fixtures` | Done, not merged |
| HPO-167 | Automate the release pipeline | `feature/hpo-167-tag-release-ci` (merged), `feature/hpo-167-combined-build-deploy` | Stage 1 merged; stage 2 done, not merged; full auto-trigger not started |
| HPO-168 | Publish artifact as a downloadable release asset | — | Flagged for reconsideration, not closed (see Jira) |

Per-branch detail lives in:
- `docs/hpo-158-oan-etl-sqlite-migration.md`
- `docs/hpo-160-166-oan-rest-sqlite-migration.md`
- `docs/hpo-167-release-pipeline.md`

## HPO-159 superseded: recursive CTE, not a materialized table

HPO-159 called for precomputing the full phenotype ancestor/descendant closure into its own
table (`docs/schema-design.md`'s `phenotype_descendant`), so the one real graph traversal
left in the codebase (`HAS_CHILD *0..`) becomes an index seek.

Benchmarked against the real 63MB production database instead: a `WITH RECURSIVE` query over
the `phenotype_child` table already built in HPO-158, with `CROSS JOIN` forcing SQLite's
planner to seek rather than full-scan `disease_phenotype`, averages **0.3ms** across 50
random phenotype terms — the same ballpark a materialized closure would give, for **zero**
extra storage and **zero** extra ETL step. The catch: a naively-ordered join is ~700x
slower (SQLite's planner defaults to scanning the 285k-row `disease_phenotype` table
instead of seeking per descendant), so the query has to be written carefully. `oan-rest`'s
`PhenotypeRepository` (HPO-162) implements it this way; see that ticket's Jira comment for
the full benchmark.

Consequence: no `phenotype_descendant` table exists anywhere, and the database's projected
size (relevant to HPO-168, below) is smaller than originally estimated.

## Merge order (real dependency, not preference)

`feature/hpo-164-sqlite-repo-fixtures` was branched off `main`, not off
`feature/hpo-158-sqlite-etl` — but it has a real *runtime* dependency on it: `oan-rest`
reads a SQLite file that only the migrated `oan-etl` can produce. **HPO-158 has to merge to
`main` before HPO-160/161/162/164/166 do.** Merging the `oan-rest` work first would leave
`main` with an `oan-etl` that still only writes to Neo4j and an `oan-rest` expecting a file
nothing produces.

`feature/hpo-167-combined-build-deploy` depends on both — its pipeline calls `oan-etl -o`
(HPO-158) and expects `oan-rest` to read `sqlite.path` (HPO-164/166). It's written and
reviewable now but inert until both land.

Order: **HPO-158 → HPO-160/161/162/164/166 → HPO-167 (combined pipeline)**.

## What's genuinely still open

- **HPO-163** — real FTS. Both repositories currently use a plain SQL `LIKE` in place of
  Neo4j's regex name matching, which is a documented, accepted difference for HPO-165's
  diff, not a final state.
- **HPO-165** — stand up the SQLite build at a second URL, diff it live against the
  currently-deployed Neo4j service, using the `last-neo4j-capable` tag as the rebuild point
  if that service ever needs reproducing. This is a deployment task once the above merges
  land, not something coded in a branch.
- **HPO-167 full automation** — `feature/hpo-167-combined-build-deploy` replaces the
  build/deploy shape, but the trigger is still manual (`workflow_dispatch`). Full automation
  means hooking into `ontology-service`'s existing cron (which already polls the HP ontology
  release on a schedule) rather than adding a second independent poller — since HP and
  `phenotype.hpoa` release together, that one check is sufficient signal for OAN too. This
  needs a small, coordinated change in the separate `ontology-service` repository, not just
  this one.
- **HPO-168** — descope recommendation from the schema doc is now under active reconsideration
  given the real (measured, not estimated) artifact size and its growth trajectory. See the
  Jira comment thread on that ticket.
