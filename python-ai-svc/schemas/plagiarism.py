"""Pydantic schemas for plagiarism detection endpoint."""

from typing import List, Tuple
from pydantic import BaseModel, Field


class FlaggedPair(BaseModel):
    candidateIndex: int = Field(..., description="Index into candidateTexts list")
    similarity: float = Field(..., ge=0.0, le=1.0)
    excerpt: str = Field(..., description="Short excerpt of the overlapping content")


class PlagiarismDetectRequest(BaseModel):
    text: str = Field(..., min_length=10, description="Primary text to check")
    candidateTexts: List[str] = Field(
        ..., min_length=1, description="Reference texts to compare against"
    )


class PlagiarismDetectResponse(BaseModel):
    isPlagiarized: bool
    maxSimilarity: float = Field(..., ge=0.0, le=1.0)
    flaggedPairs: List[FlaggedPair]
    modelVersion: str
