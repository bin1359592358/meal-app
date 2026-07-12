"""分类、菜品、调味和图片上传校验测试。"""

import asyncio
import importlib
import io

import pytest
from fastapi import HTTPException, UploadFile
from pydantic import ValidationError


def _create_chef_room(api_client, username="chefuser"):
    register = api_client.post(
        "/api/auth/register",
        json={"username": username, "nickname": "主厨", "pin": "1234"},
    )
    token = register.json()["data"]["token"]
    headers = {"Authorization": f"Bearer {token}"}
    room = api_client.post("/api/rooms", headers=headers, json={"name": "测试餐桌"})
    return headers, room.json()["data"]["id"]


def _create_category(api_client, headers, room_id, name="热菜"):
    response = api_client.post(
        f"/api/rooms/{room_id}/categories",
        headers=headers,
        json={"name": name},
    )
    assert response.status_code == 200
    return response.json()["data"]["id"]


@pytest.mark.parametrize(
    ("schema_name", "payload"),
    [
        ("CategoryCreate", {"name": "   "}),
        ("CategoryCreate", {"name": "菜" * 31}),
        ("CategoryCreate", {"name": "菜", "icon": "图" * 11}),
        ("DishCreate", {"category_id": 1, "name": "  ", "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜" * 51, "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜", "description": "文" * 201, "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜", "image_url": "x" * 501, "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜", "tags": ["标签"] * 11, "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜", "tags": ["  "], "price": 1}),
        ("DishCreate", {"category_id": 1, "name": "菜", "price": 0}),
        ("DishCreate", {"category_id": 1, "name": "菜", "price": 100000.01}),
        ("DishUpdate", {"sort_order": 1_000_001}),
    ],
)
def test_category_and_dish_fields_reject_invalid_values(api_client, schema_name, payload):
    """空白、超长、越界价格和排序值必须在模型层被拒绝。"""
    schemas = importlib.import_module("schemas")
    with pytest.raises(ValidationError):
        getattr(schemas, schema_name).model_validate(payload)


def test_category_and_dish_fields_are_trimmed(api_client):
    """名称、描述、图片地址和标签应在写入前去除首尾空白。"""
    schemas = importlib.import_module("schemas")
    category = schemas.CategoryCreate(name="  热菜  ", icon="  🍲  ")
    dish = schemas.DishCreate(
        category_id=1,
        name="  红烧肉  ",
        description="  香甜  ",
        image_url="  /a.png  ",
        tags=["  招牌  "],
        price=12,
    )
    assert category.name == "热菜"
    assert category.icon == "🍲"
    assert dish.name == "红烧肉"
    assert dish.description == "香甜"
    assert dish.image_url == "/a.png"
    assert dish.tags == ["招牌"]


def test_create_and_update_dish_persist_is_available(api_client):
    """新增和普通编辑接口都应真实持久化菜品上下架状态。"""
    headers, room_id = _create_chef_room(api_client)
    category_id = _create_category(api_client, headers, room_id)
    created = api_client.post(
        f"/api/rooms/{room_id}/dishes",
        headers=headers,
        json={"category_id": category_id, "name": "凉菜", "price": 8, "is_available": False},
    )
    assert created.status_code == 200
    dish_id = created.json()["data"]["id"]
    assert created.json()["data"]["is_available"] is False

    updated = api_client.put(
        f"/api/rooms/{room_id}/dishes/{dish_id}",
        headers=headers,
        json={"is_available": True},
    )
    assert updated.status_code == 200
    assert updated.json()["data"]["is_available"] is True
    fetched = api_client.get(f"/api/rooms/{room_id}/dishes/{dish_id}", headers=headers)
    assert fetched.json()["data"]["is_available"] is True


def test_create_dish_rejects_category_from_another_room(api_client):
    """不能把其他餐桌的分类用于当前餐桌菜品。"""
    headers, first_room = _create_chef_room(api_client)
    second_room = api_client.post("/api/rooms", headers=headers, json={"name": "另一个餐桌"}).json()["data"]["id"]
    foreign_category = _create_category(api_client, headers, first_room)
    response = api_client.post(
        f"/api/rooms/{second_room}/dishes",
        headers=headers,
        json={"category_id": foreign_category, "name": "越界菜", "price": 10},
    )
    assert response.status_code == 400
    assert "不属于此餐桌" in response.json()["detail"]


@pytest.mark.parametrize(
    "seasonings",
    [
        [{"name": "辣度", "type": "single", "options": ["微辣", "微辣"]}],
        [{"name": "辣度", "type": "single", "options": ["微辣"]}, {"name": "辣度", "type": "text"}],
        [{"name": "辣度", "type": "single", "options": ["微辣"], "default": "重辣"}],
        [{"name": "加料", "type": "multi", "options": [], "default": []}],
        [{"name": "甜度", "type": "scale", "min": 5, "max": 1, "step": 1, "default": 3}],
        [{"name": "甜度", "type": "scale", "min": 0, "max": 5, "step": 2, "default": 3}],
        [{"name": "备注", "type": "text", "max_length": 3, "default": "太长了呀"}],
        [{"name": "未知", "type": "select", "options": ["A"]}],
    ],
)
def test_invalid_or_duplicate_seasoning_definitions_are_rejected(api_client, seasonings):
    """调味定义中的类型、唯一性、默认值和范围必须严格校验。"""
    schemas = importlib.import_module("schemas")
    with pytest.raises(ValidationError):
        schemas.DishCreate(category_id=1, name="菜", price=1, seasonings=seasonings)


