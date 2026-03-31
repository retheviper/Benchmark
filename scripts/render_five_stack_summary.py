#!/usr/bin/env python3
from __future__ import annotations

import json
from pathlib import Path
from typing import Dict, List, Tuple


ROOT = Path("/Users/youngbinkim/Workspace/github/Benchmark")
BASE = ROOT / "benchmark-runner" / "build" / "reports" / "benchmarks" / "five-stack"
OUTPUT = BASE / "five-stack-summary.html"
SCENARIO_ORDER = [
    "health",
    "smallJson",
    "largeJson",
    "bookById",
    "bookList",
    "bookList500",
    "bookSearch",
    "aggregateReport",
    "checkoutCreate",
    "checkoutHotspot",
    "checkoutDistributed",
]


def load_latest(folder: str) -> dict:
    files = sorted((BASE / folder).glob("benchmark-*.json"))
    if not files:
        raise FileNotFoundError(f"Missing benchmark json for {folder}")
    return json.loads(files[-1].read_text())


def rows_by_scenario(data: dict) -> Dict[str, dict]:
    return {row["scenario"]: row for row in data["results"]}


def esc(value: str) -> str:
    return (
        value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace('"', "&quot;")
    )


def fmt_rps(value: float) -> str:
    return f"{value:,.0f}"


def fmt_ms(value: float) -> str:
    return f"{value:.1f}"


def render_compare_table(title: str, datasets: List[Tuple[str, dict]]) -> str:
    headers = "".join(f"<th>{esc(name)}</th>" for name, _ in datasets)
    body = []
    for scenario in SCENARIO_ORDER:
        rows = [(name, rows_by_scenario(data)[scenario]) for name, data in datasets]
        best = max(row["requestsPerSecond"] for _, row in rows)
        cols = []
        for _, row in rows:
            winner = row["requestsPerSecond"] == best
            cols.append(
                f"<td class=\"{'winner' if winner else ''}\">"
                f"<strong>{fmt_rps(row['requestsPerSecond'])}{' <em>best</em>' if winner else ''}</strong>"
                f"<span>{fmt_ms(row['latency']['p95Ms'])} ms p95</span>"
                f"<span>{row['failures']} failures</span>"
                "</td>"
            )
        body.append(f"<tr><th>{esc(scenario)}</th>{''.join(cols)}</tr>")
    return f"""
    <section class="panel">
      <h2>{esc(title)}</h2>
      <table>
        <thead><tr><th>Scenario</th>{headers}</tr></thead>
        <tbody>{''.join(body)}</tbody>
      </table>
    </section>
    """


def render_scaling_table(title: str, low: List[Tuple[str, dict]], high: List[Tuple[str, dict]]) -> str:
    low_map = {name: rows_by_scenario(data) for name, data in low}
    high_map = {name: rows_by_scenario(data) for name, data in high}
    systems = [name for name, _ in low]
    headers = "".join(f"<th>{esc(name)}</th>" for name in systems)
    body = []
    for scenario in SCENARIO_ORDER:
        cols = []
        for name in systems:
            low_row = low_map[name][scenario]
            high_row = high_map[name][scenario]
            delta = ((high_row["requestsPerSecond"] - low_row["requestsPerSecond"]) / low_row["requestsPerSecond"] * 100.0) if low_row["requestsPerSecond"] else 0.0
            cols.append(
                f"<td class=\"{'positive' if delta >= 0 else 'negative'}\">"
                f"<strong>{delta:+.1f}%</strong>"
                f"<span>{fmt_rps(low_row['requestsPerSecond'])} -> {fmt_rps(high_row['requestsPerSecond'])}</span>"
                "</td>"
            )
        body.append(f"<tr><th>{esc(scenario)}</th>{''.join(cols)}</tr>")
    return f"""
    <section class="panel">
      <h2>{esc(title)}</h2>
      <table>
        <thead><tr><th>Scenario</th>{headers}</tr></thead>
        <tbody>{''.join(body)}</tbody>
      </table>
    </section>
    """


def render_winners(datasets: List[Tuple[str, dict]]) -> str:
    rows = []
    scenario_maps = {name: rows_by_scenario(data) for name, data in datasets}
    for scenario in SCENARIO_ORDER:
        ranked = sorted(
            ((name, scenario_maps[name][scenario]) for name, _ in datasets),
            key=lambda item: item[1]["requestsPerSecond"],
            reverse=True,
        )
        best_name, best_row = ranked[0]
        second_name, second_row = ranked[1]
        gap = ((best_row["requestsPerSecond"] - second_row["requestsPerSecond"]) / second_row["requestsPerSecond"] * 100.0) if second_row["requestsPerSecond"] else 0.0
        rows.append(
            "<tr>"
            f"<th>{esc(scenario)}</th>"
            f"<td>{esc(best_name)}</td>"
            f"<td>{fmt_rps(best_row['requestsPerSecond'])}</td>"
            f"<td>{esc(second_name)}</td>"
            f"<td>{gap:+.1f}%</td>"
            "</tr>"
        )
    return f"""
    <section class="panel">
      <h2>Scenario Winners</h2>
      <table>
        <thead><tr><th>Scenario</th><th>Winner</th><th>Req/s</th><th>Runner-up</th><th>Gap</th></tr></thead>
        <tbody>{''.join(rows)}</tbody>
      </table>
    </section>
    """


