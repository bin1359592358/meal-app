"""User profile routes: view and update current user info."""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from database import get_db
from middleware import get_current_user
from models import User
from schemas import ApiResponse, NicknameUpdate, UserResponse

router = APIRouter(prefix="/users")


@router.get("/me")
def get_me(current_user: User = Depends(get_current_user)):
    """Return the authenticated user's profile."""
    return ApiResponse(data=UserResponse.model_validate(current_user).model_dump())


@router.put("/me")
def update_me(
    body: NicknameUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Update the authenticated user's nickname."""
    current_user.nickname = body.nickname
    db.commit()
    db.refresh(current_user)

    return ApiResponse(data=UserResponse.model_validate(current_user).model_dump())
