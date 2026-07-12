"""Pydantic schemas for request validation and response serialization."""

from __future__ import annotations

import json
import math
import re
from decimal import Decimal, InvalidOperation
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


SORT_ORDER_MIN = -1_000_000
SORT_ORDER_MAX = 1_000_000
MAX_DISH_PRICE = 100_000


def _trimmed(value: Any) -> Any:
    """去除字符串首尾空白，其他类型原样返回。"""
    return value.strip() if isinstance(value, str) else value


def _step_aligned(value: float, minimum: float, step: float) -> bool:
    """使用十进制定点运算判断数值是否落在步长网格上。"""
    try:
        quotient = (Decimal(str(value)) - Decimal(str(minimum))) / Decimal(str(step))
    except (InvalidOperation, ZeroDivisionError):
        return False
    return quotient == quotient.to_integral_value()


def normalize_seasoning_definitions(value: Any) -> list[dict[str, Any]]:
    """严格校验调味定义，并统一转换为列表格式。"""
    if value is None:
        return []
    if isinstance(value, dict):
        value = [
            {"name": name, **config}
            for name, config in value.items()
            if isinstance(config, dict)
        ]
    if not isinstance(value, list):
        raise ValueError("调味定义必须是列表或对象")
    if len(value) > 10:
        raise ValueError("调味项最多 10 项")

    normalized: list[dict[str, Any]] = []
    names: set[str] = set()
    for raw in value:
        if not isinstance(raw, dict):
            raise ValueError("每个调味项必须是对象")
        name = _trimmed(raw.get("name"))
        if not isinstance(name, str) or not 1 <= len(name) <= 20:
            raise ValueError("调味项名称长度必须为 1-20 个字符")
        if name in names:
            raise ValueError(f"调味项名称重复：{name}")
        names.add(name)

        seasoning_type = raw.get("type")
        if seasoning_type not in {"single", "multi", "scale", "text"}:
            raise ValueError(f"调味项 {name} 的类型无效")
        item: dict[str, Any] = {"name": name, "type": seasoning_type}

        if seasoning_type in {"single", "multi"}:
            options = raw.get("options")
            if not isinstance(options, list) or not 1 <= len(options) <= 20:
                raise ValueError(f"调味项 {name} 的选项数量必须为 1-20 个")
            clean_options: list[str] = []
            for option in options:
                option = _trimmed(option)
                if not isinstance(option, str) or not 1 <= len(option) <= 30:
                    raise ValueError(f"调味项 {name} 的选项长度必须为 1-30 个字符")
                if option in clean_options:
                    raise ValueError(f"调味项 {name} 存在重复选项")
                clean_options.append(option)
            item["options"] = clean_options
            if "default" in raw and raw["default"] is not None:
                default = raw["default"]
                if seasoning_type == "single":
                    default = _trimmed(default)
                    if default not in clean_options:
                        raise ValueError(f"调味项 {name} 的默认值不在可选项中")
                else:
                    if not isinstance(default, list):
                        raise ValueError(f"调味项 {name} 的默认值必须是列表")
                    if len(default) != len(set(default)) or any(option not in clean_options for option in default):
                        raise ValueError(f"调味项 {name} 的默认值包含重复或无效选项")
                item["default"] = default
        elif seasoning_type == "scale":
            minimum = raw.get("min", 0)
            maximum = raw.get("max", 5)
            step = raw.get("step", 1)
            if any(isinstance(number, bool) or not isinstance(number, (int, float)) for number in (minimum, maximum, step)):
                raise ValueError(f"调味项 {name} 的范围和步长必须是数字")
            if not all(math.isfinite(float(number)) for number in (minimum, maximum, step)):
                raise ValueError(f"调味项 {name} 的范围和步长必须是有限数字")
            if minimum >= maximum or step <= 0 or step > maximum - minimum:
                raise ValueError(f"调味项 {name} 的范围或步长无效")
            item.update({"min": minimum, "max": maximum, "step": step})
            if "default" in raw and raw["default"] is not None:
                default = raw["default"]
                if isinstance(default, bool) or not isinstance(default, (int, float)):
                    raise ValueError(f"调味项 {name} 的默认值必须是数字")
                if not minimum <= default <= maximum or not _step_aligned(default, minimum, step):
                    raise ValueError(f"调味项 {name} 的默认值超出范围或不符合步长")
                item["default"] = default
        else:
            max_length = raw.get("max_length", 100)
            if isinstance(max_length, bool) or not isinstance(max_length, int) or not 1 <= max_length <= 200:
                raise ValueError(f"调味项 {name} 的文本长度限制必须为 1-200")
            item["max_length"] = max_length
            if "default" in raw and raw["default"] is not None:
                default = raw["default"]
                if not isinstance(default, str) or len(default) > max_length:
                    raise ValueError(f"调味项 {name} 的默认文本超过长度限制")
                item["default"] = default
        normalized.append(item)
    return normalized


