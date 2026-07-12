"""Order routes: create, list, detail, status management, and summary."""

import json
from collections import defaultdict

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session

from auth import validate_and_serialize_seasonings
from database import get_db
from middleware import require_member
from models import Dish, Order, OrderItem, Room, User
from schemas import (
    ApiResponse,
    OrderCreate,
    OrderItemCreate,
    OrderItemResponse,
    OrderResponse,
    OrderSummaryItem,
    OrderSummaryResponse,
    StatusUpdate,
)

router = APIRouter(prefix="/rooms/{room_id}/orders")

# Allowed status transitions: current_status -> set of valid next statuses
VALID_TRANSITIONS: dict[str, set[str]] = {
    "pending": {"preparing", "cancelled"},
    "preparing": {"served", "cancelled"},
    "served": {"completed"},
    "completed": set(),
    "cancelled": set(),
}


def _is_chef(room_id: int, user_id: int, db: Session) -> bool:
    """Return True if the user is the chef of the given room."""
    room = db.query(Room).filter(Room.id == room_id).first()
    return room is not None and room.chef_id == user_id


def _build_order_response(order: Order, user_nickname: str) -> dict:
    """Convert an Order ORM instance into an OrderResponse dict."""
    items = None
    if order.items:
        items = [OrderItemResponse.from_item(i).model_dump() for i in order.items]

    return OrderResponse(
        id=order.id,
        room_id=order.room_id,
        user_id=order.user_id,
        user_nickname=user_nickname,
        total_price=order.total_price,
        status=order.status,
        note=order.note or "",
        people_count=order.people_count,
        created_at=order.created_at,
        items=items,
    ).model_dump()


