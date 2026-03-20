#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Tuple


ROOT = Path("/Users/youngbinkim/Workspace/github/Benchmark")
MATRIX_DIR = ROOT / "benchmark-runner" / "build" / "reports" / "benchmarks" / "matrix"
OUTPUT = MATRIX_DIR / "matrix-summary.html"


def load_json(path: Path) -> dict:
    return json.loads(path.read_text())


def latest_json(folder: str) -> Path:
    files = sorted((MATRIX_DIR / folder).glob("benchmark-*.json"))
    if not files:
        raise FileNotFoundError(f"No benchmark JSON found in {folder}")
    return files[-1]


def rows_by_scenario(data: dict) -> Dict[str, dict]:
    return {row["scenario"]: row for row in data["results"]}


def fmt_rps(value: float) -> str:
    return f"{value:,.0f}"


def fmt_ms(value: float) -> str:
    return f"{value:.1f}"


def esc(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def render_section_table(title: str, scenario_order: List[str], datasets: List[Tuple[str, dict]]) -> str:
    header = "".join(f"<th>{esc(name)}</th>" for name, _ in datasets)
    body_rows = []
    for scenario in scenario_order:
        scenario_rows = [(name, rows_by_scenario(data)[scenario]) for name, data in datasets]
        winner_rps = max(item[1]["requestsPerSecond"] for item in scenario_rows)
        cols = []
        for name, row in scenario_rows:
            winner = row["requestsPerSecond"] == winner_rps
            cols.append(
                f"<td class=\"{'winner' if winner else ''}\">"
                f"<strong>{fmt_rps(row['requestsPerSecond'])}{' <em>best</em>' if winner else ''}</strong>"
                f"<span>{fmt_ms(row['latency']['p95Ms'])} ms p95</span>"
                f"<span>{row['failures']} failures</span>"
                "</td>"
            )
        body_rows.append(f"<tr><th>{esc(scenario)}</th>{''.join(cols)}</tr>")
    return f"""
    <section class="panel">
      <h2>{esc(title)}</h2>
      <table>
        <thead>
          <tr>
            <th>Scenario</th>
            {header}
          </tr>
        </thead>
        <tbody>
          {''.join(body_rows)}
        </tbody>
      </table>
    </section>
    """


def render_delta_table(title: str, scenario_order: List[str], before_name: str, before: dict, after_name: str, after: dict) -> str:
    before_rows = rows_by_scenario(before)
    after_rows = rows_by_scenario(after)
    body_rows = []
    for scenario in scenario_order:
        left = before_rows[scenario]
        right = after_rows[scenario]
        rps_delta = ((right["requestsPerSecond"] - left["requestsPerSecond"]) / left["requestsPerSecond"] * 100.0) if left["requestsPerSecond"] else 0.0
        p95_delta = right["latency"]["p95Ms"] - left["latency"]["p95Ms"]
        body_rows.append(
            "<tr>"
            f"<th>{esc(scenario)}</th>"
            f"<td>{fmt_rps(left['requestsPerSecond'])}<span>{fmt_ms(left['latency']['p95Ms'])} ms p95</span></td>"
            f"<td>{fmt_rps(right['requestsPerSecond'])}<span>{fmt_ms(right['latency']['p95Ms'])} ms p95</span></td>"
            f"<td class=\"{'positive' if rps_delta >= 0 else 'negative'}\">{rps_delta:+.1f}% req/s</td>"
            f"<td class=\"{'positive' if p95_delta <= 0 else 'negative'}\">{p95_delta:+.1f} ms p95</td>"
            "</tr>"
        )
    return f"""
    <section class="panel">
      <h2>{esc(title)}</h2>
      <table>
        <thead>
          <tr>
            <th>Scenario</th>
            <th>{esc(before_name)}</th>
            <th>{esc(after_name)}</th>
            <th>Req/s Delta</th>
            <th>P95 Delta</th>
          </tr>
        </thead>
        <tbody>
          {''.join(body_rows)}
        </tbody>
      </table>
    </section>
    """


def render_top_statements(title: str, data: dict) -> str:
    metrics = data.get("postgresMetrics") or {}
    statements = metrics.get("topStatements") or []
    stats = metrics.get("database") or {}
    rows = []
    for statement in statements[:8]:
        rows.append(
            "<tr>"
            f"<td><code>{esc(statement['query'])}</code></td>"
            f"<td>{statement['calls']}</td>"
            f"<td>{statement['totalExecTimeMs']:.1f}</td>"
            f"<td>{statement['meanExecTimeMs']:.2f}</td>"
            "</tr>"
        )
    cache_hit_ratio = 100.0
    total_blocks = stats.get("blocksRead", 0) + stats.get("blocksHit", 0)
    if total_blocks:
        cache_hit_ratio = stats.get("blocksHit", 0) * 100.0 / total_blocks
    return f"""
    <section class="panel">
      <h2>{esc(title)}</h2>
      <div class="metrics">
        <div class="metric"><label>xactCommit</label><strong>{stats.get('xactCommit', 0):,}</strong></div>
        <div class="metric"><label>Cache Hit</label><strong>{cache_hit_ratio:.3f}%</strong></div>
        <div class="metric"><label>Block Read</label><strong>{stats.get('blockReadTimeMs', 0.0):.1f} ms</strong></div>
        <div class="metric"><label>Block Write</label><strong>{stats.get('blockWriteTimeMs', 0.0):.1f} ms</strong></div>
      </div>
      <table>
        <thead>
          <tr>
            <th>Query</th>
            <th>Calls</th>
            <th>Total ms</th>
            <th>Mean ms</th>
          </tr>
        </thead>
        <tbody>
          {''.join(rows)}
        </tbody>
      </table>
    </section>
    """


def render_insights() -> str:
    cards = [
        (
            "Overall Winner",
            "Current benchmark baseline still favors Spring MVC on platform threads for DB-backed reads and distributed writes.",
        ),
        (
            "Ktor Strength",
            "Ktor is strongest on lightweight JSON/HTTP paths. Small non-DB responses are consistently competitive or faster.",
        ),
        (
            "Pool Sensitivity",
            "Both stacks improve materially when DB pool size increases. Ktor is especially sensitive to undersized pools.",
        ),
        (
            "Hotspot Caveat",
            "Hotspot checkout results are dominated by contention and stock depletion. High req/s there includes fast failures, not only useful work.",
        ),
    ]
    items = "".join(
        f"<div class='metric'><label>{esc(title)}</label><strong>{esc(body)}</strong></div>"
        for title, body in cards
    )
    return f"""
    <section class="panel">
      <h2>Key Conclusions</h2>
      <div class="metrics insights">{items}</div>
    </section>
    """


def render_recommendations() -> str:
    rows = [
        ("Small JSON / non-DB", "Ktor", "Use Ktor when the path is mostly serialization and routing overhead."),
        ("DB-backed point reads", "Spring platform threads", "Spring + JDBC + jOOQ remains the strongest baseline in this sample."),
        ("Large list / large payload", "Spring platform threads", "Large DB result materialization still favors the Spring path here."),
        ("Distributed write load", "Spring platform threads", "Spring leads overall, but Ktor closes the gap after pool tuning."),
        ("Pool tuning priority", "Both", "Increase DB pool sizes before drawing conclusions from higher concurrency tests."),
    ]
    tr = "".join(
        "<tr>"
        f"<th>{esc(a)}</th><td>{esc(b)}</td><td>{esc(c)}</td>"
        "</tr>"
        for a, b, c in rows
    )
    return f"""
    <section class="panel">
      <h2>Scenario Recommendations</h2>
      <table>
        <thead>
          <tr>
            <th>Scenario Type</th>
            <th>Recommended Baseline</th>
            <th>Reason</th>
          </tr>
        </thead>
        <tbody>{tr}</tbody>
      </table>
    </section>
    """


def render_notes() -> str:
    notes = [
        "Hotspot checkout hits the same book row repeatedly. Once stock is exhausted, failures rise quickly and throughput no longer means successful business work.",
        "The Ktor aggregate report path is not a fully equivalent DB GROUP BY implementation yet, so treat that row as directional rather than authoritative.",
        "All matrix runs used fresh PostgreSQL containers with pg_stat_statements enabled and generated HTML/JSON reports from the same runner.",
    ]
    items = "".join(f"<li>{esc(note)}</li>" for note in notes)
    return f"""
    <section class="panel">
      <h2>Notes</h2>
      <ul class="notes">{items}</ul>
    </section>
    """


def render_stack() -> str:
    rows = [
        ("Language / Build", "Kotlin 2.3.20, Gradle 9.4.1, Version Catalog"),
        ("Database", "PostgreSQL 17 (Docker Compose)"),
        ("Spring App", "Spring Boot MVC, Jackson, jOOQ, JDBC, HikariCP, optional virtual threads"),
        ("Ktor App", "Ktor, kotlinx.serialization, Koin, Exposed, R2DBC PostgreSQL, R2DBC pool"),
        ("Benchmarking", "Custom Kotlin runner, HTML report generation, pg_stat_statements, pg_stat_database"),
    ]
    tr = "".join(
        "<tr>"
        f"<th>{esc(left)}</th><td>{esc(right)}</td>"
        "</tr>"
        for left, right in rows
    )
    return f"""
    <section class="panel">
      <h2>Tech Stack</h2>
      <table>
        <thead>
          <tr>
            <th>Area</th>
            <th>Stack</th>
          </tr>
        </thead>
        <tbody>{tr}</tbody>
      </table>
    </section>
    """


def main() -> None:
    concurrency = [
        ("Spring c64", load_json(latest_json("concurrency-spring-c64"))),
        ("Spring c128", load_json(latest_json("concurrency-spring-c128"))),
        ("Spring c256", load_json(latest_json("concurrency-spring-c256"))),
        ("Ktor c64", load_json(latest_json("concurrency-ktor-c64"))),
        ("Ktor c128", load_json(latest_json("concurrency-ktor-c128"))),
        ("Ktor c256", load_json(latest_json("concurrency-ktor-c256"))),
    ]
    pool = [
        ("Spring pool 8", load_json(latest_json("pool-spring-p8"))),
        ("Spring pool 32", load_json(latest_json("pool-spring-p32"))),
        ("Ktor pool 8", load_json(latest_json("pool-ktor-p8"))),
        ("Ktor pool 32", load_json(latest_json("pool-ktor-p32"))),
    ]
    write = [
        ("Spring c64", load_json(latest_json("write-spring-c64"))),
        ("Spring c128", load_json(latest_json("write-spring-c128"))),
        ("Spring c256", load_json(latest_json("write-spring-c256"))),
        ("Ktor c64", load_json(latest_json("write-ktor-c64"))),
        ("Ktor c128", load_json(latest_json("write-ktor-c128"))),
        ("Ktor c256", load_json(latest_json("write-ktor-c256"))),
    ]
    virtual_compare = [
        ("Spring virtual on", load_json(ROOT / "benchmark-runner" / "build" / "reports" / "benchmarks" / "spring-solo-metrics" / "benchmark-20260320-000541.json")),
        ("Spring virtual off", load_json(ROOT / "benchmark-runner" / "build" / "reports" / "benchmarks" / "spring-platform-threads" / "benchmark-20260320-001100.json")),
    ]

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Benchmark Matrix Summary</title>
  <style>
    :root {{
      --bg: #0b1020;
      --panel: #121a31;
      --panel-2: #1b2746;
      --text: #eef3ff;
      --muted: #a9b6d3;
      --accent: #f7b32b;
      --accent-2: #5bc0eb;
      --border: rgba(255,255,255,0.08);
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background:
        radial-gradient(circle at top left, rgba(91,192,235,0.18), transparent 30%),
        radial-gradient(circle at top right, rgba(247,179,43,0.12), transparent 26%),
        var(--bg);
      color: var(--text);
      font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
    }}
    .wrap {{
      width: min(1440px, calc(100vw - 32px));
      margin: 0 auto;
      padding: 28px 0 40px;
    }}
    h1, h2 {{ margin: 0 0 12px; }}
    p {{ color: var(--muted); margin: 0; }}
    .hero {{
      background: linear-gradient(135deg, rgba(18,26,49,0.92), rgba(27,39,70,0.92));
      border: 1px solid var(--border);
      border-radius: 20px;
      padding: 24px;
      box-shadow: 0 20px 60px rgba(0,0,0,0.25);
    }}
    .sections {{
      display: grid;
      gap: 18px;
      margin-top: 18px;
    }}
    .panel {{
      background: rgba(18,26,49,0.92);
      border: 1px solid var(--border);
      border-radius: 18px;
      padding: 20px;
      box-shadow: 0 18px 50px rgba(0,0,0,0.18);
    }}
    .metrics {{
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 12px;
      margin: 12px 0 18px;
    }}
    .insights .metric strong {{
      font-size: 1rem;
      line-height: 1.5;
      color: var(--text);
    }}
    .metric {{
      background: rgba(255,255,255,0.03);
      border-radius: 14px;
      padding: 14px;
    }}
    .metric label {{
      color: var(--muted);
      display: block;
      font-size: 0.9rem;
    }}
    .metric strong {{
      display: block;
      margin-top: 6px;
      font-size: 1.3rem;
    }}
    .winner {{
      background: rgba(247,179,43,0.08);
    }}
    .winner em {{
      color: var(--accent);
      font-style: normal;
      font-size: 0.78rem;
    }}
    .positive {{
      color: #7ae582;
    }}
    .negative {{
      color: #ff7b7b;
    }}
    table {{
      width: 100%;
      border-collapse: collapse;
      font-size: 0.94rem;
    }}
    th, td {{
      border-bottom: 1px solid var(--border);
      padding: 12px 10px;
      text-align: left;
      vertical-align: top;
    }}
    th {{
      color: var(--muted);
      font-weight: 600;
    }}
    td strong {{
      display: block;
      color: var(--accent);
    }}
    td span {{
      display: block;
      color: var(--muted);
      margin-top: 3px;
      font-size: 0.86rem;
    }}
    code {{
      white-space: pre-wrap;
      color: var(--accent-2);
      font-size: 0.84rem;
    }}
    .notes {{
      margin: 0;
      padding-left: 18px;
      color: var(--muted);
    }}
    .notes li {{
      margin: 10px 0;
    }}
  </style>
</head>
<body>
  <div class="wrap">
    <section class="hero">
      <h1>Benchmark Matrix Summary</h1>
      <p>Scenario-expanded results for concurrency scaling, DB pool sizing, and write-heavy contention. Each cell shows requests/sec, p95 latency, and failures.</p>
    </section>
    <div class="sections">
      {render_stack()}
      {render_insights()}
      {render_section_table("Concurrency Matrix", ["smallJson", "largeJson", "bookById", "bookList500", "aggregateReport"], concurrency)}
      {render_section_table("Pool Matrix", ["bookById", "bookList500", "aggregateReport", "checkoutDistributed"], pool)}
      {render_section_table("Write-Heavy Matrix", ["checkoutCreate", "checkoutHotspot", "checkoutDistributed"], write)}
      {render_delta_table("Spring Pool 8 -> 32 Delta", ["bookById", "bookList500", "aggregateReport", "checkoutDistributed"], "Spring pool 8", pool[0][1], "Spring pool 32", pool[1][1])}
      {render_delta_table("Ktor Pool 8 -> 32 Delta", ["bookById", "bookList500", "aggregateReport", "checkoutDistributed"], "Ktor pool 8", pool[2][1], "Ktor pool 32", pool[3][1])}
      {render_section_table("Spring Virtual Threads Comparison", ["health", "bookById", "bookList", "bookSearch", "checkoutCreate"], virtual_compare)}
      {render_recommendations()}
      {render_notes()}
      {render_top_statements("Spring Pool 32 Top SQL", pool[1][1])}
      {render_top_statements("Ktor Pool 32 Top SQL", pool[3][1])}
    </div>
  </div>
</body>
</html>
"""

    OUTPUT.write_text(html)
    print(OUTPUT)


if __name__ == "__main__":
    main()
