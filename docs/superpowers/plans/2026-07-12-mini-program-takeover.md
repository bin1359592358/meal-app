# 「超大一碗饭」小程序接管与重构实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 删除已停止维护的 Android 客户端，修复后端和微信小程序的已知逻辑问题，补齐点菜闭环，并将界面重做为以现有厨娘角色为核心的轻二次元蓝白奶油风格。

**架构：** 保留 FastAPI + SQLAlchemy 后端和原生微信小程序。后端是权限、价格、订单状态和调味合法性的最终可信来源；小程序把会话、购物车、调味和格式化逻辑提取为可测试的纯 JavaScript 模块，页面只负责生命周期和交互。现有 API 主路径和数据库表保持兼容。

**技术栈：** Python 3.10+、FastAPI 0.115、SQLAlchemy 2.0、Pydantic 2.9、pytest、原生微信小程序、Node.js 内置测试工具、WXSS。

---

## 文件结构

### 删除

- `android/`：停止维护的 Android 客户端及构建文件。
- `backend/test_api.py`：依赖手动服务器并污染持久数据库的旧测试。
- `backend/test_auth.py`：依赖手动服务器的旧认证测试。
- `docs/logic-audit.md`：已过时的问题快照，由新测试和 README 取代。

### 创建

- `backend/tests/conftest.py`：临时数据库、TestClient 和测试数据夹具。
- `backend/tests/test_auth.py`：认证、登出、PIN 和生产种子策略测试。
- `backend/tests/test_rooms.py`：餐桌、邀请码和成员权限测试。
- `backend/tests/test_dishes.py`：分类、菜品、图片和调味 schema 测试。
- `backend/tests/test_orders.py`：订单、状态、调味和统计口径测试。
- `miniprogram/utils/cart.js`：购物车唯一键、餐桌隔离和数量规则。
- `miniprogram/utils/seasoning.js`：调味初始化、规范化和摘要格式化。
- `miniprogram/utils/session.js`：登录态、当前餐桌和全局清理策略。
- `miniprogram/tests/cart.test.js`：购物车纯逻辑测试。
- `miniprogram/tests/seasoning.test.js`：调味纯逻辑测试。
- `miniprogram/tests/session.test.js`：会话与餐桌切换测试。
- `miniprogram/components/dish-detail/`：菜品详情与调味合并面板。
- `miniprogram/components/loading-state/`：统一加载状态。
- `miniprogram/images/brand/`：压缩头像、登录图、分享图和状态插画。

### 重点修改

- `backend/config.py`、`backend/database.py`：数据库 URL 类型识别和环境策略。
- `backend/main.py`、`backend/seed_data.py`：生产环境禁用自动种子和敏感日志。
- `backend/schemas.py`：字段长度、状态、图片、调味和上下架校验。
- `backend/routers/*.py`：认证、餐桌、菜品、上传、订单与统计修复。
- `miniprogram/app.js`：全局购物车和餐桌变化协调。
- `miniprogram/app.json`、`miniprogram/app.wxss`：品牌名、组件和设计令牌。
- `miniprogram/utils/api.js`、`storage.js`、`auth.js`：统一错误和清理流程。
- `miniprogram/pages/menu/`：首页、搜索、随机推荐和详情面板。
- `miniprogram/pages/cart/`：购物车变体、摘要和提交体验。
- `miniprogram/pages/orders/`、`order-detail/`：分页、筛选、角标和调味展示。
- `miniprogram/pages/profile/`：餐桌切换、成员管理和主厨工作台。
- `miniprogram/pages/admin/`、`dish-edit/`、`overview/`：主厨管理与统计。
- `README.md`、`docs/architecture.md`：删除 Android 并同步最终实现。

---

### 任务 1：建立隔离的后端测试基线

**文件：**
- 创建：`backend/tests/conftest.py`
- 创建：`backend/tests/test_auth.py`
- 修改：`backend/requirements.txt`
- 修改：`requirements.txt`

