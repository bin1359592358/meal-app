"""Application configuration using pydantic-settings."""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables and .env file."""

    # Database
    TURSO_DATABASE_URL: str = ""
    TURSO_AUTH_TOKEN: str = ""

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = ""
    CLOUDINARY_API_KEY: str = ""
    CLOUDINARY_API_SECRET: str = ""

    # App
    SECRET_KEY: str = "dev-secret"
    TOKEN_EXPIRE_DAYS: int = 30

    @property
    def is_local_db(self) -> bool:
        """Return True if using local SQLite (no Turso URL configured)."""
        return not self.TURSO_DATABASE_URL

    @property
    def database_url(self) -> str:
        """Return the appropriate database URL."""
        if self.is_local_db:
            return "sqlite:///./meal_app.db"
        return self.TURSO_DATABASE_URL

    @property
    def is_cloudinary_configured(self) -> bool:
        """Return True if Cloudinary credentials are set."""
        return bool(
            self.CLOUDINARY_CLOUD_NAME
            and self.CLOUDINARY_API_KEY
            and self.CLOUDINARY_API_SECRET
        )

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
