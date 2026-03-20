package benchmark

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Collections
import kotlin.math.ceil
import kotlin.random.Random

private val json = Json { prettyPrint = true }

fun main(args: Array<String>) {
    BenchmarkRunner().run(args.toList())
}

class BenchmarkRunner {
    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .version(HttpClient.Version.HTTP_1_1)
        .build()

    fun run(args: List<String>) {
        val options = CliOptions.parse(args)
        val targets = options.targets.ifEmpty {
            listOf(
                BenchmarkTarget("spring-mvc-jdbc", "http://localhost:8080"),
                BenchmarkTarget("ktor-r2dbc", "http://localhost:8081"),
            )
        }
        val scenarios = defaultScenarios(options.maxBookId)
            .filter { options.scenarios.isEmpty() || it.name in options.scenarios }
        val metricsCollector = options.createMetricsCollector()

        metricsCollector?.reset()

        val results = runBlockingBenchmarks(
            targets = targets,
            scenarios = scenarios,
            warmupSeconds = options.warmupSeconds,
            measureSeconds = options.measureSeconds,
            concurrency = options.concurrency,
        )
        val postgresMetrics = metricsCollector?.collect()

        val reportDir = Path.of(options.outputDir)
        Files.createDirectories(reportDir)

        val timestamp = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneId.systemDefault())
            .format(Instant.now())
        val jsonPath = reportDir.resolve("benchmark-$timestamp.json")
        val htmlPath = reportDir.resolve("benchmark-$timestamp.html")

        val suiteResult = results.copy(postgresMetrics = postgresMetrics)
        Files.writeString(jsonPath, json.encodeToString(suiteResult))
        Files.writeString(htmlPath, HtmlReport.render(suiteResult, options))

        println("Benchmark JSON report: $jsonPath")
        println("Benchmark HTML report: $htmlPath")
    }

    private fun runBlockingBenchmarks(
        targets: List<BenchmarkTarget>,
        scenarios: List<Scenario>,
        warmupSeconds: Int,
        measureSeconds: Int,
        concurrency: Int,
    ): BenchmarkSuiteResult {
        val suiteStartedAt = Instant.now()
        val scenarioResults = buildList {
            for (scenario in scenarios) {
                for (target in targets) {
                    warmup(target, scenario, warmupSeconds)
                    add(
                        measure(
                            target = target,
                            scenario = scenario,
                            measureSeconds = measureSeconds,
                            concurrency = concurrency,
                        )
                    )
                }
            }
        }

        return BenchmarkSuiteResult(
            generatedAt = suiteStartedAt.toString(),
            warmupSeconds = warmupSeconds,
            measureSeconds = measureSeconds,
            concurrency = concurrency,
            results = scenarioResults,
        )
    }

    private fun warmup(target: BenchmarkTarget, scenario: Scenario, warmupSeconds: Int) {
        if (warmupSeconds <= 0) return
        val warmupDeadline = System.nanoTime() + Duration.ofSeconds(warmupSeconds.toLong()).toNanos()
        while (System.nanoTime() < warmupDeadline) {
            executeRequest(target.baseUrl, scenario.request())
        }
    }

    private fun measure(
        target: BenchmarkTarget,
        scenario: Scenario,
        measureSeconds: Int,
        concurrency: Int,
    ): ScenarioResult {
        val latenciesMicros = Collections.synchronizedList(mutableListOf<Long>())
        var successes = 0L
        var failures = 0L
        val deadline = System.nanoTime() + Duration.ofSeconds(measureSeconds.toLong()).toNanos()
        val startedAt = Instant.now()

        kotlinx.coroutines.runBlocking {
            coroutineScope {
                val jobs = mutableListOf<Job>()
                repeat(concurrency) {
                    jobs += launch(Dispatchers.IO) {
                        while (System.nanoTime() < deadline) {
                            val request = scenario.request()
                            val started = System.nanoTime()
                            val ok = executeRequest(target.baseUrl, request)
                            val elapsedMicros = Duration.ofNanos(System.nanoTime() - started).toNanos() / 1_000
                            latenciesMicros += elapsedMicros
                            if (ok) {
                                synchronized(this@BenchmarkRunner) { successes++ }
                            } else {
                                synchronized(this@BenchmarkRunner) { failures++ }
                            }
                        }
                    }
                }
                jobs.forEach { it.join() }
            }
        }

        val measuredSeconds = Duration.between(startedAt, Instant.now()).toMillis() / 1000.0
        val latencyStats = LatencyStats.from(latenciesMicros)
        return ScenarioResult(
            target = target.name,
            scenario = scenario.name,
            method = scenario.method,
            pathTemplate = scenario.pathTemplate,
            measuredSeconds = measuredSeconds,
            totalRequests = successes + failures,
            successes = successes,
            failures = failures,
            requestsPerSecond = if (measuredSeconds == 0.0) 0.0 else (successes + failures) / measuredSeconds,
            latency = latencyStats,
        )
    }

    private fun executeRequest(baseUrl: String, requestTemplate: RequestTemplate): Boolean {
        val bodyPublisher = requestTemplate.body?.let { HttpRequest.BodyPublishers.ofString(it) }
            ?: HttpRequest.BodyPublishers.noBody()

        val builder = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl.trimEnd('/') + requestTemplate.path))
            .timeout(Duration.ofSeconds(5))

        requestTemplate.headers.forEach(builder::header)
        val request = builder.method(requestTemplate.method, bodyPublisher).build()
        return runCatching {
            client.send(request, HttpResponse.BodyHandlers.discarding())
        }.map { response ->
            response.statusCode() in 200..299
        }.getOrDefault(false)
    }
}

