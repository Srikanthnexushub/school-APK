"""Pydantic schemas for question generation endpoint."""

from typing import List, Optional, Dict, Any, Literal
from pydantic import BaseModel, Field


QuestionType = Literal["MCQ", "TRUE_FALSE", "SHORT_ANSWER", "ESSAY", "FILL_BLANK"]
Difficulty = Literal["EASY", "MEDIUM", "HARD"]


class GenerateQuestionsRequest(BaseModel):
    subject: str = Field(..., min_length=1)
    topic: str = Field(..., min_length=1)
    difficulty: Difficulty
    count: int = Field(..., ge=1, le=20)
    questionTypes: List[QuestionType] = Field(..., min_length=1)


class IrtParams(BaseModel):
    difficulty: float = Field(..., description="IRT b-parameter (-3 to 3)")
    discrimination: float = Field(..., ge=0.0, description="IRT a-parameter")
    guessing: float = Field(..., ge=0.0, le=1.0, description="IRT c-parameter")


class GeneratedQuestion(BaseModel):
    text: str
    type: QuestionType
    options: Optional[List[str]] = None
    correctAnswer: str
    explanation: str
    irtParams: IrtParams


class GenerateQuestionsResponse(BaseModel):
    questions: List[GeneratedQuestion]
    modelVersion: str
