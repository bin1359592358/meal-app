"""Pydantic schemas for request validation and response serialization."""

from __future__ import annotations

import json
import re
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator


# ──────────────────────────── Auth / User ────────────────────────────


class UserRegister(BaseModel):
    username: str
    nickname: str = Field(..., min_length=1, max_length=50)
    pin: str

    @field_validator("username")
    @classmethod
    def validate_username(cls, v: str) -> str:
        if not re.match(r"^[a-zA-Z0-9]{3,20}$", v):
            raise ValueError(
                "Username must be 3-20 characters, alphanumeric only."
            )
        return v

    @field_validator("pin")
    @classmethod
    def validate_pin(cls, v: str) -> str:
        if not re.match(r"^\d{4,6}$", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class UserLogin(BaseModel):
    username: str
    pin: str

    @field_validator("pin")
    @classmethod
    def validate_pin(cls, v: str) -> str:
        if not re.match(r"^\d{4,6}$", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    username: str
    nickname: str
    created_at: str


class AuthResponse(BaseModel):
    user: UserResponse
    token: str


class PinChange(BaseModel):
    old_pin: str
    new_pin: str

    @field_validator("new_pin")
    @classmethod
    def validate_new_pin(cls, v: str) -> str:
        if not re.match(r"^\d{4,6}$", v):
            raise ValueError("PIN must be 4-6 digits.")
        return v


class NicknameUpdate(BaseModel):
    nickname: str = Field(..., min_length=1, max_length=50)


# ──────────────────────────────── Room ────────────────────────────────


class MemberResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    user_id: int
    username: str
    nickname: str
    role: str
    joined_at: str


class RoomCreate(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)


class RoomRename(BaseModel):
    name: str = Field(..., min_length=1, max_length=50)


class RoomJoin(BaseModel):
    code: str


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
    name: str
    icon: str = ""
    sort_order: int = 0


class CategoryUpdate(BaseModel):
    name: str | None = None
    icon: str | None = None
    sort_order: int | None = None


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
    name: str
    description: str = ""
    price: float = Field(..., gt=0)
    image_url: str | None = None
    tags: list[str] = []
    seasonings: list | None = None
    sort_order: int = 0

    def tags_json(self) -> str:
        """Serialize tags list to JSON string for database storage."""
        return json.dumps(self.tags, ensure_ascii=False)

    def seasonings_json(self) -> str:
        """Serialize seasonings list to JSON string for database storage."""
        return json.dumps(self.seasonings or [], ensure_ascii=False)


class DishUpdate(BaseModel):
    category_id: int | None = None
    name: str | None = None
    description: str | None = None
    price: float | None = Field(None, gt=0)
    image_url: str | None = None
    tags: list[str] | None = None
    seasonings: list | None = None
    sort_order: int | None = None


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
    dish_name: str
    total_quantity: int
    order_count: int
    seasonings_list: list[str]


class OrderSummaryResponse(BaseModel):
    summary: list[OrderSummaryItem]
    total_orders: int
    total_price: float


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