private fun defaultScenarios(maxBookId: Long): List<Scenario> = listOf(
    Scenario(
        name = "health",
        method = "GET",
        pathTemplate = "/health",
        request = { RequestTemplate("GET", "/health") },
    ),
    Scenario(
        name = "smallJson",
        method = "GET",
        pathTemplate = "/api/books/bench/small-json",
        request = { RequestTemplate("GET", "/api/books/bench/small-json") },
    ),
    Scenario(
        name = "largeJson",
        method = "GET",
        pathTemplate = "/api/books/bench/large-json?limit=200",
        request = { RequestTemplate("GET", "/api/books/bench/large-json?limit=200") },
    ),
    Scenario(
        name = "bookById",
        method = "GET",
        pathTemplate = "/api/books/{id}",
        request = {
            val id = Random.nextLong(1, maxBookId + 1)
            RequestTemplate("GET", "/api/books/$id")
        },
    ),
    Scenario(
        name = "bookList",
        method = "GET",
        pathTemplate = "/api/books?limit=50",
        request = { RequestTemplate("GET", "/api/books?limit=50") },
    ),
    Scenario(
        name = "bookList500",
        method = "GET",
        pathTemplate = "/api/books?limit=500",
        request = { RequestTemplate("GET", "/api/books?limit=500") },
    ),
    Scenario(
        name = "bookSearch",
        method = "GET",
        pathTemplate = "/api/books/search?q=Book&limit=20",
        request = { RequestTemplate("GET", "/api/books/search?q=Book&limit=20") },
    ),
    Scenario(
        name = "aggregateReport",
        method = "GET",
        pathTemplate = "/api/books/bench/aggregate-report?limit=50",
        request = { RequestTemplate("GET", "/api/books/bench/aggregate-report?limit=50") },
    ),
    Scenario(
        name = "checkoutCreate",
        method = "POST",
        pathTemplate = "/api/checkouts",
        request = {
            val id = Random.nextLong(1, maxBookId + 1)
            RequestTemplate(
                method = "POST",
                path = "/api/checkouts",
                body = """{"bookId":$id,"customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}""",
                headers = mapOf("Content-Type" to "application/json"),
            )
        },
    ),
    Scenario(
        name = "checkoutHotspot",
        method = "POST",
        pathTemplate = "/api/checkouts",
        request = {
            RequestTemplate(
                method = "POST",
                path = "/api/checkouts",
                body = """{"bookId":1,"customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}""",
                headers = mapOf("Content-Type" to "application/json"),
            )
        },
    ),
    Scenario(
        name = "checkoutDistributed",
        method = "POST",
        pathTemplate = "/api/checkouts",
        request = {
            val id = Random.nextLong(1, maxBookId + 1)
            RequestTemplate(
                method = "POST",
                path = "/api/checkouts",
                body = """{"bookId":$id,"customerName":"Benchmark User","customerEmail":"bench@example.com","quantity":1}""",
                headers = mapOf("Content-Type" to "application/json"),
            )
        },
    ),
)

