"""Dropout risk prediction model.

Mirrors the Java DropoutRiskCalculator signal logic but adds a trained
logistic-regression layer on top of synthetic data for probabilistic output.
Falls back to a pure rule-based scorer when sklearn is unavailable.
"""

from __future__ import annotations

import logging
from typing import List, Tuple

import numpy as np

logger = logging.getLogger(__name__)

# Risk level thresholds
_THRESHOLDS = {
    "CRITICAL": 0.75,
    "HIGH":     0.50,
    "MEDIUM":   0.25,
    "LOW":      0.0,
}


def _risk_level(score: float) -> str:
    if score >= _THRESHOLDS["CRITICAL"]:
        return "CRITICAL"
    if score >= _THRESHOLDS["HIGH"]:
        return "HIGH"
    if score >= _THRESHOLDS["MEDIUM"]:
        return "MEDIUM"
    return "LOW"


_RECOMMENDATIONS = {
    "CRITICAL": (
        "Immediate intervention required. Schedule a one-on-one counselling session, "
        "assign a peer mentor, and notify the student's guardian."
    ),
    "HIGH": (
        "High dropout risk detected. Review study plan, increase check-in frequency, "
        "and consider adaptive content delivery."
    ),
    "MEDIUM": (
        "Moderate risk. Encourage consistent attendance, share performance analytics "
        "with the student, and suggest supplementary resources."
    ),
    "LOW": (
        "Student appears on track. Continue regular monitoring and celebrate milestones "
        "to maintain motivation."
    ),
}


class DropoutRiskModel:
    """Predict dropout risk from engagement & performance signals."""

    def __init__(self) -> None:
        self._lr_model = None
        self._trained = False
        self._train_synthetic()

    # ------------------------------------------------------------------
    # Training
    # ------------------------------------------------------------------

    def _train_synthetic(self) -> None:
        try:
            from sklearn.linear_model import LogisticRegression  # type: ignore
        except ImportError:
            logger.warning("scikit-learn not available — DropoutRiskModel using rule-based only")
            return

        rng = np.random.default_rng(99)
        n = 2000

        # Features: ers_score/100, attendance_rate, weekly_study_hours/40, accuracy_rate, missed_sessions/20
        # Low-risk students
        low_risk = np.column_stack([
            rng.uniform(60, 100, n // 4) / 100,
            rng.uniform(0.80, 1.00, n // 4),
            rng.uniform(15, 40,  n // 4) / 40,
            rng.uniform(0.70, 1.00, n // 4),
            rng.uniform(0,  3,    n // 4) / 20,
        ])
        # Medium-risk students
        med_risk = np.column_stack([
            rng.uniform(40, 70, n // 4) / 100,
            rng.uniform(0.60, 0.85, n // 4),
            rng.uniform(8,  20,  n // 4) / 40,
            rng.uniform(0.45, 0.70, n // 4),
            rng.uniform(3,  8,    n // 4) / 20,
        ])
        # High-risk students
        high_risk = np.column_stack([
            rng.uniform(20, 50, n // 4) / 100,
            rng.uniform(0.40, 0.65, n // 4),
            rng.uniform(4,  12,  n // 4) / 40,
            rng.uniform(0.25, 0.50, n // 4),
            rng.uniform(6,  14,   n // 4) / 20,
        ])
        # Critical-risk students
        crit_risk = np.column_stack([
            rng.uniform(0,  30, n // 4) / 100,
            rng.uniform(0.00, 0.45, n // 4),
            rng.uniform(0,   8,   n // 4) / 40,
            rng.uniform(0.00, 0.30, n // 4),
            rng.uniform(10, 20,   n // 4) / 20,
        ])

        X = np.vstack([low_risk, med_risk, high_risk, crit_risk])
        y = np.array(
            [0] * (n // 4) + [1] * (n // 4) + [2] * (n // 4) + [3] * (n // 4)
        )

        lr = LogisticRegression(max_iter=500, random_state=42)
        lr.fit(X, y)
        self._lr_model = lr
        self._trained = True
        logger.info("DropoutRiskModel: trained LogisticRegression on %d synthetic samples", n)

    # ------------------------------------------------------------------
    # Rule-based scorer (always runs; blended with ML when available)
    # ------------------------------------------------------------------

    def _rule_based_score(
        self,
        ers_score: float,
        attendance_rate: float,
        weekly_study_hours: float,
        accuracy_rate: float,
        missed_sessions: int,
    ) -> float:
        """Return a risk score in [0, 1] using weighted signal inversion."""
        # Normalise inputs to 0-1 danger scales (higher = more at risk)
        ers_danger = 1.0 - min(ers_score / 100.0, 1.0)
        attendance_danger = 1.0 - attendance_rate
        study_danger = 1.0 - min(weekly_study_hours / 20.0, 1.0)
        accuracy_danger = 1.0 - accuracy_rate
        missed_danger = min(missed_sessions / 15.0, 1.0)

        score = (
            0.25 * ers_danger
            + 0.25 * attendance_danger
            + 0.15 * study_danger
            + 0.20 * accuracy_danger
            + 0.15 * missed_danger
        )
        return float(np.clip(score, 0.0, 1.0))

    # ------------------------------------------------------------------
    # Contributing factors
    # ------------------------------------------------------------------

    def _identify_factors(
        self,
        ers_score: float,
        attendance_rate: float,
        weekly_study_hours: float,
        accuracy_rate: float,
        missed_sessions: int,
    ) -> List[str]:
        factors: List[str] = []
        if ers_score < 50:
            factors.append(f"Low exam readiness score ({ers_score:.1f}/100)")
        if attendance_rate < 0.75:
            factors.append(f"Poor attendance rate ({attendance_rate * 100:.1f}%)")
        if weekly_study_hours < 10:
            factors.append(f"Insufficient weekly study hours ({weekly_study_hours:.1f} hrs)")
        if accuracy_rate < 0.50:
            factors.append(f"Low answer accuracy ({accuracy_rate * 100:.1f}%)")
        if missed_sessions >= 5:
            factors.append(f"High number of missed sessions ({missed_sessions})")
        if not factors:
            factors.append("No significant risk factors detected")
        return factors

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def predict(
        self,
        ers_score: float,
        attendance_rate: float,
        weekly_study_hours: float,
        accuracy_rate: float,
        missed_sessions: int,
    ) -> Tuple[float, str, List[str], str]:
        """Return (risk_score, risk_level, factors, recommendation)."""

        rule_score = self._rule_based_score(
            ers_score, attendance_rate, weekly_study_hours, accuracy_rate, missed_sessions
        )

        if self._trained and self._lr_model is not None:
            features = np.array([[
                ers_score / 100.0,
                attendance_rate,
                min(weekly_study_hours / 40.0, 1.0),
                accuracy_rate,
                min(missed_sessions / 20.0, 1.0),
            ]])
            proba = self._lr_model.predict_proba(features)[0]
            # Classes: 0=LOW, 1=MEDIUM, 2=HIGH, 3=CRITICAL
            ml_score = float(0.0 * proba[0] + 0.33 * proba[1] + 0.66 * proba[2] + 1.0 * proba[3])
            final_score = float(np.clip(0.5 * ml_score + 0.5 * rule_score, 0.0, 1.0))
        else:
            final_score = rule_score

        level = _risk_level(final_score)
        factors = self._identify_factors(
            ers_score, attendance_rate, weekly_study_hours, accuracy_rate, missed_sessions
        )
        recommendation = _RECOMMENDATIONS[level]
        return final_score, level, factors, recommendation
