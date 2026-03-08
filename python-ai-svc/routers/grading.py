"""Essay grading endpoint."""

import logging

from fastapi import APIRouter

from config import settings
from models.grading_model import EssayGrader
from schemas.grading import EssayGradeRequest, EssayGradeResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["grading"])

_grader = EssayGrader()


@router.post("/grade-essay", response_model=EssayGradeResponse)
def grade_essay(request: EssayGradeRequest) -> EssayGradeResponse:
    logger.info(
        "grade-essay called for studentId=%s maxScore=%.1f",
        request.studentId,
        request.maxScore,
    )

    awarded_score, feedback, detailed_feedback = _grader.grade(
        question_text=request.questionText,
        answer_text=request.answerText,
        max_score=request.maxScore,
        rubric=request.rubric,
    )

    return EssayGradeResponse(
        score=awarded_score,
        feedback=feedback,
        detailedFeedback=detailed_feedback,
        modelVersion=settings.model_version,
    )
