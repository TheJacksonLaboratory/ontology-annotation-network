# HPO-163 Full-Text Search Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ranked disease and gene search over primary names and synonyms using FTS5. Results are paged in SQL, never return fewer results than 1.1.0, and the API response is unchanged.

**Architecture:** The ETL writes one FTS5 row per searchable name into `disease_fts` / `gene_fts`. Each repository runs one SQL statement that unions the FTS hits, the id matches and the 1.1.0 substring matches, collapses them to one row per entity at its best tier, and pages with `COUNT(*) OVER ()`. `SearchService` maps the resulting `SearchPage` to the unchanged `SearchDto`.

**Tech Stack:** Java 21, Micronaut 4.10, sqlite-jdbc 3.45.1.0 (FTS5 bundled), phenol 2.1.4, JUnit 5, Mockito.

**Spec:** `docs/superpowers/specs/2026-11-18-hpo-163-full-text-search-design.md`. This plan replaces `2026-10-26-hpo-163-full-text-search.md`, which is untracked in the main checkout. Tasks 1–2 carry over from it largely unchanged.

## Global Constraints

- Worktree `~/Develop/oan-hpo-163`, branch `feature/hpo-163-fts5`.
- No change to `SearchDto`, `Disease`, `Gene`, or `SearchController` validation.
- FTS DDL, exactly: `fts5(<entity>_id UNINDEXED, name, kind UNINDEXED, tokenize = 'unicode61 remove_diacritics 2', prefix = '2 3')`.
- `kind` is exactly `primary` or `synonym`. Names per entity are de-duplicated case-insensitively, the primary wins, and blanks are dropped.
- MONDO synonyms: scopes EXACT and RELATED only, plus the MONDO label when it differs from the hpoa label.
- HGNC synonym fields: `name`, `alias_symbol`, `alias_name`, `prev_symbol`, `prev_name`. Read them by header name, split on `|`, and strip surrounding `"`.
- Tiers: 0 id or (disease) `mondo_id` equals the query · 1 primary name equals · 2 synonym equals · 3 primary starts with · 4 other FTS hit by `bm25` · 5 id contains · 6 1.1.0 substring `LIKE` on the primary name. Tiers 1–3 compare the normalised key: lowercase, spaces and hyphens removed.
- Final order: `tier, score (NULLs last), name, id`.
- `limit == -1` → no limit, offset 0. An offset past the end, or `limit == 0`, → empty results with the real total.
- Repositories log SQL failures at **error** and return an empty page.
- Before writing any new class or method, invoke the `code-documentation` skill (user rule).
- Tests: `./mvnw clean test -pl oan-etl -am` and `./mvnw clean test -pl oan-rest -am`.

## Review Focus

1. **FTS operator words in uppercase input** (`LOSS OR GAIN`, `NOT`, `NEAR`). These must be treated as plain words, never as syntax. *(Task 3 `operatorWordsAreQuoted`, Task 4 `operatorWordsDoNotThrow`)*
2. **Totals count entities, not name rows.** A disease with 5 matching synonyms counts once. *(Task 4 `totalCountsEntities`)*
3. **Paging edges.** `limit=-1` with `page>0` must not throw, and an offset past the end returns an empty page with the real total. *(Task 6 `unlimitedIgnoresPage`, Task 4 `offsetPastEndKeepsTotal`)*
4. **A missing FTS table must not read as "no results"** without a loud error log. *(Task 2 `ftsTablesExist`; Task 4 step 4 logs at error)*
5. **Substring-only hits rank below every word match.** `BAD` → Kabadi comes last. *(Task 4 `substringHitsRankLast`)*

---

### Task 1: HGNC alternate-name reader (oan-etl)

Same as October Task 1.

**Files:** Create `oan-etl/src/main/java/org/jax/oan/ontology/HgncNames.java`; Test `oan-etl/src/test/java/org/jax/oan/ontology/HgncNamesTest.java`.

**Interfaces:**
- Produces: `static Map<String, List<String>> HgncNames.read(Path hgncCompleteSet) throws IOException`.
  - Key: `entrez_id` digits.
  - Value: the alternate names in field order, blanks dropped.
  - Rows with an empty `entrez_id` are skipped; for duplicate ids, the first row wins.