def render_stack() -> str:
    rows = [
        ("Spring MVC JDBC (platform)", "Tomcat, Jackson, jOOQ, JDBC, HikariCP, platform threads"),
        ("Spring MVC JDBC (virtual)", "Tomcat, Jackson, jOOQ, JDBC, HikariCP, virtual threads"),
        ("Spring WebFlux R2DBC", "Netty, Jackson, jOOQ SQL builder, R2DBC PostgreSQL, R2DBC pool"),
        ("Ktor JDBC", "Netty, kotlinx.serialization, Koin, Exposed JDBC, HikariCP"),
        ("Ktor R2DBC", "Netty, kotlinx.serialization, Koin, Exposed R2DBC, R2DBC PostgreSQL"),
        ("Benchmark harness", "Custom Kotlin runner, HTML/JSON reports, pg_stat_statements, PostgreSQL 17"),
    ]
    body = "".join(f"<tr><th>{esc(a)}</th><td>{esc(b)}</td></tr>" for a, b in rows)
    return f"""
    <section class="panel">
      <h2>Tech Stack</h2>
      <table>
        <thead><tr><th>System</th><th>Stack</th></tr></thead>
        <tbody>{body}</tbody>
      </table>
    </section>
    """


def render_insights() -> str:
    cards = [
        ("Blocking JDBC baseline", "Spring MVC on platform threads remains the main DB-heavy baseline to beat in this repository."),
        ("Virtual threads", "Spring MVC virtual threads are still competitive, but they are not automatically faster than platform threads in this workload."),
        ("Reactive stacks", "Spring WebFlux R2DBC and Ktor R2DBC need to be interpreted as end-to-end reactive paths, not just framework overhead."),
        ("Ktor split", "Ktor JDBC isolates Ktor from R2DBC overhead and helps separate framework cost from driver/ORM cost."),
        ("Hotspot caution", "Checkout hotspot includes rapid failures after stock depletion, so success rate matters more than raw req/s."),
    ]
    body = "".join(f"<div class='metric'><label>{esc(a)}</label><strong>{esc(b)}</strong></div>" for a, b in cards)
    return f"""
    <section class="panel">
      <h2>Key Conclusions</h2>
      <div class="metrics insights">{body}</div>
    </section>
    """


def render_notes() -> str:
    items = [
        "Every run used a fresh PostgreSQL container and the same benchmark scenarios.",
        "Spring WebFlux uses jOOQ as a SQL builder over DatabaseClient rather than generated jOOQ reactive repositories.",
        "aggregateReport now uses DB-side aggregation across all stacks, but framework, serializer, SQL layer, and driver differences remain part of the comparison.",
        "The comparison is intentionally stack-level: framework, serialization, DI, ORM/SQL layer, and driver are all part of the measured path.",
    ]
    return f"""
    <section class="panel">
      <h2>Notes</h2>
      <ul class="notes">{''.join(f'<li>{esc(item)}</li>' for item in items)}</ul>
    </section>
    """


def render_scenarios() -> str:
    rows = [
        ("health", "Non-DB", "GET", "/health", "-", "Liveness endpoint without DB access."),
        ("smallJson", "Read", "GET", "/api/books/bench/small-json", "-", "Small non-DB JSON serialization path."),
        ("largeJson", "Read", "GET", "/api/books/bench/large-json?limit=200", "-", "Large JSON payload assembly with 200 items."),
        ("bookById", "Read", "GET", "/api/books/{id}", "-", "Single book detail read by primary key."),
        ("bookList", "Read", "GET", "/api/books?limit=50", "-", "List read for 50 books."),
        ("bookList500", "Read", "GET", "/api/books?limit=500", "-", "Large list read for 500 books."),
        ("bookSearch", "Read", "GET", "/api/books/search?q=Book&limit=20", "-", "Search read using title/author filtering."),
        ("aggregateReport", "Read", "GET", "/api/books/bench/aggregate-report?limit=50", "-", "Aggregate or summary style read path."),
        ("checkoutCreate", "Write", "POST", "/api/checkouts", '{"bookId":123,"customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}', "Write transaction with random book checkout."),
        ("checkoutHotspot", "Write", "POST", "/api/checkouts", '{"bookId":1,"customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}', "Write contention test on the same book row."),
        ("checkoutDistributed", "Write", "POST", "/api/checkouts", '{"bookId":"random","customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}', "Write-heavy test distributed across many rows."),
    ]
    body = "".join(
        "<tr>"
        f"<th>{esc(name)}</th>"
        f"<td>{esc(kind)}</td>"
        f"<td><code>{esc(method)} {esc(endpoint)}</code></td>"
        f"<td><code>{esc(example)}</code></td>"
        f"<td>{esc(description)}</td>"
        "</tr>"
        for name, kind, method, endpoint, example, description in rows
    )
    notes = [
        "Checkout scenarios share the same POST endpoint and differ only in how benchmark inputs pick book IDs.",
        "checkoutHotspot is intentionally adversarial: it repeatedly hits the same row and quickly turns into lock contention and stock depletion.",
        "For checkoutHotspot, high req/s can include many fast failures. Interpret it together with failure count, not as pure successful throughput.",
    ]
    return f"""
    <section class="panel">
      <h2>Scenario Map</h2>
      <table>
        <thead><tr><th>Scenario</th><th>Type</th><th>Endpoint</th><th>Example Body</th><th>Purpose</th></tr></thead>
        <tbody>{body}</tbody>
      </table>
      <ul class="notes">{''.join(f'<li>{esc(note)}</li>' for note in notes)}</ul>
    </section>
    """


