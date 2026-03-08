"""Career prediction and psychometric analysis endpoints."""

import logging
from typing import List

from fastapi import APIRouter

from config import settings
from models.career_model import CareerPredictor, RIASEC_DOMAIN
from schemas.career import (
    CareerPredictRequest,
    CareerPredictResponse,
    PsychometricAnalysisRequest,
    PsychometricAnalysisResponse,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["career"])

# Module-level singleton — trained once at import time
_predictor = CareerPredictor()


# ---------------------------------------------------------------------------
# Personality archetype helpers
# ---------------------------------------------------------------------------

def _derive_personality_type(
    openness: float,
    conscientiousness: float,
    extraversion: float,
    agreeableness: float,
    neuroticism: float,
) -> str:
    scores = {
        "The Innovator": openness * 0.5 + conscientiousness * 0.2 + extraversion * 0.3,
        "The Analyst":   openness * 0.4 + conscientiousness * 0.4 + (1 - extraversion) * 0.2,
        "The Leader":    extraversion * 0.5 + conscientiousness * 0.3 + (1 - neuroticism) * 0.2,
        "The Caregiver": agreeableness * 0.5 + extraversion * 0.3 + (1 - neuroticism) * 0.2,
        "The Organiser": conscientiousness * 0.6 + (1 - neuroticism) * 0.2 + agreeableness * 0.2,
        "The Creator":   openness * 0.6 + extraversion * 0.2 + agreeableness * 0.2,
    }
    return max(scores, key=lambda k: scores[k])


def _derive_learning_style(
    openness: float,
    extraversion: float,
    conscientiousness: float,
) -> str:
    if extraversion > 0.65 and openness > 0.55:
        return "Collaborative / Discussion-based"
    if openness > 0.70:
        return "Exploratory / Self-directed"
    if conscientiousness > 0.70:
        return "Structured / Step-by-step"
    if extraversion < 0.40:
        return "Reflective / Individual study"
    return "Multimodal / Adaptive"


def _derive_strengths(
    openness: float,
    conscientiousness: float,
    extraversion: float,
    agreeableness: float,
    neuroticism: float,
) -> List[str]:
    strengths = []
    if openness > 0.60:
        strengths.append("Creative thinking and curiosity")
    if conscientiousness > 0.65:
        strengths.append("Discipline and goal orientation")
    if extraversion > 0.60:
        strengths.append("Communication and networking")
    if agreeableness > 0.65:
        strengths.append("Teamwork and empathy")
    if neuroticism < 0.35:
        strengths.append("Emotional resilience under pressure")
    if not strengths:
        strengths.append("Adaptability and balanced perspective")
    return strengths


def _derive_challenges(
    openness: float,
    conscientiousness: float,
    extraversion: float,
    agreeableness: float,
    neuroticism: float,
) -> List[str]:
    challenges = []
    if openness < 0.35:
        challenges.append("Resistance to novel approaches")
    if conscientiousness < 0.35:
        challenges.append("Difficulty with long-term planning")
    if extraversion < 0.30:
        challenges.append("Tendency to avoid networking opportunities")
    if agreeableness < 0.30:
        challenges.append("Potential interpersonal friction in teams")
    if neuroticism > 0.65:
        challenges.append("Susceptibility to stress and anxiety")
    if not challenges:
        challenges.append("Balancing multiple competing priorities")
    return challenges


# ---------------------------------------------------------------------------
# Routes
# ---------------------------------------------------------------------------

@router.post("/predict-careers", response_model=CareerPredictResponse)
def predict_careers(request: CareerPredictRequest) -> CareerPredictResponse:
    logger.info("predict-careers called for profileId=%s riasec=%s", request.profileId, request.riasecCode)

    top_careers, reasoning = _predictor.predict(
        openness=request.openness,
        conscientiousness=request.conscientiousness,
        extraversion=request.extraversion,
        agreeableness=request.agreeableness,
        neuroticism=request.neuroticism,
        riasec_code=request.riasecCode,
        top_n=5,
    )

    return CareerPredictResponse(
        topCareers=top_careers,
        reasoning=reasoning,
        modelVersion=settings.model_version,
    )


@router.post("/analyze-psychometric", response_model=PsychometricAnalysisResponse)
def analyze_psychometric(request: PsychometricAnalysisRequest) -> PsychometricAnalysisResponse:
    logger.info("analyze-psychometric called for profileId=%s", request.profileId)

    personality_type = _derive_personality_type(
        request.openness, request.conscientiousness,
        request.extraversion, request.agreeableness, request.neuroticism,
    )
    learning_style = _derive_learning_style(
        request.openness, request.extraversion, request.conscientiousness,
    )
    strengths = _derive_strengths(
        request.openness, request.conscientiousness,
        request.extraversion, request.agreeableness, request.neuroticism,
    )
    challenges = _derive_challenges(
        request.openness, request.conscientiousness,
        request.extraversion, request.agreeableness, request.neuroticism,
    )

    # Career alignment from RIASEC code
    career_alignment = [
        RIASEC_DOMAIN[letter]
        for letter in request.riasecCode
        if letter in RIASEC_DOMAIN
    ]
    if not career_alignment:
        career_alignment = ["General / Undecided"]

    return PsychometricAnalysisResponse(
        personalityType=personality_type,
        learningStyle=learning_style,
        strengths=strengths,
        challenges=challenges,
        careerAlignment=career_alignment,
        modelVersion=settings.model_version,
    )
