package com.edutech.performance.infrastructure.persistence;

import com.edutech.performance.domain.port.out.ReadinessTimeSeriesPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Infrastructure adapter for time-series readiness queries.
 * Uses native JDBC against the TimescaleDB {@code daily_readiness_avg} continuous aggregate
 * to avoid full table scans on the raw hypertable.
 */
@Component
public class ReadinessTimeSeriesAdapter implements ReadinessTimeSeriesPort {

    private static final String SQL =
            "SELECT day, avg_ers_score, snapshot_count " +
            "FROM performance_schema.daily_readiness_avg " +
            "WHERE student_id = ?::uuid " +
            "  AND day >= CURRENT_DATE - (? * INTERVAL '1 day') " +
            "ORDER BY day ASC";

    private final JdbcTemplate jdbcTemplate;

    public ReadinessTimeSeriesAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<DailyReadiness> getDailyReadiness(String studentId, int lastDays) {
        return jdbcTemplate.query(
                SQL,
                (rs, rowNum) -> new DailyReadiness(
                        rs.getObject("day", LocalDate.class),
                        rs.getObject("avg_ers_score", BigDecimal.class),
                        rs.getInt("snapshot_count")
                ),
                studentId,
                lastDays
        );
    }
}
