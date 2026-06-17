"""Seed the database with sample data for development and testing."""

import json
import sys
from datetime import datetime, timedelta, timezone

# Ensure models are registered with Base before create_all is called.
from database import Base, SessionLocal, engine
from models import (
    Category,
    Dish,
    Order,
    OrderItem,
    Room,
    RoomMember,
    Session,
    User,
)
from auth import hash_pin, generate_token
from config import settings


def _iso_now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _iso_future(days: int) -> str:
    return (datetime.now(timezone.utc) + timedelta(days=days)).isoformat()


def seed() -> None:
    """Populate the database with sample data."""

    # ── Step 1: Create tables ──────────────────────────────────────────────
    print("[1/10] Creating tables (if not exist)...")
    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        # ── Step 2: Check existing data ─────────────────────────────────────
        existing_users = db.query(User).count()
        if existing_users > 0:
            print(f"[!] Database already contains {existing_users} user(s). Skipping seed.")
            print("    To re-seed, delete the database file (meal_app.db) and run again.")
            return

        print("[2/10] Database is empty. Proceeding with seed...")

        # ── Step 3: Create demo chef user ────────────────────────────────────
        print("[3/10] Creating demo chef user...")
        chef = User(
            username="chef001",
            nickname="主厨小王",
            pin_hash=hash_pin("1234"),
            created_at=_iso_now(),
        )
        db.add(chef)
        db.flush()  # flush to get chef.id

        # ── Step 4: Create room ──────────────────────────────────────────────
        print("[4/10] Creating room...")
        room = Room(
            name="周末聚餐",
            code="DEMO01",
            chef_id=chef.id,
            is_active=True,
            created_at=_iso_now(),
        )
        db.add(room)
        db.flush()

        # ── Step 5: Create RoomMember ────────────────────────────────────────
        print("[5/10] Creating room membership (chef)...")
        membership = RoomMember(
            room_id=room.id,
            user_id=chef.id,
            role="chef",
            joined_at=_iso_now(),
        )
        db.add(membership)

        # ── Step 6: Create session token ─────────────────────────────────────
        print("[6/10] Creating session token for chef...")
        token = generate_token()
        session = Session(
            user_id=chef.id,
            token=token,
            created_at=_iso_now(),
            expires_at=_iso_future(settings.TOKEN_EXPIRE_DAYS),
        )
        db.add(session)

        # ── Step 7: Create categories ────────────────────────────────────────
        print("[7/10] Creating categories...")
        cat_data = [
            ("🌶️", "川菜", 1),
            ("🥬", "粤菜", 2),
            ("🍚", "主食", 3),
            ("🥤", "饮品", 4),
        ]
        categories: dict[str, Category] = {}
        for icon, name, order in cat_data:
            cat = Category(
                room_id=room.id,
                name=name,
                icon=icon,
                sort_order=order,
            )
            db.add(cat)
            categories[name] = cat
        db.flush()

        # ── Step 8: Create dishes ────────────────────────────────────────────
        print("[8/10] Creating dishes...")

        # Seasoning templates
        spice_select = json.dumps(
            {"辣度": {"type": "single", "options": ["微辣", "中辣", "重辣"]}},
            ensure_ascii=False,
        )
        spice_and_toppings = json.dumps(
            {
                "辣度": {"type": "single", "options": ["微辣", "中辣", "重辣"]},
                "配料": {"type": "multi", "options": ["加葱", "加蒜", "加香菜", "加花生碎"]},
            },
            ensure_ascii=False,
        )
        sweetness = json.dumps(
            {"甜度": {"type": "single", "options": ["少糖", "正常糖", "多糖"]}},
            ensure_ascii=False,
        )

        dishes_data = [
            # ── 川菜 ──
            ("川菜", "麻婆豆腐", 28.0, "经典川味名菜，麻辣鲜香，嫩滑入味", spice_and_toppings, 1),
            ("川菜", "回锅肉", 38.0, "蒜苗炒制五花肉，肥而不腻，下饭首选", spice_select, 2),
            ("川菜", "宫保鸡丁", 35.0, "鸡丁配花生米，糊辣荔枝味型", spice_select, 3),
            ("川菜", "水煮鱼", 58.0, "鲜嫩鱼片，麻辣汤底，配豆芽莴笋", spice_and_toppings, 4),
            ("川菜", "鱼香肉丝", 32.0, "甜酸辣味，下饭经典，配木耳笋丝", spice_select, 5),
            # ── 粤菜 ──
            ("粤菜", "白切鸡", 48.0, "整鸡白切，皮爽肉滑，蘸姜葱酱", "[]", 1),
            ("粤菜", "烧鹅", 68.0, "广式烧腊招牌，皮脆肉嫩，酱汁浓郁", "[]", 2),
            ("粤菜", "蚝油生菜", 18.0, "清脆生菜淋蚝油，简单鲜美", "[]", 3),
            ("粤菜", "清蒸鲈鱼", 58.0, "鲜活鲈鱼清蒸，豉油调味，原汁原味", "[]", 4),
            # ── 主食 ──
            ("主食", "蛋炒饭", 15.0, "粒粒分明，蛋香浓郁，经典快手主食", "[]", 1),
            ("主食", "阳春面", 12.0, "清汤挂面，葱花点缀，朴素鲜美", "[]", 2),
            ("主食", "手工水饺", 22.0, "猪肉白菜馅，手工现包，皮薄馅大", "[]", 3),
            # ── 饮品 ──
            ("饮品", "酸梅汤", 8.0, "传统酸梅汤，冰镇解腻，酸甜开胃", sweetness, 1),
            ("饮品", "冰红茶", 6.0, "经典冰红茶，清爽解渴", sweetness, 2),
            ("饮品", "鲜榨橙汁", 18.0, "当季鲜橙现榨，富含维C，天然健康", sweetness, 3),
            ("饮品", "椰子水", 12.0, "天然椰子水，清甜解暑", "[]", 4),
        ]

        dish_count = 0
        for cat_name, name, price, desc, seasonings, order in dishes_data:
            dish = Dish(
                category_id=categories[cat_name].id,
                room_id=room.id,
                name=name,
                description=desc,
                price=price,
                is_available=True,
                is_deleted=False,
                tags="[]",
                seasonings=seasonings,
                sort_order=order,
            )
            db.add(dish)
            dish_count += 1

        # ── Step 9: Commit ───────────────────────────────────────────────────
        print("[9/10] Committing to database...")
        db.commit()

        # ── Step 10: Summary ─────────────────────────────────────────────────
        print("[10/10] Seed completed successfully!\n")
        print("=" * 55)
        print("  Seed Summary")
        print("=" * 55)
        print(f"  Users:       1  (chef001)")
        print(f"  Rooms:       1  (周末聚餐 / DEMO01)")
        print(f"  Memberships: 1  (chef)")
        print(f"  Sessions:    1  (active token)")
        print(f"  Categories:  {len(categories)}")
        print(f"  Dishes:      {dish_count}")
        print("=" * 55)

        print()
        print("-" * 55)
        print("  Chef Login Credentials (for API testing)")
        print("-" * 55)
        print(f"  Username:  chef001")
        print(f"  PIN:       1234")
        print(f"  Token:     {token}")
        print(f"  Room Code: DEMO01")
        print("-" * 55)
        print()
        print("You can now start the server with:  python main.py")
        print("Then test with: GET http://localhost:8000/api/health")

    except Exception as exc:
        db.rollback()
        print(f"[ERROR] Seed failed: {exc}", file=sys.stderr)
        raise
    finally:
        db.close()


if __name__ == "__main__":
    seed()
