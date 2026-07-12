"""Authentication routes: register, login, and PIN management."""

import asyncio
import json
import re
import time as _time
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, Header, HTTPException
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
    WechatLogin,
)

router = APIRouter(prefix="/auth")

# ── PIN lockout tracking (in-memory, per-process) ──────────────────────
MAX_FAILED_ATTEMPTS = 5
LOCKOUT_SECONDS = 60
_login_attempts: dict[str, int] = {}        # username -> consecutive failures
_lockout_until: dict[str, float] = {}       # username -> lockout expiry (epoch)


def _check_lockout(username: str) -> None:
    """Raise 429 if the account is currently locked out."""
    import time
    expiry = _lockout_until.get(username, 0)
    if time.time() < expiry:
        remaining = int(expiry - time.time())
        raise HTTPException(
            status_code=429,
            detail=f"登录尝试次数过多，请{remaining}秒后再试",
        )
    # Lockout expired — reset
    if expiry > 0 and time.time() >= expiry:
        _login_attempts.pop(username, None)
        _lockout_until.pop(username, None)


def _record_failure(username: str) -> None:
    """Increment failed attempts; lock out if threshold reached."""
    import time
    count = _login_attempts.get(username, 0) + 1
    _login_attempts[username] = count
    if count >= MAX_FAILED_ATTEMPTS:
        _lockout_until[username] = time.time() + LOCKOUT_SECONDS
        _login_attempts[username] = 0  # reset counter for next cycle


def _clear_failure(username: str) -> None:
    """Reset failed-attempt counters on successful login."""
    _login_attempts.pop(username, None)
    _lockout_until.pop(username, None)


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
    _check_lockout(body.username)

    user = db.query(User).filter(User.username == body.username).first()
    if not user or not verify_pin(body.pin, user.pin_hash):
        _record_failure(body.username)
        if body.username in _lockout_until and _time.time() < _lockout_until[body.username]:
            detail = "登录尝试次数过多，请60秒后再试"
        else:
            attempts_left = MAX_FAILED_ATTEMPTS - _login_attempts.get(body.username, 0)
            detail = f"用户名或PIN错误（剩余{attempts_left}次尝试）"
        raise HTTPException(status_code=401, detail=detail)

    _clear_failure(body.username)

    # Clean up expired sessions
    now_iso = datetime.now(timezone.utc).isoformat()
    db.query(SessionModel).filter(
        SessionModel.user_id == user.id,
        SessionModel.expires_at < now_iso,
    ).delete(synchronize_session=False)

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


@router.post("/wechat-login")
async def wechat_login(body: WechatLogin, db: Session = Depends(get_db)):
    """Authenticate via WeChat Mini Program code and return an auth token."""
    if not settings.is_wechat_configured:
        raise HTTPException(status_code=500, detail="微信登录未配置，请联系管理员")

    # Exchange code for openid via WeChat API
    wx_url = "https://api.weixin.qq.com/sns/jscode2session"
    params = {
        "appid": settings.WX_APPID,
        "secret": settings.WX_SECRET,
        "js_code": body.code,
        "grant_type": "authorization_code",
    }
    url = f"{wx_url}?{urllib.parse.urlencode(params)}"

    def _fetch_wx():
        with urllib.request.urlopen(url, timeout=10) as resp:
            return json.loads(resp.read().decode())

    wx_data = await asyncio.to_thread(_fetch_wx)

    if "errcode" in wx_data and wx_data["errcode"] != 0:
        raise HTTPException(
            status_code=400,
            detail=f"微信登录失败: {wx_data.get('errmsg', '未知错误')}",
        )

    openid = wx_data.get("openid")
    if not openid:
        raise HTTPException(status_code=400, detail="微信登录失败: 未获取到openid")

    # Find or create user by openid
    user = db.query(User).filter(User.openid == openid).first()

    if not user:
        # Auto-create user for new WeChat login
        nickname = body.nickname or "微信用户"
        auto_username = f"wx_{openid[:12]}"

        user = User(
            username=auto_username,
            nickname=nickname,
            pin_hash="",  # empty string — satisfies old NOT NULL constraint; falsy so PIN-change guard still works
            openid=openid,
        )
        db.add(user)
        db.commit()
        db.refresh(user)
    else:
        # Update nickname if provided
        if body.nickname:
            user.nickname = body.nickname
            db.commit()

    # Clean up expired sessions
    now_iso = datetime.now(timezone.utc).isoformat()
    db.query(SessionModel).filter(
        SessionModel.user_id == user.id,
        SessionModel.expires_at < now_iso,
    ).delete(synchronize_session=False)

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
    if not current_user.pin_hash:
        raise HTTPException(status_code=400, detail="微信登录用户无法修改PIN码")

    lockout_key = f"pin_change:{current_user.id}"
    _check_lockout(lockout_key)

    if not verify_pin(body.old_pin, current_user.pin_hash):
        _record_failure(lockout_key)
        if lockout_key in _lockout_until and _time.time() < _lockout_until[lockout_key]:
            detail = "PIN修改尝试次数过多，请60秒后再试"
        else:
            attempts_left = MAX_FAILED_ATTEMPTS - _login_attempts.get(lockout_key, 0)
            detail = f"旧PIN错误（剩余{attempts_left}次尝试）"
        raise HTTPException(status_code=400, detail=detail)

    _clear_failure(lockout_key)

    current_user.pin_hash = hash_pin(body.new_pin)

    # Invalidate all other sessions
    db.query(SessionModel).filter(
        SessionModel.user_id == current_user.id,
    ).delete(synchronize_session=False)

    # Create a new session for the current request
    new_token = generate_token()
    new_session = SessionModel(
        user_id=current_user.id,
        token=new_token,
        expires_at=(datetime.now(timezone.utc) + timedelta(days=settings.TOKEN_EXPIRE_DAYS)).isoformat(),
    )
    db.add(new_session)
    db.commit()

    return ApiResponse(data={"token": new_token}, message="PIN修改成功，请重新登录")


@router.post("/logout")
def logout(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
    authorization: str = Header(default=""),
):
    """Invalidate the current session token."""
    token = authorization.replace("Bearer ", "") if authorization.startswith("Bearer ") else ""
    if token:
        session = db.query(SessionModel).filter(SessionModel.token == token).first()
        if session:
            db.delete(session)
            db.commit()
    return ApiResponse(message="已登出")
