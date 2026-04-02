package com.edutech.curricular.application.dto;

import java.util.List;

public record InstitutionSummaryResponse(List<CenterCoverageReport> centerReports, double platformAvgCoverage) {}