- [ ] **Step 1: Write failing tests** `readsAlternateNamesKeyedByEntrezId` and `stripsQuotesAndSplitsOnPipe` against `src/test/resources/hgnc_complete_set.txt`.
  - `keySet == {"10058","4905"}`.
  - NSF (`4905`) list equals the fixture's exact field-order values. Read the fixture row first: it has 3 lines, header plus 2 rows.
  - ABCB6 (`10058`) contains `EST45597`, `umat`, `MTABC3`, and no value contains `"` or `|` or is blank.
- [ ] **Step 2: Run** `./mvnw test -pl oan-etl -am -Dtest=HgncNamesTest`. Expected: compile failure.
- [ ] **Step 3: Implement** `read`. Locate columns from the header row and use `split("\t", -1)`.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(etl): read HGNC alternate gene names for search (HPO-163)`

---

### Task 2: FTS tables and name rows in the ETL

**Files:**
- Modify `SqliteSchema.java` (`TABLES`).
- Modify `HpoOntologyAnnotationLoader.java` (`load`, `diseases`, `genes`).
- Modify `oan-etl/src/test/resources/mondo-base.json`.
- Test `HpoOntologyAnnotationLoaderTest.java`.

**Interfaces:**
- Consumes: `HgncNames.read` (Task 1).
- Produces the DB contract read by Tasks 4–5:
  - `disease_fts(disease_id, name, kind)` and `gene_fts(gene_id, name, kind)`, with the DDL in Global Constraints.
  - `static List<Object[]> nameRows(String id, String primary, Collection<String> synonyms)` in the loader returns `[id, name, kind]` rows with the primary first.
- `genes(HpoAssociationData associations, Map<String, List<String>> hgncNames)`. `load` passes `HgncNames.read(<the de-duplicated HGNC path it already builds>)`, so hoist that path into a local.

- [ ] **Step 1: Add fixture synonyms** to `MONDO_0008854` `meta.synonyms`:
  - `hasExactSynonym "BBS1"`
  - `hasRelatedSynonym "Bardet-Biedl syndrome type 1"`
  - `hasNarrowSynonym "BBS narrow test"`
  - `hasBroadSynonym "BBS broad test"`
  - `hasExactSynonym "fake orphanet DISEASE"`

  Re-run the existing loader test. It must still pass.
- [ ] **Step 2: Write failing tests**:
  - `diseaseFtsRows`: ORPHA:99999 has primary `["Fake Orphanet disease"]` and synonyms `{"Bardet-Biedl Syndrome 1","BBS1","Bardet-Biedl syndrome type 1"}`. The `primary` count is 3.
  - `geneFtsRows`: NCBIGene:4905 has primary `["NSF"]`, and its synonyms include `SKD2`, `SEC18`. There are no case-insensitive duplicates per gene.
  - `ftsMatchFindsSynonym`: `MATCH '"sec18"'` → 1 gene, `MATCH '"bbs1"'` → 1 disease, and `MATCH '"se"*'` runs. This last check proves the `prefix='2 3'` index and FTS5 work under sqlite-jdbc 3.45.1.0.
  - `ftsTablesExist`: both tables are present in `sqlite_master` and non-empty.
- [ ] **Step 3: Run** `./mvnw test -pl oan-etl -am -Dtest=HpoOntologyAnnotationLoaderTest`. Expected: the 4 new tests FAIL.
- [ ] **Step 4: Implement.**
  - In `diseases`, reuse `equivalent`. Synonyms are the MONDO label plus `getSynonyms()` filtered to EXACT/RELATED; verify the scope enum names in phenol 2.1.4.
  - In `genes`, look up the entrez part of `g.id()`.
  - Batch-insert `nameRows` in both.
- [ ] **Step 5: Run** `./mvnw clean test -pl oan-etl -am`. Expected: all PASS, with existing counts unchanged.
- [ ] **Step 6: Commit** `feat(etl): load disease and gene names into FTS5 tables (HPO-163)`

---

### Task 3: Query helpers (oan-rest)

**Files:** Modify `oan-rest/src/main/java/org/jax/oan/repository/SearchQuery.java`; Create `SearchPage.java` (same package); Test `oan-rest/src/test/java/org/jax/oan/repository/SearchQueryTest.java`.

**Interfaces:**
- Produces:
  - `record SearchPage<T>(List<T> results, int total)` (public).
  - In `SearchQuery`, package-private static methods:
    - `List<String> tokens(String query)`: split on `[^\p{L}\p{N}]+`, lowercase, drop empties.
    - `String ftsMatch(List<String> tokens)`: each token double-quoted, `*` after the last, space-joined.
    - `String key(String text)`: the existing private method, made package-private.
  - `likePattern` stays. `ranking` is deleted in Task 5, once neither repository uses it.

- [ ] **Step 1: Write failing tests**
```java
@Test void tokensSplitOnPunctuation() {
	assertEquals(List.of("marfan", "s", "syndrome", "type", "1"), SearchQuery.tokens("MARFAN'S SYNDROME, TYPE-1"));
	assertEquals(List.of("omim", "154700"), SearchQuery.tokens("OMIM:154700"));
}
@Test void tokensOfPunctuationOnlyIsEmpty() { assertEquals(List.of(), SearchQuery.tokens(" - : , ")); }
@Test void ftsMatchPrefixesLastToken() { assertEquals("\"marfan\" \"synd\"*", SearchQuery.ftsMatch(List.of("marfan", "synd"))); }
@Test void operatorWordsAreQuoted() { assertEquals("\"loss\" \"or\" \"gain\"*", SearchQuery.ftsMatch(SearchQuery.tokens("LOSS OR GAIN"))); }
@Test void keyDropsCaseSpacesHyphens() { assertEquals("loeysdietz", SearchQuery.key("Loeys- Dietz")); }
```
- [ ] **Step 2: Run** `./mvnw test -pl oan-rest -am -Dtest=SearchQueryTest`. Expected: compile failure.
- [ ] **Step 3: Implement** the methods and the record.
- [ ] **Step 4: Run** the same command. Expected: PASS.
- [ ] **Step 5: Commit** `feat(search): add FTS5 query helpers and SearchPage (HPO-163)`

---

### Task 4: Ranked, paged disease search

**Files:**
- Modify `DiseaseRepository.java`: add `searchDiseases`. `findDiseases(String)` becomes a one-line delegate, `searchDiseases(q, 0, -1).results()`, so `SearchService` still compiles; Task 6 deletes it.
- Test `DiseaseRepositoryTest.java`.

**Interfaces:**
- Consumes: Task 3 helpers and `SearchPage`; the `disease_fts` contract from Task 2.
- Produces: `public SearchPage<Disease> searchDiseases(String query, int offset, int limit)`. `limit == -1` means unlimited.

- [ ] **Step 1: Extend the fixture.**
  - In `initialize()`, create `disease_fts` with the Global DDL and insert a `primary` row for every existing disease.
  - Add these diseases, with FTS rows:

| id | primary | synonyms | mondo_id |
|---|---|---|---|
| `OMIM:154700` | Marfan syndrome | MFS | `MONDO:0007947` |
| `OMIM:105400` | Amyotrophic lateral sclerosis 1 | ALS | |
| `ORPHA:64` | Alstrom syndrome | — | |
| `OMIM:115150` | Cardiofaciocutaneous syndrome | — | |
| `ORPHA:2322` | Kabadi syndrome | — | |
| `ORPHA:558` | Marfan syndrome type 1 | MFS1, Marfan syndrome classic, Marfan disease, Marfan | `MONDO:0007947` |

  - Change the existing `findDiseases` test to call `searchDiseases(q, 0, -1).results()`.
  - Helper: `ids(q)` = result ids, in order, from `searchDiseases(q, 0, -1)`.
- [ ] **Step 2: Write failing tests.** Inputs are uppercase, as the controller sends them.
```java
@Test void wordsInAnyOrder()        { assertTrue(ids("SYNDROME MARFAN").contains("OMIM:154700")); }
@Test void prefixOnLastWord()       { assertEquals("OMIM:154700", ids("MARFAN SYND").get(0)); }
@Test void exactNameRanksFirst() {
	List<String> r = ids("BAD DISEASE");
	assertEquals(List.of("OMIM:555555", "OMIM:092320"), r.subList(0, 2));
	assertEquals(r.size(), Set.copyOf(r).size());
}
@Test void exactSynonymBeatsPrimaryPrefix() { assertEquals(List.of("OMIM:105400", "ORPHA:64"), ids("ALS").subList(0, 2)); }
@Test void synonymOnlyHit()         { assertEquals("OMIM:154700", ids("MFS").get(0)); }
@Test void exactIdRanksFirst()      { assertEquals("OMIM:154700", ids("OMIM:154700").get(0)); assertTrue(ids("15470").contains("OMIM:154700")); }
@Test void mondoIdFindsMappedDiseases() { assertEquals(Set.of("OMIM:154700", "ORPHA:558"), Set.copyOf(ids("MONDO:0007947").subList(0, 2))); }
@Test void substringHitsRankLast() {
	assertEquals(List.of("OMIM:555555", "OMIM:092320", "ORPHA:2322"), ids("BAD"));
	assertTrue(ids("ARDIO").contains("OMIM:115150"));
}
@Test void punctuationIsTokenized() { assertEquals("OMIM:154700", ids("MARFAN-SYNDROME").get(0)); }
@Test void operatorWordsDoNotThrow() { assertNotNull(repo.searchDiseases("LOSS OR GAIN", 0, 10)); assertNotNull(repo.searchDiseases("NOT NEAR", 0, 10)); }
@Test void noTokensReturnsEmpty()   { assertEquals(new SearchPage<>(List.of(), 0), repo.searchDiseases("-", 0, 10)); }
@Test void totalCountsEntities()    { assertEquals(ids("MARFAN").size(), repo.searchDiseases("MARFAN", 0, 1).total()); }
@Test void pagesAreStableSlices()   { assertEquals(ids("SYNDROME").subList(1, 3), repo.searchDiseases("SYNDROME", 1, 2).results().stream().map(d -> d.getId().getValue()).toList()); }
@Test void offsetPastEndKeepsTotal() {
	SearchPage<Disease> p = repo.searchDiseases("SYNDROME", 1000, 10);
	assertEquals(List.of(), p.results()); assertEquals(ids("SYNDROME").size(), p.total());
}
@Test void limitZeroKeepsTotal()    { assertEquals(ids("SYNDROME").size(), repo.searchDiseases("SYNDROME", 0, 0).total()); }
```
- [ ] **Step 3: Run** `./mvnw test -pl oan-rest -am -Dtest=DiseaseRepositoryTest`. Expected: compile failure, then the new tests FAIL.
- [ ] **Step 4: Implement `searchDiseases`.**
  - Empty `tokens` → `new SearchPage<>(List.of(), 0)` with no SQL.
  - Bind `q` (stripped, lowercase), `key`, `ftsMatch`, `likePattern`, `limit` and `offset` positionally.
  - On `SQLException`, call `log.error` and return an empty page.
  - Drop the Java sort.
  - The SQL shape is the part the tests don't fully pin:

```sql
WITH hits(id, tier, score) AS (
  SELECT disease_id,
         CASE WHEN kind='primary' AND REPLACE(REPLACE(LOWER(name),' ',''),'-','') = :key THEN 1
              WHEN kind='synonym' AND REPLACE(REPLACE(LOWER(name),' ',''),'-','') = :key THEN 2
              WHEN kind='primary' AND REPLACE(REPLACE(LOWER(name),' ',''),'-','') LIKE :key || '%' THEN 3
              ELSE 4 END,
         bm25(disease_fts)
  FROM disease_fts WHERE disease_fts MATCH :match
  UNION ALL SELECT id, 0, NULL FROM disease WHERE LOWER(id) = :q OR LOWER(mondo_id) = :q
  UNION ALL SELECT id, 5, NULL FROM disease WHERE LOWER(id) LIKE '%' || :q || '%'
  UNION ALL SELECT id, 6, NULL FROM disease WHERE LOWER(name) LIKE :like
), ranked AS (
  SELECT id, MIN(tier) AS tier, MIN(score) AS score FROM hits GROUP BY id
)
SELECT d.id, d.name, d.mondo_id, d.description, COUNT(*) OVER () AS total
FROM ranked r JOIN disease d ON d.id = r.id
ORDER BY r.tier, r.score IS NULL, r.score, d.name, d.id
LIMIT :limit OFFSET :offset
```

  - When no rows come back and (`offset > 0` or `limit == 0`), run `<same CTE> SELECT COUNT(*) FROM ranked` for the total.
  - Keep the CTE in one constant so the count query reuses it.
- [ ] **Step 5: Run** `./mvnw test -pl oan-rest -am -Dtest=DiseaseRepositoryTest`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(search): rank and page disease search with FTS5 tiers (HPO-163)`