def main() -> None:
    c128 = [
        ("Spring MVC platform", load_latest("c128-spring-mvc-platform")),
        ("Spring MVC virtual", load_latest("c128-spring-mvc-virtual")),
        ("Spring WebFlux R2DBC", load_latest("c128-spring-webflux-r2dbc")),
        ("Ktor JDBC", load_latest("c128-ktor-jdbc")),
        ("Ktor R2DBC", load_latest("c128-ktor-r2dbc")),
    ]
    c256 = [
        ("Spring MVC platform", load_latest("c256-spring-mvc-platform")),
        ("Spring MVC virtual", load_latest("c256-spring-mvc-virtual")),
        ("Spring WebFlux R2DBC", load_latest("c256-spring-webflux-r2dbc")),
        ("Ktor JDBC", load_latest("c256-ktor-jdbc")),
        ("Ktor R2DBC", load_latest("c256-ktor-r2dbc")),
    ]

    html = f"""<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <title>Five-Stack Benchmark Summary</title>
  <style>
    :root {{
      --bg: #0a1220;
      --panel: rgba(15, 24, 43, 0.92);
      --border: rgba(255,255,255,0.08);
      --text: #eef4ff;
      --muted: #a9b7d6;
      --gold: #ffbf47;
      --teal: #59d2fe;
      --green: #72e0a5;
      --red: #ff8a8a;
    }}
    * {{ box-sizing: border-box; }}
    body {{
      margin: 0;
      background:
        radial-gradient(circle at top left, rgba(89,210,254,0.16), transparent 28%),
        radial-gradient(circle at top right, rgba(255,191,71,0.14), transparent 24%),
        var(--bg);
      color: var(--text);
      font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
    }}
    .wrap {{ width: min(1500px, calc(100vw - 32px)); margin: 0 auto; padding: 28px 0 40px; }}
    .hero, .panel {{
      background: var(--panel);
      border: 1px solid var(--border);
      border-radius: 20px;
      box-shadow: 0 18px 50px rgba(0,0,0,0.22);
    }}
    .hero {{ padding: 24px; }}
    .sections {{ display: grid; gap: 18px; margin-top: 18px; }}
    .panel {{ padding: 20px; }}
    h1, h2 {{ margin: 0 0 12px; }}
    p {{ margin: 0; color: var(--muted); }}
    table {{ width: 100%; border-collapse: collapse; }}
    th, td {{ padding: 12px 10px; border-top: 1px solid var(--border); vertical-align: top; text-align: left; }}
    td span {{ display: block; color: var(--muted); font-size: 0.9rem; margin-top: 4px; }}
    .winner strong {{ color: var(--gold); }}
    .positive strong {{ color: var(--green); }}
    .negative strong {{ color: var(--red); }}
    .metrics {{ display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 12px; margin-top: 12px; }}
    .metric {{ padding: 14px; border-radius: 16px; border: 1px solid var(--border); background: rgba(255,255,255,0.03); }}
    .metric label {{ display: block; color: var(--teal); margin-bottom: 6px; font-size: 0.9rem; }}
    .metric strong {{ font-size: 1rem; line-height: 1.45; }}
    .notes {{ margin: 0; padding-left: 20px; color: var(--muted); }}
    code {{ font-family: "IBM Plex Mono", monospace; }}
  </style>
</head>
<body>
  <div class="wrap">
    <section class="hero">
      <h1>Five-Stack Benchmark Summary</h1>
      <p>Spring MVC JDBC platform/virtual, Spring WebFlux R2DBC, Ktor JDBC, and Ktor R2DBC under the same PostgreSQL-backed benchmark scenarios.</p>
    </section>
    <div class="sections">
      {render_stack()}
      {render_scenarios()}
      {render_insights()}
      {render_compare_table("Concurrency 128", c128)}
      {render_compare_table("Concurrency 256", c256)}
      {render_scaling_table("Scaling: 128 -> 256", c128, c256)}
      {render_winners(c256)}
      {render_notes()}
    </div>
  </div>
</body>
</html>
"""

    OUTPUT.write_text(html)


if __name__ == "__main__":
    main()
