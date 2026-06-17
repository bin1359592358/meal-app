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
