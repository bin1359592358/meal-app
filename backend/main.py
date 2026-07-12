"""Meal App API - FastAPI application entry point."""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from config import settings
from database import Base, engine
from models import (  # noqa: F401  – ensure all models are registered
    Category,
    Dish,
    Order,
    OrderItem,
    Room,
    RoomMember,
    Session,
    User,
)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan: create tables, run migrations, and auto-seed on startup."""
    Base.metadata.create_all(bind=engine)
    # Run inline migrations for new columns
    _run_migrations(engine)
    # 演示数据必须显式开启，避免生产环境创建默认账号。
    if settings.AUTO_SEED:
        try:
            from seed_data import seed
            seed()
        except Exception as e:
            import logging
            logging.getLogger("seed").warning("Auto-seed skipped: %s", e)
    yield


def _run_migrations(eng) -> None:
    """Add missing columns and fix NOT NULL constraints on existing tables (SQLite compatible)."""
    import logging
    logger = logging.getLogger("migration")

    try:
        from sqlalchemy import text, inspect

        inspector = inspect(eng)
        if "users" not in inspector.get_table_names():
            return

        existing_cols = {c["name"] for c in inspector.get_columns("users")}

        # Step 1: Add missing columns (openid, avatar_url) via simple ALTER TABLE
        add_col_migrations = [
            ("openid", "VARCHAR(64)"),
            ("avatar_url", "VARCHAR(500)"),
        ]
        with eng.connect() as conn:
            for col_name, col_type in add_col_migrations:
                if col_name not in existing_cols:
                    conn.execute(
                        text(f'ALTER TABLE users ADD COLUMN "{col_name}" {col_type}')
                    )
                    conn.commit()
                    logger.info("Added column users.%s", col_name)

        # Step 2: Check if pin_hash or username have NOT NULL constraint that needs fixing
        # SQLite cannot ALTER COLUMN, so we must recreate the table if constraints are wrong
        cols_info = {c["name"]: c for c in inspector.get_columns("users")}
        needs_recreate = False
        if "pin_hash" in cols_info and not cols_info["pin_hash"].get("nullable", True):
            needs_recreate = True
        if "username" in cols_info and not cols_info["username"].get("nullable", True):
            needs_recreate = True

        if needs_recreate:
            with eng.connect() as conn:
                # Create new table with correct schema (nullable pin_hash & username)
                conn.execute(text("""
                    CREATE TABLE users_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        username VARCHAR(20) UNIQUE,
                        nickname VARCHAR(50) NOT NULL,
                        pin_hash VARCHAR(128),
                        openid VARCHAR(64) UNIQUE,
                        avatar_url VARCHAR(500),
                        created_at VARCHAR(30) NOT NULL
                    )
                """))

                # Copy data from old table, handling missing columns gracefully
                select_cols = []
                for col in ["id", "username", "nickname", "pin_hash", "openid", "avatar_url", "created_at"]:
                    if col in existing_cols:
                        select_cols.append(f'"{col}"')
                    else:
                        select_cols.append("NULL")
                select_sql = ", ".join(select_cols)

                conn.execute(text(f"""
                    INSERT INTO users_new (id, username, nickname, pin_hash, openid, avatar_url, created_at)
                    SELECT {select_sql} FROM users
                """))

                # Drop old table and rename new one
                conn.execute(text("DROP TABLE users"))
                conn.execute(text("ALTER TABLE users_new RENAME TO users"))

                # Recreate indexes
                conn.execute(text("CREATE INDEX IF NOT EXISTS ix_users_username ON users (username)"))
                conn.execute(text("CREATE INDEX IF NOT EXISTS ix_users_openid ON users (openid)"))

                conn.commit()
                logger.info("Recreated users table with nullable pin_hash/username columns")

    except Exception as e:
        logging.getLogger("migration").warning("Migration skipped: %s", e)


app = FastAPI(
    title="Meal App API",
    description="Backend API for the meal ordering application.",
    version="1.0.0",
    lifespan=lifespan,
)

# ──────────────────────────── CORS ────────────────────────────

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ──────────────────── Static files (local uploads) ────────────────────

_uploads_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "uploads")
os.makedirs(_uploads_dir, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=_uploads_dir), name="uploads")


# ──────────────────────────── Routers ────────────────────────────

# Import routers lazily to avoid circular imports at module level.
# Each router module is expected to expose an `APIRouter` named `router`.

def _include_routers() -> None:
    """Dynamically import and include all API routers."""
    import importlib
    from pathlib import Path

    routers_dir = Path(__file__).parent / "routers"
    if not routers_dir.is_dir():
        return

    for py_file in sorted(routers_dir.glob("*.py")):
        if py_file.name.startswith("_"):
            continue
        module_name = f"routers.{py_file.stem}"
        try:
            mod = importlib.import_module(module_name)
        except ImportError:
            continue
        router = getattr(mod, "router", None)
        if router is not None:
            app.include_router(router, prefix="/api")


_include_routers()


# ──────────────────────── Health check ────────────────────────


@app.get("/api/health", tags=["health"])
async def health_check():
    """Simple health check endpoint."""
    return {"status": "ok", "message": "Meal App API is running."}


# ──────────────────────── Direct execution ────────────────────────

if __name__ == "__main__":
    import uvicorn

    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
