"""Authentication and utility helpers."""

import secrets
import math

import bcrypt
from fastapi import HTTPException

from schemas import _step_aligned


def hash_pin(pin: str) -> str:
    """Hash a PIN using bcrypt and return the hash string."""
    return bcrypt.hashpw(pin.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_pin(pin: str, pin_hash: str) -> bool:
    """Verify a PIN against its bcrypt hash."""
    return bcrypt.checkpw(pin.encode("utf-8"), pin_hash.encode("utf-8"))


def generate_token() -> str:
    """Generate a cryptographically secure 32-character hex token."""
    return secrets.token_hex(16)


def serialize_seasonings(seasonings_data, user_selections: dict) -> str:
    """Convert seasoning definitions and user selections into readable text.

    Args:
        seasonings_data: The dish's seasoning configuration. May be a dict
            (e.g. {"辣度": {"type": "select", "options": [...]}}) or a list
            (e.g. [{"name": "辣度", "type": "single", "options": [...]}]).
        user_selections: The user's choices, e.g.
            {"辣度": "中辣", "加料": ["加葱", "加蒜"], "甜度": 3}

    Returns:
        A comma-separated readable string like "中辣，加葱加蒜，甜度3"
    """
    # Normalize: convert list format to dict format if needed
    if isinstance(seasonings_data, list):
        seasonings_data = {
            item["name"]: item
            for item in seasonings_data
            if isinstance(item, dict) and "name" in item
        }

    if not seasonings_data or not user_selections:
        return ""

    # Normalize user_selections values that come as single-element lists
    normalized_selections: dict = {}
    for key, value in user_selections.items():
        if isinstance(value, list) and len(value) == 1:
            # Single-element list: treat as single-select string
            normalized_selections[key] = value[0]
        elif isinstance(value, list) and len(value) > 1:
            normalized_selections[key] = value
        else:
            normalized_selections[key] = value

    parts: list[str] = []

    for key, value in normalized_selections.items():
        if value is None:
            continue

        if isinstance(value, list):
            # Multi-select: join items directly (e.g., "加葱加蒜")
            parts.append("".join(value))
        elif isinstance(value, (int, float)):
            # Numeric selection: prefix with key name (e.g., "甜度3")
            parts.append(f"{key}{value}")
        elif isinstance(value, str) and value:
            # Try to parse as number for scale-type seasonings
            try:
                num = int(value)
                parts.append(f"{key}{num}")
            except ValueError:
                # Single select: just the value (e.g., "中辣")
                parts.append(value)

    return "\uff0c".join(parts)  # Chinese comma separator


def _seasoning_definition_map(seasonings_data) -> tuple[list[str], dict]:
    """兼容列表及旧版名称映射对象，并保留定义顺序。"""
    if isinstance(seasonings_data, dict):
        definitions = [
            {"name": name, **config}
            for name, config in seasonings_data.items()
            if isinstance(config, dict)
        ]
    elif isinstance(seasonings_data, list):
        definitions = seasonings_data
    else:
        definitions = []
    order = []
    definition_map = {}
    for definition in definitions:
        if isinstance(definition, dict) and isinstance(definition.get("name"), str):
            name = definition["name"]
            order.append(name)
            definition_map[name] = definition
    return order, definition_map


def validate_and_serialize_seasonings(seasonings_data, user_selections: dict | None) -> str:
    """验证用户调味选择，并生成可读文本；非法选择统一返回 400。"""
    if not user_selections:
        return ""
    if not isinstance(user_selections, dict):
        raise HTTPException(status_code=400, detail="调味选择必须是对象")

    order, definitions = _seasoning_definition_map(seasonings_data)
    unknown = [name for name in user_selections if name not in definitions]
    if unknown:
        raise HTTPException(status_code=400, detail=f"未知调味项：{unknown[0]}")

    parts: list[str] = []
    for name in order:
        if name not in user_selections:
            continue
        definition = definitions[name]
        value = user_selections[name]
        seasoning_type = definition.get("type")

        if seasoning_type == "single":
            if not isinstance(value, str) or value not in definition.get("options", []):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值不在可选项中")
            parts.append(value)
        elif seasoning_type == "multi":
            if not isinstance(value, list):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值必须是列表")
            if len(value) > 20:
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的选择数量超过限制")
            unique_values = []
            for option in value:
                if not isinstance(option, str) or option not in definition.get("options", []):
                    raise HTTPException(status_code=400, detail=f"调味项 {name} 包含无效可选项")
                if option not in unique_values:
                    unique_values.append(option)
            if unique_values:
                parts.append("".join(unique_values))
        elif seasoning_type == "scale":
            if isinstance(value, bool) or not isinstance(value, (int, float)) or not math.isfinite(float(value)):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值必须是数字")
            minimum = definition.get("min", 0)
            maximum = definition.get("max", 5)
            step = definition.get("step", 1)
            if not minimum <= value <= maximum:
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值超出范围")
            if not _step_aligned(value, minimum, step):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值不符合步长")
            parts.append(f"{name}{value}")
        elif seasoning_type == "text":
            if not isinstance(value, str):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的值必须是字符串")
            if len(value) > definition.get("max_length", 100):
                raise HTTPException(status_code=400, detail=f"调味项 {name} 的文本超过长度限制")
            if value:
                parts.append(value)
        else:
            raise HTTPException(status_code=400, detail=f"调味项 {name} 的定义类型无效")
    return "，".join(parts)
