"""认证 API 的端到端基线测试。"""


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
