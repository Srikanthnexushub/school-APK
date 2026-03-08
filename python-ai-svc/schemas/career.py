"""Pydantic schemas for career prediction and psychometric analysis endpoints."""

from typing import List
from pydantic import BaseModel, Field, field_validator


class CareerPredictRequest(BaseModel):
    profileId: str = Field(..., description="UUID of the psych profile")
    openness: float = Field(..., ge=0.0, le=1.0)
    conscientiousness: float = Field(..., ge=0.0, le=1.0)
    extraversion: float = Field(..., ge=0.0, le=1.0)
    agreeableness: float = Field(..., ge=0.0, le=1.0)
    neuroticism: float = Field(..., ge=0.0, le=1.0)
    riasecCode: str = Field(..., min_length=1, max_length=6, description="RIASEC code e.g. 'RIASEC'")

    @field_validator("riasecCode")
    @classmethod
    def validate_riasec(cls, v: str) -> str:
        valid_chars = set("RIASEC")
        if not all(c in valid_chars for c in v.upper()):
            raise ValueError("riasecCode must only contain letters from R, I, A, S, E, C")
        return v.upper()


class CareerPredictResponse(BaseModel):
    topCareers: List[str] = Field(..., description="Ordered list of recommended career paths")
    reasoning: str = Field(..., description="Human-readable explanation of recommendations")
    modelVersion: str = Field(..., description="Model version identifier")


class PsychometricAnalysisRequest(BaseModel):
    profileId: str
    openness: float = Field(..., ge=0.0, le=1.0)
    conscientiousness: float = Field(..., ge=0.0, le=1.0)
    extraversion: float = Field(..., ge=0.0, le=1.0)
    agreeableness: float = Field(..., ge=0.0, le=1.0)
    neuroticism: float = Field(..., ge=0.0, le=1.0)
    riasecCode: str = Field(..., min_length=1, max_length=6)

    @field_validator("riasecCode")
    @classmethod
    def validate_riasec(cls, v: str) -> str:
        valid_chars = set("RIASEC")
        if not all(c in valid_chars for c in v.upper()):
            raise ValueError("riasecCode must only contain letters from R, I, A, S, E, C")
        return v.upper()


class PsychometricAnalysisResponse(BaseModel):
    personalityType: str = Field(..., description="Derived personality archetype")
    learningStyle: str = Field(..., description="Preferred learning modality")
    strengths: List[str]
    challenges: List[str]
    careerAlignment: List[str] = Field(..., description="Career domains well-aligned to profile")
    modelVersion: str
