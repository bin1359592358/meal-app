"""Dish routes: CRUD, search, and availability toggle scoped to a room."""

import json
from typing import Optional

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from database import get_db
from middleware import require_chef, require_member
from models import Category, Dish, User
from schemas import ApiResponse, DishCreate, DishResponse, DishUpdate

router = APIRouter(prefix="/rooms/{room_id}/dishes")


@router.get("")
def list_dishes(
    room_id: int,
    category_id: Optional[int] = Query(None, description="Filter by category"),
    q: Optional[str] = Query(None, description="Search dish name"),
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(100, ge=1, le=200, description="Items per page"),
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """List non-deleted dishes in the room, optionally filtered or searched."""
    base_query = (
        db.query(Dish)
        .filter(Dish.room_id == room_id, Dish.is_deleted == False)
    )

    if category_id is not None:
        base_query = base_query.filter(Dish.category_id == category_id)

    if q:
        escaped_q = q.replace("%", r"\%").replace("_", r"\_")
        base_query = base_query.filter(Dish.name.like(f"%{escaped_q}%", escape="\\"))

    base_query = base_query.order_by(Dish.sort_order)
    total = base_query.count()
    dishes = base_query.offset((page - 1) * page_size).limit(page_size).all()

    return ApiResponse(
        data=[DishResponse.model_validate(d).model_dump() for d in dishes],
        total=total,
        page=page,
        page_size=page_size,
    )


@router.get("/{dish_id}")
def get_dish(
    room_id: int,
    dish_id: int,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Get a single dish by ID."""
    dish = (
        db.query(Dish)
        .filter(
            Dish.id == dish_id,
            Dish.room_id == room_id,
            Dish.is_deleted == False,
        )
        .first()
    )
    if not dish:
        raise HTTPException(status_code=404, detail="菜品不存在")

    return ApiResponse(data=DishResponse.model_validate(dish).model_dump())


@router.post("")
def create_dish(
    room_id: int,
    body: DishCreate,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Create a new dish. Chef only."""
    # Validate category belongs to this room
    category = db.query(Category).filter(Category.id == body.category_id, Category.room_id == room_id).first()
    if not category:
        raise HTTPException(status_code=400, detail="分类不存在或不属于此餐桌")

    dish = Dish(
        category_id=body.category_id,
        room_id=room_id,
        name=body.name,
        description=body.description,
        price=body.price,
        image_url=body.image_url,
        tags=body.tags_json(),
        seasonings=body.seasonings_json(),
        sort_order=body.sort_order,
        is_available=body.is_available,
    )
    db.add(dish)
    db.commit()
    db.refresh(dish)

    return ApiResponse(data=DishResponse.model_validate(dish).model_dump())


@router.put("/{dish_id}")
def update_dish(
    room_id: int,
    dish_id: int,
    body: DishUpdate,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Update a dish's fields. Chef only."""
    dish = (
        db.query(Dish)
        .filter(Dish.id == dish_id, Dish.room_id == room_id)
        .first()
    )
    if not dish:
        raise HTTPException(status_code=404, detail="菜品不存在")

    # Validate category belongs to this room if provided
    if body.category_id is not None:
        category = db.query(Category).filter(Category.id == body.category_id, Category.room_id == room_id).first()
        if not category:
            raise HTTPException(status_code=400, detail="分类不存在或不属于此餐桌")

    update_data = body.model_dump(exclude_unset=True)

    # Serialize list fields to JSON strings for storage
    if "tags" in update_data:
        update_data["tags"] = json.dumps(
            update_data["tags"] or [], ensure_ascii=False
        )
    if "seasonings" in update_data:
        update_data["seasonings"] = json.dumps(
            update_data["seasonings"] or [], ensure_ascii=False
        )

    for field, value in update_data.items():
        setattr(dish, field, value)

    db.commit()
    db.refresh(dish)

    return ApiResponse(data=DishResponse.model_validate(dish).model_dump())


@router.delete("/{dish_id}")
def delete_dish(
    room_id: int,
    dish_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Soft-delete a dish by setting is_deleted=True. Chef only."""
    dish = (
        db.query(Dish)
        .filter(Dish.id == dish_id, Dish.room_id == room_id)
        .first()
    )
    if not dish:
        raise HTTPException(status_code=404, detail="菜品不存在")

    dish.is_deleted = True
    db.commit()

    return ApiResponse(message="菜品已删除")


@router.patch("/{dish_id}/toggle")
def toggle_dish(
    room_id: int,
    dish_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Toggle a dish's availability status. Chef only."""
    dish = (
        db.query(Dish)
        .filter(Dish.id == dish_id, Dish.room_id == room_id)
        .first()
    )
    if not dish:
        raise HTTPException(status_code=404, detail="菜品不存在")

    dish.is_available = not dish.is_available
    db.commit()
    db.refresh(dish)

    return ApiResponse(data=DishResponse.model_validate(dish).model_dump())
