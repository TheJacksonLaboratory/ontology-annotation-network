# HPO-163: Full-text search for diseases and genes — design

**Status:** draft for approval · **Branch:** `feature/hpo-163-fts5` (worktree `~/Develop/oan-hpo-163`, cut from `chore/59-micronaut-java21`) · **Jira:** HPO-163 (parent HPO-154)

This is the spec the 2026-10-26 plan (`docs/superpowers/plans/2026-10-26-hpo-163-full-text-search.md`, untracked in the main checkout) pointed to but never had. It supersedes parts of that plan — see [Superseded](#superseded-from-the-october-plan).

## Intent

1.1.0 replaced Neo4j's regex name search with a placeholder: `LOWER(name) LIKE '%a%b%' OR LOWER(id) LIKE '%q%'`, a full scan, sorted in Java (exact → starts-with → rest), with `SearchService` loading every match and paging in memory. Speed is already within budget (k6 SQA 2026-11-18: search p95 140 ms vs. 300 ms threshold). **Michael's goal: search that is more accurate and supports synonyms.** Accuracy here means the right entity near the top. Substring matches stay, ranked last: the type-ahead only shows the first few results, and the full result list keeps everything 1.1.0 found.

## Goals

1. Disease and gene search use FTS5 tables (`disease_fts`, `gene_fts`) built by the ETL.
2. Search covers primary names **and** synonyms (MONDO EXACT/RELATED; HGNC aliases and previous symbols/names).
3. Ranking happens in SQL, in tiers, with `bm25` inside the free-text tier.
4. Paging and `totalCount` happen in SQL; the API response (`SearchDto`) is unchanged.
5. A query set with a 1.1.0 baseline proves results are at least as good (Jira AC).

## Non-goals

- Typo tolerance / fuzzy matching / "did you mean".
- Trigram or substring indexing.
- Phenotype search, or any other endpoint.
- Changes to `SearchDto`, `Disease`, `Gene`, `SearchController` input validation, or the UI.
- Exposing "matched via synonym X" in the response (the SQL may select it; nothing maps it).

## Anti-goals (fails even if it "works")

- A query that returned results in 1.1.0 returns nothing.
- An exact name, symbol, or id is not the first result.
- Search p95 on the existing k6 SQA run exceeds 300 ms.
- A database missing the FTS tables silently returns empty search results in production.

## Constraints

- Java 21, Micronaut 4.10 (inherited from `chore/59`). sqlite-jdbc **3.45.1.0** in both `oan-etl` and `oan-rest` — FTS5, `prefix=` and window functions must be verified against that driver, not the local `sqlite3` CLI (3.51).
- Controller passes `q.toUpperCase().trim()`; allowed characters `[a-zA-Z0-9\s\-':,]`, max 255. Every comparison is case-insensitive.
- Repository tests in `oan-rest` hand-roll their DDL per test class (no shared fixture DB); FTS DDL must be added there.
- Data size today: ~12.9k diseases, ~44.4k genes.
- Before writing any new class or method, invoke the `code-documentation` skill (user rule).

## Approaches considered

| | Approach | Trade-off | |
|---|---|---|---|
| A | **FTS5 `unicode61`, names + synonyms, tiered SQL ranking, 1.1.0 `LIKE` matches appended as the last tier** | Word/prefix matching with real ranking and synonym recall; nothing 1.1.0 found is lost; substring-only hits rank last | **Chosen** |
| B | FTS5 over primary names only | Smallest change, but no synonym recall — FTS barely earns its keep | Rejected |
| C | FTS5 `trigram` tokenizer | Keeps substring matching, but ~2× index, weak ranking, needs ≥3 chars | Rejected |

Also chosen: paging moves into SQL (over keeping Java paging).

## Design

### Tables (ETL, `SqliteSchema.TABLES`)

```sql
CREATE VIRTUAL TABLE disease_fts USING fts5(
  disease_id UNINDEXED, name, kind UNINDEXED,
  tokenize = 'unicode61 remove_diacritics 2', prefix = '2 3');
CREATE VIRTUAL TABLE gene_fts USING fts5(
  gene_id UNINDEXED, name, kind UNINDEXED,
  tokenize = 'unicode61 remove_diacritics 2', prefix = '2 3');
```

- One row per searchable name. `kind` ∈ {`primary`, `synonym`}.
- Per entity, names are de-duplicated case-insensitively; the primary wins; blanks dropped.
- **Disease** primary = hpoa label. Synonyms = the matched MONDO term's label (when it differs) + its EXACT and RELATED synonyms (reuse the `mondoEquivalents` map already computed in `load`).
- **Gene** primary = symbol (`gene.name`). Synonyms = HGNC `name`, `alias_symbol`, `alias_name`, `prev_symbol`, `prev_name` — columns located by header, `|`-split, surrounding `"` stripped — keyed by `entrez_id`. Read from the de-duplicated HGNC file `load` already produces.
- The October plan's Tasks 1–2 (`HgncNames.read`, `nameRows`, fixture synonyms in `mondo-base.json`) carry over as written.

### Query building (`oan-rest`, package-private helper)

- Tokens: split on `[^\p{L}\p{N}]+`, lowercase, drop empties. This matches `unicode61`'s separators for every allowed character (`-`, `'`, `:`, `,`, space).
- No tokens → empty page, total 0, no SQL run.
- FTS `MATCH` string: every token double-quoted, `*` appended to the last, space-joined (implicit AND). Quoting neutralises FTS operators, which arrive uppercase from the controller (`LOSS OR GAIN`, `NOT`, `NEAR`).
- Normalised key for exact/starts-with tiers: lowercase, spaces and hyphens removed (same rule as 1.1.0's `SearchQuery.key`).

### Ranking (one SQL statement per entity)

| Tier | Match |
|---|---|
| 0 | id equals query (case-insensitive); for diseases, `mondo_id` equals query too (`MONDO:0007947` → its OMIM/ORPHA diseases) |
| 1 | primary name equals query (normalised key) |
| 2 | a synonym equals query (normalised key) |
| 3 | primary name starts with query (normalised key) |
| 4 | other FTS hit, ordered by `bm25` |
| 5 | id contains query (keeps "154700" → `OMIM:154700`) |
| 6 | 1.1.0 substring match on primary name only (`LIKE '%a%b%'`), ordered by name |

Each entity appears once, at its best tier (collapse with `GROUP BY id` / `MIN(tier)` before counting). Final `ORDER BY tier, score, name, id` — `id` last so pages are stable.

### Substring tier (always on)

Tier 6 is always part of the query (Michael's call, 2026-11-19). Every entity 1.1.0 returned is still returned, so `totalCount` never falls below 1.1.0. Substring-only hits always rank below every FTS, id and synonym hit (`BAD` → "Kabadi syndrome" still appears, but at the bottom). The cost is the same full scan 1.1.0 runs today, which is within budget. The id-contains check is case-insensitive for genes too (1.1.0's gene check was case-sensitive and could never match the uppercased query).

### Paging (supersedes "SearchService untouched")

- Repositories expose `SearchPage<T> search(String query, int offset, int limit)`; `SearchPage` is a small record `(List<T> results, int total)` in `oan-rest`. `SearchService` maps it to `SearchDto`.
- SQL: ranked CTE, then `SELECT …, COUNT(*) OVER () AS total … LIMIT ? OFFSET ?`.
- **`limit == -1`** → no limit (SQLite `LIMIT -1`) and offset 0, ignoring `page`. (Today `page>0, limit=-1` throws on a negative `skip`; this fixes it.)
- **Offset past the end** → window returns no rows; run a `COUNT` of the ranked set so the response is an empty page with the real total (today's behaviour).
- `findDiseases(String)` / `findGenes(String)` are removed or delegate to `search(query, 0, -1)` — builder's call based on remaining callers.

### Failure visibility

- Repositories keep returning an empty result on `SQLException` but log at **error**, not warn.
- ETL test asserts both FTS tables exist and are populated; `oan-rest` repository tests create the FTS tables and exercise search through them, so a missing table fails tests rather than reading as "no results".

## Verification

### Query set and baseline

- `scripts/hpo-163/queries.tsv`: `entity  query  expected_id_or_blank`, ~50 rows. Source: real queries from Cloud Run request logs for `/search/*` if obtainable; otherwise curated. Must include: the six search pages in `Plan/oan-cutover/page_counts.py`; multi-word in order; reordered words; half-typed last word; exact disease names; gene symbols; full gene names; synonyms (e.g. `BBS1`, `SEC18`); mid-word (`ardio`); id fragments (`154700`, `OMIM:154700`, `NCBIGene:2200`); FTS-operator words (`loss or gain`).
- `scripts/hpo-163/compare.py` records **ordered ids** and `totalCount` for each query from a base URL (`limit=-1`), and diffs two recordings.
- **Baseline = 1.1.0 code on the same data** (Claude's call). Run `v1.1.0` `oan-rest` against the new build's `oan.db`. It still reads `disease`/`gene` and ignores the FTS tables, so the diff shows only what the code changed, not data churn between releases. Production (`https://ontology.jax.org/api/network`) is recorded too, for reference only. Not Neo4j, and not the local August `oan.db`.

### Gate (all must hold)

1. For every query, the result set contains every entity 1.1.0 returned (same-data baseline).
2. Every `expected_id` is in the top 10 and ranks no lower than in 1.1.0.
3. The diff report lists, per query, entities that are new (synonym hits) and the top-10 before and after, for Michael to read. This is informational and does not block.
4. k6 SQA search p95 ≤ 300 ms on the new build.
5. `./mvnw clean test -pl oan-etl -am` and `./mvnw clean test -pl oan-rest -am` pass.

### Required tests (beyond the October plan's)

Punctuation-only input → empty; uppercase input; `MARFAN-SYNDROME`, `OMIM:154700`, `LOSS OR GAIN` don't throw; exact id/name/synonym rank first; no duplicate entities; `totalCount` counts entities, not name rows; `limit=-1` with `page>0`; offset past end returns empty page with real total; substring-only hits (`ardio`, `BAD` → Kabadi) are returned below every FTS hit and page correctly; result set ⊇ 1.1.0 for the fixture queries.

## Docs to update

`sqlite-migration-overview.md` (HPO-163 → done on this branch), `schema-design.md` (FTS DDL with `kind`, `prefix`; SQL tiers replace Java sort), `CHANGELOG.md`.

## Superseded from the October plan

- "No change to `SearchService`" → it now maps `SearchPage` to `SearchDto`.
- "`findDiseases`/`findGenes` return the full ranked list" → paging in SQL.
- "Today = production Neo4j (`last-neo4j-capable`)" → today = 1.1.0 SQLite.
- Tokenizer gains `prefix = '2 3'`; ranking gains tier 5 (id contains). The October always-on `LIKE` branch stays, as tier 6, and so does its `BAD` → Kabadi test, which now also asserts Kabadi ranks below "Bad disease".

## Left to the builder

SQL shape (CTE vs. `UNION ALL` branches), helper class names, whether `findDiseases`/`findGenes` survive, how `compare.py` relates to `page_counts.py`, task split and order.
