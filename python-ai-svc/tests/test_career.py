"""Tests for /api/v1/predict-careers and /api/v1/analyze-psychometric."""

import pytest
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)

_BASE_PAYLOAD = {
    "profileId": "550e8400-e29b-41d4-a716-446655440000",
    "openness": 0.75,
    "conscientiousness": 0.65,
    "extraversion": 0.40,
    "agreeableness": 0.55,
    "neuroticism": 0.30,
    "riasecCode": "ISA",
}


def test_predict_careers_returns_200() -> None:
    response = client.post("/api/v1/predict-careers", json=_BASE_PAYLOAD)
    assert response.status_code == 200


def test_predict_careers_response_structure() -> None:
    response = client.post("/api/v1/predict-careers", json=_BASE_PAYLOAD)
    data = response.json()
    assert "topCareers" in data
    assert "reasoning" in data
    assert "modelVersion" in data
    assert isinstance(data["topCareers"], list)
    assert len(data["topCareers"]) >= 1


def test_predict_careers_riasec_influences_results() -> None:
    """High-R (Realistic) profile should surface engineering/trades careers."""
    payload = {**_BASE_PAYLOAD, "riasecCode": "RIC", "openness": 0.40, "conscientiousness": 0.80}
    response = client.post("/api/v1/predict-careers", json=payload)
    assert response.status_code == 200
    data = response.json()
    # At least one engineering-related career expected
    engineering_keywords = {"engineer", "Engineer", "construction", "Construction", "DevOps"}
    top_careers_str = " ".join(data["topCareers"])
    assert any(kw in top_careers_str for kw in engineering_keywords)


def test_predict_careers_invalid_riasec_returns_422() -> None:
    payload = {**_BASE_PAYLOAD, "riasecCode": "XYZ"}
    response = client.post("/api/v1/predict-careers", json=payload)
    assert response.status_code == 422


def test_analyze_psychometric_returns_correct_fields() -> None:
    response = client.post("/api/v1/analyze-psychometric", json=_BASE_PAYLOAD)
    assert response.status_code == 200
    data = response.json()
    assert "personalityType" in data
    assert "learningStyle" in data
    assert isinstance(data["strengths"], list)
    assert isinstance(data["challenges"], list)
    assert isinstance(data["careerAlignment"], list)
    assert "modelVersion" in data
    assert len(data["careerAlignment"]) >= 1
