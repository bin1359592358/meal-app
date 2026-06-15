## Meal App 架构设计 v4

### 一、项目概览

安卓点菜 App，支持"主厨"管理菜单（含菜品、图片、调味参数），
邀请"食客"加入餐桌进行点菜。数据存储在云端，手机在任何网络下都可访问。

**核心概念：**
- **餐桌 (Room)**：一个共享空间，主厨创建餐桌，食客通过邀请码加入
- **主厨 (Chef)**：餐桌创建者，管理菜品和查看所有人订单
- **食客 (Guest)**：通过邀请码加入，浏览菜单和下单

**技术栈：**

| 层       | 技术                     | 说明                  |
| -------- | ------------------------ | --------------------- |
| 前端     | Kotlin + Jetpack Compose | 现代 Android UI 框架  |
| 后端     | Python FastAPI           | 轻量高性能 REST API   |
| 数据库   | Turso (云端 SQLite)      | 免费 9GB，边缘部署    |
| 部署     | Railway                  | 免费 $5/月额度        |
| 图片存储 | Cloudinary (免费层)      | 菜品图片 CDN 分发     |

---

### 二、模块分层设计

> **设计原则：** 核心模块只负责最基本的业务闭环，可优化模块通过明确的接口"插入"核心模块，
> 任何可优化模块的增删改都不影响核心模块的正常运行。

#### 模块总览

```
┌─────────────────────────────────────────────────────────┐
│                    核心模块 (Core)                        │
│  必须存在，构成 App 最小可用闭环                          │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │ 用户模块 │→│ 餐桌模块 │→│ 菜单模块 │→│ 订单模块   │ │
│  │ (Auth)   │ │ (Room)   │ │ (Menu)   │ │ (Order)    │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                  可优化模块 (Enhancement)                  │
│  独立插拔，每个模块有明确的扩展点                         │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │ 多餐桌   │ │ 订单总览 │ │ 订单状态 │ │ 搜索模块   │ │
│  │ 切换     │ │ (Overview)│ │ 细化     │ │ (Search)   │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │ 通知角标 │ │ 空状态   │ │ 图片上传 │               │
│  │ (Badge)  │ │ (Empty)  │ │ (Image)  │               │
│  └──────────┘ └──────────┘ └──────────┘               │
│                                                          │
├─────────────────────────────────────────────────────────┤
│                    基础设施层 (Infra)                      │
│  底层支撑，所有模块共享                                    │
│                                                          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ │
│  │ 网络层   │ │ 本地存储 │ │ 图片加载 │ │ 错误处理   │ │
│  │ Retrofit │ │ Pref/Data│ │ Coil     │ │ 统一异常   │ │
│  └──────────┘ └──────────┘ └──────────┘ └────────────┘ │
└─────────────────────────────────────────────────────────┘
```

#### 各模块职责与边界

##### 核心模块 — 构成最小可用 App

| 模块        | 职责                                              | 输入              | 输出                  |
| ----------- | ------------------------------------------------- | ----------------- | --------------------- |
| Auth        | 用户注册/登录、token 管理、本地 session   | 用户名+昵称+PIN | user_id + token + 本地缓存 |
| Room        | 创建餐桌、邀请码加入、成员查询            | 邀请码          | room_id + role        |
| Menu        | 分类列表 + 菜品卡片浏览 + 调味参数选择            | room_id           | 菜品数据 + 调味快照   |
| Order       | 购物车管理、下单、订单历史、订单详情              | 菜品 + 调味 + 数量 | 订单数据              |
| Admin       | 菜品/分类的增删改查（仅 chef）                    | room_id + role    | CRUD 操作             |

##### 可优化模块 — 独立插拔，不侵入核心

| 模块       | 扩展点                 | 职责                          | 影响的核心模块    |
| ---------- | ---------------------- | ----------------------------- | ----------------- |
| RoomSwitch | Room 模块出口          | 多餐桌切换、餐桌列表          | Room              |
| Overview   | Order 模块出口         | chef 视角按菜品聚合所有订单   | Order             |
| OrderFlow  | Order 模块状态机       | pending→preparing→served→done | Order             |
| Search     | Menu 模块入口          | 跨分类关键字搜索菜品          | Menu              |
| Badge      | Navigation 层          | 订单 Tab 红点/数字角标        | Order + Nav       |
| EmptyState | 各 Screen 的渲染分支   | 空数据时的引导页面            | Menu + Order      |
| ImageUpload| Admin 模块工具          | 拍照/相册选图 + Cloudinary   | Admin             |
| Seasoning  | Menu 模块的 SeasoningPanel | 调味参数动态渲染（v1可简化为文本备注） | Menu      |

##### 模块间接口约定