- [ ] **步骤 1：编写临时数据库和 TestClient 夹具**

夹具必须在导入应用前设置独立 SQLite URL，并在测试结束后释放引擎：

```python
@pytest.fixture()
def client(tmp_path, monkeypatch):
    monkeypatch.setenv("DATABASE_URL", f"sqlite:///{tmp_path / 'test.db'}")
    monkeypatch.setenv("AUTO_SEED", "false")
    with TestClient(app) as test_client:
        yield test_client
```

- [ ] **步骤 2：编写最小认证冒烟测试**

```python
def test_register_login_and_logout(client):
    registered = client.post("/api/auth/register", json={
        "username": "tester", "nickname": "测试用户", "pin": "1234"
    })
    assert registered.status_code == 200
    token = registered.json()["data"]["token"]
    assert client.get("/api/users/me", headers=auth(token)).status_code == 200
    assert client.post("/api/auth/logout", headers=auth(token)).status_code == 200
    assert client.get("/api/users/me", headers=auth(token)).status_code == 401
```

- [ ] **步骤 3：运行测试并确认测试基础设施失败**

运行：`python -m pytest backend/tests/test_auth.py -v`

预期：FAIL，原因是测试依赖和应用数据库初始化尚未支持隔离加载，而不是连接真实服务器。

- [ ] **步骤 4：增加 pytest/httpx 依赖并完成夹具初始化**

在两份 requirements 中加入固定兼容版本：

```text
pytest==8.3.3
httpx==0.27.2
```

- [ ] **步骤 5：运行认证测试确认通过**

运行：`python -m pytest backend/tests/test_auth.py -v`

预期：PASS，且项目目录中不出现新的持久测试数据库。

- [ ] **步骤 6：提交**

```bash
git add backend/tests backend/requirements.txt requirements.txt
git commit -m "test(后端): 建立隔离 API 测试基线"
```

### 任务 2：删除 Android 和过时资产

**文件：**
- 删除：`android/`
- 删除：`backend/test_api.py`
- 删除：`backend/test_auth.py`
- 删除：`docs/logic-audit.md`
- 修改：`.gitignore`
- 修改：`README.md`
- 修改：`docs/architecture.md`

- [ ] **步骤 1：记录删除前状态**

运行：`git status --short && git ls-files android backend/test_api.py backend/test_auth.py docs/logic-audit.md`

预期：明确区分已跟踪文件和用户原有未跟踪文件；删除前保留状态输出。

- [ ] **步骤 2：删除 Android 目录和旧测试**

使用单一 PowerShell 流程删除已确认路径，先验证所有解析后的绝对路径均位于 `D:\Claude_Project\meal-app` 下，再执行 `Remove-Item -Recurse -Force`。

- [ ] **步骤 3：更新忽略规则**

保留数据库、备份、截图和生成中间文件的忽略规则；为正式品牌资源添加例外：

```gitignore
*.db.bak
*.db.old
backend/test_api.py
backend/test_auth.py
.superpowers/
!miniprogram/images/**/*.png
```

- [ ] **步骤 4：清理文档中的 Android 内容**

README 和架构文档只描述微信小程序、FastAPI、Railway、Cloudflare、Turso 和 Cloudinary。应用名称统一为「超大一碗饭」。

- [ ] **步骤 5：验证没有 Android 引用**

运行：

```powershell
Get-ChildItem README.md,docs,miniprogram,backend -Recurse -File |
  Select-String -Pattern 'Android|Kotlin|Compose|Gradle|APK'
```

预期：没有产品和构建说明残留；历史设计规格中的范围说明除外。

- [ ] **步骤 6：提交**

```bash
git add -A android backend/test_api.py backend/test_auth.py docs/logic-audit.md .gitignore README.md docs/architecture.md
git commit -m "chore(项目): 移除已停用 Android 客户端"
```

### 任务 3：修复数据库配置、认证和生产种子策略

