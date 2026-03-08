package com.edutech.performance.domain.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Port for querying time-series readiness data from TimescaleDB hypertables.
 * Implementation uses native JDBC to leverage continuous aggregates.
 */
public interface ReadinessTimeSeriesPort {

    /**
     * Returns daily average ERS scores for a student over the last {@code lastDays} days.
     * Queries the {@code daily_readiness_avg} continuous aggregate for fast response.
     *
     * @param studentId UUID string of the student
     * @param lastDays  number of calendar days to look back (inclusive)
     * @return ordered list of daily readiness snapshots, oldest first
     */
    List<DailyReadiness> getDailyReadiness(String studentId, int lastDays);

    record DailyReadiness(LocalDate day, BigDecimal avgErsScore, int snapshotCount) {}
}
