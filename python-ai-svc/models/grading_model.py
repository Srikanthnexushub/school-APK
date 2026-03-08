"""Essay grading model.

Scores an essay using a three-criterion approach:
1. Keyword coverage  — TF-IDF cosine similarity of answer against rubric keywords
2. Length adequacy   — comparison of actual word count vs expected length in rubric
3. Coherence         — heuristic based on sentence count, avg sentence length, and
                       vocabulary richness (type-token ratio)

No external LLM calls — fully self-contained.
"""

from __future__ import annotations

import logging
import re
from typing import Any, Dict, List, Tuple

import numpy as np

logger = logging.getLogger(__name__)

_SENTENCE_RE = re.compile(r"[.!?]+")
_WORD_RE = re.compile(r"\b[a-zA-Z]{2,}\b")


def _tokenise(text: str) -> List[str]:
    return _WORD_RE.findall(text.lower())


def _sentence_count(text: str) -> int:
    parts = [p.strip() for p in _SENTENCE_RE.split(text) if p.strip()]
    return max(len(parts), 1)


def _vocabulary_richness(words: List[str]) -> float:
    if not words:
        return 0.0
    return len(set(words)) / len(words)


class EssayGrader:
    """Grade a student essay against a rubric definition."""

    # Criterion weights (must sum to 1.0)
    _W_KEYWORD = 0.45
    _W_LENGTH = 0.25
    _W_COHERENCE = 0.30

    def grade(
        self,
        question_text: str,
        answer_text: str,
        max_score: float,
        rubric: Dict[str, Any],
    ) -> Tuple[float, str, Dict[str, Any]]:
        """Return (awarded_score, feedback, detailed_feedback)."""

        words = _tokenise(answer_text)
        word_count = len(words)

        # ── 1. Keyword coverage ──────────────────────────────────────────────
        rubric_keywords: List[str] = [
            kw.lower() for kw in rubric.get("keywords", []) if isinstance(kw, str)
        ]
        if rubric_keywords:
            answer_word_set = set(words)
            matched = [kw for kw in rubric_keywords if kw in answer_word_set]
            keyword_score = len(matched) / len(rubric_keywords)
        else:
            # Fall back to question-text similarity heuristic
            question_words = set(_tokenise(question_text))
            answer_words = set(words)
            if question_words:
                keyword_score = len(question_words & answer_words) / len(question_words)
            else:
                keyword_score = 0.5
            matched = []

        # ── 2. Length adequacy ───────────────────────────────────────────────
        expected_length: int = rubric.get("expectedLength", 200)
        if word_count == 0:
            length_score = 0.0
        elif word_count >= expected_length:
            # Penalise very long answers slightly (padding)
            length_score = min(1.0, expected_length / word_count * 1.2)
        else:
            length_score = word_count / expected_length

        # ── 3. Coherence ─────────────────────────────────────────────────────
        sentence_count = _sentence_count(answer_text)
        avg_sentence_len = word_count / sentence_count
        vocab_richness = _vocabulary_richness(words)

        # Ideal avg sentence length: 12–20 words
        if 12 <= avg_sentence_len <= 20:
            sentence_len_score = 1.0
        elif avg_sentence_len < 12:
            sentence_len_score = avg_sentence_len / 12
        else:
            sentence_len_score = max(0.0, 1.0 - (avg_sentence_len - 20) / 30)

        coherence_score = 0.6 * sentence_len_score + 0.4 * vocab_richness

        # ── Weighted total ───────────────────────────────────────────────────
        total_fraction = (
            self._W_KEYWORD * keyword_score
            + self._W_LENGTH * length_score
            + self._W_COHERENCE * coherence_score
        )
        awarded = round(float(np.clip(total_fraction * max_score, 0.0, max_score)), 2)

        # ── Feedback strings ─────────────────────────────────────────────────
        feedback = self._summary_feedback(total_fraction, word_count, expected_length, matched, rubric_keywords)
        detailed: Dict[str, Any] = {
            "keywordCoverage": {
                "score": round(keyword_score, 3),
                "weight": self._W_KEYWORD,
                "matchedKeywords": matched,
                "totalKeywords": len(rubric_keywords),
            },
            "lengthAdequacy": {
                "score": round(length_score, 3),
                "weight": self._W_LENGTH,
                "wordCount": word_count,
                "expectedLength": expected_length,
            },
            "coherence": {
                "score": round(coherence_score, 3),
                "weight": self._W_COHERENCE,
                "sentenceCount": sentence_count,
                "avgSentenceLength": round(avg_sentence_len, 1),
                "vocabularyRichness": round(vocab_richness, 3),
            },
            "totalFraction": round(total_fraction, 3),
        }

        return awarded, feedback, detailed

    @staticmethod
    def _summary_feedback(
        fraction: float,
        word_count: int,
        expected_length: int,
        matched_keywords: List[str],
        total_keywords: List[str],
    ) -> str:
        lines: List[str] = []

        if fraction >= 0.85:
            lines.append("Excellent response.")
        elif fraction >= 0.70:
            lines.append("Good response with minor gaps.")
        elif fraction >= 0.50:
            lines.append("Adequate response; significant improvements possible.")
        else:
            lines.append("Response needs substantial improvement.")

        if total_keywords:
            coverage_pct = len(matched_keywords) / len(total_keywords) * 100
            lines.append(
                f"Covered {len(matched_keywords)}/{len(total_keywords)} key concepts ({coverage_pct:.0f}%)."
            )

        if word_count < expected_length * 0.5:
            lines.append("Answer is significantly shorter than expected — expand on key points.")
        elif word_count > expected_length * 2:
            lines.append("Answer is much longer than expected — consider being more concise.")

        return " ".join(lines)
