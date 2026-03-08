"""Tests for /api/v1/predict-dropout-risk."""

import pytest
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)

_LOW_RISK_PAYLOAD = {
    "studentId": "stu-001",
    "ersScore": 82.0,
    "attendanceRate": 0.95,
    "weeklyStudyHours": 18.0,
    "accuracyRate": 0.88,
    "missedSessions": 1,
}

_HIGH_RISK_PAYLOAD = {
    "studentId": "stu-002",
    "ersScore": 18.0,
    "attendanceRate": 0.30,
    "weeklyStudyHours": 2.5,
    "accuracyRate": 0.20,
    "missedSessions": 14,
}


def test_dropout_risk_returns_200() -> None:
    response = client.post("/api/v1/predict-dropout-risk", json=_LOW_RISK_PAYLOAD)
    assert response.status_code == 200


def test_dropout_risk_response_structure() -> None:
    response = client.post("/api/v1/predict-dropout-risk", json=_LOW_RISK_PAYLOAD)
    data = response.json()
    assert "riskScore" in data
    assert "riskLevel" in data
    assert "factors" in data
    assert "recommendation" in data
    assert "modelVersion" in data
    assert data["riskLevel"] in {"LOW", "MEDIUM", "HIGH", "CRITICAL"}
    assert 0.0 <= data["riskScore"] <= 1.0


def test_low_risk_student_classified_correctly() -> None:
    response = client.post("/api/v1/predict-dropout-risk", json=_LOW_RISK_PAYLOAD)
    data = response.json()
    assert data["riskLevel"] in {"LOW", "MEDIUM"}, (
        f"Expected LOW or MEDIUM for a well-performing student, got {data['riskLevel']}"
    )


def test_high_risk_student_classified_correctly() -> None:
    response = client.post("/api/v1/predict-dropout-risk", json=_HIGH_RISK_PAYLOAD)
    data = response.json()
    assert data["riskLevel"] in {"HIGH", "CRITICAL"}, (
        f"Expected HIGH or CRITICAL for a struggling student, got {data['riskLevel']}"
    )


def test_dropout_risk_factors_are_non_empty() -> None:
    response = client.post("/api/v1/predict-dropout-risk", json=_HIGH_RISK_PAYLOAD)
    data = response.json()
    assert len(data["factors"]) >= 1
    assert all(isinstance(f, str) for f in data["factors"])