**文件：**
- 修改：`backend/config.py`
- 修改：`backend/database.py`
- 修改：`backend/main.py`
- 修改：`backend/seed_data.py`
- 修改：`backend/routers/auth.py`
- 修改：`backend/schemas.py`
- 测试：`backend/tests/test_auth.py`

- [ ] **步骤 1：编写失败测试**

覆盖以下行为：

```python
def test_database_kind_respects_database_url(monkeypatch): ...
def test_production_does_not_auto_seed_default_user(client): ...
def test_login_with_empty_pin_hash_returns_401(client, db_session): ...
def test_old_pin_must_match_pin_format(client, token): ...
```

- [ ] **步骤 2：运行测试验证失败**

运行：`python -m pytest backend/tests/test_auth.py -v`

预期：数据库类型、AUTO_SEED 或空 pin_hash 用例至少一项 FAIL。

- [ ] **步骤 3：实现配置和认证修复**

设置项增加：

```python
ENVIRONMENT: str = "development"
AUTO_SEED: bool = False
```

数据库类型根据 `urlparse(settings.database_url).scheme` 判断。登录条件改为先判断 `user.pin_hash`，再调用 `verify_pin`。`PinChange.old_pin` 和 `new_pin` 使用同一格式校验。

- [ ] **步骤 4：限制种子执行和敏感日志**

只有 `AUTO_SEED=true` 时启动种子；`seed_data.py` 不打印有效 Token。README 的本地开发步骤改为显式运行 `python seed_data.py`。

- [ ] **步骤 5：运行测试确认通过**

运行：`python -m pytest backend/tests/test_auth.py -v`

预期：全部 PASS。

- [ ] **步骤 6：提交**

```bash
git add backend/config.py backend/database.py backend/main.py backend/seed_data.py backend/routers/auth.py backend/schemas.py backend/tests/test_auth.py
git commit -m "fix(认证): 修复数据库配置和生产种子策略"
```

### 任务 4：强化菜品、调味和上传校验

**文件：**
- 修改：`backend/schemas.py`
- 修改：`backend/auth.py`
- 修改：`backend/image_upload.py`
- 修改：`backend/routers/dishes.py`
- 修改：`backend/routers/upload.py`
- 创建：`backend/tests/test_dishes.py`

- [ ] **步骤 1：编写字段和上传失败测试**

测试应覆盖：空菜名、超长描述、零价格、跨餐桌分类、`is_available` 保存、未知调味类型、伪造调味选项、伪装成 PNG 的文本文件。

```python
def test_update_dish_persists_availability(chef_client, dish): ...
def test_fake_png_is_rejected(authorized_client): ...
def test_unknown_seasoning_option_is_rejected(guest_client, dish): ...
```

- [ ] **步骤 2：运行测试确认失败**

运行：`python -m pytest backend/tests/test_dishes.py -v`

预期：`is_available` 和文件头校验用例 FAIL。

- [ ] **步骤 3：实现严格 schema**

为名称、描述、标签、图片 URL、备注和调味文本设置长度；为调味定义建立判别联合或显式 validator；`DishCreate` 和 `DishUpdate` 增加 `is_available`。

- [ ] **步骤 4：实现图片文件头校验**

仅接受 JPEG、PNG、WebP 和 GIF 的真实签名；内容类型、扩展名和文件头必须一致，最大 5 MB。

- [ ] **步骤 5：实现服务端调味选择校验**

将菜品调味定义规范化为按名称索引的数据结构，验证单选、多选、滑块范围和文本长度，再生成订单快照文字。

- [ ] **步骤 6：运行测试确认通过**

运行：`python -m pytest backend/tests/test_dishes.py -v`

预期：全部 PASS。

- [ ] **步骤 7：提交**

```bash
git add backend/schemas.py backend/auth.py backend/image_upload.py backend/routers/dishes.py backend/routers/upload.py backend/tests/test_dishes.py
git commit -m "fix(菜品): 强化调味和图片上传校验"
```

