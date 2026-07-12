"""Application configuration using pydantic-settings."""

from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Application settings loaded from environment variables and .env file."""

    # Database
    TURSO_DATABASE_URL: str = ""
    TURSO_AUTH_TOKEN: str = ""
    DATABASE_URL: str = ""  # Override: e.g. sqlite:////data/meal_app.db

    # Cloudinary
    CLOUDINARY_CLOUD_NAME: str = ""
    CLOUDINARY_API_KEY: str = ""
    CLOUDINARY_API_SECRET: str = ""

    # App
    ENVIRONMENT: str = "development"
    AUTO_SEED: bool = False
    SECRET_KEY: str = "dev-secret"
    TOKEN_EXPIRE_DAYS: int = 30

    # WeChat Mini Program
    WX_APPID: str = ""
    WX_SECRET: str = ""

    @property
    def is_local_db(self) -> bool:
        """最终生效的数据库 URL 使用 SQLite 时返回 True。"""
        return self.database_scheme == "sqlite"

    @property
    def database_scheme(self) -> str:
        """返回最终生效数据库 URL 对应的 SQLAlchemy 方言。"""
        driver_name = self.database_url.split(":", 1)[0]
        return driver_name.split("+", 1)[0].lower()

    @property
    def database_url(self) -> str:
        """Return the appropriate database URL."""
        # Priority: DATABASE_URL env > Turso > local SQLite
        if self.DATABASE_URL:
            return self.DATABASE_URL
        if self.TURSO_DATABASE_URL:
            return self.TURSO_DATABASE_URL
        return "sqlite:///./meal_app.db"

    @property
    def is_cloudinary_configured(self) -> bool:
        """Return True if Cloudinary credentials are set."""
        return bool(
            self.CLOUDINARY_CLOUD_NAME
            and self.CLOUDINARY_API_KEY
            and self.CLOUDINARY_API_SECRET
        )

    @property
    def is_wechat_configured(self) -> bool:
        """Return True if WeChat Mini Program credentials are set."""
        return bool(self.WX_APPID and self.WX_SECRET)

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
