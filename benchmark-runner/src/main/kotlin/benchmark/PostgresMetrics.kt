package benchmark

import kotlinx.serialization.Serializable
import java.sql.DriverManager

class PostgresMetricsCollector(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String,
) {
    fun reset() {
        connection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("select pg_stat_statements_reset()")
                statement.execute("select pg_stat_reset()")
            }
        }
    }

    fun collect(): PostgresMetrics {
        connection().use { connection ->
            val database = connection.prepareStatement(
                """
                select
                    xact_commit,
                    xact_rollback,
                    blks_read,
                    blks_hit,
                    temp_files,
                    temp_bytes,
                    deadlocks,
                    blk_read_time,
                    blk_write_time,
                    tup_returned,
                    tup_fetched,
                    tup_inserted,
                    tup_updated,
                    tup_deleted
                from pg_stat_database
                where datname = current_database()
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    check(rs.next()) { "pg_stat_database did not return a row" }
                    DatabaseStats(
                        xactCommit = rs.getLong("xact_commit"),
                        xactRollback = rs.getLong("xact_rollback"),
                        blocksRead = rs.getLong("blks_read"),
                        blocksHit = rs.getLong("blks_hit"),
                        tempFiles = rs.getLong("temp_files"),
                        tempBytes = rs.getLong("temp_bytes"),
                        deadlocks = rs.getLong("deadlocks"),
                        blockReadTimeMs = rs.getDouble("blk_read_time"),
                        blockWriteTimeMs = rs.getDouble("blk_write_time"),
                        tuplesReturned = rs.getLong("tup_returned"),
                        tuplesFetched = rs.getLong("tup_fetched"),
                        tuplesInserted = rs.getLong("tup_inserted"),
                        tuplesUpdated = rs.getLong("tup_updated"),
                        tuplesDeleted = rs.getLong("tup_deleted"),
                    )
                }
            }

            val statements = connection.prepareStatement(
                """
                select
                    calls,
                    total_exec_time,
                    mean_exec_time,
                    rows,
                    shared_blks_hit,
                    shared_blks_read,
                    temp_blks_read,
                    temp_blks_written,
                    query
                from pg_stat_statements
                where dbid = (select oid from pg_database where datname = current_database())
                  and query not like 'select pg_stat%'
                  and query not like 'show shared_preload_libraries%'
                order by total_exec_time desc
                limit 12
                """.trimIndent(),
            ).use { statement ->
                statement.executeQuery().use { rs ->
                    buildList {
                        while (rs.next()) {
                            add(
                                StatementStat(
                                    query = rs.getString("query"),
                                    calls = rs.getLong("calls"),
                                    totalExecTimeMs = rs.getDouble("total_exec_time"),
                                    meanExecTimeMs = rs.getDouble("mean_exec_time"),
                                    rows = rs.getLong("rows"),
                                    sharedBlocksHit = rs.getLong("shared_blks_hit"),
                                    sharedBlocksRead = rs.getLong("shared_blks_read"),
                                    tempBlocksRead = rs.getLong("temp_blks_read"),
                                    tempBlocksWritten = rs.getLong("temp_blks_written"),
                                ),
                            )
                        }
                    }
                }
            }

            return PostgresMetrics(
                database = database,
                topStatements = statements,
            )
        }
    }

    private fun connection() = DriverManager.getConnection(jdbcUrl, username, password)
}

@Serializable
data class PostgresMetrics(
    val database: DatabaseStats,
    val topStatements: List<StatementStat>,
)

@Serializable
data class DatabaseStats(
    val xactCommit: Long,
    val xactRollback: Long,
    val blocksRead: Long,
    val blocksHit: Long,
    val tempFiles: Long,
    val tempBytes: Long,
    val deadlocks: Long,
    val blockReadTimeMs: Double,
    val blockWriteTimeMs: Double,
    val tuplesReturned: Long,
    val tuplesFetched: Long,
    val tuplesInserted: Long,
    val tuplesUpdated: Long,
    val tuplesDeleted: Long,
) {
    val cacheHitRatio: Double
        get() {
            val total = blocksRead + blocksHit
            return if (total == 0L) 100.0 else blocksHit.toDouble() * 100.0 / total.toDouble()
        }
}

@Serializable
data class StatementStat(
    val query: String,
    val calls: Long,
    val totalExecTimeMs: Double,
    val meanExecTimeMs: Double,
    val rows: Long,
    val sharedBlocksHit: Long,
    val sharedBlocksRead: Long,
    val tempBlocksRead: Long,
    val tempBlocksWritten: Long,
)
