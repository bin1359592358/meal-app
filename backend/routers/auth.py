"""Authentication routes: register, login, and PIN management."""

import re
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session

from auth import generate_token, hash_pin, verify_pin
from config import settings
from database import get_db
from middleware import get_current_user
from models import Session as SessionModel
from models import User
from schemas import (
    ApiResponse,
    AuthResponse,
    PinChange,
    UserLogin,
    UserRegister,
    UserResponse,
)

router = APIRouter(prefix="/auth")


@router.post("/register")
def register(body: UserRegister, db: Session = Depends(get_db)):
    """Register a new user and return an auth token."""
    # Validate username format (defense in depth; schema validator also checks)
    if not re.match(r"^[a-zA-Z0-9]{3,20}$", body.username):
        raise HTTPException(status_code=400, detail="用户名格式错误，仅允许3-20位字母和数字")

    # Check username uniqueness
    existing = db.query(User).filter(User.username == body.username).first()
    if existing:
        raise HTTPException(status_code=400, detail="用户名已存在")

    # Create user
    user = User(
        username=body.username,
        nickname=body.nickname,
        pin_hash=hash_pin(body.pin),
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    # Generate token and create session
    token = generate_token()
    session = SessionModel(
        user_id=user.id,
        token=token,
        expires_at=(
            datetime.now(timezone.utc) + timedelta(days=settings.TOKEN_EXPIRE_DAYS)
        ).isoformat(),
    )
    db.add(session)
    db.commit()

    return ApiResponse(
        data=AuthResponse(
            user=UserResponse.model_validate(user),
            token=token,
        ).model_dump()
    )


@router.post("/login")
def login(body: UserLogin, db: Session = Depends(get_db)):
    """Authenticate a user and return an auth token."""
    user = db.query(User).filter(User.username == body.username).first()
    if not user or not verify_pin(body.pin, user.pin_hash):
        raise HTTPException(status_code=401, detail="用户名或PIN错误")

    # Generate token and create session
    token = generate_token()
    session = SessionModel(
        user_id=user.id,
        token=token,
        expires_at=(
            datetime.now(timezone.utc) + timedelta(days=settings.TOKEN_EXPIRE_DAYS)
        ).isoformat(),
    )
    db.add(session)
    db.commit()

    return ApiResponse(
        data=AuthResponse(
            user=UserResponse.model_validate(user),
            token=token,
        ).model_dump()
    )


@router.post("/pin/change")
def change_pin(
    body: PinChange,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Change the authenticated user's PIN."""
    if not verify_pin(body.old_pin, current_user.pin_hash):
        raise HTTPException(status_code=400, detail="旧PIN错误")

    current_user.pin_hash = hash_pin(body.new_pin)
    db.commit()

    return ApiResponse(message="PIN修改成功")
