# Prod cutover to SQLite and rollback to Neo4j

How to move `ontology-annotation-network-rest` from the Neo4j build to the SQLite
build, and how to send traffic back to Neo4j if something goes wrong.

## Current prod (the rollback target)

| Item | Value |
|---|---|
| Project / region | `jax-robinson-hpo-01` / `us-east1` |
| Cloud Run service | `ontology-annotation-network-rest` |
| Neo4j revision | `ontology-annotation-network-rest-00059-qs4`, 100% of traffic |
| Neo4j revision image | `us-docker.pkg.dev/jax-cs-registry/docker/ontology/ontology-annotation-network-rest@sha256:8bd9d27df7118e350e356cbc51e1edc19b264152610c700193f1f8ed9567aba5` (tags `1.0.15`, `latest`) |
| Neo4j revision settings | 2 vCPU, 4Gi, CPU always allocated, min 1, max 5, concurrency 30 |
| Neo4j VM | `ontology-annotation-network-containerized` (`us-east1-b`, `10.142.0.7:7687`), disk `graph-data` |
| VPC egress | `jax-robinson-hpo-01-vpc` / `jax-robinson-hpo-01-subnet`, tag `rest-bolt-connector`, `private-ranges-only` |
| Load balancer | NEG `jax-robinson-ontology-annotation-neg` -> service (not a revision), Cloud Armor `default-ontology-service-policy` |
| Last Neo4j source | tag `v1.0.16` (`33eb0d8`); `last-neo4j-capable` points at `v1.0.15` |

Rollback works because:

- A Cloud Run revision keeps its image, CPU, memory, scaling, and VPC settings, so
  sending traffic back to `00059-qs4` restores the Neo4j setup exactly.
- The image is pinned by digest. The registry cleanup policy runs in dry-run mode,
  so the image is not deleted.
- The deploy command does not set the VPC flags, so the SQLite revision inherits
  them, and the Neo4j revision keeps them either way.
- The load balancer targets the service, so a traffic change needs no LB change.

## Before the cutover

1. Merge `feature/cloud-run-hardening` to `main` and create a release tag (for
   example `v1.1.0`). The prod workflow deploys the latest `v*` tag; `v1.0.16`
   still contains Neo4j.
2. Confirm the Neo4j path is healthy, so there is something to roll back to:
   ```sh
   gcloud compute instances describe ontology-annotation-network-containerized \
     --zone us-east1-b --project jax-robinson-hpo-01 --format='value(status)'   # RUNNING
   gcloud run revisions describe ontology-annotation-network-rest-00059-qs4 \
     --region us-east1 --project jax-robinson-hpo-01 --format='value(status.conditions[0].status)'  # True
   ```
   Prod logs show Neo4j connection timeouts at instance startup
   (`ServiceUnavailableException: Unable to connect to 10.142.0.7:7687`, 04:07 and
   08:09 UTC on 2026-11-17). Check that the VM is up before relying on it.
3. Do not run the "Only Swap Disks & Copy Data" workflow until Neo4j is
   decommissioned. It stops the Neo4j VM.
4. Write down the current revision name again on the day, in case another deploy
   happened:
   ```sh
   gcloud run services describe ontology-annotation-network-rest --region us-east1 \
     --project jax-robinson-hpo-01 --format='value(status.traffic)'
   ```

## Cutover

1. Run the `network-build-deploy` workflow (workflow_dispatch). It deploys the new
   tag and sends 100% of traffic to the new revision.
2. Confirm the new revision uses SQLite: its startup log has
   `HikariPool - sqlite - Start completed` and no `org.neo4j` lines.
   ```sh
   gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="ontology-annotation-network-rest" AND textPayload:"sqlite"' \
     --project jax-robinson-hpo-01 --freshness=30m --limit=5 --format='value(timestamp,resource.labels.revision_name,textPayload)'
   ```
3. Run the k6 smoke profile against prod (2 rps for 1 minute, below the Cloud Armor
   limit):
   ```sh
   k6 run -e BASE_URL=https://ontology.jax.org/api/network load-test/oan-load.js
   ```
4. Watch the signals below for the first hour, then daily for two weeks.

## When to roll back

Roll back if any of these hold:

| Signal | Threshold |
|---|---|
| 5xx responses | over 1% for 5 minutes |
| Latency, search and annotation | p95 over 1 s for 10 minutes |
| `Repository query failed` warnings | repeated (each one is a 200 with empty data) |
| Memory utilization | over 80%, or rising steadily over days |
| Instances | crash loops or failed startups |
| Users | reports of missing or empty results |

`annotation/HP:0000118` taking 7-9 s is known (issue #58) and is not a rollback
trigger on its own.

Checks:

```sh
# 5xx and other errors on the service
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="ontology-annotation-network-rest" AND httpRequest.status>=500' \
  --project jax-robinson-hpo-01 --freshness=1h --limit=50 --format='table(timestamp,httpRequest.status,httpRequest.requestUrl)'
# Swallowed query errors
gcloud logging read 'resource.type="cloud_run_revision" AND resource.labels.service_name="ontology-annotation-network-rest" AND textPayload:"Repository query failed"' \
  --project jax-robinson-hpo-01 --freshness=1h --limit=20 --format='value(timestamp,textPayload)'
```

The Cloud Run console's Metrics tab shows request latency, instance count, CPU, and
memory for the same window.

## Rollback

One command; takes effect in seconds:

```sh
gcloud run services update-traffic ontology-annotation-network-rest \
  --to-revisions ontology-annotation-network-rest-00059-qs4=100 \
  --region us-east1 --project jax-robinson-hpo-01
```

Then:

1. Confirm `status.traffic` shows `00059-qs4` at 100%, and that requests return 200.
2. Leave the SQLite revision in place (it gets no traffic) so its logs stay
   available for debugging.
3. Rollback serves the data from the last Neo4j load, which is older than the
   SQLite build.

Traffic stays pinned to `00059-qs4` until the next change. To try SQLite again,
either run the deploy workflow again (a deploy sends 100% to the new revision), or
send traffic to the existing SQLite revision:

```sh
gcloud run services update-traffic ontology-annotation-network-rest \
  --to-latest --region us-east1 --project jax-robinson-hpo-01
```

## Decommissioning Neo4j (after two clean weeks)

1. Snapshot the `graph-data` disk, and keep the snapshot until you are sure it isn't needed:
   ```sh
   gcloud compute disks snapshot graph-data --zone us-east1-b --project jax-robinson-hpo-01 \
     --snapshot-names graph-data-final-neo4j
   ```
2. Stop, then later delete, `ontology-annotation-network-containerized` and
   `ontology-annotation-loader`.
3. Remove the VPC egress from the service:
   `gcloud run services update ontology-annotation-network-rest --clear-network --region us-east1 --project jax-robinson-hpo-01`.
4. Remove or archive the "Only Swap Disks & Copy Data" workflow.

After step 2, rollback to `00059-qs4` no longer works. Restoring Neo4j would mean
recreating the VM from the snapshot.
