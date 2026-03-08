"""Career prediction model.

Uses a weighted rule-based scoring system that maps Big Five personality
traits and RIASEC codes to career domains, then ranks specific careers
within each domain.  A scikit-learn Random Forest is trained on synthetic
data at startup and used when available; the rule-based scorer is always
run as a fallback / cross-check.
"""

from __future__ import annotations

import logging
import os
import pickle
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Career domain definitions
# ---------------------------------------------------------------------------

# RIASEC letter → primary career domain label
RIASEC_DOMAIN: Dict[str, str] = {
    "R": "Engineering & Trades",
    "I": "Science & Research",
    "A": "Arts & Media",
    "S": "Education & Healthcare",
    "E": "Business & Law",
    "C": "Finance & Administration",
}

# For each RIASEC domain: list of concrete career titles to recommend
DOMAIN_CAREERS: Dict[str, List[str]] = {
    "Engineering & Trades": [
        "Software Engineer",
        "Mechanical Engineer",
        "Civil Engineer",
        "Electrical Engineer",
        "Data Engineer",
        "DevOps Engineer",
        "Robotics Engineer",
        "Construction Manager",
    ],
    "Science & Research": [
        "Data Scientist",
        "Research Scientist",
        "Biologist",
        "Physicist",
        "Chemist",
        "Epidemiologist",
        "Environmental Scientist",
        "Neuroscientist",
    ],
    "Arts & Media": [
        "Graphic Designer",
        "UX/UI Designer",
        "Journalist",
        "Content Creator",
        "Animator",
        "Photographer",
        "Musician",
        "Film Director",
    ],
    "Education & Healthcare": [
        "Teacher",
        "School Counselor",
        "Nurse",
        "Social Worker",
        "Psychologist",
        "Physical Therapist",
        "Pediatrician",
        "Special Education Specialist",
    ],
    "Business & Law": [
        "Entrepreneur",
        "Marketing Manager",
        "Sales Director",
        "Lawyer",
        "Business Analyst",
        "Product Manager",
        "HR Manager",
        "Financial Advisor",
    ],
    "Finance & Administration": [
        "Accountant",
        "Financial Analyst",
        "Auditor",
        "Budget Analyst",
        "Tax Consultant",
        "Compliance Officer",
        "Operations Manager",
        "Administrative Director",
    ],
}

# Big Five trait boost per RIASEC domain.
# Each tuple: (openness, conscientiousness, extraversion, agreeableness, neuroticism_inverse)
# neuroticism is inverted: high emotional stability = high (1 - neuroticism) boosts certain careers.
TRAIT_WEIGHTS: Dict[str, Tuple[float, float, float, float, float]] = {
    "Engineering & Trades":       (0.30, 0.40, 0.10, 0.05, 0.15),
    "Science & Research":         (0.45, 0.35, 0.05, 0.05, 0.10),
    "Arts & Media":               (0.50, 0.10, 0.20, 0.10, 0.10),
    "Education & Healthcare":     (0.20, 0.25, 0.25, 0.25, 0.05),
    "Business & Law":             (0.15, 0.25, 0.35, 0.15, 0.10),
    "Finance & Administration":   (0.10, 0.50, 0.10, 0.10, 0.20),
}


def _big_five_score(
    domain: str,
    openness: float,
    conscientiousness: float,
    extraversion: float,
    agreeableness: float,
    neuroticism: float,
) -> float:
    """Compute a trait alignment score for a domain (0–1)."""
    w = TRAIT_WEIGHTS[domain]
    stability = 1.0 - neuroticism  # higher stability is generally positive
    raw = (
        w[0] * openness
        + w[1] * conscientiousness
        + w[2] * extraversion
        + w[3] * agreeableness
        + w[4] * stability
    )
    return float(np.clip(raw, 0.0, 1.0))


def _riasec_domain_scores(riasec_code: str) -> Dict[str, float]:
    """Assign a positional weight to each RIASEC letter in the code.

    The first letter in the code is the dominant interest (weight 1.0),
    subsequent letters decay by 0.2 per position.
    """
    scores: Dict[str, float] = {d: 0.0 for d in DOMAIN_CAREERS}
    for pos, letter in enumerate(riasec_code):
        domain = RIASEC_DOMAIN.get(letter)
        if domain and domain in scores:
            scores[domain] = max(scores[domain], max(0.0, 1.0 - pos * 0.2))
    return scores


