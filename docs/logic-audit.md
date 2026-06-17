## 逻辑漏洞审查报告

对后端全部14个文件和前端全部23个文件进行了三维度交叉审查（后端逻辑、前端逻辑、业务边界），共发现 **4个严重 / 11个高危 / 14个中危** 问题。

---

### 严重 (Critical) — 必须修复

**C1. 无服务端登出接口，Session 永不失效**
- 涉及: `backend/routers/auth.py`（缺失）、`AuthRepository.kt`
- 问题: 客户端 `logout()` 只清除本地 token，从不调用服务端接口删除 Session。一旦 token 泄露，攻击者可在 30 天内持续使用。架构文档明确要求"退出登录 → 服务端删除 Session"，但此接口不存在。
- 修复: 新增 `POST /api/auth/logout` 端点，删除当前 token 对应的 Session 记录。

**C2. 订单数量无下限验证，可操纵总价**
- 涉及: `backend/schemas.py`（`OrderItemCreate.quantity`）、`backend/routers/orders.py`
- 问题: `quantity: int` 无 `ge=1` 约束。恶意客户端可提交 `quantity: -5`，导致 `dish.price * (-5)` 产生负数小计，直接操纵订单总价为负数或零。
- 修复: `quantity: int = Field(..., ge=1, le=999)`

**C3. 前端无 401/Token 过期处理**
- 涉及: `data/remote/ApiClient.kt`、所有 Repository 文件
- 问题: `authInterceptor` 附加 Bearer token，但无响应拦截器处理 401。Token 过期后所有 API 调用静默失败，用户看到的是"加载失败"而非"请重新登录"，永远无法自动跳转登录页。
- 修复: 添加 OkHttp `Authenticator`，检测 401 时清除 Preferences 并导航到登录页。

**C4. 登出不触发页面跳转，用户卡在空白页**
- 涉及: `ui/screens/profile/ProfileScreen.kt`、`ProfileViewModel.kt`
- 问题: `ProfileViewModel.logout()` 设置 `loggedOut = true`，但 `ProfileScreen` 从未观察该状态，不会导航到登录页。登出后用户停留在空白的个人中心页面，所有后续请求失败，无法恢复。
- 修复: 添加 `LaunchedEffect(uiState.loggedOut)` → `navController.navigate(Routes.WELCOME) { popUpTo(0) { inclusive = true } }`

---

### 高危 (High) — 强烈建议修复

**H1. 已关闭餐桌仍可下单和更新订单状态**
- 涉及: `backend/routers/orders.py`（`create_order`、`update_order_status`）
- 问题: `require_member` 只检查成员身份，不检查 `Room.is_active`。厨师关闭餐桌后，客人仍可创建新订单。
- 修复: 在 `create_order` 中增加 `room.is_active` 检查。

**H2. 切换餐桌不清空购物车**
- 涉及: `RoomSwitchViewModel.kt`、`MenuViewModel.kt`
- 问题: `switchRoom()` 更新 Preferences 但从不清除 `CartStore`。切换后购物车仍包含前一个餐桌的菜品（dish ID 在新餐桌无效），提交订单将失败或产生错误数据。
- 修复: 在 `switchRoom()` 中调用 `CartStore.clear()`。

**H3. 切换餐桌后菜单显示旧数据**
- 涉及: `MenuViewModel.kt`、`NavGraph.kt`
- 问题: `MenuViewModel` 在 `init` 时加载分类和菜品。由于底部导航使用 `launchSingleTop + restoreState`，切换餐桌后 ViewModel 不会重建，菜单仍显示旧餐桌数据。
- 修复: 使用共享的 `activeRoomId` StateFlow，MenuViewModel 观察变化时自动重新加载。

**H4. PIN 修改不使旧 Session 失效**
- 涉及: `backend/routers/auth.py`（`change_pin`）
- 问题: 修改 PIN 后所有现有 Session 仍然有效直到自然过期。如果用户因怀疑被盗而修改 PIN，攻击者的 Session 仍可继续使用 30 天。
- 修复: PIN 修改成功后删除该用户除当前 Session 外的所有 Session。

**H5. 文件上传无类型和大小验证**
- 涉及: `backend/routers/upload.py`、`backend/image_upload.py`
- 问题: 上传端点接受任意文件类型（包括 `.html`、`.php`、`.exe`）和任意大小。由于 `/uploads` 作为静态目录服务，上传 HTML 文件可造成存储型 XSS。
- 修复: 白名单验证 `content_type`（仅允许 `image/jpeg`、`image/png`、`image/webp`），限制 5MB 大小，验证文件头魔数。

**H6. 菜品价格可为负数**
- 涉及: `backend/schemas.py`（`DishCreate.price`、`DishUpdate.price`）
- 问题: `price: float` 无最小值约束。恶意厨师可创建价格为 -100 的菜品，下单时从总价中扣减。
- 修复: `price: float = Field(gt=0)`

