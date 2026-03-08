"""Tests for /health endpoint."""

import pytest
from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def test_health_returns_200() -> None:
    response = client.get("/health")
    assert response.status_code == 200


def test_health_response_structure() -> None:
    response = client.get("/health")
    data = response.json()
    assert data["status"] == "healthy"
    assert "modelVersion" in data
    assert isinstance(data["modelVersion"], str)
    assert len(data["modelVersion"]) > 0
