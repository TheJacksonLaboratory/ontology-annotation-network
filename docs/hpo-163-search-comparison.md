# HPO-163 search comparison

Old: `v1.1.0` (`45cc122`) oan-rest. New: `feature/hpo-163-fts5` oan-rest. Both servers ran locally against the **same** `oan.db`, built 2026-11-19 by this branch's ETL from fresh `update.sh` data. The 1.1.0 server ignores the FTS tables. Each query requests `page=0&limit=-1`, and the timings are local, measured after one warm-up pass.

Reproduce:

```
python3 -I scripts/hpo-163/compare_search.py record --base <url>/api/network --queries scripts/hpo-163/queries.tsv --out <file>.json
python3 -I scripts/hpo-163/compare_search.py diff --old old.json --new new.json --queries scripts/hpo-163/queries.tsv
```

## Notes

- **Baseline check:** the 1.1.0 server returned `loeys dietz` → 7 and `x linked` → 346, the same totals as production on 2026-11-18 (`Plan/oan-cutover/prod-sqlite-2026-11-18.tsv`).
- **Database size:** 61.5 MB without the FTS tables and 95.9 MB with them, an increase of 34.5 MB. The tables hold 12,833 disease and 44,403 gene primary names, plus 54,743 disease and 152,862 gene synonyms.
- **Speed, `limit=10` locally:** `a` takes 0.08 s on both servers and `ma` 0.02 s on both. With `limit=-1`, the slowest query was gene `a` (19,264 results) at 0.14 s.
- **Ranking:** `cystic fibrosis transmembrane` ranks CFTR 4th. Three pseudogene and antisense genes with shorter matching synonyms score higher on `bm25`. The result still passes the gate (top 10).
- **Not yet run:** the SQA k6 load check, and recording production for reference.

## Summary

LOST 0, MISS 0, RANK 0, SLOW 0

## Per query

