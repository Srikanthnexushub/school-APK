"""Dropout risk prediction endpoint."""

import logging

from fastapi import APIRouter

from config import settings
from models.risk_model import DropoutRiskModel
from schemas.risk import DropoutRiskRequest, DropoutRiskResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["risk"])

_risk_model = DropoutRiskModel()


@router.post("/predict-dropout-risk", response_model=DropoutRiskResponse)
def predict_dropout_risk(request: DropoutRiskRequest) -> DropoutRiskResponse:
    logger.info(
        "predict-dropout-risk called for studentId=%s ers=%.1f attendance=%.2f",
        request.studentId,
        request.ersScore,
        request.attendanceRate,
    )

    risk_score, risk_level, factors, recommendation = _risk_model.predict(
        ers_score=request.ersScore,
        attendance_rate=request.attendanceRate,
        weekly_study_hours=request.weeklyStudyHours,
        accuracy_rate=request.accuracyRate,
        missed_sessions=request.missedSessions,
    )

    return DropoutRiskResponse(
        riskScore=round(risk_score, 4),
        riskLevel=risk_level,
        factors=factors,
        recommendation=recommendation,
        modelVersion=settings.model_version,
    )
