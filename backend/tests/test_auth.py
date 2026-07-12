"""认证 API 的端到端基线测试。"""

import asyncio
import importlib
import sys
import types
from unittest.mock import Mock

import pytest

from conftest import BACKEND_DIR, _remove_project_modules


def test_register_profile_and_logout_invalidates_token(api_client):
    """注册令牌可访问个人资料，登出后同一令牌失效。"""
    register_response = api_client.post(
        "/api/auth/register",
        json={"username": "testuser", "nickname": "测试用户", "pin": "1234"},
    )

    assert register_response.status_code == 200
    register_data = register_response.json()["data"]
    token = register_data["token"]
    authorization = {"Authorization": f"Bearer {token}"}

    profile_response = api_client.get("/api/users/me", headers=authorization)
    assert profile_response.status_code == 200
    assert profile_response.json()["data"]["username"] == "testuser"

    logout_response = api_client.post("/api/auth/logout", headers=authorization)
    assert logout_response.status_code == 200

    invalidated_response = api_client.get("/api/users/me", headers=authorization)
    assert invalidated_response.status_code == 401


def test_pin_change_rotates_token_and_updates_login_pin(api_client):
    """PIN 修改后旧令牌失效，返回的新令牌可用，且新 PIN 可登录。"""
    register_response = api_client.post(
        "/api/auth/register",
        json={"username": "rotateuser", "nickname": "换码用户", "pin": "1234"},
    )
    old_token = register_response.json()["data"]["token"]

    change_response = api_client.post(
        "/api/auth/pin/change",
        headers={"Authorization": f"Bearer {old_token}"},
        json={"old_pin": "1234", "new_pin": "5678"},
    )
    assert change_response.status_code == 200
    new_token = change_response.json()["data"]["token"]
    assert new_token != old_token
    assert api_client.get(
        "/api/users/me", headers={"Authorization": f"Bearer {old_token}"}
    ).status_code == 401
    assert api_client.get(
        "/api/users/me", headers={"Authorization": f"Bearer {new_token}"}
    ).status_code == 200
    assert api_client.post(
        "/api/auth/login", json={"username": "rotateuser", "pin": "1234"}
    ).status_code == 401
    assert api_client.post(
        "/api/auth/login", json={"username": "rotateuser", "pin": "5678"}
    ).status_code == 200


@pytest.mark.parametrize(
    ("database_url", "turso_url", "expected_url", "expected_connect_args"),
    [
        (
            "sqlite:///override.db",
            "libsql://ignored.turso.io",
            "sqlite:///override.db",
            {"check_same_thread": False},
        ),
        (
            "postgresql://user:pass@db/app",
            "libsql://ignored.turso.io",
            "postgresql://user:pass@db/app",
            None,
        ),
        (
            "",
            "libsql://meal.turso.io",
            "libsql://meal.turso.io",
            {"auth_token": "secret"},
        ),
    ],
)
def test_database_engine_uses_effective_url_scheme(
    monkeypatch,
    database_url,
    turso_url,
    expected_url,
    expected_connect_args,
):
    """引擎参数由最终生效 URL 的 scheme 决定，DATABASE_URL 优先。"""
    monkeypatch.syspath_prepend(str(BACKEND_DIR))
    monkeypatch.setenv("DATABASE_URL", database_url)
    monkeypatch.setenv("TURSO_DATABASE_URL", turso_url)
    monkeypatch.setenv("TURSO_AUTH_TOKEN", "secret")
    original_modules = _remove_project_modules()
    create_engine = Mock(return_value=Mock())
    monkeypatch.setattr("sqlalchemy.create_engine", create_engine)
    try:
        importlib.import_module("database")
        kwargs = {"echo": False}
        if expected_connect_args is not None:
            kwargs["connect_args"] = expected_connect_args
        create_engine.assert_called_once_with(expected_url, **kwargs)
    finally:
        _remove_project_modules()
        sys.modules.update(original_modules)


@pytest.mark.parametrize(("auto_seed", "expected_calls"), [(False, 0), (True, 1)])
def test_lifespan_only_seeds_when_enabled(monkeypatch, auto_seed, expected_calls):
    """AUTO_SEED 默认关闭，显式开启时才执行种子。"""
    monkeypatch.syspath_prepend(str(BACKEND_DIR))
    monkeypatch.setenv("DATABASE_URL", "sqlite:///:memory:")
    monkeypatch.setenv("AUTO_SEED", str(auto_seed).lower())
    original_modules = _remove_project_modules()
    try:
        main = importlib.import_module("main")
        seed = Mock()
        monkeypatch.setitem(sys.modules, "seed_data", types.SimpleNamespace(seed=seed))
        monkeypatch.setattr(main.Base.metadata, "create_all", Mock())
        monkeypatch.setattr(main, "_run_migrations", Mock())

        async def run_lifespan():
            async with main.lifespan(main.app):
                pass

        asyncio.run(run_lifespan())
        assert seed.call_count == expected_calls
    finally:
        _remove_project_modules()
        sys.modules.update(original_modules)


def test_seed_output_does_not_expose_session_token(api_client, monkeypatch, capsys):
    """种子日志可报告会话已创建，但不能输出有效 token。"""
    seed_data = importlib.import_module("seed_data")
    secret_token = "active-session-token-must-stay-secret"
    monkeypatch.setattr(seed_data, "generate_token", lambda: secret_token)

    seed_data.seed()

    output = capsys.readouterr().out
    assert secret_token not in output
    assert "Session:   created" in output


@pytest.mark.parametrize("pin_hash", [None, ""])
def test_login_without_pin_hash_returns_401(api_client, pin_hash):
    """未设置 PIN 的账号统一返回 401，不调用 bcrypt。"""
    database = importlib.import_module("database")
    models = importlib.import_module("models")
    db = database.SessionLocal()
    try:
        db.add(
            models.User(
                username=f"nopin{pin_hash is None}",
                nickname="微信用户",
                pin_hash=pin_hash,
            )
        )
        db.commit()
    finally:
        db.close()

    response = api_client.post(
        "/api/auth/login",
        json={"username": f"nopin{pin_hash is None}", "pin": "1234"},
    )
    assert response.status_code == 401


@pytest.mark.parametrize("old_pin", ["abcd", "123", "1234567"])
def test_pin_change_rejects_invalid_old_pin(api_client, old_pin):
    """旧 PIN 与新 PIN 一样只允许 4-6 位数字。"""
    register_response = api_client.post(
        "/api/auth/register",
        json={"username": "pinuser", "nickname": "PIN用户", "pin": "1234"},
    )
    token = register_response.json()["data"]["token"]
    response = api_client.post(
        "/api/auth/pin/change",
        headers={"Authorization": f"Bearer {token}"},
        json={"old_pin": old_pin, "new_pin": "5678"},
    )
    assert response.status_code == 422
