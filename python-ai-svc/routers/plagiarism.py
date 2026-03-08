"""Plagiarism detection endpoint."""

import logging
from typing import List

import numpy as np
from fastapi import APIRouter

from config import settings
from models.embedding_model import EmbeddingModel
from schemas.plagiarism import FlaggedPair, PlagiarismDetectRequest, PlagiarismDetectResponse

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["plagiarism"])

_embed_model = EmbeddingModel(model_name=settings.sentence_transformer_model)

_THRESHOLD = settings.plagiarism_similarity_threshold


def _short_excerpt(text: str, max_chars: int = 120) -> str:
    """Return a leading excerpt of the text for diagnostic display."""
    text = text.strip()
    if len(text) <= max_chars:
        return text
    return text[:max_chars].rsplit(" ", 1)[0] + "…"


@router.post("/detect-plagiarism", response_model=PlagiarismDetectResponse)
def detect_plagiarism(request: PlagiarismDetectRequest) -> PlagiarismDetectResponse:
    logger.info(
        "detect-plagiarism called — text length=%d, candidates=%d",
        len(request.text),
        len(request.candidateTexts),
    )

    all_texts: List[str] = [request.text] + request.candidateTexts
    embeddings = _embed_model.encode(all_texts)

    primary_vec: np.ndarray = embeddings[0]
    candidate_vecs: np.ndarray = embeddings[1:]

    flagged_pairs: List[FlaggedPair] = []
    max_similarity: float = 0.0

    for idx, cand_vec in enumerate(candidate_vecs):
        sim = float(np.dot(primary_vec, cand_vec))
        sim = float(np.clip(sim, 0.0, 1.0))
        if sim > max_similarity:
            max_similarity = sim
        if sim >= _THRESHOLD:
            flagged_pairs.append(
                FlaggedPair(
                    candidateIndex=idx,
                    similarity=round(sim, 4),
                    excerpt=_short_excerpt(request.candidateTexts[idx]),
                )
            )

    flagged_pairs.sort(key=lambda fp: fp.similarity, reverse=True)

    return PlagiarismDetectResponse(
        isPlagiarized=len(flagged_pairs) > 0,
        maxSimilarity=round(max_similarity, 4),
        flaggedPairs=flagged_pairs,
        modelVersion=settings.model_version,
    )