| entity | query | old total | new total | old s | new s | expected (old→new rank) | flags | new ids (not in old, first 5) |
|---|---|---|---|---|---|---|---|---|
| gene | `FBN1` | 2 | 2 | 0.017 | 0.023 | NCBIGene:2200 (1→1) |  |  |
| disease | `marfan` | 9 | 14 | 0.008 | 0.01 |  |  | OMIM:182212, ORPHA:2462, OMIM:309520, ORPHA:776, OMIM:617602 |
| disease | `epilepsy` | 162 | 232 | 0.01 | 0.013 |  |  | ORPHA:1934, OMIM:614558, OMIM:300607, OMIM:309530, ORPHA:3006 |
| disease | `loeys dietz` | 7 | 8 | 0.008 | 0.01 |  |  | ORPHA:284984 |
| disease | `x linked` | 346 | 635 | 0.011 | 0.019 |  |  | OMIM:313900, ORPHA:293707, OMIM:300200, OMIM:300100, ORPHA:2239 |
| gene | `hla drb1` | 1 | 2 | 0.01 | 0.017 | NCBIGene:3123 (1→1) |  | NCBIGene:3132 |
| disease | `marfan syndrome` | 7 | 7 | 0.007 | 0.012 | OMIM:154700 (2→1) |  |  |
| disease | `cystic fibrosis` | 4 | 4 | 0.007 | 0.011 | OMIM:219700 (1→1) |  |  |
| disease | `tuberous sclerosis` | 4 | 4 | 0.007 | 0.01 |  |  |  |
| disease | `noonan syndrome` | 22 | 25 | 0.007 | 0.011 |  |  | OMIM:613091, ORPHA:93271, OMIM:613707 |
| disease | `ehlers danlos syndrome` | 42 | 49 | 0.008 | 0.012 |  |  | OMIM:616471, OMIM:618343, ORPHA:90354, ORPHA:2295, OMIM:304150 |
| disease | `spinal muscular atrophy` | 36 | 45 | 0.008 | 0.01 |  |  | OMIM:602440, ORPHA:276435, ORPHA:65684, OMIM:600175, ORPHA:139536 |
| disease | `duchenne muscular dystrophy` | 2 | 3 | 0.007 | 0.01 |  |  | ORPHA:206546 |
| disease | `developmental and epileptic encephalopathy` | 68 | 136 | 0.008 | 0.012 |  |  | ORPHA:324422, ORPHA:505652, OMIM:615369, ORPHA:411986, OMIM:613721 |
| disease | `syndrome marfan` | 0 | 11 | 0.008 | 0.01 | OMIM:154700 (None→1) |  | OMIM:154700, ORPHA:558, OMIM:616914, ORPHA:284979, OMIM:182212 |
| disease | `dietz loeys` | 0 | 8 | 0.008 | 0.01 |  |  | ORPHA:60030, ORPHA:284984, OMIM:609192, OMIM:610168, OMIM:613795 |
| disease | `fibrosis cystic` | 0 | 4 | 0.007 | 0.01 | OMIM:219700 (None→1) |  | OMIM:219700, ORPHA:586, OMIM:603855, ORPHA:2575 |
| disease | `dystrophy duchenne` | 1 | 3 | 0.007 | 0.01 |  |  | OMIM:310200, ORPHA:98896 |
| disease | `syndrome noonan` | 0 | 25 | 0.007 | 0.01 |  |  | ORPHA:648, OMIM:613091, OMIM:601321, ORPHA:638, OMIM:163950 |
| disease | `linked x intellectual` | 0 | 195 | 0.007 | 0.013 |  |  | ORPHA:485350, OMIM:309530, OMIM:300923, OMIM:300928, OMIM:300982 |
| disease | `marfan synd` | 7 | 7 | 0.007 | 0.011 | OMIM:154700 (2→1) |  |  |
| disease | `cystic fib` | 4 | 4 | 0.007 | 0.01 | OMIM:219700 (1→1) |  |  |
| disease | `tuberous scl` | 4 | 4 | 0.007 | 0.01 |  |  |  |
| disease | `noonan syn` | 22 | 25 | 0.008 | 0.01 |  |  | OMIM:613091, ORPHA:93271, OMIM:613707 |
| disease | `ehlers dan` | 42 | 50 | 0.008 | 0.011 |  |  | OMIM:616471, OMIM:300049, OMIM:618343, ORPHA:90354, ORPHA:2295 |
| disease | `spinal muscular atr` | 36 | 45 | 0.008 | 0.011 |  |  | OMIM:602440, ORPHA:276435, ORPHA:65684, OMIM:600175, ORPHA:139536 |
| disease | `bardet bie` | 23 | 23 | 0.007 | 0.009 |  |  |  |
| disease | `prader wil` | 9 | 14 | 0.007 | 0.009 |  |  | OMIM:619326, ORPHA:3409, OMIM:615547, ORPHA:398069, ORPHA:171829 |
| disease | `Prader-Willi syndrome` | 9 | 14 | 0.007 | 0.011 |  |  | OMIM:619326, ORPHA:3409, OMIM:615547, ORPHA:398069, ORPHA:171829 |
| disease | `Angelman syndrome` | 6 | 9 | 0.007 | 0.011 |  |  | ORPHA:228402, ORPHA:85278, OMIM:300243 |
| disease | `Rett syndrome` | 6 | 7 | 0.007 | 0.011 |  |  | ORPHA:561854 |
| disease | `Williams syndrome` | 4 | 10 | 0.006 | 0.01 |  |  | OMIM:277590, ORPHA:3447, OMIM:139600, ORPHA:2220, ORPHA:2835 |
| disease | `Fragile X syndrome` | 4 | 4 | 0.007 | 0.011 |  |  |  |
| disease | `Huntington disease` | 10 | 12 | 0.007 | 0.01 |  |  | OMIM:607136, ORPHA:98759 |
| gene | `TP53` | 23 | 66 | 0.01 | 0.016 | NCBIGene:7157 (1→1) |  | NCBIGene:338440, NCBIGene:100129046, NCBIGene:9538, NCBIGene:8507, NCBIGene:339476 |
| gene | `BRCA1` | 3 | 19 | 0.01 | 0.016 | NCBIGene:672 (1→1) |  | NCBIGene:8315, NCBIGene:25920, NCBIGene:8314, NCBIGene:83990, NCBIGene:580 |
| gene | `CFTR` | 5 | 18 | 0.009 | 0.016 | NCBIGene:1080 (1→1) |  | NCBIGene:5174, NCBIGene:4363, NCBIGene:89845, NCBIGene:85320, NCBIGene:94160 |
| gene | `HLA-A` | 12 | 27 | 0.009 | 0.02 | NCBIGene:3105 (1→1) |  | NCBIGene:7920, NCBIGene:7917, NCBIGene:7919, NCBIGene:10919, NCBIGene:7918 |
| gene | `MECP2` | 1 | 1 | 0.01 | 0.016 | NCBIGene:4204 (1→1) |  |  |
| gene | `SCN1A` | 2 | 2 | 0.01 | 0.016 | NCBIGene:6323 (1→1) |  |  |
| gene | `fibrillin 1` | 0 | 1 | 0.009 | 0.02 | NCBIGene:2200 (None→1) |  | NCBIGene:2200 |
| gene | `tumor protein p53` | 0 | 14 | 0.01 | 0.017 | NCBIGene:7157 (None→1) |  | NCBIGene:7157, NCBIGene:8626, NCBIGene:338440, NCBIGene:9536, NCBIGene:7158 |
| gene | `cystic fibrosis transmembrane` | 0 | 4 | 0.01 | 0.016 | NCBIGene:1080 (None→4) |  | NCBIGene:140871, NCBIGene:107080633, NCBIGene:106481718, NCBIGene:1080 |
| disease | `BBS1` | 0 | 11 | 0.006 | 0.01 |  |  | OMIM:209900, OMIM:615987, OMIM:615988, OMIM:615989, OMIM:615990 |
| gene | `SEC18` | 0 | 1 | 0.011 | 0.016 | NCBIGene:4905 (None→1) |  | NCBIGene:4905 |
| gene | `SKD2` | 0 | 2 | 0.01 | 0.016 | NCBIGene:4905 (None→1) |  | NCBIGene:4905, NCBIGene:27183 |
| disease | `ardio` | 159 | 159 | 0.01 | 0.011 |  |  |  |
| disease | `trophy` | 574 | 574 | 0.012 | 0.014 |  |  |  |
| disease | `154700` | 1 | 1 | 0.007 | 0.01 | OMIM:154700 (1→1) |  |  |
| disease | `OMIM:154700` | 1 | 1 | 0.007 | 0.01 | OMIM:154700 (1→1) |  |  |
| gene | `NCBIGene:2200` | 12 | 12 | 0.011 | 0.017 | NCBIGene:2200 (1→1) |  |  |
| disease | `MONDO:0007947` | 0 | 2 | 0.008 | 0.01 |  |  | OMIM:154700, ORPHA:558 |
| disease | `loss or gain` | 0 | 0 | 0.008 | 0.01 |  |  |  |
| disease | `not near` | 0 | 0 | 0.008 | 0.01 |  |  |  |
| disease | `a` | 12051 | 12253 | 0.112 | 0.161 |  |  | OMIM:300855, OMIM:616005, OMIM:300310, OMIM:137600, OMIM:101600 |
| disease | `ma` | 3503 | 3910 | 0.037 | 0.046 |  |  | ORPHA:950, OMIM:257350, OMIM:153550, ORPHA:1942, ORPHA:99802 |
| gene | `hl` | 187 | 322 | 0.011 | 0.018 |  |  | NCBIGene:3155, NCBIGene:3990, NCBIGene:646023, NCBIGene:109729172, NCBIGene:10878 |
| gene | `fb` | 260 | 300 | 0.011 | 0.017 |  |  | NCBIGene:1175, NCBIGene:26053, NCBIGene:80127, NCBIGene:8945, NCBIGene:899 |
| gene | `a` | 14631 | 19264 | 0.037 | 0.142 |  |  | NCBIGene:171022, NCBIGene:967, NCBIGene:729970, NCBIGene:23549, NCBIGene:100507466 |