核心模块之间只通过 Repository 层通信，不直接互相引用：

```
Screen (UI) → ViewModel → Repository → ApiService (Network)
                                         ↕
                                    Turso DB (Server)
```

可优化模块通过"扩展点"接入，具体方式：
- **UI 扩展点**：在核心 Screen 中预留 slot/composable 插槽，可优化模块提供 Composable 填入
- **数据扩展点**：Repository 返回的数据中，可优化字段用 nullable/默认值，核心模块不依赖这些字段
- **导航扩展点**：NavGraph 中可优化模块的路由独立注册，核心导航不引用

---

### 三、数据模型设计

#### 3.1 核心表（Core Schema）

##### User（用户）

| 字段       | 类型     | 说明                                      |
| ---------- | -------- | ----------------------------------------- |
| id         | INTEGER  | 主键，自增                                |
| username   | TEXT     | 用户名（唯一索引，仅英文+数字，登录凭证）|
| nickname   | TEXT     | 昵称（任意字符，展示用）                  |
| pin_hash   | TEXT     | PIN 码的 bcrypt 哈希（4-6位数字）          |
| created_at | TEXT     | 注册时间                                  |

> username 是登录凭证，只能包含英文字母和数字（如 `xiaowang2024`），全局唯一。
> nickname 是展示名，可以是中文/emoji/任意字符（如"小王🧑‍🍳"），不要求唯一。

##### Room（餐桌）

| 字段       | 类型     | 说明                              |
| ---------- | -------- | --------------------------------- |
| id         | INTEGER  | 主键，自增                        |
| name       | TEXT     | 餐桌名称（如"周末聚餐"）        |
| code       | TEXT     | 6位邀请码，唯一索引               |
| chef_id    | INTEGER  | 外键 → User.id                    |
| is_active  | BOOLEAN  | 是否活跃（默认 true）             |
| created_at | TEXT     | 创建时间                          |

##### RoomMember（餐桌成员）

| 字段      | 类型     | 说明                     |
| --------- | -------- | ------------------------ |
| id        | INTEGER  | 主键，自增               |
| room_id   | INTEGER  | 外键 → Room.id           |
| user_id   | INTEGER  | 外键 → User.id           |
| role      | TEXT     | "chef" / "guest"         |
| joined_at | TEXT     | 加入时间                 |

> 约束：UNIQUE(room_id, user_id)

##### Category（菜品分类）

| 字段       | 类型     | 说明                     |
| ---------- | -------- | ------------------------ |
| id         | INTEGER  | 主键，自增               |
| room_id    | INTEGER  | 外键 → Room.id           |
| name       | TEXT     | 分类名称                 |
| icon       | TEXT     | 分类图标 emoji           |
| sort_order | INTEGER  | 排序权重                 |

##### Dish（菜品）

| 字段         | 类型     | 说明                              |
| ------------ | -------- | --------------------------------- |
| id           | INTEGER  | 主键，自增                        |
| category_id  | INTEGER  | 外键 → Category.id                |
| room_id      | INTEGER  | 外键 → Room.id                    |
| name         | TEXT     | 菜名                              |
| description  | TEXT     | 菜品描述                          |
| price        | REAL     | 单价（元）                        |
| image_url    | TEXT     | 图片 URL（nullable，无图用占位图）|
| is_available | BOOLEAN  | 是否可点                          |
| is_deleted   | BOOLEAN  | 软删除标记（默认 false）          |
| tags         | TEXT     | 标签 JSON 数组                    |
| sort_order   | INTEGER  | 排序权重                          |

##### Order（订单）

| 字段         | 类型     | 说明                                  |
| ------------ | -------- | ------------------------------------- |
| id           | INTEGER  | 主键，自增                            |
| room_id      | INTEGER  | 外键 → Room.id                        |
| user_id      | INTEGER  | 外键 → User.id                        |
| total_price  | REAL     | 订单总价                              |
| status       | TEXT     | 状态（见 3.3 订单状态设计）           |
| note         | TEXT     | 全局备注                              |
| people_count | INTEGER  | 用餐人数（默认1）                     |
| created_at   | TEXT     | 下单时间                              |

##### OrderItem（订单明细）

| 字段           | 类型     | 说明                                  |
| -------------- | -------- | ------------------------------------- |
| id             | INTEGER  | 主键，自增                            |
| order_id       | INTEGER  | 外键 → Order.id                       |
| dish_id        | INTEGER  | 菜品 ID（不设外键约束，仅做参考）     |
| dish_name      | TEXT     | 冗余菜名                              |
| dish_price     | REAL     | 下单时单价                            |
| quantity       | INTEGER  | 数量                                  |
| seasoning_text | TEXT     | 调味描述快照（如"中辣，加葱加蒜"）   |