# ──────────────────────────── Auth / User ────────────────────────────


class UserRegister(BaseModel):
    username: str
    nickname: str = Field(..., min_length=1, max_length=50)
    pin: str

    @field_validator("username")
    @classmethod
    def validate_username(cls, v: str) -> str:
        if not re.fullmatch(r"[a-zA-Z0-9]{3,20}", v):
            raise ValueError(
                "Username must be 3-20 characters, alphanumeric only."
            )
        return v

    @field_validator("pin")
    @classmethod
    def validate_pin(cls, v: str) -> str:
        if not re.fullmatch(r"[0-9]{4,6}", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class UserLogin(BaseModel):
    username: str
    pin: str

    @field_validator("pin")
    @classmethod
    def validate_pin(cls, v: str) -> str:
        if not re.fullmatch(r"[0-9]{4,6}", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class WechatLogin(BaseModel):
    code: str = Field(..., min_length=1)
    nickname: str | None = None


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    username: str | None = None
    nickname: str
    created_at: str
    avatar_url: str | None = None


class AuthResponse(BaseModel):
    user: UserResponse
    token: str


class PinChange(BaseModel):
    old_pin: str
    new_pin: str

    @field_validator("old_pin", "new_pin")
    @classmethod
    def validate_pin(cls, v: str) -> str:
        if not re.fullmatch(r"[0-9]{4,6}", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class NicknameUpdate(BaseModel):
    nickname: str = Field(..., min_length=1, max_length=50)


# ──────────────────────────────── Room ────────────────────────────────


class MemberResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    username: str | None = None
    nickname: str
    role: str
    joined_at: str


class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)


class RoomRename(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)


class RoomJoin(BaseModel):
    code: str = Field(..., min_length=6, max_length=6)

    @field_validator("code", mode="before")
    @classmethod
    def normalize_code(cls, value: Any) -> Any:
        return value.strip().upper() if isinstance(value, str) else value


class RoomResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str
    code: str
    chef_id: int
    is_active: bool
    created_at: str
    members: list[MemberResponse] | None = None


# ────────────────────────────── Category ──────────────────────────────


class CategoryCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=30)
    icon: str = Field(default="", max_length=10)
    sort_order: int = Field(default=0, ge=SORT_ORDER_MIN, le=SORT_ORDER_MAX)

    @field_validator("name", "icon", mode="before")
    @classmethod
    def trim_strings(cls, value: Any) -> Any:
        return _trimmed(value)


class CategoryUpdate(BaseModel):
    name: str | None = Field(default=None, min_length=1, max_length=30)
    icon: str | None = Field(default=None, max_length=10)
    sort_order: int | None = Field(default=None, ge=SORT_ORDER_MIN, le=SORT_ORDER_MAX)

    @field_validator("name", "icon", mode="before")
    @classmethod
    def trim_strings(cls, value: Any) -> Any:
        return _trimmed(value)


class CategoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    room_id: int
    name: str
    icon: str
    sort_order: int
    dish_count: int = 0


# ──────────────────────────────── Dish ────────────────────────────────


class DishCreate(BaseModel):
    category_id: int
    name: str = Field(..., min_length=1, max_length=50)
    description: str = Field(default="", max_length=200)
    price: float = Field(..., gt=0, le=MAX_DISH_PRICE)
    image_url: str | None = Field(default=None, max_length=500)
    tags: list[str] = Field(default_factory=list, max_length=10)
    seasonings: list[dict[str, Any]] = Field(default_factory=list)
    sort_order: int = Field(default=0, ge=SORT_ORDER_MIN, le=SORT_ORDER_MAX)
    is_available: bool = True

    @field_validator("name", "description", "image_url", mode="before")
    @classmethod
    def trim_strings(cls, value: Any) -> Any:
        return _trimmed(value)

    @field_validator("tags", mode="before")
    @classmethod
    def validate_tags(cls, value: Any) -> Any:
        if not isinstance(value, list):
            return value
        result = []
        for tag in value:
            tag = _trimmed(tag)
            if not isinstance(tag, str) or not 1 <= len(tag) <= 20:
                raise ValueError("每个标签长度必须为 1-20 个字符")
            result.append(tag)
        return result

    @field_validator("seasonings", mode="before")
    @classmethod
    def validate_seasonings(cls, value: Any) -> list[dict[str, Any]]:
        return normalize_seasoning_definitions(value)

    def tags_json(self) -> str:
        """Serialize tags list to JSON string for database storage."""
        return json.dumps(self.tags, ensure_ascii=False)

    def seasonings_json(self) -> str:
        """Serialize seasonings list to JSON string for database storage."""
        return json.dumps(self.seasonings or [], ensure_ascii=False)


class DishUpdate(BaseModel):
    category_id: int | None = None
    name: str | None = Field(default=None, min_length=1, max_length=50)
    description: str | None = Field(default=None, max_length=200)
    price: float | None = Field(None, gt=0, le=MAX_DISH_PRICE)
    image_url: str | None = Field(default=None, max_length=500)
    tags: list[str] | None = Field(default=None, max_length=10)
    seasonings: list[dict[str, Any]] | None = None
    sort_order: int | None = Field(default=None, ge=SORT_ORDER_MIN, le=SORT_ORDER_MAX)
    is_available: bool | None = None

    @field_validator("name", "description", "image_url", mode="before")
    @classmethod
    def trim_strings(cls, value: Any) -> Any:
        return _trimmed(value)

    @field_validator("tags", mode="before")
    @classmethod
    def validate_tags(cls, value: Any) -> Any:
        if value is None or not isinstance(value, list):
            return value
        result = []
        for tag in value:
            tag = _trimmed(tag)
            if not isinstance(tag, str) or not 1 <= len(tag) <= 20:
                raise ValueError("每个标签长度必须为 1-20 个字符")
            result.append(tag)
        return result

    @field_validator("seasonings", mode="before")
    @classmethod
    def validate_seasonings(cls, value: Any) -> list[dict[str, Any]] | None:
        return None if value is None else normalize_seasoning_definitions(value)


class DishResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    category_id: int
    room_id: int
    name: str
    description: str
    price: float
    image_url: str | None
    is_available: bool
    tags: list[str]
    seasonings: list
    sort_order: int

    @field_validator("tags", mode="before")
    @classmethod
    def parse_tags(cls, v: Any) -> list[str]:
        if isinstance(v, str):
            try:
                return json.loads(v)
            except (json.JSONDecodeError, TypeError):
                return []
        return v if isinstance(v, list) else []

    @field_validator("seasonings", mode="before")
    @classmethod
    def parse_seasonings(cls, v: Any) -> list:
        if isinstance(v, str):
            try:
                parsed = json.loads(v)
            except (json.JSONDecodeError, TypeError):
                return []
            # If stored as a dict (name → config), convert to list format
            if isinstance(parsed, dict):
                return [
                    {"name": name, **config}
                    for name, config in parsed.items()
                    if isinstance(config, dict)
                ]
            return parsed if isinstance(parsed, list) else []
        if isinstance(v, dict):
            return [
                {"name": name, **config}
                for name, config in v.items()
                if isinstance(config, dict)
            ]
        return v if isinstance(v, list) else []


# ──────────────────────────────── Order ────────────────────────────────


class OrderItemCreate(BaseModel):
    dish_id: int
    quantity: int = Field(..., ge=1, le=999)
    seasonings: dict | None = None


class OrderCreate(BaseModel):
    items: list[OrderItemCreate]
    note: str = ""
    people_count: int = Field(default=1, ge=1, le=100)


class OrderItemResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    dish_id: int
    dish_name: str
    dish_price: float
    quantity: int
    seasoning_text: str
    subtotal: float = 0.0

    @classmethod
    def from_item(cls, item: Any) -> OrderItemResponse:
        """Build response from an OrderItem ORM instance, computing subtotal."""
        return cls(
            id=item.id,
            dish_id=item.dish_id,
            dish_name=item.dish_name,
            dish_price=item.dish_price,
            quantity=item.quantity,
            seasoning_text=item.seasoning_text,
            subtotal=round(item.dish_price * item.quantity, 2),
        )


class OrderResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    room_id: int
    user_id: int
    user_nickname: str
    total_price: float
    status: str
    note: str
    people_count: int
    created_at: str
    items: list[OrderItemResponse] | None = None


class OrderSummaryItem(BaseModel):
    dish_id: int
    dish_name: str
    total_quantity: int
    order_count: int
    seasonings_list: list[str]


class OrderSummaryResponse(BaseModel):
    summary: list[OrderSummaryItem]
    total_orders: int
    total_price: float
    completed_orders: int = 0
    revenue: float = 0.0
    sales_summary: list[OrderSummaryItem] = Field(default_factory=list)


class StatusUpdate(BaseModel):
    status: str


# ────────────────────────────── Generic ──────────────────────────────


class ApiResponse(BaseModel):
    code: int = 0
    data: Any = None
    message: str = "ok"
    total: int | None = None
    page: int | None = None
    page_size: int | None = None