private data class Scenario(
    val name: String,
    val method: String,
    val pathTemplate: String,
    val request: () -> RequestTemplate,
)

private data class RequestTemplate(
    val method: String,
    val path: String,
    val body: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

data class CliOptions(
    val warmupSeconds: Int = 5,
    val measureSeconds: Int = 15,
    val concurrency: Int = 64,
    val maxBookId: Long = 20_000,
    val outputDir: String = "build/reports/benchmarks",
    val jdbcUrl: String = "jdbc:postgresql://localhost:5432/benchmark?ApplicationName=benchmark-runner",
    val dbUsername: String = "benchmark",
    val dbPassword: String = "benchmark",
    val collectPostgresMetrics: Boolean = true,
    val scenarios: Set<String> = emptySet(),
    val targets: List<BenchmarkTarget> = emptyList(),
) {
    companion object {
        fun parse(args: List<String>): CliOptions {
            val values = args
                .mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size == 2 && parts[0].startsWith("--")) parts[0].removePrefix("--") to parts[1] else null
                }
                .toMap()

            return CliOptions(
                warmupSeconds = values["warmup-seconds"]?.toIntOrNull() ?: 5,
                measureSeconds = values["measure-seconds"]?.toIntOrNull() ?: 15,
                concurrency = values["concurrency"]?.toIntOrNull() ?: 64,
                maxBookId = values["max-book-id"]?.toLongOrNull() ?: 20_000,
                outputDir = values["output-dir"] ?: "build/reports/benchmarks",
                jdbcUrl = values["jdbc-url"] ?: "jdbc:postgresql://localhost:5432/benchmark?ApplicationName=benchmark-runner",
                dbUsername = values["db-username"] ?: "benchmark",
                dbPassword = values["db-password"] ?: "benchmark",
                collectPostgresMetrics = values["collect-postgres-metrics"]?.toBooleanStrictOrNull() ?: true,
                scenarios = values["scenarios"]?.split(",")?.map(String::trim)?.filter(String::isNotEmpty)?.toSet() ?: emptySet(),
                targets = parseTargets(values["targets"]),
            )
        }

        private fun parseTargets(raw: String?): List<BenchmarkTarget> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map {
                    val parts = it.split("@", limit = 2)
                    require(parts.size == 2) { "targets format must be name@http://host:port" }
                    BenchmarkTarget(parts[0], parts[1])
                }
        }
    }

    fun createMetricsCollector(): PostgresMetricsCollector? {
        if (!collectPostgresMetrics) return null
        return PostgresMetricsCollector(
            jdbcUrl = jdbcUrl,
            username = dbUsername,
            password = dbPassword,
        )
    }
}

@Serializable
data class BenchmarkSuiteResult(
    val generatedAt: String,
    val warmupSeconds: Int,
    val measureSeconds: Int,
    val concurrency: Int,
    val results: List<ScenarioResult>,
    val postgresMetrics: PostgresMetrics? = null,
)

@Serializable
data class ScenarioResult(
    val target: String,
    val scenario: String,
    val method: String,
    val pathTemplate: String,
    val measuredSeconds: Double,
    val totalRequests: Long,
    val successes: Long,
    val failures: Long,
    val requestsPerSecond: Double,
    val latency: LatencyStats,
)

@Serializable
data class LatencyStats(
    val minMs: Double,
    val avgMs: Double,
    val p50Ms: Double,
    val p95Ms: Double,
    val p99Ms: Double,
    val maxMs: Double,
) {
    companion object {
        fun from(samplesMicros: List<Long>): LatencyStats {
            if (samplesMicros.isEmpty()) {
                return LatencyStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
            }
            val sorted = samplesMicros.sorted()
            val avg = sorted.average() / 1000.0
            return LatencyStats(
                minMs = sorted.first() / 1000.0,
                avgMs = avg,
                p50Ms = percentile(sorted, 0.50),
                p95Ms = percentile(sorted, 0.95),
                p99Ms = percentile(sorted, 0.99),
                maxMs = sorted.last() / 1000.0,
            )
        }

        private fun percentile(sortedMicros: List<Long>, percentile: Double): Double {
            val index = ceil((sortedMicros.size - 1) * percentile).toInt().coerceIn(0, sortedMicros.lastIndex)
            return sortedMicros[index] / 1000.0
        }
    }
}

