"""EduTech Python AI Sidecar Service.

Entry point — creates the FastAPI application, registers all routers,
configures CORS (gateway handles enforcement in prod) and structured logging.
"""

import logging
import logging.config
import os
from contextlib import asynccontextmanager
from typing import AsyncGenerator

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from config import settings
from routers import career, grading, plagiarism, questions, risk

# ---------------------------------------------------------------------------
# Logging setup
# ---------------------------------------------------------------------------

LOGGING_CONFIG = {
    "version": 1,
    "disable_existing_loggers": False,
    "formatters": {
        "json_like": {
            "format": '{"time": "%(asctime)s", "level": "%(levelname)s", "logger": "%(name)s", "message": "%(message)s"}',
            "datefmt": "%Y-%m-%dT%H:%M:%S",
        }
    },
    "handlers": {
        "console": {
            "class": "logging.StreamHandler",
            "formatter": "json_like",
            "stream": "ext://sys.stdout",
        }
    },
    "root": {"level": settings.log_level.upper(), "handlers": ["console"]},
}

logging.config.dictConfig(LOGGING_CONFIG)
logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# Application lifecycle
# ---------------------------------------------------------------------------

@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    logger.info(
        "EduTech Python AI Sidecar starting — version=%s port=%d",
        settings.model_version,
        settings.python_ai_svc_port,
    )
    yield
    logger.info("EduTech Python AI Sidecar shutting down")


# ---------------------------------------------------------------------------
# FastAPI application
# ---------------------------------------------------------------------------

app = FastAPI(
    title="EduTech Python AI Sidecar",
    description=(
        "Self-contained ML sidecar service providing career prediction, "
        "dropout risk scoring, psychometric analysis, essay grading, "
        "plagiarism detection, and question generation."
    ),
    version=settings.model_version,
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)

# CORS — allow all origins; the API gateway enforces restrictions in production
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ---------------------------------------------------------------------------
# Routers
# ---------------------------------------------------------------------------

app.include_router(career.router)
app.include_router(risk.router)
app.include_router(grading.router)
app.include_router(plagiarism.router)
app.include_router(questions.router)


# ---------------------------------------------------------------------------
# Health endpoint
# ---------------------------------------------------------------------------

@app.get("/health", tags=["ops"])
def health_check() -> dict:
    return {"status": "healthy", "modelVersion": settings.model_version}


# ---------------------------------------------------------------------------
# Entrypoint
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=settings.python_ai_svc_port,
        log_config=None,  # use our own logging config
        access_log=True,
    )
