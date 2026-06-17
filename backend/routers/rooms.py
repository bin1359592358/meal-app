"""Room management routes: create, join, view, leave, and member removal."""

import secrets
import string

from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from database import get_db
from middleware import get_current_user, require_chef, require_member
from models import Room, RoomMember, User
from schemas import (
    ApiResponse,
    MemberResponse,
    RoomCreate,
    RoomJoin,
    RoomRename,
    RoomResponse,
)

router = APIRouter(prefix="/rooms")


def _generate_room_code(length: int = 6) -> str:
    """Generate a random 6-character code using uppercase letters and digits."""
    chars = string.ascii_uppercase + string.digits
    return "".join(secrets.choice(chars) for _ in range(length))


def _build_room_response(room: Room, db: Session) -> dict:
    """Build a RoomResponse dict including members with user info."""
    members = (
        db.query(RoomMember, User)
        .join(User, RoomMember.user_id == User.id)
        .filter(RoomMember.room_id == room.id)
        .all()
    )

    member_list = [
        MemberResponse(
            id=m.id,
            user_id=m.user_id,
            username=u.username,
            nickname=u.nickname,
            role=m.role,
            joined_at=m.joined_at,
        )
        for m, u in members
    ]

    return RoomResponse(
        id=room.id,
        name=room.name,
        code=room.code,
        chef_id=room.chef_id,
        is_active=room.is_active,
        created_at=room.created_at,
        members=member_list,
    ).model_dump()


@router.post("")
def create_room(
    body: RoomCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Create a new room. The creator becomes the chef."""
    # Generate a unique room code
    code = _generate_room_code()
    while db.query(Room).filter(Room.code == code).first():
        code = _generate_room_code()

    room = Room(name=body.name, code=code, chef_id=current_user.id)
    db.add(room)
    db.flush()

    # Add creator as chef member
    member = RoomMember(room_id=room.id, user_id=current_user.id, role="chef")
    db.add(member)

    try:
        db.commit()
    except IntegrityError:
        db.rollback()
        # Retry with a new code (race condition: duplicate code)
        room.code = _generate_room_code()
        db.add(room)
        db.add(member)
        db.commit()

    db.refresh(room)

    return ApiResponse(data=_build_room_response(room, db))


@router.post("/join")
def join_room(
    body: RoomJoin,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Join an existing room by its invitation code."""
    room = (
        db.query(Room)
        .filter(Room.code == body.code, Room.is_active == True)
        .first()
    )
    if not room:
        raise HTTPException(status_code=404, detail="餐桌不存在或已关闭")

    existing = (
        db.query(RoomMember)
        .filter(RoomMember.room_id == room.id, RoomMember.user_id == current_user.id)
        .first()
    )
    if existing:
        raise HTTPException(status_code=400, detail="你已经是该餐桌的成员")

    member = RoomMember(room_id=room.id, user_id=current_user.id, role="guest")
    db.add(member)
    db.commit()
    db.refresh(room)

    return ApiResponse(data=_build_room_response(room, db))


@router.get("/mine")
def get_my_rooms(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Return all rooms the current user belongs to."""
    memberships = (
        db.query(RoomMember, Room)
        .join(Room, RoomMember.room_id == Room.id)
        .filter(RoomMember.user_id == current_user.id)
        .all()
    )

    rooms = [
        _build_room_response(room, db) for _, room in memberships
    ]

    return ApiResponse(data=rooms)


@router.get("/{room_id}")
def get_room(
    room_id: int,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Get room details. Requires the caller to be a room member."""
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room:
        raise HTTPException(status_code=404, detail="餐桌不存在")

    return ApiResponse(data=_build_room_response(room, db))


@router.delete("/{room_id}/leave")
def leave_room(
    room_id: int,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Leave a room. The chef cannot leave their own room."""
    membership = (
        db.query(RoomMember)
        .filter(RoomMember.room_id == room_id, RoomMember.user_id == current_user.id)
        .first()
    )

    if membership.role == "chef":
        raise HTTPException(status_code=400, detail="主厨不能退出餐桌")

    db.delete(membership)
    db.commit()

    return ApiResponse(message="已退出餐桌")


@router.delete("/{room_id}/members/{user_id}")
def remove_member(
    room_id: int,
    user_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Remove a member from the room. Only the chef can do this."""
    if user_id == current_user.id:
        raise HTTPException(status_code=400, detail="不能移除自己")

    membership = (
        db.query(RoomMember)
        .filter(RoomMember.room_id == room_id, RoomMember.user_id == user_id)
        .first()
    )
    if not membership:
        raise HTTPException(status_code=404, detail="成员不存在")

    db.delete(membership)
    db.commit()

    return ApiResponse(message="成员已移除")


@router.put("/{room_id}")
def rename_room(
    room_id: int,
    body: RoomRename,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Rename a room. Only the chef can do this."""
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room:
        raise HTTPException(status_code=404, detail="餐桌不存在")

    room.name = body.name
    db.commit()
    db.refresh(room)

    return ApiResponse(data=_build_room_response(room, db))


@router.patch("/{room_id}/close")
def close_room(
    room_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Close a room. Only the chef can do this."""
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room:
        raise HTTPException(status_code=404, detail="餐桌不存在")

    room.is_active = False
    db.commit()
    db.refresh(room)

    return ApiResponse(data=_build_room_response(room, db), message="餐桌已关闭")


@router.patch("/{room_id}/refresh-code")
def refresh_room_code(
    room_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Regenerate the invitation code for a room. Only the chef can do this."""
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room:
        raise HTTPException(status_code=404, detail="餐桌不存在")

    code = _generate_room_code()
    while db.query(Room).filter(Room.code == code, Room.id != room_id).first():
        code = _generate_room_code()

    room.code = code
    db.commit()
    db.refresh(room)

    return ApiResponse(data=_build_room_response(room, db))
