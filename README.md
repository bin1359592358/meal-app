# Meal App - 聚餐点菜应用

一款面向多人聚餐场景的点菜应用，支持"主厨"管理菜单、"食客"通过邀请码加入餐桌并点菜。采用 Android 原生客户端 + Python 后端 API 的前后端分离架构。参考"好大一颗菜"等美食类应用的 UI/UX 设计进行了全面优化。

## 功能概览

**食客端**
- 通过邀请码加入餐桌
- 按分类浏览菜品，支持口味/配料定制（辣度、配料等多维度选择）
- 购物车管理：增减菜品数量、查看合计金额
- 下单并查看订单历史与详情
- **随机选菜**：点击顶部骰子图标，"今天吃什么？"随机推荐菜品，支持"换一个"和"就它了"快捷操作
- **菜品详情**：点击菜品图片/名称区域，弹出详情面板查看完整描述、标签、调味选项
- **售罄提示**：已售罄菜品自动显示半透明遮罩和"已售罄"标识，禁止加购

**主厨端（Chef）**
- 创建餐桌并生成邀请码，分享邀请码给食客
- 菜品管理：增删改查菜品（名称、描述、价格、分类、标签、上下架）
- 口味配置：为菜品设置可选调味参数（单选/多选）
- 查看所有食客的订单，支持订单总览（按菜品聚合）

## 技术栈

| 层 | 技术 | 版本 |
|---|---|---|
| **前端** | Kotlin + Jetpack Compose + Material 3 | Compose BOM 2024.09.00, compileSdk 35 |
| **后端** | Python FastAPI + SQLAlchemy | FastAPI 0.115, SQLAlchemy 2.0 |
| **数据库** | SQLite（本地开发）/ Turso（云端部署） | libsql-client 0.3 |
| **网络** | Retrofit + OkHttp + Gson | Retrofit 2.11, OkHttp 4.12 |
| **图片** | Coil (客户端加载) + Cloudinary (可选云端存储) | Coil 2.7 |

## 项目结构

```
meal-app/
├── backend/                        # Python 后端
│   ├── main.py                     # FastAPI 入口、CORS、路由注册
│   ├── config.py                   # 环境变量配置
│   ├── database.py                 # 数据库连接与会话管理
│   ├── models.py                   # SQLAlchemy ORM 模型
│   ├── schemas.py                  # Pydantic 请求/响应模型
│   ├── auth.py                     # 认证工具（PIN 哈希、Token 生成）
│   ├── middleware.py               # Token 校验中间件
│   ├── seed_data.py                # 种子数据（演示数据填充）
│   ├── image_upload.py             # Cloudinary 图片上传
│   ├── routers/                    # API 路由模块
│   │   ├── auth.py                 # 注册 / 登录 / PIN 修改
│   │   ├── users.py                # 用户信息
│   │   ├── rooms.py                # 餐桌管理
│   │   ├── categories.py           # 分类 CRUD
│   │   ├── dishes.py               # 菜品 CRUD + 搜索 + 上下架
│   │   ├── orders.py               # 订单 + 总览
│   │   └── upload.py               # 图片上传
│   ├── requirements.txt
│   └── .env.example
│
├── android/                        # Android 客户端
│   └── app/src/main/java/com/meals/app/
│       ├── MainActivity.kt
│       ├── data/
│       │   ├── remote/             # Retrofit API 定义 + 客户端
│       │   ├── dto/                # 数据传输对象
│       │   ├── repository/         # 数据仓库层
│       │   └── local/              # SharedPreferences 本地存储
│       ├── ui/
│       │   ├── navigation/         # NavGraph 路由 + 底部导航栏
│       │   ├── theme/              # Material 3 主题色
│       │   ├── screens/            # 核心页面
│       │   │   ├── welcome/        # 登录 / 注册
│       │   │   ├── menu/           # 菜单浏览 + 购物车悬浮栏
│       │   │   ├── cart/           # 购物车确认 + 下单
│       │   │   ├── orders/         # 订单历史 + 详情
│       │   │   ├── profile/        # 个人中心
│       │   │   └── admin/          # 菜品管理 + 编辑
│       │   ├── enhancement/        # 可插拔增强模块
│       │   │   ├── overview/       # 订单总览（Chef 视角）
│       │   │   ├── roomswitch/     # 多餐桌切换
│       │   │   ├── search/         # 搜索栏
│       │   │   └── empty/          # 空状态组件
│       │   └── components/         # 共享 UI 组件
│       └── util/                   # 工具类
│
└── docs/
    └── architecture.md             # 详细架构设计文档
```

## 快速开始

### 1. 启动后端

```bash
cd backend

# 创建虚拟环境
python -m venv venv
source venv/bin/activate        # Linux/macOS
venv\Scripts\activate           # Windows

# 安装依赖
pip install -r requirements.txt

# 配置环境变量（可选，默认使用本地 SQLite）
cp .env.example .env
# 编辑 .env 填入 Turso / Cloudinary 凭据（留空则使用本地 SQLite）

# 填充种子数据（首次运行）
python seed_data.py

# 启动服务
python main.py
# 服务运行在 http://localhost:8000
# API 文档：http://localhost:8000/docs
```

### 2. 构建 Android 客户端

用 Android Studio 打开 `android/` 目录，等待 Gradle 同步完成后直接运行。

**配置服务器地址**：首次启动 App 后，在「我的」页面中设置服务器地址，本地开发填 `http://<电脑IP>:8000`（需手机和电脑在同一局域网）。

### 3. 演示账号

种子数据预置了以下账号：

| 用户名 | PIN | 昵称 | 角色 |
|--------|-----|------|------|
| `chef001` | `1234` | 主厨小王 | Chef（主厨） |

