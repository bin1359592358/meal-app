"""Category routes: CRUD operations scoped to a room."""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from database import get_db
from middleware import require_chef, require_member
from models import Category, Dish, User
from schemas import ApiResponse, CategoryCreate, CategoryResponse, CategoryUpdate

router = APIRouter(prefix="/rooms/{room_id}/categories")


@router.get("")
def list_categories(
    room_id: int,
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(50, ge=1, le=100, description="Items per page"),
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """List all categories in the room, ordered by sort_order, with dish counts."""
    base_query = (
        db.query(Category)
        .filter(Category.room_id == room_id)
        .order_by(Category.sort_order)
    )

    total = base_query.count()
    categories = base_query.offset((page - 1) * page_size).limit(page_size).all()

    result = []
    for cat in categories:
        dish_count = (
            db.query(Dish)
            .filter(
                Dish.category_id == cat.id,
                Dish.is_deleted == False,
            )
            .count()
        )
        result.append(
            CategoryResponse(
                id=cat.id,
                room_id=cat.room_id,
                name=cat.name,
                icon=cat.icon or "",
                sort_order=cat.sort_order,
                dish_count=dish_count,
            ).model_dump()
        )

    return ApiResponse(
        data=result,
        total=total,
        page=page,
        page_size=page_size,
    )


@router.post("")
def create_category(
    room_id: int,
    body: CategoryCreate,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Create a new category in the room. Chef only."""
    category = Category(
        room_id=room_id,
        name=body.name,
        icon=body.icon,
        sort_order=body.sort_order,
    )
    db.add(category)
    db.commit()
    db.refresh(category)

    return ApiResponse(
        data=CategoryResponse.model_validate(category).model_dump()
    )


@router.put("/{category_id}")
def update_category(
    room_id: int,
    category_id: int,
    body: CategoryUpdate,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Update a category's fields. Chef only."""
    category = (
        db.query(Category)
        .filter(Category.id == category_id, Category.room_id == room_id)
        .first()
    )
    if not category:
        raise HTTPException(status_code=404, detail="分类不存在")

    update_data = body.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(category, field, value)

    db.commit()
    db.refresh(category)

    return ApiResponse(
        data=CategoryResponse.model_validate(category).model_dump()
    )


@router.delete("/{category_id}")
def delete_category(
    room_id: int,
    category_id: int,
    current_user: User = Depends(require_chef),
    db: Session = Depends(get_db),
):
    """Delete a category. Fails if the category still contains dishes."""
    category = (
        db.query(Category)
        .filter(Category.id == category_id, Category.room_id == room_id)
        .first()
    )
    if not category:
        raise HTTPException(status_code=404, detail="分类不存在")

    # Check for non-deleted dishes in this category
    dish_count = (
        db.query(Dish)
        .filter(Dish.category_id == category_id, Dish.is_deleted == False)
        .count()
    )
    if dish_count > 0:
        raise HTTPException(status_code=400, detail="该分类下还有菜品，无法删除")

    db.delete(category)
    db.commit()

    return ApiResponse(message="分类已删除")