---

### Task 5: Ranked, paged gene search

**Files:**
- Modify `GeneRepository.java`: add `searchGenes`. `findGenes(String)` becomes a delegate until Task 6.
- Modify `SearchQuery.java`: delete `ranking`.
- Test `GeneRepositoryTest.java`.

**Interfaces:**
- Consumes: the same as Task 4, with `gene_fts`.
- Produces: `public SearchPage<Gene> searchGenes(String query, int offset, int limit)`.
- SQL is Task 4's shape with `gene` / `gene_fts` / `gene_id`, selecting `id, name`. There is no `mondo_id` branch, and the id match is now case-insensitive.

- [ ] **Step 1: Extend the fixture.**
  - Create `gene_fts` with primary rows for every existing gene.
  - Add `NCBIGene:2200` `FBN1` with synonyms `fibrillin 1`, `MFS1`, `FBN`.
  - Add `NCBIGene:2201` `FBN2` with synonym `fibrillin 2`.
  - Move existing tests to `searchGenes(q, 0, -1).results()`.
- [ ] **Step 2: Write failing tests**
```java
@Test void exactSymbolRanksFirst()    { assertEquals("NCBIGene:2200", ids("FBN1").get(0)); }
@Test void fullNameHit()              { assertEquals(Set.of("NCBIGene:2200", "NCBIGene:2201"), Set.copyOf(ids("FIBRILLIN"))); }
@Test void multiWordFullName()        { assertEquals("NCBIGene:2201", ids("FIBRILLIN 2").get(0)); }
@Test void previousSymbolHit()        { assertEquals("NCBIGene:2200", ids("MFS1").get(0)); }
@Test void exactSynonymBeatsPrefix()  { assertEquals(List.of("NCBIGene:2200", "NCBIGene:2201"), ids("FBN").subList(0, 2)); }
@Test void exactIdRanksFirst()        { assertEquals("NCBIGene:2200", ids("NCBIGENE:2200").get(0)); }
@Test void hyphenatedSymbolExactFirst() { assertEquals("NCBIGene:3123", ids("HLA-DRB1").get(0)); }
@Test void limitZeroKeepsTotal()      { assertEquals(ids("HLA").size(), repo.searchGenes("HLA", 0, 0).total()); }
```
- [ ] **Step 3: Run** `./mvnw test -pl oan-rest -am -Dtest=GeneRepositoryTest`. Expected: FAIL.
- [ ] **Step 4: Implement** as in Task 4, and delete `SearchQuery.ranking` along with its test.
- [ ] **Step 5: Run** `./mvnw test -pl oan-rest -am -Dtest='GeneRepositoryTest,SearchQueryTest'`. Expected: PASS.
- [ ] **Step 6: Commit** `feat(search): rank and page gene search with FTS5 tiers (HPO-163)`