预置餐桌邀请码：`DEMO01`，包含川菜、粤菜、主食、饮品等分类及示例菜品。

## 核心页面说明

### 菜单页 (MenuScreen)
- 顶部渐变橙色标题栏，显示餐桌名称和邀请码
- 右上角骰子图标触发"今天吃什么？"随机选菜功能
- 左侧分类栏（90dp宽，暖米色背景）显示emoji图标、分类名和菜品数量角标，选中时带橙色侧边条指示器
- 右侧菜品卡片（96dp大图、暖色价格标签、紧凑标签展示、已售罄遮罩）
- 点击菜品图片/名称区域弹出详情底部面板（DishDetailSheet），展示完整描述、全部标签和调味信息
- 底部悬浮购物车栏带渐变深色背景，购物车数量变化时弹性动画出入场，总价变化带缩放动画
- 主厨视角菜品卡片显示编辑按钮
- 下拉刷新支持（Pull-to-Refresh）

### 购物车页 (CartScreen)
- 已选菜品列表，支持数量增减和删除
- 用餐人数选择、全局备注
- 订单概览卡片（菜品数、人数、总价）
- 底部固定下单按钮

### 管理后台 (AdminScreen)
- 仅主厨可见，从「我的」页面或右上角齿轮图标进入
- 左侧分类管理（增删）+ 右侧菜品列表
- 支持添加/编辑菜品、修改价格、配置口味、上下架

### 菜品编辑页 (DishEditScreen)
- 菜品基本信息：名称、描述、价格
- 分类下拉选择（ExposedDropdownMenuBox）
- 标签管理（添加/删除）
- 口味配置编辑器（SeasoningEditor）
- 上架开关 + 删除按钮

## API 接口概览

所有接口统一前缀 `/api`，需认证接口在 Header 中携带 `Authorization: Bearer <token>`。

| 模块 | 关键接口 |
|------|---------|
| 认证 | `POST /auth/register`, `POST /auth/login`, `GET /users/me` |
| 餐桌 | `POST /rooms`, `POST /rooms/join`, `GET /rooms/mine` |
| 分类 | `GET/POST /rooms/{id}/categories`, `PUT/DELETE /rooms/{id}/categories/{cid}` |
| 菜品 | `GET/POST /rooms/{id}/dishes`, `PUT/DELETE /rooms/{id}/dishes/{did}`, `PATCH /dishes/{did}/toggle` |
| 订单 | `GET/POST /rooms/{id}/orders`, `GET /rooms/{id}/orders/{oid}`, `GET /rooms/{id}/orders/summary` |

详细接口文档启动后端后访问 `http://localhost:8000/docs`（Swagger UI）。

## 数据库设计

核心表结构：

| 表名 | 说明 | 关键字段 |
|------|------|---------|
| `users` | 用户 | username(唯一), nickname, pin_hash |
| `rooms` | 餐桌 | name, code(邀请码), chef_id |
| `room_members` | 餐桌成员 | room_id, user_id, role(chef/guest) |
| `categories` | 菜品分类 | room_id, name, icon |
| `dishes` | 菜品 | category_id, name, price, seasonings(JSON), tags(JSON) |
| `orders` | 订单 | room_id, user_id, total_price, people_count |
| `order_items` | 订单明细 | order_id, dish_id, dish_name, quantity, seasoning_text |
| `sessions` | 登录会话 | user_id, token, expires_at |

## 架构特点

- **模块化分层**：核心模块（Auth → Room → Menu → Order → Admin）构成最小可用闭环，增强模块（搜索、总览、多餐桌切换等）通过扩展点独立插拔
- **MVVM 架构**：Screen → ViewModel → Repository → ApiService 单向数据流
- **共享购物车**：MenuViewModel 与 CartViewModel 通过 CartStore 单例共享状态
- **软删除**：菜品使用 `is_deleted` 标记删除，确保历史订单数据完整性
- **动画交互**：分类切换颜色过渡（200ms tween）、购物车弹性弹出（Spring.DampingRatioMediumBouncy）、数量变化缩放动画（AnimatedContent + scaleIn/scaleOut）、购物车状态边框提示、价格标签渐变、已售罄遮罩淡入淡出
- **UI 视觉优化**：暖色调背景（#F8F6F3）、渐变标题栏（OrangeLight→OrangePrimary）、圆角菜品卡片（14dp）、暖色价格标签背景（#FFF0EB 圆角药丸）、分类emoji图标+菜品数量角标、友好的空状态插图
- **功能增强**：随机选菜对话框（RandomDishPickerDialog）支持"换一个"重新随机；菜品详情底部面板（DishDetailSheet）展示完整信息并支持加购；已售罄菜品半透明遮罩+禁止加购

## 已知限制与待完善项

- 图片上传功能框架已搭建，但 UI 中的图片选择器尚未完整对接
- Cloudinary 图片存储为可选配置，未配置时菜品使用 emoji 占位图
- 菜品搜索功能（SearchBar）已有代码框架，搜索栏组件已创建但未集成到主界面
- 订单状态目前仅支持 pending → completed/cancelled 的简单流转
- 多餐桌切换（RoomSwitch）模块已有代码框架，但未完全集成到主流程
- 本地 SQLite 模式下不支持多设备数据同步，需配置 Turso 云端数据库

## 开发环境要求

- **Android Studio**：Ladybug (2024.2) 或更高版本
- **JDK**：17+（Android Studio 自带 JetBrains Runtime）
- **Python**：3.10+
- **Android SDK**：compileSdk 35, minSdk 26
- **Gradle**：8.x（通过 Gradle Wrapper 自动下载）