@Serializable
data class BenchmarkTarget(
    val name: String,
    val baseUrl: String,
)

private object HtmlReport {
    fun render(result: BenchmarkSuiteResult, options: CliOptions): String {
        val payload = json.encodeToString(result)
        val metrics = result.postgresMetrics
        val rows = result.results.joinToString("\n") { entry ->
            """
            <tr>
              <td>${entry.target}</td>
              <td>${entry.scenario}</td>
              <td><code>${entry.method}</code></td>
              <td><code>${entry.pathTemplate}</code></td>
              <td>${"%.1f".format(entry.requestsPerSecond)}</td>
              <td>${"%.2f".format(entry.latency.avgMs)}</td>
              <td>${"%.2f".format(entry.latency.p95Ms)}</td>
              <td>${"%.2f".format(entry.latency.p99Ms)}</td>
              <td>${entry.failures}</td>
            </tr>
            """.trimIndent()
        }

        val postgresSection = if (metrics == null) {
            ""
        } else {
            val statementRows = metrics.topStatements.joinToString("\n") { statement ->
                """
                <tr>
                  <td><code>${escapeHtml(statement.query)}</code></td>
                  <td>${statement.calls}</td>
                  <td>${"%.2f".format(statement.totalExecTimeMs)}</td>
                  <td>${"%.2f".format(statement.meanExecTimeMs)}</td>
                  <td>${statement.rows}</td>
                  <td>${statement.sharedBlocksHit}</td>
                  <td>${statement.sharedBlocksRead}</td>
                </tr>
                """.trimIndent()
            }

            """
            <div class="panel" style="margin-top: 24px;">
              <h2>PostgreSQL Metrics</h2>
              <div class="grid">
                <div class="metric">Transactions<strong>${metrics.database.xactCommit}</strong></div>
                <div class="metric">Cache Hit Ratio<strong>${"%.2f".format(metrics.database.cacheHitRatio)}%</strong></div>
                <div class="metric">Block Read Time<strong>${"%.2f".format(metrics.database.blockReadTimeMs)} ms</strong></div>
                <div class="metric">Block Write Time<strong>${"%.2f".format(metrics.database.blockWriteTimeMs)} ms</strong></div>
                <div class="metric">Temp Files<strong>${metrics.database.tempFiles}</strong></div>
                <div class="metric">Deadlocks<strong>${metrics.database.deadlocks}</strong></div>
              </div>
              <table>
                <thead>
                  <tr>
                    <th>Query</th>
                    <th>Calls</th>
                    <th>Total ms</th>
                    <th>Mean ms</th>
                    <th>Rows</th>
                    <th>Blk Hit</th>
                    <th>Blk Read</th>
                  </tr>
                </thead>
                <tbody>
                  $statementRows
                </tbody>
              </table>
            </div>
            """.trimIndent()
        }

        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8" />
          <meta name="viewport" content="width=device-width, initial-scale=1.0" />
          <title>Framework Benchmark Report</title>
          <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
          <style>
            :root {
              --bg: #0d1321;
              --panel: #1d2d44;
              --accent: #f0a202;
              --accent-2: #3e92cc;
              --text: #f8f9fb;
              --muted: #b9c5d6;
              --good: #60d394;
              --bad: #ff6b6b;
            }
            * { box-sizing: border-box; }
            body {
              margin: 0;
              font-family: "IBM Plex Sans", "Segoe UI", sans-serif;
              background: radial-gradient(circle at top left, #274060 0%, var(--bg) 55%);
              color: var(--text);
            }
            .wrap {
              width: min(1180px, calc(100vw - 32px));
              margin: 0 auto;
              padding: 32px 0 48px;
            }
            h1, h2 { margin: 0 0 12px; }
            p { color: var(--muted); }
            .meta, .panel {
              background: rgba(29, 45, 68, 0.85);
              border: 1px solid rgba(255,255,255,0.08);
              border-radius: 18px;
              padding: 20px;
              backdrop-filter: blur(10px);
              box-shadow: 0 18px 50px rgba(0,0,0,0.22);
            }
            .grid {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
              gap: 16px;
              margin: 20px 0 28px;
            }
            .metric {
              padding: 18px;
              border-radius: 16px;
              background: rgba(255,255,255,0.04);
            }
            .metric strong {
              display: block;
              font-size: 1.9rem;
              margin-top: 10px;
            }
            .charts {
              display: grid;
              grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
              gap: 18px;
              margin-top: 24px;
            }
            table {
              width: 100%;
              border-collapse: collapse;
              margin-top: 20px;
              font-size: 0.95rem;
            }
            th, td {
              padding: 12px 10px;
              border-bottom: 1px solid rgba(255,255,255,0.08);
              text-align: left;
            }
            th { color: var(--muted); }
            code { color: #ffd166; }
            .good { color: var(--good); }
            .bad { color: var(--bad); }
          </style>
        </head>
        <body>
          <div class="wrap">
            <div class="meta">
              <h1>Framework Benchmark Report</h1>
              <p>Generated at ${result.generatedAt}. Warmup ${options.warmupSeconds}s, measure ${options.measureSeconds}s, concurrency ${options.concurrency}.</p>
              <div class="grid">
                <div class="metric">
                  Targets
                  <strong>${result.results.map { it.target }.distinct().size}</strong>
                </div>
                <div class="metric">
                  Scenarios
                  <strong>${result.results.map { it.scenario }.distinct().size}</strong>
                </div>
                <div class="metric">
                  Requests
                  <strong>${result.results.sumOf { it.totalRequests }}</strong>
                </div>
                <div class="metric">
                  Failures
                  <strong class="${if (result.results.sumOf { it.failures } == 0L) "good" else "bad"}">${result.results.sumOf { it.failures }}</strong>
                </div>
              </div>
            </div>

            <div class="charts">
              <div class="panel">
                <h2>Throughput</h2>
                <canvas id="throughputChart"></canvas>
              </div>
              <div class="panel">
                <h2>P95 Latency</h2>
                <canvas id="latencyChart"></canvas>
              </div>
            </div>

            <div class="panel" style="margin-top: 24px;">
              <h2>Detailed Results</h2>
              <table>
                <thead>
                  <tr>
                    <th>Target</th>
                    <th>Scenario</th>
                    <th>Method</th>
                    <th>Path</th>
                    <th>Req/s</th>
                    <th>Avg ms</th>
                    <th>P95 ms</th>
                    <th>P99 ms</th>
                    <th>Failures</th>
                  </tr>
                </thead>
                <tbody>
                  $rows
                </tbody>
              </table>
            </div>
            $postgresSection
          </div>

          <script>
            const data = $payload;
            const results = data.results;
            const labels = results.map(item => `${'$'}{item.target}: ${'$'}{item.scenario}`);

            new Chart(document.getElementById("throughputChart"), {
              type: "bar",
              data: {
                labels,
                datasets: [{
                  label: "Requests / sec",
                  data: results.map(item => item.requestsPerSecond),
                  backgroundColor: "#f0a202"
                }]
              },
              options: {
                responsive: true,
                plugins: { legend: { display: false } }
              }
            });

            new Chart(document.getElementById("latencyChart"), {
              type: "bar",
              data: {
                labels,
                datasets: [{
                  label: "P95 latency (ms)",
                  data: results.map(item => item.latency.p95Ms),
                  backgroundColor: "#3e92cc"
                }]
              },
              options: {
                responsive: true,
                plugins: { legend: { display: false } }
              }
            });
          </script>
        </body>
        </html>
        """.trimIndent()
    }
}

private fun escapeHtml(value: String): String = buildString(value.length) {
    value.forEach { char ->
        append(
            when (char) {
                '<' -> "&lt;"
                '>' -> "&gt;"
                '&' -> "&amp;"
                '"' -> "&quot;"
                else -> char
            },
        )
    }
}
