"""后端 API 测试的隔离夹具。"""

import importlib
import sys
from pathlib import Path

import pytest
from fastapi.testclient import TestClient


BACKEND_DIR = Path(__file__).resolve().parents[1]
PROJECT_MODULES = {
    "auth",
    "config",
    "database",
    "image_upload",
    "main",
    "middleware",
    "models",
    "schemas",
    "seed_data",
}


def _remove_project_modules() -> dict[str, object]:
    """移除并返回持有模块级数据库对象的项目模块。"""
    removed_modules = {}
    for module_name in list(sys.modules):
        if module_name in PROJECT_MODULES or module_name.startswith("routers"):
            removed_modules[module_name] = sys.modules.pop(module_name)
    return removed_modules


@pytest.fixture
def api_client(tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    """返回指向临时 SQLite 数据库的 FastAPI 测试客户端。"""
    database_path = tmp_path / "meal_app_test.db"
    monkeypatch.syspath_prepend(str(BACKEND_DIR))
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{database_path.as_posix()}")
    monkeypatch.setenv("TURSO_DATABASE_URL", "")
    monkeypatch.setenv("TURSO_AUTH_TOKEN", "")
    monkeypatch.setenv("AUTO_SEED", "false")
    original_modules = _remove_project_modules()
    database = None
    client = None
    try:
        main = importlib.import_module("main")
        database = importlib.import_module("database")
        # 当前 lifespan 会无条件执行 seed，测试因此不进入 lifespan；这里手工建表，
        # AUTO_SEED=false 同时为生产代码支持该开关后保留明确的测试环境契约。
        database.Base.metadata.create_all(bind=database.engine)
        client = TestClient(main.app)
        yield client
    finally:
        try:
            if client is not None:
                client.close()
        finally:
            try:
                if database is not None:
                    database.engine.dispose()
            finally:
                _remove_project_modules()
                sys.modules.update(original_modules)
