"""Meal App API - FastAPI application entry point."""

import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

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
    """Application lifespan: create tables on startup."""
    Base.metadata.create_all(bind=engine)
    yield


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
    allow_credentials=True,
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