> **关键设计**：OrderItem.dish_id 不设外键约束，菜品软删除后订单数据不受影响。

##### Session（登录会话）

| 字段       | 类型     | 说明                          |
| ---------- | -------- | ----------------------------- |
| id         | INTEGER  | 主键，自增                    |
| user_id    | INTEGER  | 外键 → User.id                |
| token      | TEXT     | 认证 token（唯一索引）        |
| created_at | TEXT     | 创建时间                      |
| expires_at | TEXT     | 过期时间（默认 30 天后）      |

> 一个用户可以有多个有效 Session（多设备登录），退出登录只删除当前设备的 Session。

#### 3.2 扩展字段（Enhancement Schema）

以下字段/表只被可优化模块读写，核心模块完全忽略它们：

##### Dish.seasonings（调味参数 - 被 Seasoning 可优化模块使用）

JSON 字段，核心模块把它当透明字符串传递，不解析不依赖。

```json
[
  {
    "name": "辣度",
    "type": "single",
    "options": ["不辣", "微辣", "中辣", "特辣"],
    "default": "微辣"
  },
  {
    "name": "配料",
    "type": "multi",
    "options": ["加葱", "加蒜", "加香菜", "加花生碎"],
    "default": []
  },
  {
    "name": "甜度",
    "type": "scale",
    "min": 0, "max": 5,
    "default": 2
  },
  {
    "name": "忌口",
    "type": "text",
    "placeholder": "如：不要味精、少油",
    "default": ""
  }
]
```

调味类型说明：

| type   | 含义     | UI 组件    | 示例                        |
| ------ | -------- | ---------- | --------------------------- |
| single | 单选     | RadioButton| 辣度：不辣/微辣/中辣/特辣   |
| multi  | 多选     | CheckBox   | 配料：加葱 ✓ 加蒜 ✓        |
| scale  | 数值滑块 | Slider     | 甜度：0-5                   |
| text   | 自由文本 | TextField  | 忌口："不要味精"            |

**v1 降级方案**：Seasoning 模块未启用时，食客点菜只看到一个"备注"文本框，
等同于 OrderItem 的 note 字段，不调味也能下单。

#### 3.3 订单状态设计

##### 核心状态（Core - 必须实现）

```
pending → completed
pending → cancelled
```

- `pending`：刚下单，等待处理
- `completed`：已完成（简单闭环）
- `cancelled`：已取消

##### 扩展状态（OrderFlow 可优化模块启用后）

```
pending → preparing → served → completed
                                ↗
pending ──────────────────→ cancelled
```

- `preparing`：主厨已接单，正在做
- `served`：已出餐/上桌
- `completed`：用餐结束

**切换方式**：核心模块只处理 pending→completed / pending→cancelled。
OrderFlow 模块启用后，拦截状态变更，走细化流程。
前端通过判断 status 值来决定渲染哪种状态 UI，新增状态不影响旧逻辑。

#### 3.4 认证机制设计

##### 用户名规则

- 仅允许英文字母（a-z, A-Z）和数字（0-9）
- 长度 3-20 个字符
- 全局唯一（数据库 UNIQUE 约束）
- 注册时服务端校验格式和唯一性

##### PIN 码规则

- 4-6 位纯数字
- 服务端用 bcrypt 哈希存储，数据库中不保存明文
- 连续 5 次输错锁定 1 分钟（防暴力破解）

##### Token 机制

- 注册/登录成功后，服务端生成随机 token（32 字符十六进制字符串）
- Token 存储在数据库的 Session 表中：

| 字段       | 类型     | 说明                          |
| ---------- | -------- | ----------------------------- |
| id         | INTEGER  | 主键                          |
| user_id    | INTEGER  | 外键 → User.id                |
| token      | TEXT     | token 字符串（唯一索引）      |
| created_at | TEXT     | 创建时间                      |
| expires_at | TEXT     | 过期时间（默认 30 天）        |

- 客户端每次请求携带 `Authorization: Bearer <token>`
- 服务端中间件校验 token 存在且未过期，解析出 user_id
- Token 过期或无效 → 返回 401，客户端弹出登录页
- 用户可主动"退出登录"→ 客户端清除本地 token，服务端删除对应 Session

##### 认证流程图

```
注册：
  客户端 → POST /api/auth/register {username: "xw2024", nickname: "小王", pin: "1234"}
  服务端 → 校验 username 格式+唯一性 → bcrypt("1234") → 存 User + 生成 token → 返回
  客户端 → 保存 token 到本地

登录：
  客户端 → POST /api/auth/login {username: "xw2024", pin: "1234"}
  服务端 → 查 User by username → bcrypt("1234") == pin_hash? → 生成新 token → 返回
  客户端 → 更新本地 token

自动登录（App 重启）：
  客户端 → GET /api/users/me (Header: Authorization: Bearer <token>)
  服务端 → 查 Session by token → 未过期 → 返回用户信息
  客户端 → 直接进入主界面

Token 过期：
  服务端 → 返回 401
  客户端 → 清除本地 token → 跳转登录页
```

