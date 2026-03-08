"""Pydantic schemas for essay grading endpoint."""

from typing import Dict, Any, Optional
from pydantic import BaseModel, Field


class EssayGradeRequest(BaseModel):
    studentId: str
    questionText: str = Field(..., min_length=5)
    answerText: str = Field(..., min_length=1)
    maxScore: float = Field(..., gt=0.0)
    rubric: Dict[str, Any] = Field(
        ...,
        description="Rubric dict, e.g. {'keywords': [...], 'expectedLength': 300, 'criteria': {...}}",
    )


class EssayGradeResponse(BaseModel):
    score: float = Field(..., description="Awarded score")
    feedback: str = Field(..., description="Short overall feedback")
    detailedFeedback: Dict[str, Any] = Field(..., description="Per-criterion breakdown")
    modelVersion: str