## Top 10 before and after

- **gene `FBN1`** — old top 10: NCBIGene:2200, NCBIGene:105370809; new top 10: NCBIGene:2200, NCBIGene:105370809
- **disease `marfan`** — old top 10: OMIM:616914, ORPHA:558, OMIM:154700, OMIM:609008, ORPHA:2463, ORPHA:171719, OMIM:614100, ORPHA:2172, ORPHA:284979; new top 10: OMIM:154700, ORPHA:558, OMIM:616914, OMIM:609008, ORPHA:2463, ORPHA:284979, OMIM:182212, ORPHA:2462, OMIM:614100, ORPHA:171719
- **disease `epilepsy`** — old top 10: OMIM:603204, OMIM:616540, OMIM:615127, OMIM:616461, OMIM:616436, OMIM:615005, OMIM:604364, OMIM:618876, OMIM:616230, OMIM:618832; new top 10: OMIM:132300, ORPHA:1951, OMIM:254800, OMIM:612899, OMIM:607628, ORPHA:1942, OMIM:132090, OMIM:608096, OMIM:600669, OMIM:607682
- **disease `loeys dietz`** — old top 10: OMIM:615582, OMIM:614816, OMIM:613795, ORPHA:60030, OMIM:609192, OMIM:610168, OMIM:619656; new top 10: ORPHA:60030, OMIM:609192, OMIM:610168, OMIM:613795, OMIM:614816, OMIM:615582, OMIM:619656, ORPHA:284984
- **disease `x linked`** — old top 10: ORPHA:47, ORPHA:43, ORPHA:54, ORPHA:435938, ORPHA:314978, ORPHA:300373, ORPHA:93952, ORPHA:93950, ORPHA:93945, ORPHA:93947; new top 10: ORPHA:47, ORPHA:596, ORPHA:53351, ORPHA:101075, ORPHA:300373, ORPHA:95702, ORPHA:43, ORPHA:89936, ORPHA:792, ORPHA:88917
- **gene `hla drb1`** — old top 10: NCBIGene:3123; new top 10: NCBIGene:3123, NCBIGene:3132
- **disease `marfan syndrome`** — old top 10: ORPHA:558, OMIM:154700, ORPHA:171719, OMIM:616914, ORPHA:2463, ORPHA:2172, ORPHA:284979; new top 10: OMIM:154700, ORPHA:558, OMIM:616914, ORPHA:284979, ORPHA:171719, ORPHA:2463, ORPHA:2172
- **disease `cystic fibrosis`** — old top 10: OMIM:219700, ORPHA:586, OMIM:603855, ORPHA:2575; new top 10: OMIM:219700, ORPHA:586, OMIM:603855, ORPHA:2575
- **disease `tuberous sclerosis`** — old top 10: OMIM:613254, ORPHA:805, OMIM:191100, OMIM:600273; new top 10: ORPHA:805, OMIM:191100, OMIM:613254, OMIM:600273
- **disease `noonan syndrome`** — old top 10: ORPHA:648, OMIM:616564, OMIM:616559, OMIM:617506, OMIM:615355, ORPHA:2701, OMIM:613224, ORPHA:500, OMIM:613706, OMIM:613563; new top 10: ORPHA:648, OMIM:163950, OMIM:616564, OMIM:618499, OMIM:618624, OMIM:619087, OMIM:619745, OMIM:605275, OMIM:609942, OMIM:610733
- **disease `ehlers danlos syndrome`** — old top 10: OMIM:617821, OMIM:615539, OMIM:615349, OMIM:614557, OMIM:130070, OMIM:130080, OMIM:130050, OMIM:130060, OMIM:130000, OMIM:130010; new top 10: OMIM:225410, OMIM:225400, OMIM:612350, OMIM:130020, OMIM:225310, OMIM:130050, OMIM:130060, OMIM:617821, OMIM:225320, OMIM:130010
- **disease `spinal muscular atrophy`** — old top 10: OMIM:615290, OMIM:183020, OMIM:605726, OMIM:604320, OMIM:615048, OMIM:616867, OMIM:616866, OMIM:158600, OMIM:159950, OMIM:253400; new top 10: OMIM:300489, OMIM:604320, OMIM:271220, OMIM:183020, OMIM:253300, OMIM:253550, OMIM:253400, OMIM:271150, OMIM:182970, OMIM:615048
- **disease `duchenne muscular dystrophy`** — old top 10: OMIM:310200, ORPHA:98896; new top 10: OMIM:310200, ORPHA:98896, ORPHA:206546
- **disease `developmental and epileptic encephalopathy`** — old top 10: OMIM:617830, OMIM:617771, OMIM:616457, OMIM:617711, OMIM:616409, OMIM:615006, OMIM:616211, OMIM:615473, OMIM:615476, OMIM:301044; new top 10: OMIM:308350, OMIM:619777, OMIM:619814, OMIM:619881, OMIM:619913, OMIM:619970, OMIM:620028, OMIM:620033, OMIM:620115, OMIM:620145
- **disease `syndrome marfan`** — old top 10: —; new top 10: OMIM:154700, ORPHA:558, OMIM:616914, ORPHA:284979, OMIM:182212, ORPHA:2462, OMIM:614100, ORPHA:171719, ORPHA:2463, ORPHA:2172
- **disease `dietz loeys`** — old top 10: —; new top 10: ORPHA:60030, ORPHA:284984, OMIM:609192, OMIM:610168, OMIM:613795, OMIM:614816, OMIM:615582, OMIM:619656
- **disease `fibrosis cystic`** — old top 10: —; new top 10: OMIM:219700, ORPHA:586, OMIM:603855, ORPHA:2575
- **disease `dystrophy duchenne`** — old top 10: ORPHA:206546; new top 10: OMIM:310200, ORPHA:98896, ORPHA:206546
- **disease `syndrome noonan`** — old top 10: —; new top 10: ORPHA:648, OMIM:613091, OMIM:601321, ORPHA:638, OMIM:163950, OMIM:616564, OMIM:618499, OMIM:618624, OMIM:619087, OMIM:619745
- **disease `linked x intellectual`** — old top 10: —; new top 10: ORPHA:485350, OMIM:309530, OMIM:300923, OMIM:300928, OMIM:300982, OMIM:300983, OMIM:300984, OMIM:300997, OMIM:301013, OMIM:300062
- **disease `marfan synd`** — old top 10: ORPHA:558, OMIM:154700, ORPHA:171719, OMIM:616914, ORPHA:2463, ORPHA:2172, ORPHA:284979; new top 10: OMIM:154700, ORPHA:558, OMIM:616914, ORPHA:284979, ORPHA:171719, ORPHA:2463, ORPHA:2172
- **disease `cystic fib`** — old top 10: OMIM:219700, OMIM:603855, ORPHA:2575, ORPHA:586; new top 10: OMIM:219700, ORPHA:586, OMIM:603855, ORPHA:2575
- **disease `tuberous scl`** — old top 10: OMIM:613254, ORPHA:805, OMIM:191100, OMIM:600273; new top 10: ORPHA:805, OMIM:191100, OMIM:613254, OMIM:600273
- **disease `noonan syn`** — old top 10: OMIM:616564, OMIM:616559, OMIM:617506, OMIM:615355, ORPHA:2701, OMIM:613224, ORPHA:648, ORPHA:500, OMIM:613706, OMIM:613563; new top 10: ORPHA:648, OMIM:163950, OMIM:616564, OMIM:618499, OMIM:618624, OMIM:619087, OMIM:619745, OMIM:605275, OMIM:609942, OMIM:610733
- **disease `ehlers dan`** — old top 10: OMIM:617821, OMIM:615539, OMIM:615349, OMIM:614557, OMIM:130070, OMIM:130080, OMIM:130050, OMIM:130060, OMIM:130000, OMIM:130010; new top 10: OMIM:225410, OMIM:225400, OMIM:612350, OMIM:130020, OMIM:225310, OMIM:130050, OMIM:130060, OMIM:617821, OMIM:225320, OMIM:130010
- **disease `spinal muscular atr`** — old top 10: OMIM:615290, OMIM:183020, OMIM:605726, OMIM:604320, OMIM:615048, OMIM:616867, OMIM:616866, OMIM:158600, OMIM:159950, OMIM:253400; new top 10: OMIM:300489, OMIM:604320, OMIM:271220, OMIM:183020, OMIM:253300, OMIM:253550, OMIM:253400, OMIM:271150, OMIM:182970, OMIM:615048
- **disease `bardet bie`** — old top 10: OMIM:600151, OMIM:615981, OMIM:615984, OMIM:615985, OMIM:615982, OMIM:615983, OMIM:615988, OMIM:615989, OMIM:615986, OMIM:615987; new top 10: ORPHA:110, OMIM:615981, OMIM:209900, OMIM:615987, OMIM:615988, OMIM:615989, OMIM:615990, OMIM:615991, OMIM:615992, OMIM:615993
- **disease `prader wil`** — old top 10: ORPHA:739, ORPHA:177904, ORPHA:177901, ORPHA:177910, ORPHA:177907, ORPHA:98793, ORPHA:98754, OMIM:176270, ORPHA:398079; new top 10: OMIM:176270, ORPHA:739, ORPHA:177901, ORPHA:177904, ORPHA:177907, ORPHA:177910, ORPHA:98793, ORPHA:98754, OMIM:619326, ORPHA:398079
- **disease `Prader-Willi syndrome`** — old top 10: ORPHA:739, OMIM:176270, ORPHA:177904, ORPHA:177901, ORPHA:177910, ORPHA:177907, ORPHA:98793, ORPHA:98754, ORPHA:398079; new top 10: OMIM:176270, ORPHA:739, ORPHA:177901, ORPHA:177904, ORPHA:177907, ORPHA:177910, ORPHA:98793, ORPHA:98754, OMIM:619326, ORPHA:398079
- **disease `Angelman syndrome`** — old top 10: ORPHA:72, OMIM:105830, ORPHA:411511, ORPHA:411515, ORPHA:98794, ORPHA:98795; new top 10: OMIM:105830, ORPHA:72, ORPHA:411511, ORPHA:98794, ORPHA:411515, ORPHA:98795, ORPHA:228402, ORPHA:85278, OMIM:300243
- **disease `Rett syndrome`** — old top 10: ORPHA:778, OMIM:312750, OMIM:613454, OMIM:615960, ORPHA:3095, OMIM:137580; new top 10: OMIM:312750, ORPHA:778, OMIM:613454, ORPHA:3095, ORPHA:561854, OMIM:137580, OMIM:615960
- **disease `Williams syndrome`** — old top 10: ORPHA:904, OMIM:194050, OMIM:609757, ORPHA:3448; new top 10: ORPHA:904, OMIM:194050, OMIM:277590, ORPHA:3447, ORPHA:3448, OMIM:139600, ORPHA:2220, ORPHA:2835, ORPHA:50809, OMIM:609757
- **disease `Fragile X syndrome`** — old top 10: OMIM:300624, ORPHA:908, ORPHA:93256, OMIM:300623; new top 10: OMIM:300624, ORPHA:908, OMIM:300623, ORPHA:93256
- **disease `Huntington disease`** — old top 10: ORPHA:399, OMIM:143100, OMIM:603218, ORPHA:157946, ORPHA:157941, OMIM:604802, ORPHA:401901, ORPHA:98934, OMIM:606438, ORPHA:248111; new top 10: OMIM:143100, ORPHA:399, OMIM:603218, ORPHA:157941, OMIM:606438, ORPHA:98934, OMIM:604802, ORPHA:157946, ORPHA:401901, ORPHA:248111
- **gene `TP53`** — old top 10: NCBIGene:7157, NCBIGene:63970, NCBIGene:7158, NCBIGene:7159, NCBIGene:94299, NCBIGene:102800311, NCBIGene:9540, NCBIGene:9537, NCBIGene:90313, NCBIGene:94241; new top 10: NCBIGene:7157, NCBIGene:63970, NCBIGene:7158, NCBIGene:7159, NCBIGene:94299, NCBIGene:102800311, NCBIGene:9537, NCBIGene:90313, NCBIGene:9540, NCBIGene:94241
- **gene `BRCA1`** — old top 10: NCBIGene:672, NCBIGene:101929767, NCBIGene:394269; new top 10: NCBIGene:672, NCBIGene:394269, NCBIGene:101929767, NCBIGene:8315, NCBIGene:25920, NCBIGene:8314, NCBIGene:83990, NCBIGene:580, NCBIGene:221927, NCBIGene:4077
- **gene `CFTR`** — old top 10: NCBIGene:1080, NCBIGene:111082987, NCBIGene:140871, NCBIGene:107080633, NCBIGene:106481718; new top 10: NCBIGene:1080, NCBIGene:140871, NCBIGene:107080633, NCBIGene:106481718, NCBIGene:111082987, NCBIGene:5174, NCBIGene:4363, NCBIGene:89845, NCBIGene:85320, NCBIGene:94160
- **gene `HLA-A`** — old top 10: NCBIGene:3105, NCBIGene:3108, NCBIGene:3111, NCBIGene:3113, NCBIGene:646702, NCBIGene:267013, NCBIGene:3117, NCBIGene:124901301, NCBIGene:3118, NCBIGene:106480429; new top 10: NCBIGene:3105, NCBIGene:3111, NCBIGene:124901301, NCBIGene:106480429, NCBIGene:285830, NCBIGene:7920, NCBIGene:7917, NCBIGene:7919, NCBIGene:10919, NCBIGene:7918
- **gene `MECP2`** — old top 10: NCBIGene:4204; new top 10: NCBIGene:4204
- **gene `SCN1A`** — old top 10: NCBIGene:6323, NCBIGene:101929680; new top 10: NCBIGene:6323, NCBIGene:101929680
- **gene `fibrillin 1`** — old top 10: —; new top 10: NCBIGene:2200
- **gene `tumor protein p53`** — old top 10: —; new top 10: NCBIGene:7157, NCBIGene:8626, NCBIGene:338440, NCBIGene:9536, NCBIGene:7158, NCBIGene:7159, NCBIGene:9537, NCBIGene:90313, NCBIGene:9540, NCBIGene:102800311
- **gene `cystic fibrosis transmembrane`** — old top 10: —; new top 10: NCBIGene:140871, NCBIGene:107080633, NCBIGene:106481718, NCBIGene:1080
- **disease `BBS1`** — old top 10: —; new top 10: OMIM:209900, OMIM:615987, OMIM:615988, OMIM:615989, OMIM:615990, OMIM:615991, OMIM:615992, OMIM:615993, OMIM:615994, OMIM:615995
- **gene `SEC18`** — old top 10: —; new top 10: NCBIGene:4905
- **gene `SKD2`** — old top 10: —; new top 10: NCBIGene:4905, NCBIGene:27183
- **disease `ardio`** — old top 10: OMIM:615235, OMIM:615248, OMIM:615278, OMIM:615279, OMIM:615280, OMIM:302045, ORPHA:217622, OMIM:208750, OMIM:616500, OMIM:615184; new top 10: ORPHA:2008, OMIM:170390, ORPHA:1101, OMIM:620519, OMIM:208750, OMIM:621367, OMIM:616117, OMIM:619142, OMIM:619143, OMIM:212100
- **disease `trophy`** — old top 10: OMIM:617899, OMIM:615238, OMIM:615249, OMIM:221800, OMIM:615287, OMIM:615290, OMIM:122000, OMIM:136600, ORPHA:62, ORPHA:70; new top 10: ORPHA:289494, ORPHA:79085, ORPHA:79086, ORPHA:79087, OMIM:300100, ORPHA:977, ORPHA:99027, ORPHA:99000, ORPHA:280333, ORPHA:62
- **disease `154700`** — old top 10: OMIM:154700; new top 10: OMIM:154700
- **disease `OMIM:154700`** — old top 10: OMIM:154700; new top 10: OMIM:154700
- **gene `NCBIGene:2200`** — old top 10: NCBIGene:2200, NCBIGene:220032, NCBIGene:220074, NCBIGene:220064, NCBIGene:220004, NCBIGene:220070, NCBIGene:220082, NCBIGene:220047, NCBIGene:220001, NCBIGene:220002; new top 10: NCBIGene:2200, NCBIGene:220082, NCBIGene:220047, NCBIGene:220002, NCBIGene:220042, NCBIGene:220081, NCBIGene:220032, NCBIGene:220074, NCBIGene:220064, NCBIGene:220004
- **disease `MONDO:0007947`** — old top 10: —; new top 10: OMIM:154700, ORPHA:558
- **disease `loss or gain`** — old top 10: —; new top 10: —
- **disease `not near`** — old top 10: —; new top 10: —
- **disease `a`** — old top 10: OMIM:617892, OMIM:615234, OMIM:161600, ORPHA:79408, OMIM:616580, OMIM:616589, OMIM:109050, OMIM:615297, OMIM:208850, ORPHA:23; new top 10: ORPHA:85445, OMIM:200900, OMIM:204750, OMIM:100070, ORPHA:439232, ORPHA:324723, ORPHA:100006, ORPHA:324708, ORPHA:324713, ORPHA:324703
- **disease `ma`** — old top 10: ORPHA:79457, OMIM:616511, ORPHA:399808, ORPHA:399805, OMIM:136550, OMIM:615135, OMIM:125850, OMIM:125851, OMIM:243060, ORPHA:457485; new top 10: OMIM:602562, ORPHA:79457, ORPHA:52417, OMIM:618286, ORPHA:158061, OMIM:602499, OMIM:155100, OMIM:613112, OMIM:619840, OMIM:125480
- **gene `hl`** — old top 10: NCBIGene:3105, NCBIGene:3106, NCBIGene:3107, NCBIGene:3108, NCBIGene:3109, NCBIGene:3111, NCBIGene:3112, NCBIGene:3113, NCBIGene:646702, NCBIGene:267013; new top 10: NCBIGene:3155, NCBIGene:3990, NCBIGene:3138, NCBIGene:3139, NCBIGene:3141, NCBIGene:3131, NCBIGene:100873813, NCBIGene:100507389, NCBIGene:6596, NCBIGene:3142
- **gene `fb`** — old top 10: NCBIGene:85302, NCBIGene:84893, NCBIGene:2091, NCBIGene:54751, NCBIGene:392536, NCBIGene:401679, NCBIGene:345630, NCBIGene:2192, NCBIGene:2199, NCBIGene:10516; new top 10: NCBIGene:85302, NCBIGene:84893, NCBIGene:2091, NCBIGene:54751, NCBIGene:392536, NCBIGene:401679, NCBIGene:345630, NCBIGene:2192, NCBIGene:2199, NCBIGene:10516
- **gene `a`** — old top 10: NCBIGene:1, NCBIGene:503538, NCBIGene:29974, NCBIGene:2, NCBIGene:144571, NCBIGene:144568, NCBIGene:100874108, NCBIGene:106478979, NCBIGene:127550, NCBIGene:53947; new top 10: NCBIGene:166378, NCBIGene:79029, NCBIGene:503538, NCBIGene:144571, NCBIGene:100874108, NCBIGene:106478979, NCBIGene:101928142, NCBIGene:51166, NCBIGene:104355297, NCBIGene:100873982