---

### 四、后端 API 设计

#### 4.1 通用约定

- 请求头 `Authorization: Bearer <token>` 标识当前用户（登录/注册接口除外）
- 响应格式统一：`{ "code": 0, "data": {...}, "message": "ok" }`
- 错误格式：`{ "code": 40001, "data": null, "message": "错误描述" }`
- 所有列表接口支持分页：`?page=1&page_size=20`

#### 4.2 核心接口

##### 用户 & 认证

```
POST   /api/auth/register          # 注册 {username, nickname, pin} → {user, token}
POST   /api/auth/login             # 登录 {username, pin} → {user, token}
GET    /api/users/me               # 获取当前用户信息（需 token）
PUT    /api/users/me               # 修改昵称 {nickname}
POST   /api/auth/pin/change        # 修改 PIN {old_pin, new_pin}
```

##### 餐桌

```
POST   /api/rooms                  # 创建餐桌 {name} → {id, code, ...}
POST   /api/rooms/join             # 加入餐桌 {code} → {room, role}
GET    /api/rooms/{id}             # 餐桌详情（含成员列表）
GET    /api/rooms/mine             # 我加入的所有餐桌列表
DELETE /api/rooms/{id}/leave        # 退出餐桌（guest 自己退出）
DELETE /api/rooms/{id}/members/{userId}  # 移除成员（仅 chef）
```

##### 分类（chef 可写，所有人可读）

```
GET    /api/rooms/{roomId}/categories
POST   /api/rooms/{roomId}/categories
PUT    /api/rooms/{roomId}/categories/{id}
DELETE /api/rooms/{roomId}/categories/{id}   # 有菜品时拒绝删除
```

##### 菜品（chef 可写，所有人可读）

```
GET    /api/rooms/{roomId}/dishes            # 菜品列表（?category_id=X&q=关键字）
GET    /api/rooms/{roomId}/dishes/{id}       # 菜品详情
POST   /api/rooms/{roomId}/dishes            # 创建菜品
PUT    /api/rooms/{roomId}/dishes/{id}       # 修改菜品
DELETE /api/rooms/{roomId}/dishes/{id}       # 软删除菜品
PATCH  /api/rooms/{roomId}/dishes/{id}/toggle # 上架/下架
```

> 菜品列表接口默认过滤 is_deleted=false。
> q 参数为搜索关键字（Search 可优化模块的后端支撑，核心接口就支持）。

##### 订单

```
GET    /api/rooms/{roomId}/orders            # 订单列表（chef 看全部，guest 看自己）
GET    /api/rooms/{roomId}/orders/{id}       # 订单详情（含 OrderItem）
POST   /api/rooms/{roomId}/orders            # 创建订单
PATCH  /api/rooms/{roomId}/orders/{id}/status # 变更状态
```

创建订单请求体：

```json
{
  "note": "一共3个人吃",
  "people_count": 3,
  "items": [
    {
      "dish_id": 1,
      "quantity": 2,
      "seasonings": { "辣度": "中辣", "配料": ["加葱", "加香菜"] }
    }
  ]
}
```

后端处理流程：
1. 校验 user 是 room 成员
2. 遍历 items，查 Dish 表获取当前菜品信息（检查 is_available, is_deleted）
3. 将调味参数序列化为可读文本
4. 计算总价，原子写入 Order + OrderItem
5. 返回完整订单详情

##### 图片上传

```
POST   /api/upload/image           # multipart/form-data 上传图片 → {url}
```

#### 4.3 可优化模块的后端接口

##### 订单总览（Overview 模块）

```
GET /api/rooms/{roomId}/orders/summary  # 按菜品聚合当前所有 pending 订单
```

返回示例：

```json
{
  "summary": [
    { "dish_name": "麻婆豆腐", "total_quantity": 3, "order_count": 2 },
    { "dish_name": "回锅肉",   "total_quantity": 2, "order_count": 1 }
  ],
  "total_orders": 5,
  "total_price": 256.00
}
```

---

### 五、Android 前端架构

#### 5.1 分层架构

