"""Application configuration using pydantic-settings."""

from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from pydantic_settings import BaseSettings
from sqlalchemy.engine import make_url


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
        return self.database_drivername in {"sqlite", "sqlite+pysqlite"}

    @property
    def database_drivername(self) -> str:
        """返回最终生效 URL 的完整 SQLAlchemy drivername。"""
        return make_url(self.database_url).drivername.lower()

    @property
    def database_url(self) -> str:
        """Return the appropriate database URL."""
        # Priority: DATABASE_URL env > Turso > local SQLite
        if self.DATABASE_URL:
            if self.DATABASE_URL.startswith("postgresql://"):
                return self.DATABASE_URL.replace(
                    "postgresql://", "postgresql+psycopg://", 1
                )
            return self.DATABASE_URL
        if self.TURSO_DATABASE_URL:
            return self._normalize_turso_url(self.TURSO_DATABASE_URL)
        return "sqlite:///./meal_app.db"

    @staticmethod
    def _normalize_turso_url(url: str) -> str:
        """将 Turso 常见 URL 转换为 sqlalchemy-libsql 可用格式。"""
        if url.startswith("libsql://"):
            url = url.replace("libsql://", "sqlite+libsql://", 1)
        parts = urlsplit(url)
        query = parse_qsl(parts.query, keep_blank_values=True)
        if not any(key.lower() == "secure" for key, _ in query):
            query.append(("secure", "true"))
        return urlunsplit(parts._replace(query=urlencode(query)))

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