**H7. `apiService` 缺少 `@Volatile` 注解**
- 涉及: `data/remote/ApiClient.kt`
- 问题: 双重检查锁定的 `apiService` 变量未声明 `@Volatile`，多线程下可能读到过期引用。`reset()` 后某些线程仍使用旧的 ApiService。
- 修复: 添加 `@Volatile private var apiService: ApiService? = null`

**H8. 底部导航返回栈无限增长**
- 涉及: `NavGraph.kt`
- 问题: `popUpTo(navController.graph.findStartDestination().id)` 的目标是 WELCOME，但登录后 WELCOME 已被移出栈。`popUpTo` 找不到目标时不会弹出任何内容，每次切 tab 都追加新条目，系统返回键需多次才能退出。
- 修复: 将 `popUpTo` 目标改为 `Routes.MENU` 或使用嵌套 NavHost。

**H9. `rememberHasToken()` 非响应式**
- 涉及: `NavGraph.kt`
- 问题: `Preferences.isLoggedIn` 在组合时直接读取，不包装在 `mutableStateOf` 或 `StateFlow` 中。登录/登出后 `startDestination` 逻辑不会重新计算。
- 修复: 创建全局 `AuthStateFlow` 驱动导航状态。

**H10. PIN 修改端点无锁定和频率限制**
- 涉及: `backend/routers/auth.py`（`change_pin`）
- 问题: 该端点验证旧 PIN 但无失败次数限制。攻击者持有有效 token 后可暴力遍历 10000 种 4 位 PIN。
- 修复: 对 `change_pin` 应用相同的锁定机制，成功后使其他 Session 失效。

**H11. RoomSwitchViewModel 状态竞态**
- 涉及: `RoomSwitchViewModel.kt`
- 问题: `leaveOrCloseRoom()` 中 `loadRooms()` 和 `roomLeft = true` 并发执行，两个协程同时写入 StateFlow，可能导致 `roomLeft` 被后续状态更新覆盖。
- 修复: await `loadRooms()` 完成后再设置 `roomLeft = true`。

---

### 中危 (Medium) — 建议修复

| 编号 | 问题 | 涉及文件 |
|------|------|----------|
| M1 | 第5次锁定后错误消息显示"剩余5次"（计数器先归零再计算） | `auth.py` |
| M2 | `people_count` 无最小值验证，前端 `total_price / people_count` 可能除零 | `schemas.py`、`OrderDetailScreen.kt` |
| M3 | 昵称/餐桌名无长度验证，可能存超长或含控制字符的字符串 | `schemas.py` |
| M4 | LIKE 查询未转义 `%` 和 `_` 通配符 | `dishes.py` |
| M5 | 邀请码用 `random.choices()` 生成，非密码学安全 | `rooms.py` |
| M6 | PIN 锁定仅内存存储，重启/多实例部署失效 | `auth.py` |
| M7 | `close_room` 是 toggle 而非显式关闭，双击可重新打开 | `rooms.py` |
| M8 | `category_id` 未验证是否属于同一餐桌（跨餐桌引用） | `dishes.py` |
| M9 | CORS `allow_origins=["*"]` 与 `allow_credentials=True` 并存 | `main.py` |
| M10 | 菜品价格在购物车加载后可能变更，用户看到的价格与实际扣款不一致 | `orders.py`、`CartViewModel.kt` |
| M11 | 已售罄菜品仍可通过随机选菜/详情面板加入购物车 | `MenuViewModel.kt` |
| M12 | 每次切 tab 拉取50条订单只为计算 badge 数字，性能浪费 | `NavGraph.kt` |
| M13 | `DishEditViewModel.init()` 可能被重复调用覆盖用户已编辑内容 | `DishEditViewModel.kt` |
| M14 | `Preferences.rooms` 缓存在关闭/退出餐桌后未更新 | `ProfileViewModel.kt` |

---

### 低危 (Low) — 可选修复

| 编号 | 问题 | 涉及文件 |
|------|------|----------|
| L1 | `OrderItem.dish_id` 缺少外键约束 | `models.py` |
| L2 | 餐桌代码生成的唯一性检查非原子操作 | `rooms.py` |
| L3 | `get_my_rooms` 存在 N+1 查询问题 | `rooms.py` |
| L4 | 过期 Session 仅在被使用时才清理，表中数据无限增长 | `middleware.py` |
| L5 | 同一订单中重复 `dish_id` 不会合并 | `orders.py` |
| L6 | 登录 schema 未验证 PIN 格式 | `schemas.py` |
| L7 | 订单历史空状态"去点餐"按钮无实际功能 | `OrderHistoryScreen.kt` |
| L8 | `Preferences.clear()` 会丢失自定义服务器地址 | `Preferences.kt` |
