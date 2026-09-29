# HPO-167: automate the release pipeline

**Branches:** `feature/hpo-167-tag-release-ci` (**merged to `main`**),
`feature/hpo-167-combined-build-deploy` (this branch, not yet merged)
**Status:** stage 1 done and merged; stage 2 done, not merged, and not yet functional (see
below); full automation not started

## The problem

CI triggers off `workflow_dispatch` with no release/tag boundary — whoever fires the deploy
workflow deploys whatever is currently on `main`. Combined with trunk-based development,
that means `main` has to be deployable at every commit, which a large atomic migration like
this can't guarantee mid-flight. There was also no automated trigger at all: the manual ETL
behind an ontology release is why `hpo.jax.org` can be staler than the release artifacts it
should reflect, and why OAN went untouched from 2026-02-11 until now.

## Stage 1 (merged): build from a tag, not from `main`

Tags are still cut **manually** — no CI job creates them. What changed: both
(now-superseded, see stage 2) deploy workflows resolve the latest `v*` tag
(`git describe --tags --abbrev=0 --match 'v[0-9]*'`) and check that ref out before building,
instead of building whatever `main`'s HEAD happens to be. `v1.0.15` is tagged at the current
`main` HEAD as the bootstrap point.

This unblocks merging in-progress feature work (like the two SQLite migration branches)
without risking an in-flight release — a deploy can only ever pick up a deliberately tagged
commit.

## Stage 2 (this branch): one pipeline instead of two

`docs/schema-design.md` calls for collapsing the old two-deployable release path (a Neo4j
data VM plus a separate REST service) into one Cloud Run service. This branch does that at
the CI level:

- Deletes `network-data-build-deploy.yml` (the Neo4j VM disk-swap: throwaway Neo4j
  container, GCS backup rotation, stopping the prod VM, detaching/reattaching the
  `graph-data` disk between it and a loader VM, a `sleep 60`, swapping back) and
  `network-restful-build-deploy.yml`.
- Replaces both with `network-build-deploy.yml`: checkout latest tag → build `oan-etl` →
  fetch data → run the ETL into `oan-rest/oan.db` → build `oan-rest` (with the `.db` baked
  into its Docker image) → push → deploy the single Cloud Run service.
- `oan-rest/Dockerfile` now sets `WORKDIR /app`, copies `oan.db` alongside the jar, and
  passes `-Dsqlite.path=/app/oan.db` to the running process.
- `network-data-reattach-only.yml` (the manual VM recovery workflow) is left untouched —
  its retirement is infrastructure teardown, which HPO-166 explicitly gates on a successful
  release through this pipeline, not code cleanup to do now.

**This workflow is not yet functional.** It calls `oan-etl`'s `-o` flag and expects
`oan-rest` to read `sqlite.path` — neither exists on `main` yet, since
`feature/hpo-158-sqlite-etl` and `feature/hpo-164-sqlite-repo-fixtures` haven't merged. It's
correct, reviewable YAML that goes live once both land — see the merge order in
`docs/sqlite-migration-overview.md`.

## Not started: full automation

Two pieces of the original HPO-167 scope remain, deliberately deferred:

1. **Auto-trigger, not manual dispatch.** The plan is to piggyback on `ontology-service`'s
   existing cron rather than add a second independent poller — it already polls the HP
   ontology release every 4 hours via `ontology-check-trigger.yml`, and since HP and
   `phenotype.hpoa` release together, that one check is sufficient signal for OAN too. This
   needs a small, coordinated change in the separate `ontology-service` repository (adding
   OAN as a downstream consumer of that existing check), not just this one.
2. **Sanity check before deploy**, modelled on `ontology-sanity.yml` — verify the built
   artifact actually works before promoting it, not just that the build succeeded.

Also out of scope here: HPO-165 (deploying both the old and new services side by side to
diff live before cutover) and HPO-168 (whether to also publish the artifact as an external
downloadable release asset) — both under active discussion, not blocked on this branch.
