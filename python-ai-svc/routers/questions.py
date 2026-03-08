"""Question generation endpoint — template-based with subject/topic/difficulty variation."""

from __future__ import annotations

import logging
import random
from typing import Any, Dict, List, Optional

from fastapi import APIRouter

from config import settings
from schemas.questions import (
    Difficulty,
    GeneratedQuestion,
    GenerateQuestionsRequest,
    GenerateQuestionsResponse,
    IrtParams,
    QuestionType,
)

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/v1", tags=["questions"])

# ---------------------------------------------------------------------------
# IRT parameter ranges per difficulty
# ---------------------------------------------------------------------------

_IRT_RANGES: Dict[str, Dict[str, tuple]] = {
    "EASY":   {"b": (-2.5, -0.5), "a": (0.8, 1.5),  "c": (0.20, 0.30)},
    "MEDIUM": {"b": (-0.5,  0.5), "a": (1.2, 2.0),  "c": (0.10, 0.25)},
    "HARD":   {"b": ( 0.5,  2.5), "a": (1.5, 2.5),  "c": (0.05, 0.20)},
}


def _random_irt(difficulty: Difficulty, rng: random.Random) -> IrtParams:
    ranges = _IRT_RANGES[difficulty]
    return IrtParams(
        difficulty=round(rng.uniform(*ranges["b"]), 2),
        discrimination=round(rng.uniform(*ranges["a"]), 2),
        guessing=round(rng.uniform(*ranges["c"]), 2),
    )


# ---------------------------------------------------------------------------
# Question templates per type
# ---------------------------------------------------------------------------

def _make_mcq(subject: str, topic: str, difficulty: Difficulty, rng: random.Random) -> Dict[str, Any]:
    stem_templates = [
        f"Which of the following best describes a key concept in {topic}?",
        f"What is the primary principle underlying {topic} in {subject}?",
        f"In the context of {subject}, which statement about {topic} is correct?",
        f"A student studying {topic} would most likely encounter which of the following?",
    ]
    stem = rng.choice(stem_templates)

    correct_answer = f"The foundational principle of {topic} that governs its application in {subject}."
    distractors = [
        f"A common misconception about {topic} that ignores {subject} fundamentals.",
        f"An unrelated concept from a different area of {subject}.",
        f"A partially correct statement that overlooks a critical aspect of {topic}.",
    ]
    options = [correct_answer] + distractors
    rng.shuffle(options)
    correct_letter = chr(ord("A") + options.index(correct_answer))

    return {
        "text": stem,
        "type": "MCQ",
        "options": [f"{chr(ord('A') + i)}. {opt}" for i, opt in enumerate(options)],
        "correctAnswer": correct_letter,
        "explanation": (
            f"Option {correct_letter} correctly states the foundational principle of {topic}. "
            f"The distractors contain common errors students make when studying {subject}."
        ),
    }


def _make_true_false(subject: str, topic: str, difficulty: Difficulty, rng: random.Random) -> Dict[str, Any]:
    true_templates = [
        (f"{topic} is a fundamental concept within {subject}.", "True",
         f"This statement is correct — {topic} is indeed foundational to {subject}."),
        (f"Understanding {topic} requires prior knowledge of core {subject} principles.", "True",
         f"True — {topic} builds on foundational {subject} knowledge."),
    ]
    false_templates = [
        (f"{topic} has no practical application in modern {subject}.", "False",
         f"False — {topic} has widespread and important applications in {subject}."),
        (f"All problems in {topic} can be solved using only basic {subject} concepts.", "False",
         f"False — advanced {topic} scenarios require deeper {subject} knowledge."),
    ]
    pool = true_templates + false_templates
    text, answer, explanation = rng.choice(pool)
    return {"text": text, "type": "TRUE_FALSE", "options": ["True", "False"],
            "correctAnswer": answer, "explanation": explanation}


def _make_short_answer(subject: str, topic: str, difficulty: Difficulty, rng: random.Random) -> Dict[str, Any]:
    prompts = [
        f"In 2–3 sentences, explain the significance of {topic} within {subject}.",
        f"Describe one real-world application of {topic} in {subject}.",
        f"What are two key properties of {topic} that distinguish it from related concepts in {subject}?",
    ]
    text = rng.choice(prompts)
    return {
        "text": text,
        "type": "SHORT_ANSWER",
        "options": None,
        "correctAnswer": (
            f"A complete answer should explain the core purpose of {topic} in {subject}, "
            f"provide at least one concrete example, and demonstrate conceptual understanding."
        ),
        "explanation": (
            f"Short-answer questions on {topic} assess the student's ability to articulate "
            f"understanding beyond rote memorisation."
        ),
    }


def _make_essay(subject: str, topic: str, difficulty: Difficulty, rng: random.Random) -> Dict[str, Any]:
    prompts = [
        f"Discuss the evolution and current relevance of {topic} in the field of {subject}. "
        f"Support your argument with examples.",
        f"Critically evaluate the role of {topic} in shaping modern {subject}. "
        f"Include both advantages and limitations.",
    ]
    text = rng.choice(prompts)
    return {
        "text": text,
        "type": "ESSAY",
        "options": None,
        "correctAnswer": (
            f"A high-scoring essay on {topic} should include: a clear thesis, historical context, "
            f"analysis of key principles, concrete examples from {subject}, and a nuanced conclusion."
        ),
        "explanation": (
            f"This essay question evaluates higher-order thinking skills — analysis, synthesis, "
            f"and evaluation — within the domain of {topic} in {subject}."
        ),
    }


def _make_fill_blank(subject: str, topic: str, difficulty: Difficulty, rng: random.Random) -> Dict[str, Any]:
    sentences = [
        (f"The process of _______ is central to understanding {topic} in {subject}.",
         f"applying {topic} principles"),
        (f"In {subject}, {topic} is primarily characterised by its _______ properties.",
         "fundamental"),
        (f"A key challenge in {topic} is ensuring _______ while maintaining {subject} standards.",
         "accuracy and consistency"),
    ]
    text, answer = rng.choice(sentences)
    return {
        "text": text,
        "type": "FILL_BLANK",
        "options": None,
        "correctAnswer": answer,
        "explanation": f"The blank tests recall of a core concept linking {topic} to {subject}.",
    }


_GENERATORS = {
    "MCQ": _make_mcq,
    "TRUE_FALSE": _make_true_false,
    "SHORT_ANSWER": _make_short_answer,
    "ESSAY": _make_essay,
    "FILL_BLANK": _make_fill_blank,
}


@router.post("/generate-questions", response_model=GenerateQuestionsResponse)
def generate_questions(request: GenerateQuestionsRequest) -> GenerateQuestionsResponse:
    logger.info(
        "generate-questions: subject=%s topic=%s difficulty=%s count=%d types=%s",
        request.subject, request.topic, request.difficulty, request.count, request.questionTypes,
    )

    rng = random.Random()
    questions: List[GeneratedQuestion] = []
    type_cycle = list(request.questionTypes)

    for i in range(request.count):
        q_type: QuestionType = type_cycle[i % len(type_cycle)]
        generator = _GENERATORS[q_type]
        data = generator(request.subject, request.topic, request.difficulty, rng)
        irt = _random_irt(request.difficulty, rng)

        questions.append(
            GeneratedQuestion(
                text=data["text"],
                type=q_type,
                options=data.get("options"),
                correctAnswer=data["correctAnswer"],
                explanation=data["explanation"],
                irtParams=irt,
            )
        )

    return GenerateQuestionsResponse(
        questions=questions,
        modelVersion=settings.model_version,
    )