```
┌──────────────────────────────────────────────────────────────┐
│                         UI Layer                              │
│  Compose Screens + Components + 可优化模块 UI                 │
│                                                               │
│  Core Screens:           Enhancement UIs:                     │
│  WelcomeScreen           RoomSwitchSheet (多餐桌切换)         │
│  MenuScreen              SearchBar (搜索栏)                   │
│  CartScreen              OverviewScreen (订单总览)            │
│  OrderHistoryScreen      EmptyState composables (空状态)      │
│  OrderDetailScreen                                            │
│  ProfileScreen                                                │
│  AdminScreen / DishEditScreen                                 │
├──────────────────────────────────────────────────────────────┤
│                      ViewModel Layer                          │
│  Core VMs:               Enhancement VMs:                     │
│  WelcomeViewModel        RoomSwitchViewModel                  │
│  MenuViewModel           OverviewViewModel                    │
│  CartViewModel                                                │
│  OrderViewModel                                               │
│  ProfileViewModel                                             │
│  AdminViewModel / DishEditViewModel                           │
├──────────────────────────────────────────────────────────────┤
│                      Repository Layer                         │
│  Core Repos:             Enhancement Repos:                    │
│  AuthRepository          OverviewRepository                   │
│  RoomRepository                                               │
│  MealRepository                                               │
│  OrderRepository                                              │
├──────────────────────────────────────────────────────────────┤
│                       Data Layer (Infra)                       │
│  ApiService (Retrofit) + DTOs + ApiClient                     │
│  Preferences (本地存储)                                        │
│  ImageUploadUtil                                               │
└──────────────────────────────────────────────────────────────┘
```

#### 5.2 导航结构

```
App 启动
  │
  ├─ 无 token 或 token 失效 → WelcomeScreen
  │                              ├─ 注册 Tab → 填用户名+昵称+PIN → 注册成功
  │                              │              → 创建餐桌 → 邀请码分享页 → MainScreen
  │                              │              → 输入邀请码 → 确认加入页 → MainScreen
  │                              └─ 登录 Tab → 填用户名+PIN → 登录成功 → MainScreen
  │
  ├─ 有有效 token（单餐桌模式）→ MainScreen
  │
  └─ 有有效 token（多餐桌，RoomSwitch 模块启用）→ RoomListScreen → MainScreen

MainScreen (底部 3 Tab)
  ├── 菜单 Tab → MenuScreen
  │                ├── [SeasoningPanel] 底部弹出（调味选择）
  │                ├── CartScreen (跳转)
  │                └── OrderDetailScreen (跳转)
  │
  ├── 订单 Tab → OrderHistoryScreen
  │                ├── OrderDetailScreen (跳转)
  │                └── [OverviewScreen] (跳转，仅 chef)
  │
  └── 我的 Tab → ProfileScreen
                   └── AdminScreen (仅 chef)
                         └── DishEditScreen
```

#### 5.3 各屏幕功能详解

##### WelcomeScreen（首次入口 / 登录页）

**核心功能：**
- 两个 Tab：「登录」/「注册」
- 登录 Tab：输入用户名 + PIN 码 → 登录成功拿 token
- 注册 Tab：输入用户名（英文数字）+ 昵称（任意）+ PIN 码（4-6位）→ 注册
- 注册成功后进入餐桌选择：「创建餐桌」/「输入邀请码加入」
- 创建餐桌后显示邀请码 + 分享按钮（系统分享）
- 登录成功且有本地餐桌缓存 → 直接进入主界面

**[RoomSwitch 可优化模块]：**
- 加入成功后显示确认页："你已加入 [餐桌名]，主厨：XXX"

##### MenuScreen（菜单页 - 核心）

**核心功能：**
- 左侧分类列表（竖向，点击切换高亮）
- 右侧菜品卡片列表
- 卡片内容：图片/占位图 + 菜名 + 价格 + 标签 + 加减按钮
- 点击卡片 → SeasoningPanel 弹出（调味选择）或直接加入购物车
- 底部悬浮 CartBar：已选 X 道菜 ¥XX + "去结算"
- Chef 视角：卡片右上角显示"编辑"快捷按钮

**[Search 可优化模块]：**
- 顶部搜索栏，输入关键字跨分类筛选
- 后端接口已支持 q 参数

**[EmptyState 可优化模块]：**
- Chef 视角空状态：插画 + "还没有菜品，去添加第一道菜" → 跳转 AdminScreen
- Guest 视角空状态：插画 + "主厨正在准备菜单，稍后再来"

**[Seasoning 可优化模块]：**
- SeasoningPanel 根据 dish.seasonings JSON 动态渲染四种控件
- 未启用时降级为简单文本备注

##### CartScreen（购物车确认页）

**核心功能：**
- 已选菜品列表，每行显示：菜名、调味摘要、数量、小计
- 可调整数量、删除菜品
- 用餐人数输入
- 全局备注输入
- "确认下单"按钮
- 下单成功 → 清空购物车 → 跳转 OrderDetailScreen

##### OrderHistoryScreen（订单历史页）