### 任务 5：修复餐桌成员和订单统计逻辑

**文件：**
- 修改：`backend/routers/rooms.py`
- 修改：`backend/routers/orders.py`
- 修改：`backend/schemas.py`
- 创建：`backend/tests/test_rooms.py`
- 创建：`backend/tests/test_orders.py`

- [ ] **步骤 1：编写失败测试**

```python
def test_join_code_is_trimmed_and_uppercased(client, room): ...
def test_remove_member_requires_user_id(chef_client, room, guest): ...
def test_closed_room_cannot_accept_orders(guest_client, room): ...
def test_completed_orders_contribute_to_revenue(chef_client, completed_order): ...
def test_same_name_dishes_remain_separate_in_summary(chef_client): ...
```

- [ ] **步骤 2：运行测试验证失败**

运行：`python -m pytest backend/tests/test_rooms.py backend/tests/test_orders.py -v`

预期：邀请码规范化、统计口径和同名菜品用例 FAIL。

- [ ] **步骤 3：实现餐桌修复**

`RoomJoin.code` 使用 validator 规范化；房间创建和邀请码刷新统一使用带唯一键重试的辅助函数；成员接口保持 `user_id` 语义。

- [ ] **步骤 4：实现双口径统计**

响应增加明确字段：

```json
{
  "active_orders": 2,
  "active_amount": 58.0,
  "completed_orders": 12,
  "revenue": 356.0,
  "summary": []
}
```

菜品聚合键使用 `(dish_id, dish_name)`，取消订单不计入销量和收入。

- [ ] **步骤 5：运行全部后端测试**

运行：`python -m pytest backend/tests -v`

预期：全部 PASS。

- [ ] **步骤 6：提交**

```bash
git add backend/routers/rooms.py backend/routers/orders.py backend/schemas.py backend/tests/test_rooms.py backend/tests/test_orders.py
git commit -m "fix(订单): 修复成员操作和统计口径"
```

### 任务 6：建立小程序纯逻辑测试和共享模块

**文件：**
- 创建：`miniprogram/utils/cart.js`
- 创建：`miniprogram/utils/seasoning.js`
- 创建：`miniprogram/utils/session.js`
- 创建：`miniprogram/tests/cart.test.js`
- 创建：`miniprogram/tests/seasoning.test.js`
- 创建：`miniprogram/tests/session.test.js`

- [ ] **步骤 1：编写购物车失败测试**

```javascript
test('同一菜品不同调味形成独立项目', () => {
  let cart = createCart(1)
  cart = addItem(cart, dish, { 饭量: '正常' })
  cart = addItem(cart, dish, { 饭量: '加饭' })
  assert.equal(cart.items.length, 2)
})
```

同时覆盖相同调味合并、切换餐桌清空、数量上限和稳定 JSON 键。

- [ ] **步骤 2：编写调味和会话失败测试**

覆盖默认值、数组摘要、滑块值 0、文本裁剪、退出登录清理和餐桌变化判断。

- [ ] **步骤 3：运行测试验证失败**

运行：`node --test miniprogram/tests/*.test.js`

预期：FAIL，提示模块不存在。

- [ ] **步骤 4：实现最小纯逻辑模块**

模块不直接调用 `wx`，通过参数接收 storage 适配器。购物车结构固定为：

```javascript
{ roomId: null, items: [{ key, dish, quantity, seasonings }] }
```

- [ ] **步骤 5：运行测试确认通过**

运行：`node --test miniprogram/tests/*.test.js`

预期：全部 PASS。

- [ ] **步骤 6：提交**

```bash
git add miniprogram/utils/cart.js miniprogram/utils/seasoning.js miniprogram/utils/session.js miniprogram/tests
git commit -m "test(小程序): 建立购物车和会话逻辑测试"
```

### 任务 7：接入会话、API 和餐桌切换状态