---

### Task 6: SearchService paging in SQL

**Files:** Modify `oan-rest/src/main/java/org/jax/oan/service/SearchService.java`; delete the `findDiseases` / `findGenes` delegates; Test `oan-rest/src/test/java/org/jax/oan/service/SearchServiceTest.java`.

**Interfaces:**
- Consumes: `searchDiseases` / `searchGenes` (Tasks 4–5).
- Produces: the unchanged public `searchGene` / `searchDisease(String query, int page, int limit)` → `SearchDto(results, total)`.

- [ ] **Step 1: Write failing tests.** Update the mocks to `searchGenes(q, offset, limit)` / `searchDiseases(...)`.
  - `pageBecomesOffset`: `page=2, limit=10` → the repository is called with `(q, 20, 10)`.
  - `unlimitedIgnoresPage`: `page=3, limit=-1` → the repository is called with `(q, 0, -1)`.
  - `totalComesFromRepository`: the mock returns `SearchPage(List.of(g), 57)` → `SearchDto` total is 57.
- [ ] **Step 2: Run** `./mvnw test -pl oan-rest -am -Dtest=SearchServiceTest`. Expected: FAIL.
- [ ] **Step 3: Implement.** `offset = limit == -1 ? 0 : page * limit`. Map the page to `SearchDto`. Remove the Java `skip/limit`.
- [ ] **Step 4: Run** `./mvnw clean test -pl oan-rest -am`. Expected: all PASS, including `SearchControllerTest`.
- [ ] **Step 5: Commit** `refactor(search): page search results in SQL (HPO-163)`

