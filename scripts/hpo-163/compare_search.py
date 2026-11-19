"""Record search results from an ontology-annotation-network API and compare two recordings.

`record` calls GET {base}/search/{entity}?q=<query>&page=0&limit=-1 for every row of a
queries TSV (columns entity, query, expected_id) and writes, per query, the ordered result
ids, the totalCount, the HTTP status and the seconds the request took, as JSON.

`diff` reads two recordings and the same queries TSV and prints a Markdown report. Each
query is flagged LOST when an id from the old recording is missing from the new one, MISS
when its expected_id is not in the new top 10, RANK when its expected_id ranks lower than
in the old recording, and SLOW when the new request took longer than 300 ms.
"""

import argparse
import csv
import json
import sys
import time
import urllib.parse
import urllib.request

SLOW_SECONDS = 0.3
TOP = 10


def read_queries(path):
    """Reads the queries TSV.

    Args:
        path: Path to a TSV with the header entity, query, expected_id.

    Returns:
        A list of dicts with the keys entity, query and expected_id (empty when blank).
    """
    with open(path, newline="") as handle:
        return [
            {"entity": row["entity"], "query": row["query"], "expected_id": (row.get("expected_id") or "").strip()}
            for row in csv.DictReader(handle, delimiter="\t")
        ]


def fetch(base, entity, query):
    """Fetches every search result for one query.

    Args:
        base: The API base URL, ending in /api/network.
        entity: Either gene or disease.
        query: The search text.

    Returns:
        A dict with the keys ids (ordered result ids), total, status and seconds.
    """
    url = f"{base.rstrip('/')}/search/{entity}?" + urllib.parse.urlencode({"q": query, "page": 0, "limit": -1})
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(url, timeout=60) as response:
            body = json.load(response)
            status = response.status
    except urllib.error.HTTPError as error:
        return {"ids": [], "total": 0, "status": error.code, "seconds": time.perf_counter() - start}
    seconds = time.perf_counter() - start
    return {
        "ids": [result["id"] for result in body.get("results", [])],
        "total": body.get("totalCount", 0),
        "status": status,
        "seconds": round(seconds, 3),
    }


def key(row):
    """Builds the recording key for a query row.

    Args:
        row: A row from read_queries.

    Returns:
        The string "<entity>\\t<query>".
    """
    return f"{row['entity']}\t{row['query']}"


def record(args):
    """Records every query in the TSV against one API and writes the JSON recording."""
    recording = {key(row): fetch(args.base, row["entity"], row["query"]) for row in read_queries(args.queries)}
    with open(args.out, "w") as handle:
        json.dump(recording, handle, indent=1)


def rank(ids, target):
    """Finds a result's 1-based rank.

    Args:
        ids: The ordered result ids.
        target: The id to look for.

    Returns:
        The rank, or None when the id is absent.
    """
    return ids.index(target) + 1 if target in ids else None


def diff(args):
    """Prints a Markdown report comparing two recordings for every query in the TSV."""
    with open(args.old) as handle:
        old = json.load(handle)
    with open(args.new) as handle:
        new = json.load(handle)
    counts = {"LOST": 0, "MISS": 0, "RANK": 0, "SLOW": 0}
    lines = [
        "| entity | query | old total | new total | old s | new s | expected (old→new rank) | flags | new ids (not in old, first 5) |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    details = []
    for row in read_queries(args.queries):
        before, after = old[key(row)], new[key(row)]
        flags = []
        lost = [i for i in before["ids"] if i not in set(after["ids"])]
        if lost:
            flags.append("LOST")
        expected = row["expected_id"]
        old_rank = rank(before["ids"], expected) if expected else None
        new_rank = rank(after["ids"], expected) if expected else None
        if expected and (new_rank is None or new_rank > TOP):
            flags.append("MISS")
        if expected and old_rank is not None and (new_rank is None or new_rank > old_rank):
            flags.append("RANK")
        if after["seconds"] > SLOW_SECONDS:
            flags.append("SLOW")
        for flag in flags:
            counts[flag] += 1
        added = [i for i in after["ids"] if i not in set(before["ids"])]
        expected_cell = f"{expected} ({old_rank}→{new_rank})" if expected else ""
        lines.append(
            f"| {row['entity']} | `{row['query']}` | {before['total']} | {after['total']} | {before['seconds']} | "
            f"{after['seconds']} | {expected_cell} | {' '.join(flags)} | {', '.join(added[:5])} |"
        )
        details.append(
            f"- **{row['entity']} `{row['query']}`** — old top {TOP}: {', '.join(before['ids'][:TOP]) or '—'}; "
            f"new top {TOP}: {', '.join(after['ids'][:TOP]) or '—'}" + (f"; LOST: {', '.join(lost[:10])}" if lost else "")
        )
    summary = ", ".join(f"{flag} {count}" for flag, count in counts.items())
    print("## Summary\n\n" + summary + "\n\n## Per query\n\n" + "\n".join(lines) + "\n\n## Top 10 before and after\n\n" + "\n".join(details))


def main(argv=None):
    """Parses the command line and runs record or diff."""
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    record_parser = commands.add_parser("record", help="record results from one API")
    record_parser.add_argument("--base", required=True, help="API base URL ending in /api/network")
    record_parser.add_argument("--queries", required=True, help="queries TSV")
    record_parser.add_argument("--out", required=True, help="JSON file to write")
    record_parser.set_defaults(run=record)
    diff_parser = commands.add_parser("diff", help="compare two recordings")
    diff_parser.add_argument("--old", required=True, help="baseline JSON recording")
    diff_parser.add_argument("--new", required=True, help="new JSON recording")
    diff_parser.add_argument("--queries", required=True, help="queries TSV")
    diff_parser.set_defaults(run=diff)
    args = parser.parse_args(argv)
    args.run(args)


if __name__ == "__main__":
    sys.exit(main())