**文件：**
- 修改：`miniprogram/app.js`
- 修改：`miniprogram/utils/api.js`
- 修改：`miniprogram/utils/auth.js`
- 修改：`miniprogram/utils/storage.js`
- 修改：`miniprogram/pages/login/login.js`
- 修改：`miniprogram/pages/profile/profile.js`
- 修改：`miniprogram/pages/profile/profile.wxml`

- [ ] **步骤 1：增加页面集成行为测试**

扩展 `session.test.js`，验证切换餐桌返回 `shouldClearCart=true`，移除成员 URL 使用 `member.user_id`。

- [ ] **步骤 2：运行测试确认旧实现失败**

运行：`node --test miniprogram/tests/session.test.js`

预期：成员 ID 或切换清理断言 FAIL。

- [ ] **步骤 3：接入全局购物车模块**

`app.js` 的 `addToCart`、`removeFromCart`、`updateCartItemQty` 和 `clearCart` 委托给 `utils/cart.js`。清空时同时重置 `roomId`。

- [ ] **步骤 4：统一 401、上传和退出处理**

普通请求和 `wx.uploadFile` 都处理 401；全局只执行一次 `reLaunch`；退出登录在服务端请求完成或超时后清理本地状态。

- [ ] **步骤 5：修复餐桌切换与成员移除**

切换餐桌前提示并清理购物车，保存完整房间缓存；成员 URL 使用 `member.user_id`；退出或关闭当前餐桌后选择下一个活跃餐桌。

- [ ] **步骤 6：运行小程序测试和语法检查**

运行：

```powershell
node --test miniprogram/tests/*.test.js
Get-ChildItem miniprogram -Recurse -Filter *.js | ForEach-Object { node --check $_.FullName }
```

预期：全部通过。

- [ ] **步骤 7：提交**

```bash
git add miniprogram/app.js miniprogram/utils miniprogram/pages/login miniprogram/pages/profile miniprogram/tests
git commit -m "fix(餐桌): 修复切换状态和成员移除"
```

### 任务 8：重做菜单并补齐详情和随机选菜

**文件：**
- 创建：`miniprogram/components/dish-detail/dish-detail.js`
- 创建：`miniprogram/components/dish-detail/dish-detail.json`
- 创建：`miniprogram/components/dish-detail/dish-detail.wxml`
- 创建：`miniprogram/components/dish-detail/dish-detail.wxss`
- 修改：`miniprogram/pages/menu/menu.js`
- 修改：`miniprogram/pages/menu/menu.json`
- 修改：`miniprogram/pages/menu/menu.wxml`
- 修改：`miniprogram/pages/menu/menu.wxss`
- 修改：`miniprogram/components/dish-card/*`
- 修改：`miniprogram/components/cart-bar/*`

- [ ] **步骤 1：编写可用菜品筛选测试**

在纯逻辑测试中验证随机池排除 `is_available=false` 的菜品，并验证旧搜索响应不会覆盖新请求序号。

- [ ] **步骤 2：运行测试确认失败**

运行：`node --test miniprogram/tests/*.test.js`

预期：随机池或请求序号行为 FAIL。

- [ ] **步骤 3：实现详情与调味合并组件**

组件接收 `dish` 和 `visible`，使用 `seasoning.js` 初始化选择，触发 `confirm`、`close` 和 `change` 事件。售罄时不触发确认。

- [ ] **步骤 4：实现菜单页面数据流**

`onShow()` 比较当前餐桌 ID；变化时清空页面状态并重新加载。搜索维护递增 request ID。随机推荐从已加载的全部可用菜品中选择。

- [ ] **步骤 5：落地菜单布局**

使用品牌头部、餐桌切换入口、搜索框、横向分类胶囊、单列菜品卡片和底部购物车。高频按钮位于右侧和底部。

- [ ] **步骤 6：运行测试和语法检查**

运行：`node --test miniprogram/tests/*.test.js`，随后检查全部 JS 语法。

预期：全部通过。

- [ ] **步骤 7：提交**

