# ── Stage 1: Build / dependency installation ────────────────────────────────
FROM python:3.11-slim AS builder

WORKDIR /build

# Install build tools needed for some Python packages (e.g. scipy wheels)
RUN apt-get update && apt-get install -y --no-install-recommends \
        build-essential \
        gcc \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .

RUN pip install --upgrade pip \
    && pip install --no-cache-dir --prefix=/install -r requirements.txt


# ── Stage 2: Runtime image ───────────────────────────────────────────────────
FROM python:3.11-slim AS runtime

# Non-root user for security
RUN groupadd -r appuser && useradd -r -g appuser appuser

WORKDIR /app

# Copy installed packages from builder
COPY --from=builder /install /usr/local

# Copy application source
COPY config.py       ./config.py
COPY main.py         ./main.py
COPY routers/        ./routers/
COPY models/         ./models/
COPY schemas/        ./schemas/

# Directory for optional pre-trained model files
RUN mkdir -p /app/model_artifacts && chown appuser:appuser /app/model_artifacts

USER appuser

# Env var defaults (override at runtime — no secrets baked in)
ENV PYTHON_AI_SVC_PORT=8090
ENV MODEL_VERSION=v1.0.0
ENV LOG_LEVEL=INFO

EXPOSE 8090

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8090/health')" \
    || exit 1

CMD ["python", "main.py"]
