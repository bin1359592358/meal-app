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


def _clear_project_modules() -> None:
    """清除持有模块级数据库对象的项目模块。"""
    for module_name in list(sys.modules):
        if module_name in PROJECT_MODULES or module_name.startswith("routers"):
            sys.modules.pop(module_name, None)


@pytest.fixture
def api_client(tmp_path: Path, monkeypatch: pytest.MonkeyPatch):
    """返回指向临时 SQLite 数据库的 FastAPI 测试客户端。"""
    database_path = tmp_path / "meal_app_test.db"
    monkeypatch.syspath_prepend(str(BACKEND_DIR))
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{database_path.as_posix()}")
    monkeypatch.setenv("AUTO_SEED", "false")
    _clear_project_modules()

    main = importlib.import_module("main")
    database = importlib.import_module("database")
    database.Base.metadata.create_all(bind=database.engine)
    client = TestClient(main.app)
    try:
        yield client
    finally:
        client.close()
        database.engine.dispose()
        _clear_project_modules()