**核心功能：**
- 按时间倒序展示订单卡片
- 卡片：下单时间 + 菜品数量 + 总价 + 状态
- Chef 视角：标注下单人昵称
- Guest 视角：只看自己的订单
- 点击跳转 OrderDetailScreen

**[OrderFlow 可优化模块]：**
- 卡片显示细化状态（preparing=正在做、served=已上桌）
- Chef 可在卡片上直接点击"标记已出餐"

**[Badge 可优化模块]：**
- 订单 Tab 显示红点或数字（pending 订单数）

**[EmptyState 可优化模块]：**
- 空状态："还没有订单，去菜单页点些菜吧"

**[Overview 可优化模块]：**
- Chef 视角顶部增加 "总览" 入口 → OverviewScreen
- 按菜品聚合：麻婆豆腐×3、回锅肉×2 ... 一目了然

##### OrderDetailScreen（订单详情页）

**核心功能：**
- 订单头部：下单人昵称 + 时间 + 状态
- 菜品明细列表：菜名×数量、调味、小计
- 底部汇总：总价 + 人均价格
- 备注信息

**[OrderFlow 可优化模块]：**
- 状态显示更丰富（preparing / served）
- Chef 可见操作按钮（"开始做" / "已出餐"）

##### ProfileScreen（我的页面）

**核心功能：**
- 用户名（只读展示）+ 昵称（可修改）
- 修改 PIN 码入口（输入旧 PIN + 新 PIN）
- 当前餐桌名称 + 邀请码 + 分享按钮
- 成员列表
- Chef 专属：「菜单管理」入口 → AdminScreen
- 服务器地址配置
- 退出登录按钮

**[RoomSwitch 可优化模块]：**
- 显示所有加入的餐桌列表
- 切换当前活跃餐桌
- 退出餐桌按钮

##### AdminScreen（菜单管理 - 仅 Chef）

**核心功能：**
- 分类管理：列表 + 增删 + 拖拽排序
- 每个分类下菜品列表：增删改 + 上下架开关
- 点击菜品 → DishEditScreen
- 添加新分类 / 添加新菜品 按钮

##### DishEditScreen（菜品编辑 - 仅 Chef）

**核心功能：**
- 菜名、描述、价格
- 分类选择（下拉）
- 标签编辑（可添加/删除 tag chips）
- 是否可点开关
- 保存 / 删除

**[ImageUpload 可优化模块]：**
- 图片选择（拍照 / 相册）
- 上传进度显示
- 已上传图片预览 + 更换

**[Seasoning 可优化模块]：**
- 调味参数编辑器：
  - 添加调味项 → 选类型（single/multi/scale/text）
  - 设置选项列表、默认值、范围
  - 拖拽排序
  - 实时预览食客视角效果
- 保存时序列化为 JSON 存入 dish.seasonings

#### 5.4 UI 风格

- 卡片式布局，大图展示菜品（参考美团/饿了么）
- 圆角 12dp 卡片，elevation 2dp 阴影
- 主色调：暖橙色 #FF6B35
- 辅助色：价格红 #E53935、描述灰 #9E9E9E、背景灰 #FAFAFA
- 调味面板：BottomSheet 弹出，圆角顶部 16dp
- 空状态插画：简洁线描风格 + 暖橙色点缀

#### 5.5 关键依赖

| 库                      | 用途              |
| ----------------------- | ----------------- |
| Jetpack Compose         | UI 框架           |
| Navigation Compose      | 页面导航          |
| Retrofit + OkHttp       | HTTP 请求         |
| Gson                    | JSON 序列化       |
| Coil                    | 图片加载          |
| Kotlin Coroutines       | 异步协程          |
| lifecycle-viewmodel-compose | ViewModel     |
| Compose Material 3      | Material 3 组件   |

---

### 六、权限控制

| 操作                 | Chef | Guest |
| -------------------- | :--: | :---: |
| 浏览菜品             | ✅   | ✅    |
| 添加/修改/删除菜品   | ✅   | ❌    |
| 管理分类             | ✅   | ❌    |
| 上传图片             | ✅   | ❌    |
| 配置调味参数         | ✅   | ❌    |
| 上下架菜品           | ✅   | ❌    |
| 点菜下单             | ✅   | ✅    |
| 查看自己的订单       | ✅   | ✅    |
| 查看所有人的订单     | ✅   | ❌    |
| 查看订单总览         | ✅   | ❌    |
| 移除成员             | ✅   | ❌    |
| 退出餐桌             | ❌   | ✅    |

后端在每个写操作中通过 RoomMember 表校验 role。

---

### 七、本地存储设计（Preferences）

本地 SharedPreferences 存储结构：

