"""Application configuration loaded from environment variables via pydantic-settings."""

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        extra="ignore",
        protected_namespaces=("settings_",),
    )

    # Server
    python_ai_svc_port: int = 8090
    log_level: str = "INFO"

    # Model versioning
    model_version: str = "v1.0.0"

    # Model paths (optional — used if pre-trained .pkl files are available)
    career_model_path: str = "/app/models/career_model.pkl"
    risk_model_path: str = "/app/models/risk_model.pkl"

    # Plagiarism
    plagiarism_similarity_threshold: float = 0.85
    sentence_transformer_model: str = "all-MiniLM-L6-v2"

    # Essay grading
    essay_min_words: int = 20
    essay_max_words: int = 2000


settings = Settings()