```bash
git add miniprogram/components/dish-detail miniprogram/components/dish-card miniprogram/components/cart-bar miniprogram/pages/menu miniprogram/tests
git commit -m "feat(菜单): 添加菜品详情和随机推荐"
```

### 任务 9：修复购物车和订单体验

**文件：**
- 修改：`miniprogram/pages/cart/*`
- 修改：`miniprogram/pages/orders/*`
- 修改：`miniprogram/pages/order-detail/*`
- 修改：`miniprogram/components/order-card/*`
- 修改：`miniprogram/utils/util.js`

- [ ] **步骤 1：编写格式化和分页失败测试**

验证 `seasoning_text` 直接显示、滑块 0 不丢失、订单按 ID 去重、加载中不重复请求。

- [ ] **步骤 2：运行测试确认失败**

运行：`node --test miniprogram/tests/*.test.js`

预期：旧字段映射或去重行为 FAIL。

- [ ] **步骤 3：重构购物车页面**

以购物车项目 `key` 而不是菜品 ID 或数组不稳定索引进行增减；展示调味摘要、人数、备注和服务端定价提示；提交成功后清空购物车。

- [ ] **步骤 4：重构订单列表和详情**

订单列表增加全部、进行中和已完成筛选；分页请求加锁并按 ID 去重；详情使用 `item.seasoning_text`；按钮只显示后端允许的下一步。

- [ ] **步骤 5：运行测试和语法检查**

运行 Node 测试和全部小程序 JS 语法检查。

预期：全部通过。

- [ ] **步骤 6：提交**

```bash
git add miniprogram/pages/cart miniprogram/pages/orders miniprogram/pages/order-detail miniprogram/components/order-card miniprogram/utils miniprogram/tests
git commit -m "fix(订单): 修复调味展示和分页状态"
```

### 任务 10：完善主厨工作台

**文件：**
- 修改：`miniprogram/pages/admin/*`
- 修改：`miniprogram/pages/dish-edit/*`
- 修改：`miniprogram/pages/overview/*`
- 修改：`miniprogram/pages/profile/*`

- [ ] **步骤 1：增加主厨请求映射测试**

验证菜品保存请求包含 `is_available` 和 `seasonings`，总览适配新的进行中与经营统计字段。

- [ ] **步骤 2：运行测试确认失败**

运行：`node --test miniprogram/tests/*.test.js`

预期：菜品编辑或统计映射用例 FAIL。

- [ ] **步骤 3：实现调味配置编辑器**

支持新增、删除和排序调味项；根据类型编辑选项、默认值、最小值、最大值和文本长度。保存前用 `seasoning.js` 验证。

- [ ] **步骤 4：修复菜品和分类管理**

保存 `is_available`；分类删除文案改为「分类下仍有菜品时无法删除」；提交期间锁定按钮；上传失败保留表单。

- [ ] **步骤 5：重做统计和工作台布局**

总览分开显示进行中订单、进行中金额、已完成订单和营业金额；菜品销量按 ID 展示。个人中心使用集中式主厨工作台入口。

- [ ] **步骤 6：运行测试和语法检查**

运行 Node 测试与 JS 语法检查。

预期：全部通过。

- [ ] **步骤 7：提交**

```bash
git add miniprogram/pages/admin miniprogram/pages/dish-edit miniprogram/pages/overview miniprogram/pages/profile miniprogram/tests
git commit -m "feat(主厨): 完善调味配置和营业统计"
```

### 任务 11：接入轻二次元品牌视觉和图片资源

**文件：**
- 修改：`miniprogram/app.json`
- 修改：`miniprogram/app.wxss`
- 修改：`miniprogram/project.config.json`
- 修改：`miniprogram/pages/login/*`
- 修改：`miniprogram/pages/menu/*`
- 修改：`miniprogram/pages/cart/*`
- 修改：`miniprogram/pages/orders/*`
- 修改：`miniprogram/pages/order-detail/*`
- 修改：`miniprogram/pages/profile/*`
- 修改：`miniprogram/pages/admin/*`
- 修改：`miniprogram/pages/dish-edit/*`
- 修改：`miniprogram/pages/overview/*`
- 创建：`miniprogram/images/brand/*`