```json
{
  "user_id": 12,
  "username": "xiaowang2024",
  "nickname": "小王",
  "token": "abc123def456...",
  "active_room_id": 3,
  "active_room_name": "周末聚餐",
  "active_room_code": "A3X9K2",
  "role": "chef",
  "server_url": "https://xxx.railway.app",
  "rooms": [
    { "room_id": 3, "name": "周末聚餐", "role": "chef" },
    { "room_id": 7, "name": "公司午餐", "role": "guest" }
  ]
}
```

- `token`：登录成功后服务端返回的认证令牌，所有 API 请求携带
- `active_room_id` 等字段：核心模块使用，标识当前操作的餐桌
- `rooms[]` 数组：RoomSwitch 可优化模块使用，核心模块忽略
- 启动时检查 token 是否存在且有效：
  - 有 token → 调 `GET /api/users/me` 验证，通过则直接进主界面
  - 无 token 或验证失败 → 进 WelcomeScreen 登录页

---

### 八、空状态设计

| 场景                     | 角色   | 展示内容                                        |
| ------------------------ | ------ | ----------------------------------------------- |
| 菜单页无分类无菜品       | Chef   | "还没有菜品" + 按钮"添加第一道菜" → AdminScreen |
| 菜单页无分类无菜品       | Guest  | "主厨正在准备菜单，稍后再来" + 刷新按钮         |
| 订单列表无订单           | 所有   | "还没有订单" + 按钮"去点菜" → 切到菜单 Tab      |
| 购物车为空               | 所有   | 不会进入 CartScreen（CartBar 不显示）            |
| 成员列表只有自己         | Chef   | "还没有人加入，分享邀请码给朋友吧"               |

---

### 九、项目文件结构

```
meal-app/
├── docs/
│   └── architecture.md
│
├── backend/
│   ├── main.py                    # FastAPI 入口 + CORS + 路由注册
│   ├── config.py                  # 环境变量管理
│   ├── database.py                # Turso 连接 + session
│   ├── models.py                  # 所有 SQLAlchemy 模型
│   ├── schemas.py                 # 所有 Pydantic 模型
│   ├── auth.py                  # 认证工具（PIN 哈希、token 生成/校验）
│   ├── middleware.py              # Token 校验中间件
│   ├── seed_data.py               # 种子数据
│   ├── image_upload.py            # Cloudinary 图片上传
│   ├── routers/
│   │   ├── __init__.py
│   │   ├── auth.py                # 注册 / 登录 / PIN 修改
│   │   ├── users.py               # 用户信息查询和修改
│   │   ├── rooms.py               # 含 join / leave / mine
│   │   ├── categories.py
│   │   ├── dishes.py              # 含 toggle / search(q)
│   │   ├── orders.py              # 含 summary(overview)
│   │   └── upload.py
│   ├── requirements.txt
│   └── .env.example
│
└── android/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    ├── gradle.properties
    └── app/
        ├── build.gradle.kts
        └── src/main/
            ├── AndroidManifest.xml
            └── java/com/meals/app/
                │
                ├── MealsApplication.kt
                ├── MainActivity.kt
                │
                ├── data/                              # ── 基础设施层 ──
                │   ├── remote/
                │   │   ├── ApiService.kt              # Retrofit 全部接口定义
                │   │   └── ApiClient.kt               # Retrofit 实例 + 拦截器
                │   ├── dto/
                │   │   ├── AuthDto.kt                 # 注册/登录请求响应
                │   │   ├── UserDto.kt
                │   │   ├── RoomDto.kt
                │   │   ├── CategoryDto.kt
                │   │   ├── DishDto.kt
                │   │   ├── OrderDto.kt
                │   │   └── CommonDto.kt               # 通用响应包装
                │   ├── repository/
                │   │   ├── AuthRepository.kt          # 用户 + 餐桌
                │   │   ├── MealRepository.kt          # 分类 + 菜品
                │   │   └── OrderRepository.kt         # 订单 + 总览
                │   └── local/
                │       └── Preferences.kt             # SharedPreferences 封装
                │
                ├── ui/
                │   ├── theme/
                │   │   ├── Color.kt
                │   │   ├── Theme.kt
                │   │   └── Type.kt
                │   │
                │   ├── navigation/
                │   │   └── NavGraph.kt                # 路由注册 + 底部导航
                │   │
                │   ├── screens/                       # ── 核心模块 Screen ──
                │   │   ├── welcome/
                │   │   │   ├── WelcomeScreen.kt
                │   │   │   └── WelcomeViewModel.kt
                │   │   ├── menu/
                │   │   │   ├── MenuScreen.kt
                │   │   │   └── MenuViewModel.kt
                │   │   ├── cart/
                │   │   │   ├── CartScreen.kt
                │   │   │   └── CartViewModel.kt
                │   │   ├── orders/
                │   │   │   ├── OrderHistoryScreen.kt
                │   │   │   ├── OrderDetailScreen.kt
                │   │   │   └── OrderViewModel.kt
                │   │   ├── profile/
                │   │   │   ├── ProfileScreen.kt
                │   │   │   └── ProfileViewModel.kt
                │   │   └── admin/
                │   │       ├── AdminScreen.kt
                │   │       ├── AdminViewModel.kt
                │   │       ├── DishEditScreen.kt
                │   │       └── DishEditViewModel.kt
                │   │
                │   ├── enhancement/                   # ── 可优化模块 UI ──
                │   │   ├── roomswitch/
                │   │   │   ├── RoomListScreen.kt
                │   │   │   └── RoomSwitchViewModel.kt
                │   │   ├── overview/
                │   │   │   ├── OverviewScreen.kt
                │   │   │   └── OverviewViewModel.kt
                │   │   ├── search/
                │   │   │   └── SearchBar.kt           # 嵌入 MenuScreen 的 composable
                │   │   └── empty/
                │   │       └── EmptyStates.kt         # 各场景空状态组件
                │   │
                │   └── components/                    # ── 共享组件 ──
                │       ├── DishCard.kt
                │       ├── CategoryItem.kt
                │       ├── CartBar.kt
                │       ├── OrderCard.kt
                │       ├── SeasoningPanel.kt
                │       └── SeasoningEditor.kt
                │
                └── util/
                    └── ImagePicker.kt                 # 图片选择 + 上传工具
```