@router.get("/summary")
def order_summary(
    room_id: int,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """返回进行中订单汇总及已完成营业统计。"""
    if not _is_chef(room_id, current_user.id, db):
        raise HTTPException(status_code=403, detail="Only the room chef can perform this action.")

    active_orders = (
        db.query(Order)
        .filter(
            Order.room_id == room_id,
            Order.status.in_(["pending", "preparing", "served"]),
        )
        .all()
    )

    completed_orders = (
        db.query(Order)
        .filter(Order.room_id == room_id, Order.status == "completed")
        .all()
    )

    def aggregate(orders: list[Order]) -> list[OrderSummaryItem]:
        """按菜品 ID 聚合，避免同名菜品互相合并。"""
        agg: dict[tuple[int, str], dict] = defaultdict(
        lambda: {"total_quantity": 0, "order_ids": set(), "seasonings": []}
        )
        for order in orders:
            for item in order.items:
                key = (item.dish_id, item.dish_name)
                entry = agg[key]
                entry["total_quantity"] += item.quantity
                entry["order_ids"].add(order.id)
                if item.seasoning_text:
                    entry["seasonings"].append(item.seasoning_text)
        return [
            OrderSummaryItem(
                dish_id=dish_id,
                dish_name=dish_name,
                total_quantity=data["total_quantity"],
                order_count=len(data["order_ids"]),
                seasonings_list=data["seasonings"],
            )
            for (dish_id, dish_name), data in agg.items()
        ]

    active_amount = round(sum(order.total_price for order in active_orders), 2)
    revenue = round(sum(order.total_price for order in completed_orders), 2)

    return ApiResponse(
        data=OrderSummaryResponse(
            summary=aggregate(active_orders),
            total_orders=len(active_orders),
            total_price=active_amount,
            completed_orders=len(completed_orders),
            revenue=revenue,
            sales_summary=aggregate(completed_orders),
        ).model_dump()
    )


@router.get("")
def list_orders(
    room_id: int,
    page: int = Query(1, ge=1, description="Page number"),
    page_size: int = Query(20, ge=1, le=100, description="Items per page"),
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """List orders in the room. Chef sees all; guests see only their own."""
    is_chef = _is_chef(room_id, current_user.id, db)

    query = (
        db.query(Order, User.nickname)
        .join(User, Order.user_id == User.id)
        .filter(Order.room_id == room_id)
    )

    if not is_chef:
        query = query.filter(Order.user_id == current_user.id)

    orders = (
        query.order_by(Order.created_at.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )

    result = [_build_order_response(order, nickname) for order, nickname in orders]

    return ApiResponse(data=result)


@router.get("/{order_id}")
def get_order(
    room_id: int,
    order_id: int,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Get a single order with its items. Guests can only view their own."""
    order = (
        db.query(Order)
        .filter(Order.id == order_id, Order.room_id == room_id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=404, detail="订单不存在")

    is_chef = _is_chef(room_id, current_user.id, db)
    if not is_chef and order.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="无权查看此订单")

    # Fetch the user's nickname
    order_user = db.query(User).filter(User.id == order.user_id).first()
    nickname = order_user.nickname if order_user else ""

    return ApiResponse(data=_build_order_response(order, nickname))


@router.post("")
def create_order(
    room_id: int,
    body: OrderCreate,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Create a new order with validated dish items."""
    if not body.items:
        raise HTTPException(status_code=400, detail="订单至少需要包含一个菜品")

    # Block orders on closed rooms
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room or not room.is_active:
        raise HTTPException(status_code=400, detail="餐桌已关闭，无法下单")

    # Merge duplicate dishes, keyed by (dish_id, canonical seasonings JSON)
    # so items with different customizations stay separate.
    merged_items: dict[tuple, OrderItemCreate] = {}
    for item in body.items:
        seasonings_key = json.dumps(item.seasonings or {}, sort_keys=True, ensure_ascii=False)
        merge_key = (item.dish_id, seasonings_key)
        if merge_key in merged_items:
            merged_items[merge_key].quantity += item.quantity
        else:
            merged_items[merge_key] = item
    body_items = list(merged_items.values())

    # Re-validate merged quantity ceiling
    for merge_key, merged in merged_items.items():
        if merged.quantity > 999:
            raise HTTPException(
                status_code=400,
                detail=f"单品数量超过上限（最大999）",
            )

    # Phase 1: validate all dishes and collect info
    dish_map: dict[int, Dish] = {}
    for item in body_items:
        dish = (
            db.query(Dish)
            .filter(Dish.id == item.dish_id, Dish.room_id == room_id)
            .first()
        )
        if not dish:
            raise HTTPException(status_code=404, detail=f"菜品 {item.dish_id} 不存在")
        if dish.is_deleted:
            raise HTTPException(status_code=400, detail=f"菜品 {dish.name} 已删除")
        if not dish.is_available:
            raise HTTPException(status_code=400, detail=f"菜品 {dish.name} 当前不可用")
        dish_map[item.dish_id] = dish

    # Phase 2: calculate total and build order items
    total_price = 0.0
    order = Order(
        room_id=room_id,
        user_id=current_user.id,
        total_price=0.0,  # placeholder; updated below
        note=body.note,
        people_count=body.people_count,
    )
    db.add(order)
    db.flush()  # get order.id

    for item in body_items:
        dish = dish_map[item.dish_id]
        seasoning_text = ""
        if item.seasonings:
            # Parse the dish's seasoning definitions from JSON
            try:
                seasoning_defs = json.loads(dish.seasonings or "{}")
            except (json.JSONDecodeError, TypeError):
                seasoning_defs = {}
            seasoning_text = validate_and_serialize_seasonings(
                seasoning_defs, item.seasonings
            )

        subtotal = dish.price * item.quantity
        total_price += subtotal

        order_item = OrderItem(
            order_id=order.id,
            dish_id=dish.id,
            dish_name=dish.name,
            dish_price=dish.price,
            quantity=item.quantity,
            seasoning_text=seasoning_text,
        )
        db.add(order_item)

    order.total_price = round(total_price, 2)
    db.commit()
    db.refresh(order)

    # Fetch nickname for the response
    nickname = current_user.nickname

    return ApiResponse(data=_build_order_response(order, nickname))


@router.patch("/{order_id}/status")
def update_order_status(
    room_id: int,
    order_id: int,
    body: StatusUpdate,
    current_user: User = Depends(require_member),
    db: Session = Depends(get_db),
):
    """Update an order's status with transition validation."""
    order = (
        db.query(Order)
        .filter(Order.id == order_id, Order.room_id == room_id)
        .first()
    )
    if not order:
        raise HTTPException(status_code=404, detail="订单不存在")

    # Block status updates on closed rooms (except cancellation)
    room = db.query(Room).filter(Room.id == room_id).first()
    if not room or not room.is_active:
        if body.status != "cancelled":
            raise HTTPException(status_code=400, detail="餐桌已关闭，无法更新订单状态")

    is_chef = _is_chef(room_id, current_user.id, db)

    # Guests can only cancel their own pending orders
    if not is_chef:
        if order.user_id != current_user.id:
            raise HTTPException(status_code=403, detail="无权修改此订单")
        if order.status != "pending" or body.status != "cancelled":
            raise HTTPException(
                status_code=400,
                detail="客人只能取消待处理的订单",
            )

    # Validate status transition
    allowed = VALID_TRANSITIONS.get(order.status, set())
    if body.status not in allowed:
        raise HTTPException(
            status_code=400,
            detail=f"无法从 {order.status} 转为 {body.status}",
        )

    order.status = body.status
    db.commit()
    db.refresh(order)

    # Fetch nickname for the response
    order_user = db.query(User).filter(User.id == order.user_id).first()
    nickname = order_user.nickname if order_user else ""

    return ApiResponse(data=_build_order_response(order, nickname))
