"""Database engine, session, and base model configuration."""

from collections.abc import Generator

from sqlalchemy import create_engine
from sqlalchemy.orm import Session, declarative_base, sessionmaker

from config import settings

engine_options = {"echo": False}
if settings.database_scheme == "sqlite":
    engine_options["connect_args"] = {"check_same_thread": False}
elif settings.database_scheme == "libsql":
    engine_options["connect_args"] = {"auth_token": settings.TURSO_AUTH_TOKEN}

engine = create_engine(settings.database_url, **engine_options)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

Base = declarative_base()


def get_db() -> Generator[Session, None, None]:
    """FastAPI dependency that yields a database session."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