**文件分层逻辑：**
- `data/` — 基础设施层，所有模块共享
- `screens/` — 核心模块的 Screen 和 ViewModel
- `enhancement/` — 可优化模块的 Screen 和 ViewModel，每个模块一个子目录
- `components/` — 被多个 Screen 复用的 UI 组件

---

### 十、开发计划

#### Phase 1 — 后端 API 核心接口

| 步骤 | 内容                                    | 产出                 |
| ---- | --------------------------------------- | -------------------- |
| 1    | FastAPI 骨架 + Turso 连接 + 配置管理    | main.py, config.py, database.py |
| 2    | 全部数据模型定义                        | models.py, schemas.py |
| 3    | 认证系统（注册/登录/token/PIN 哈希）+ 中间件 | auth.py, middleware.py, users.py |
| 4    | 餐桌接口（创建/加入/退出/成员/我的餐桌）| rooms.py |
| 5    | 分类接口 + 删除保护                     | categories.py |
| 6    | 菜品接口 + 软删除 + 搜索(q) + 上下架   | dishes.py |
| 7    | 订单接口 + 订单总览(summary)            | orders.py |
| 8    | 图片上传 + 种子数据 + 本地测试          | upload.py, seed_data.py |

#### Phase 2 — Android 核心模块

| 步骤 | 内容                                    | 产出                 |
| ---- | --------------------------------------- | -------------------- |
| 9    | Gradle 配置 + 依赖 + 主题 + 导航骨架    | 构建文件 + theme/ + NavGraph |
| 10   | 数据层：ApiService + DTO + Repository   | data/ 全部文件       |
| 11   | WelcomeScreen + 创建/加入餐桌           | welcome/ |
| 12   | MenuScreen + 菜品卡片 + CartBar         | menu/ + DishCard + CartBar |
| 13   | CartScreen + 下单流程                   | cart/ |
| 14   | OrderHistoryScreen + OrderDetailScreen  | orders/ + OrderCard |
| 15   | ProfileScreen + 邀请码分享              | profile/ |
| 16   | AdminScreen + DishEditScreen            | admin/ |
| 17   | SeasoningPanel + SeasoningEditor        | components/ |

#### Phase 3 — 可优化模块

| 步骤 | 内容                                    | 扩展点           |
| ---- | --------------------------------------- | ---------------- |
| 18   | SearchBar 嵌入 MenuScreen               | Menu 入口        |
| 19   | EmptyStates 各场景空状态                | 各 Screen 分支   |
| 20   | Badge 订单 Tab 角标                     | Navigation 层    |
| 21   | OrderFlow 订单状态细化                  | Order 状态机     |
| 22   | OverviewScreen 订单总览                 | Order Tab 出口   |
| 23   | RoomSwitch 多餐桌切换                   | Room + Profile   |
| 24   | ImageUpload 图片上传完善                | Admin DishEdit   |

#### Phase 4 — 联调部署

| 步骤 | 内容                                    |
| ---- | --------------------------------------- |
| 25   | 后端部署 Railway + Turso 数据库         |
| 26   | Cloudinary 图片服务配置                 |
| 27   | 前后端联调                              |
| 28   | 端到端测试全流程                        |