---

### Task 7: Same-data comparison, load check, docs

**Files:**
- Create `scripts/hpo-163/queries.tsv`.
- Create `scripts/hpo-163/compare_search.py`.
- Create `docs/hpo-163-search-comparison.md` (generated).
- Modify `docs/sqlite-migration-overview.md`, `docs/schema-design.md`, `CHANGELOG.md`.

**Interfaces:**
- `queries.tsv` columns: `entity<TAB>query<TAB>expected_id_or_blank`.
- `python3 -I scripts/hpo-163/compare_search.py record --base <url> --queries <tsv> --out <json>` records, for each query, the ordered ids and `totalCount` from `GET {base}/search/{entity}?q=…&page=0&limit=-1`.
- `python3 -I scripts/hpo-163/compare_search.py diff --old <json> --new <json> --queries <tsv>` writes markdown to stdout.

- [ ] **Step 1: Write about 50 queries.** Include:
  - the 6 search pages from `~/Plan/oan-cutover/page_counts.py`;
  - 8 multi-word in order, 6 reordered, 8 half-typed last word;
  - 6 exact disease names, 6 gene symbols, 3 full gene names;
  - synonyms `BBS1`, `SEC18`;
  - mid-word `ardio`;
  - ids `154700`, `OMIM:154700`, `NCBIGene:2200`, `MONDO:0007947`;
  - `loss or gain`;
  - 1–2 character type-ahead queries (`a`, `ma`, `hl`, `fb`), which are the worst case for both full scans.

  Fill in `expected_id` where the answer is obvious. If Cloud Run `/search/*` request logs are available, ask Michael before drawing queries from them.
