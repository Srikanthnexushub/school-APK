"""Pydantic schemas for dropout risk prediction endpoint."""

from typing import List, Literal
from pydantic import BaseModel, Field


class DropoutRiskRequest(BaseModel):
    studentId: str = Field(..., description="Student identifier")
    ersScore: float = Field(..., ge=0.0, le=100.0, description="Exam Readiness Score 0–100")
    attendanceRate: float = Field(..., ge=0.0, le=1.0, description="Fraction 0.0–1.0")
    weeklyStudyHours: float = Field(..., ge=0.0, description="Average hours of study per week")
    accuracyRate: float = Field(..., ge=0.0, le=1.0, description="Fraction of correct answers")
    missedSessions: int = Field(..., ge=0, description="Number of sessions missed")


class DropoutRiskResponse(BaseModel):
    riskScore: float = Field(..., ge=0.0, le=1.0, description="Continuous risk probability")
    riskLevel: Literal["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    factors: List[str] = Field(..., description="Contributing risk factors")
    recommendation: str = Field(..., description="Actionable recommendation")
    modelVersion: str