class CareerPredictor:
    """Predict top career paths from Big Five + RIASEC inputs."""

    def __init__(self, model_path: str | None = None) -> None:
        self._rf_model = None
        self._label_map: List[str] = []  # index → domain name
        self._loaded_from_disk = False

        if model_path and Path(model_path).exists():
            try:
                with open(model_path, "rb") as fh:
                    saved = pickle.load(fh)
                self._rf_model = saved["model"]
                self._label_map = saved["label_map"]
                self._loaded_from_disk = True
                logger.info("CareerPredictor: loaded Random Forest from %s", model_path)
            except Exception as exc:
                logger.warning("CareerPredictor: failed to load model from disk — %s", exc)

        if not self._loaded_from_disk:
            self._train_synthetic()

    # ------------------------------------------------------------------
    # Training on synthetic data
    # ------------------------------------------------------------------

    def _train_synthetic(self) -> None:
        """Train a Random Forest on synthetically generated samples."""
        try:
            from sklearn.ensemble import RandomForestClassifier  # type: ignore
        except ImportError:
            logger.warning("scikit-learn not available — using rule-based fallback only")
            return

        domains = list(DOMAIN_CAREERS.keys())
        self._label_map = domains

        rng = np.random.default_rng(42)
        n_per_class = 200
        X_parts, y_parts = [], []

        domain_centers = {
            "Engineering & Trades":       [0.55, 0.75, 0.35, 0.30, 0.25],
            "Science & Research":         [0.75, 0.65, 0.30, 0.35, 0.30],
            "Arts & Media":               [0.80, 0.40, 0.55, 0.45, 0.40],
            "Education & Healthcare":     [0.50, 0.55, 0.65, 0.70, 0.35],
            "Business & Law":             [0.45, 0.60, 0.75, 0.55, 0.30],
            "Finance & Administration":   [0.35, 0.80, 0.40, 0.45, 0.20],
        }

        for idx, domain in enumerate(domains):
            center = np.array(domain_centers[domain])
            samples = rng.normal(loc=center, scale=0.15, size=(n_per_class, 5))
            samples = np.clip(samples, 0.0, 1.0)
            X_parts.append(samples)
            y_parts.append(np.full(n_per_class, idx, dtype=int))

        X = np.vstack(X_parts)
        y = np.concatenate(y_parts)

        clf = RandomForestClassifier(n_estimators=100, random_state=42, n_jobs=-1)
        clf.fit(X, y)
        self._rf_model = clf
        logger.info("CareerPredictor: trained Random Forest on %d synthetic samples", len(X))

    # ------------------------------------------------------------------
    # Prediction
    # ------------------------------------------------------------------

    def predict(
        self,
        openness: float,
        conscientiousness: float,
        extraversion: float,
        agreeableness: float,
        neuroticism: float,
        riasec_code: str,
        top_n: int = 5,
    ) -> Tuple[List[str], str]:
        """Return (top_careers, reasoning)."""

        # --- Step 1: Rule-based domain scoring ---
        riasec_scores = _riasec_domain_scores(riasec_code)
        trait_scores = {
            d: _big_five_score(d, openness, conscientiousness, extraversion, agreeableness, neuroticism)
            for d in DOMAIN_CAREERS
        }

        # Blend RIASEC (60%) + trait alignment (40%)
        combined: Dict[str, float] = {}
        for domain in DOMAIN_CAREERS:
            combined[domain] = 0.60 * riasec_scores[domain] + 0.40 * trait_scores[domain]

        # --- Step 2: RF model softmax probabilities (if available) ---
        if self._rf_model is not None:
            features = np.array([[openness, conscientiousness, extraversion, agreeableness, neuroticism]])
            proba = self._rf_model.predict_proba(features)[0]
            # Blend RF (50%) + rule-based (50%)
            for i, domain in enumerate(self._label_map):
                combined[domain] = 0.50 * proba[i] + 0.50 * combined.get(domain, 0.0)

        # --- Step 3: Rank domains, pick top careers ---
        sorted_domains = sorted(combined, key=lambda d: combined[d], reverse=True)

        top_careers: List[str] = []
        for domain in sorted_domains:
            careers = DOMAIN_CAREERS[domain]
            # Pick top career(s) from this domain weighted by score
            top_careers.append(careers[0])
            if len(top_careers) >= top_n:
                break

        # Fill remaining slots from runner-up domains if needed
        if len(top_careers) < top_n:
            for domain in sorted_domains:
                for career in DOMAIN_CAREERS[domain][1:]:
                    if career not in top_careers:
                        top_careers.append(career)
                    if len(top_careers) >= top_n:
                        break
                if len(top_careers) >= top_n:
                    break

        # --- Step 4: Build reasoning string ---
        dominant_letter = riasec_code[0] if riasec_code else "?"
        dominant_domain = RIASEC_DOMAIN.get(dominant_letter, "General")
        highest_trait = max(
            [
                ("Openness", openness),
                ("Conscientiousness", conscientiousness),
                ("Extraversion", extraversion),
                ("Agreeableness", agreeableness),
            ],
            key=lambda t: t[1],
        )
        reasoning = (
            f"Primary RIASEC interest '{dominant_letter}' maps to {dominant_domain}. "
            f"Highest Big Five trait is {highest_trait[0]} ({highest_trait[1]:.2f}), "
            f"which reinforces alignment with {sorted_domains[0]}. "
            f"Neuroticism at {neuroticism:.2f} {'may present emotional regulation challenges' if neuroticism > 0.6 else 'indicates good emotional stability'}. "
            f"Top {top_n} careers selected by blending RIASEC interest weights with Big Five trait alignment scores."
        )

        return top_careers[:top_n], reasoning