- [ ] **Step 2: Write the script.**
  - Per query, the diff shows old and new top-10 ids, old and new totals, the ids that are new, and three flags:
    - `LOST`: an old id is absent from the new results (gate 1).
    - `MISS`: `expected_id` is not in the new top 10.
    - `RANK`: `expected_id` ranks lower than before.
  - `record` also stores the seconds each request took. The diff shows old and new timings and flags any query over 300 ms.
  - It ends with a count for each flag.
- [ ] **Step 3: Build the new DB and both servers.**
  - Build the new DB: `bash update.sh data/`, then the ETL jar with `-d data/ -o oan.db -t`.
  - New server: run the new `oan-rest` jar on port 8080.
  - Old server: build `v1.1.0` `oan-rest` in a throwaway worktree (`git worktree add ../oan-v110 v1.1.0`; the tag is 45cc122 and includes the PR #60 search fixes) and run it against the **same** `oan.db` on port 8081.
- [ ] **Step 4: Record and diff.** Record both servers and diff them. Also record production (`https://ontology.jax.org/api/network`) for reference only; confirm with Michael before the first production call. Before diffing, check the 1.1.0 server against `~/Plan/oan-cutover/prod-sqlite-2026-11-18.tsv` (`loeys dietz` → 7, `x linked` → 346); the totals may move only by data changes. **Pass: zero `LOST`, zero `MISS`, zero `RANK`.** If any flag is non-zero, stop and report. Don't tune the ranking without Michael. Expect some `RANK` flags by design, where an exact synonym (tier 2) now outranks a name prefix (tier 3); report those separately from real regressions.
- [ ] **Step 5: Load check.**
  - Ask Michael to deploy the branch to SQA (`network-build-deploy-sqa.yml` accepts a branch) and run the k6 load test.
  - **Pass:** search p95 ≤ 300 ms.
  - Note the `oan.db` size before and after in the comparison doc.
- [ ] **Step 6: Update docs.**
  - `sqlite-migration-overview.md`: HPO-163 → done on this branch.
  - `schema-design.md`: the FTS DDL gains `kind` and `prefix`, there is one row per name, and the tiers replace the Java sort.
  - `CHANGELOG.md` Unreleased: synonym search, ranked results, SQL paging, and the `limit=-1` paging fix.
- [ ] **Step 7: Commit** `docs(search): record HPO-163 comparison and update migration docs`
