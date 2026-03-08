"""Sentence embedding wrapper for plagiarism detection.

Lazy-loads the SentenceTransformer model on first use. Falls back to
TF-IDF cosine similarity when sentence-transformers is not installed,
so the service remains functional in lightweight environments.
"""

from __future__ import annotations

import logging
from typing import List

import numpy as np

logger = logging.getLogger(__name__)


class EmbeddingModel:
    """Compute sentence embeddings or TF-IDF vectors for similarity checks."""

    def __init__(self, model_name: str = "all-MiniLM-L6-v2") -> None:
        self._model_name = model_name
        self._st_model = None
        self._tfidf = None
        self._use_sentence_transformers = False
        self._initialized = False

    def _lazy_init(self) -> None:
        if self._initialized:
            return
        self._initialized = True

        try:
            from sentence_transformers import SentenceTransformer  # type: ignore

            self._st_model = SentenceTransformer(self._model_name)
            self._use_sentence_transformers = True
            logger.info("EmbeddingModel: using SentenceTransformer '%s'", self._model_name)
        except Exception as exc:
            logger.warning(
                "EmbeddingModel: SentenceTransformer unavailable (%s) — falling back to TF-IDF",
                exc,
            )
            try:
                from sklearn.feature_extraction.text import TfidfVectorizer  # type: ignore

                self._tfidf = TfidfVectorizer(
                    ngram_range=(1, 2), max_features=10_000, strip_accents="unicode"
                )
                logger.info("EmbeddingModel: TF-IDF fallback ready")
            except ImportError:
                logger.error("EmbeddingModel: neither sentence-transformers nor scikit-learn available")

    def encode(self, texts: List[str]) -> np.ndarray:
        """Return an (N, D) float32 embedding matrix for the given texts."""
        self._lazy_init()

        if self._use_sentence_transformers and self._st_model is not None:
            embeddings = self._st_model.encode(texts, normalize_embeddings=True, show_progress_bar=False)
            return np.array(embeddings, dtype=np.float32)

        if self._tfidf is not None:
            matrix = self._tfidf.fit_transform(texts).toarray().astype(np.float32)
            # L2-normalise rows
            norms = np.linalg.norm(matrix, axis=1, keepdims=True)
            norms = np.where(norms == 0, 1.0, norms)
            return matrix / norms

        # Last-resort: random unit vectors (service stays up, similarity is meaningless)
        logger.error("EmbeddingModel: no backend available — returning random embeddings")
        n = len(texts)
        raw = np.random.default_rng(0).standard_normal((n, 128)).astype(np.float32)
        norms = np.linalg.norm(raw, axis=1, keepdims=True)
        return raw / norms

    def cosine_similarity(self, a: np.ndarray, b: np.ndarray) -> float:
        """Cosine similarity between two already-normalised vectors."""
        return float(np.dot(a, b))
