"""Authentication and utility helpers."""

import secrets

import bcrypt


def hash_pin(pin: str) -> str:
    """Hash a PIN using bcrypt and return the hash string."""
    return bcrypt.hashpw(pin.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")


def verify_pin(pin: str, pin_hash: str) -> bool:
    """Verify a PIN against its bcrypt hash."""
    return bcrypt.checkpw(pin.encode("utf-8"), pin_hash.encode("utf-8"))


def generate_token() -> str:
    """Generate a cryptographically secure 32-character hex token."""
    return secrets.token_hex(16)


def serialize_seasonings(seasonings_data: dict, user_selections: dict) -> str:
    """Convert seasoning definitions and user selections into readable text.

    Args:
        seasonings_data: The dish's seasoning configuration, e.g.
            {"辣度": {"type": "select", "options": ["微辣", "中辣", "重辣"]},
             "加料": {"type": "multi", "options": ["加葱", "加蒜", "加香菜"]},
             "甜度": {"type": "number", "min": 0, "max": 5}}
        user_selections: The user's choices, e.g.
            {"辣度": "中辣", "加料": ["加葱", "加蒜"], "甜度": 3}

    Returns:
        A comma-separated readable string like "中辣，加葱加蒜，甜度3"
    """
    if not seasonings_data or not user_selections:
        return ""

    parts: list[str] = []

    for key, value in user_selections.items():
        if value is None:
            continue

        if isinstance(value, list):
            # Multi-select: join items directly (e.g., "加葱加蒜")
            parts.append("".join(value))
        elif isinstance(value, (int, float)):
            # Numeric selection: prefix with key name (e.g., "甜度3")
            parts.append(f"{key}{value}")
        elif isinstance(value, str) and value:
            # Single select: just the value (e.g., "中辣")
            parts.append(value)

    return "\uff0c".join(parts)  # Chinese comma separator