def test_valid_seasoning_definitions_are_normalized_for_storage(api_client):
    """合法定义会被规范化为稳定的列表 JSON。"""
    schemas = importlib.import_module("schemas")
    dish = schemas.DishCreate(
        category_id=1,
        name="菜",
        price=1,
        seasonings=[
            {"name": " 辣度 ", "type": "single", "options": [" 微辣 ", "中辣"], "default": "微辣"},
            {"name": "加料", "type": "multi", "options": ["葱", "蒜"], "default": ["蒜"]},
            {"name": "甜度", "type": "scale", "min": 0, "max": 5, "step": 0.5, "default": 1.5},
            {"name": "备注", "type": "text", "max_length": 20, "default": "少盐"},
        ],
    )
    assert dish.seasonings[0]["name"] == "辣度"
    assert dish.seasonings[0]["options"] == ["微辣", "中辣"]
    assert '"type": "scale"' in dish.seasonings_json()


@pytest.mark.parametrize(
    ("selections", "message"),
    [
        ({"不存在": "值"}, "未知调味项"),
        ({"辣度": "重辣"}, "可选项"),
        ({"加料": "葱"}, "列表"),
        ({"甜度": 1.3}, "步长"),
        ({"甜度": 6}, "范围"),
        ({"备注": 123}, "字符串"),
        ({"备注": "超过五个字啦"}, "长度"),
    ],
)
def test_user_seasoning_selection_rejects_forged_or_invalid_values(api_client, selections, message):
    """用户选择不能伪造名称、类型、选项、范围或文本长度。"""
    auth = importlib.import_module("auth")
    definitions = [
        {"name": "辣度", "type": "single", "options": ["微辣", "中辣"]},
        {"name": "加料", "type": "multi", "options": ["葱", "蒜"]},
        {"name": "甜度", "type": "scale", "min": 0, "max": 5, "step": 0.5},
        {"name": "备注", "type": "text", "max_length": 5},
    ]
    with pytest.raises(HTTPException) as exc_info:
        auth.validate_and_serialize_seasonings(definitions, selections)
    assert exc_info.value.status_code == 400
    assert message in exc_info.value.detail


def test_user_seasoning_selection_serializes_valid_values(api_client):
    """合法选择按定义顺序去重并生成人类可读文本。"""
    auth = importlib.import_module("auth")
    definitions = [
        {"name": "辣度", "type": "single", "options": ["微辣", "中辣"]},
        {"name": "加料", "type": "multi", "options": ["葱", "蒜"]},
        {"name": "甜度", "type": "scale", "min": 0, "max": 5, "step": 0.5},
        {"name": "备注", "type": "text", "max_length": 5},
    ]
    result = auth.validate_and_serialize_seasonings(
        definitions, {"备注": "少盐", "加料": ["蒜", "葱"], "辣度": "微辣", "甜度": 1.5}
    )
    assert result == "微辣，蒜葱，甜度1.5，少盐"


def test_order_rejects_forged_seasoning_selection(api_client):
    """下单接口必须调用服务端调味选择校验。"""
    headers, room_id = _create_chef_room(api_client)
    category_id = _create_category(api_client, headers, room_id)
    dish = api_client.post(
        f"/api/rooms/{room_id}/dishes",
        headers=headers,
        json={
            "category_id": category_id,
            "name": "面条",
            "price": 12,
            "seasonings": [{"name": "辣度", "type": "single", "options": ["微辣"]}],
        },
    ).json()["data"]
    response = api_client.post(
        f"/api/rooms/{room_id}/orders",
        headers=headers,
        json={"items": [{"dish_id": dish["id"], "quantity": 1, "seasonings": {"辣度": "变态辣"}}]},
    )
    assert response.status_code == 400
    assert "可选项" in response.json()["detail"]


@pytest.mark.parametrize(
    ("filename", "content_type", "content", "message"),
    [
        ("fake.png", "image/png", b"this is not png", "文件内容"),
        ("photo.png", "image/jpeg", b"\x89PNG\r\n\x1a\n", "不一致"),
        ("empty.png", "image/png", b"", "空文件"),
    ],
)
def test_image_validation_rejects_invalid_uploads(api_client, filename, content_type, content, message):
    """图片 MIME、扩展名和真实文件头必须一致，且不能是空文件。"""
    image_upload = importlib.import_module("image_upload")
    file = UploadFile(filename=filename, file=io.BytesIO(content), headers={"content-type": content_type})
    with pytest.raises(HTTPException) as exc_info:
        asyncio.run(image_upload.validate_image_file(file))
    assert exc_info.value.status_code == 400
    assert message in exc_info.value.detail


def test_image_validation_accepts_minimal_png_and_resets_stream(api_client):
    """合法最小 PNG 文件头应通过，并把文件指针复位供上传使用。"""
    image_upload = importlib.import_module("image_upload")
    content = b"\x89PNG\r\n\x1a\n"
    file = UploadFile(filename="tiny.PNG", file=io.BytesIO(content), headers={"content-type": "image/png"})
    assert asyncio.run(image_upload.validate_image_file(file)) == "png"
    assert asyncio.run(file.read()) == content
