"""Authentication and authorization middleware / dependencies."""

from datetime import datetime, timezone
from typing import Optional

from fastapi import Depends, Header, HTTPException, Path
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy.orm import Session

from database import get_db
from models import Room, RoomMember, Session as SessionModel, User

security = HTTPBearer()


def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db),
) -> User:
    """Extract and validate the bearer token, returning the authenticated user.

    Raises HTTPException(401) if the token is invalid or expired.
    """
    token = credentials.credentials

    session = (
        db.query(SessionModel)
        .filter(SessionModel.token == token)
        .first()
    )

    if session is None:
        raise HTTPException(status_code=401, detail="Invalid or missing token.")

    # Check expiration
    try:
        expires_at = datetime.fromisoformat(session.expires_at)
        if expires_at.tzinfo is None:
            expires_at = expires_at.replace(tzinfo=timezone.utc)
    except (ValueError, TypeError):
        raise HTTPException(status_code=401, detail="Invalid session expiration.")

    if datetime.now(timezone.utc) > expires_at:
        # Clean up expired session
        db.delete(session)
        db.commit()
        raise HTTPException(status_code=401, detail="Token has expired.")

    user = db.query(User).filter(User.id == session.user_id).first()
    if user is None:
        raise HTTPException(status_code=401, detail="User not found.")

    return user


def get_current_session_token(authorization: str = Header(default="")) -> Optional[str]:
    """Extract and return the bearer token from the Authorization header.

    Returns None if the header is missing or malformed.
    """
    if authorization.startswith("Bearer "):
        return authorization[7:]
    return None


def require_chef(
    room_id: int = Path(..., description="Room ID"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> User:
    """Ensure the current user is the chef (owner) of the specified room.

    Raises HTTPException(403) if the user is not the room's chef.
    """
    room = db.query(Room).filter(Room.id == room_id).first()
    if room is None:
        raise HTTPException(status_code=404, detail="Room not found.")

    if room.chef_id != current_user.id:
        raise HTTPException(
            status_code=403,
            detail="Only the room chef can perform this action.",
        )

    return current_user


def require_member(
    room_id: int = Path(..., description="Room ID"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> User:
    """Ensure the current user is a member of the specified room.

    Raises HTTPException(403) if the user is not a member.
    """
    membership = (
        db.query(RoomMember)
        .filter(
            RoomMember.room_id == room_id,
            RoomMember.user_id == current_user.id,
        )
        .first()
    )

    if membership is None:
        raise HTTPException(
            status_code=403,
            detail="You are not a member of this room.",
        )

    return current_user
