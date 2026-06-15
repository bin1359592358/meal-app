"""SQLAlchemy ORM models for the meal ordering application."""

from datetime import datetime, timezone

from sqlalchemy import (
    Boolean,
    Column,
    Float,
    ForeignKey,
    Integer,
    String,
    Text,
    UniqueConstraint,
)
from sqlalchemy.orm import relationship

from database import Base


def _iso_now() -> str:
    """Return the current UTC time as an ISO 8601 string."""
    return datetime.now(timezone.utc).isoformat()


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, autoincrement=True)
    username = Column(String(20), unique=True, nullable=False, index=True)
    nickname = Column(String(50), nullable=False)
    pin_hash = Column(String(128), nullable=False)
    created_at = Column(String(30), nullable=False, default=_iso_now)

    # Relationships
    sessions = relationship("Session", back_populates="user", cascade="all, delete-orphan")
    owned_rooms = relationship("Room", back_populates="chef", foreign_keys="Room.chef_id")
    room_memberships = relationship("RoomMember", back_populates="user")
    orders = relationship("Order", back_populates="user")


class Session(Base):
    __tablename__ = "sessions"

    id = Column(Integer, primary_key=True, autoincrement=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    token = Column(String(64), unique=True, nullable=False, index=True)
    created_at = Column(String(30), nullable=False, default=_iso_now)
    expires_at = Column(String(30), nullable=False)

    # Relationships
    user = relationship("User", back_populates="sessions")


class Room(Base):
    __tablename__ = "rooms"

    id = Column(Integer, primary_key=True, autoincrement=True)
    name = Column(String(50), nullable=False)
    code = Column(String(6), unique=True, nullable=False, index=True)
    chef_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    is_active = Column(Boolean, default=True)
    created_at = Column(String(30), nullable=False, default=_iso_now)

    # Relationships
    chef = relationship("User", back_populates="owned_rooms", foreign_keys=[chef_id])
    members = relationship("RoomMember", back_populates="room", cascade="all, delete-orphan")
    categories = relationship("Category", back_populates="room", cascade="all, delete-orphan")
    dishes = relationship("Dish", back_populates="room", cascade="all, delete-orphan")
    orders = relationship("Order", back_populates="room", cascade="all, delete-orphan")


class RoomMember(Base):
    __tablename__ = "room_members"
    __table_args__ = (
        UniqueConstraint("room_id", "user_id", name="uq_room_member"),
    )

    id = Column(Integer, primary_key=True, autoincrement=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    role = Column(String(10), nullable=False)  # "chef" or "member"
    joined_at = Column(String(30), nullable=False, default=_iso_now)

    # Relationships
    room = relationship("Room", back_populates="members")
    user = relationship("User", back_populates="room_memberships")


class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, autoincrement=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False, index=True)
    name = Column(String(30), nullable=False)
    icon = Column(String(10), default="")
    sort_order = Column(Integer, default=0)

    # Relationships
    room = relationship("Room", back_populates="categories")
    dishes = relationship("Dish", back_populates="category")


class Dish(Base):
    __tablename__ = "dishes"

    id = Column(Integer, primary_key=True, autoincrement=True)
    category_id = Column(Integer, ForeignKey("categories.id"), nullable=False, index=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False, index=True)
    name = Column(String(50), nullable=False)
    description = Column(String(200), default="")
    price = Column(Float, nullable=False)
    image_url = Column(String(500), nullable=True)
    is_available = Column(Boolean, default=True)
    is_deleted = Column(Boolean, default=False)
    tags = Column(Text, default="[]")
    seasonings = Column(Text, default="[]")
    sort_order = Column(Integer, default=0)

    # Relationships
    category = relationship("Category", back_populates="dishes")
    room = relationship("Room", back_populates="dishes")


class Order(Base):
    __tablename__ = "orders"

    id = Column(Integer, primary_key=True, autoincrement=True)
    room_id = Column(Integer, ForeignKey("rooms.id"), nullable=False, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False, index=True)
    total_price = Column(Float, nullable=False)
    status = Column(String(20), default="pending")
    note = Column(String(500), default="")
    people_count = Column(Integer, default=1)
    created_at = Column(String(30), nullable=False, default=_iso_now)

    # Relationships
    room = relationship("Room", back_populates="orders")
    user = relationship("User", back_populates="orders")
    items = relationship("OrderItem", back_populates="order", cascade="all, delete-orphan")


class OrderItem(Base):
    __tablename__ = "order_items"

    id = Column(Integer, primary_key=True, autoincrement=True)
    order_id = Column(Integer, ForeignKey("orders.id"), nullable=False, index=True)
    dish_id = Column(Integer, nullable=False)
    dish_name = Column(String(50), nullable=False)
    dish_price = Column(Float, nullable=False)
    quantity = Column(Integer, nullable=False)
    seasoning_text = Column(String(500), default="")

    # Relationships
    order = relationship("Order", back_populates="items")