- [ ] **步骤 1：处理现有头像**

从根目录 `头像.png` 生成适合小程序的压缩版本，保留原始角色，不改变身份特征。输出至少包含头像和登录页裁切版本，单张尽量控制在 300 KB 内。

- [ ] **步骤 2：生成必要的二次元状态插画**

使用现有头像作为角色参考，分别生成登录、空订单、下单成功和网络失败资源。统一提示约束：正常日系动漫人物比例、银白双丸子长发、蓝眼、蓝白厨师服、禁止 chibi、禁止 super-deformed、禁止大头小身体、无图片内文字。

- [ ] **步骤 3：验证图片资源**

检查人物一致性、尺寸、透明区域、边缘、文字缺失和文件体积。项目引用的最终资源必须复制到 `miniprogram/images/brand/`。

- [ ] **步骤 4：建立全局设计令牌**

在 `app.wxss` 定义颜色、间距、圆角、阴影和安全区变量。主色使用 `#5579BC`，页面背景使用 `#FFFDF8`，强调色使用奶黄和腮红粉。

- [ ] **步骤 5：统一页面视觉**

按已确认原型重做页面：正常比例角色插画承担二次元氛围；组件保持现代、清晰、可触达；移除不统一的橙色和过量 emoji。

- [ ] **步骤 6：更新品牌名称和图标**

小程序标题、工程描述、登录页和分享标题统一为「超大一碗饭」。TabBar 使用统一蓝色线性图标。

- [ ] **步骤 7：运行包体和语法检查**

运行 JS 语法检查，并统计 `miniprogram/images/brand` 总体积。确认没有引用工作区外部图片。

- [ ] **步骤 8：提交**

```bash
git add miniprogram/app.json miniprogram/app.wxss miniprogram/project.config.json miniprogram/pages miniprogram/components miniprogram/images/brand
git commit -m "feat(视觉): 重做轻二次元厨娘主题界面"
```

### 任务 12：完整回归、文档和发布准备

**文件：**
- 修改：`README.md`
- 修改：`docs/architecture.md`
- 修改：`backend/.env.example`
- 检查：全部后端和小程序文件

- [ ] **步骤 1：运行完整自动化测试**

```powershell
python -m pytest backend/tests -v
node --test miniprogram/tests/*.test.js
python -m compileall -q backend
Get-ChildItem miniprogram -Recurse -Filter *.js | ForEach-Object { node --check $_.FullName }
```

预期：全部退出码为 0，无失败用例。

- [ ] **步骤 2：运行临时数据库 API 冒烟测试**

覆盖注册、创建餐桌、加入、分类、菜品、调味下单、状态推进、完成统计、关闭餐桌和关闭后下单失败。临时数据库必须在进程结束后删除。

- [ ] **步骤 3：核对微信小程序核心链路**

在微信开发者工具依次验证：登录、创建餐桌、加入餐桌、切换餐桌、搜索、随机推荐、调味、购物车、下单、取消、主厨状态推进、成员移除、菜品上下架、调味配置和统计。

- [ ] **步骤 4：更新文档和环境示例**

README 与实际功能一致；明确 `AUTO_SEED`、微信、Turso、Cloudinary 和 Railway 配置；删除所有 Android 内容。

- [ ] **步骤 5：检查仓库卫生**

运行：`git status --short`、`git diff --check`，确认没有数据库、备份、密钥、构建产物或设计原型被提交。

- [ ] **步骤 6：提交**

```bash
git add README.md docs/architecture.md backend/.env.example
git commit -m "docs: 更新小程序部署和使用说明"
```

- [ ] **步骤 7：最终验证**

重新运行步骤 1 的全部命令，并记录测试数量、退出码和仍需人工验证的微信能力。
