# 分享与厨师订单通知实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 增加好友直接加入餐桌、下单后分享通知厨师、订单详情再次通知，以及两套正式分享插画。

**架构：** 新建纯函数 `utils/share.js` 统一构建分享参数；页面只保存当前餐桌或订单上下文，并通过微信原生 `open-type="share"` 触发分享。邀请继续复用现有 `inviteCode` 登录页自动加入流程，订单卡片直达受后端权限保护的订单详情。

**技术栈：** 微信小程序原生 JavaScript/WXML/WXSS、Node `node:test`、微信开发者工具、静态 PNG 资源。

---

## 文件职责

- 创建 `miniprogram/utils/share.js`：生成餐桌邀请和订单通知分享参数。
- 创建 `miniprogram/tests/share.test.js`：验证分享标题、路径和回退。
- 创建 `miniprogram/images/share/order-notify.png`：订单通知插画。
- 创建 `miniprogram/images/share/invite-friends.png`：好友邀请插画。
- 修改 `miniprogram/pages/menu/menu.js`：启用当前餐桌分享。
- 修改 `miniprogram/pages/profile/profile.js`、`profile.wxml`、`profile.wxss`：复制码与直接邀请并列入口。
- 修改 `miniprogram/pages/cart/cart.js`、`cart.wxml`、`cart.wxss`：下单成功分享弹层。
- 修改 `miniprogram/pages/order-detail/order-detail.js`、`order-detail.wxml`、`order-detail.wxss`：再次通知厨师。
- 修改 `miniprogram/pages/login/login.js`：确保分享邀请码在登录后自动加入。

### 任务 1：分享参数模块与资源

- [ ] **步骤 1：编写失败测试**

在 `miniprogram/tests/share.test.js` 验证：

```js
assert.deepEqual(buildRoomShare({ name: '周末聚餐', code: 'ABC123' }), {
  title: '「周末聚餐」邀请你一起点菜',
  path: '/pages/login/login?inviteCode=ABC123',
  imageUrl: '/images/share/invite-friends.png',
})
assert.equal(buildOrderShare({ id: 21 }).path,
  '/pages/order-detail/order-detail?id=21&from=chefShare')
```

- [ ] **步骤 2：运行 `node --test miniprogram/tests/share.test.js`，确认因模块不存在而失败。**
- [ ] **步骤 3：实现 `buildRoomShare(room)` 与 `buildOrderShare(order)`，对缺失名称、邀请码和订单 ID 提供安全回退。**
- [ ] **步骤 4：复制两张已确认生成图到 `miniprogram/images/share/`，并压缩到适合小程序包体的大小。**
- [ ] **步骤 5：运行分享测试并提交 `feat(分享): 增加分享参数与插画资源`。**

### 任务 2：餐桌直接邀请

- [ ] **步骤 1：为分享模块补充邀请码 URL 编码测试并确认失败。**
- [ ] **步骤 2：菜单页 `onShareAppMessage()` 返回 `buildRoomShare(storage.getActiveRoom())`。**
- [ ] **步骤 3：个人页分享按钮明确标注“直接邀请好友”，与“复制邀请码”并列；`onShareAppMessage()` 使用当前餐桌详情。**
- [ ] **步骤 4：登录页在邀请码自动加入失败时保留邀请码与加入页签，成功后设置当前餐桌并进入菜单。**
- [ ] **步骤 5：运行 Node 测试和 JS 语法检查，提交 `feat(餐桌): 支持好友直接邀请加入`。**

### 任务 3：下单成功通知厨师

- [ ] **步骤 1：购物车新增 `orderSuccessVisible`、`submittedOrder` 状态；订单成功后清空购物车但不立即跳转。**
- [ ] **步骤 2：WXML 增加使用订单插画的成功弹层，主按钮为 `open-type="share"`，次按钮进入订单详情或订单列表。**
- [ ] **步骤 3：`onShareAppMessage()` 在存在已提交订单时返回 `buildOrderShare()`，否则返回餐桌普通分享。**
- [ ] **步骤 4：分享按钮点击时写入本地 `chef_notified_<orderId>` 标记；取消分享不改变订单状态。**
- [ ] **步骤 5：运行测试与语法检查，提交 `feat(订单): 下单成功后支持通知厨师`。**

### 任务 4：订单详情再次通知与回归

- [ ] **步骤 1：订单详情加载本地通知标记，显示“通知厨师处理”或“再次通知厨师”。**
- [ ] **步骤 2：增加 `open-type="share"` 按钮，`onShareAppMessage()` 返回当前订单通知卡片。**
- [ ] **步骤 3：无订单 ID、无餐桌或接口无权限时保持现有回退，不渲染分享入口。**
- [ ] **步骤 4：运行 `node --test miniprogram/tests/*.test.js`、全部 JS `node --check`、后端 `pytest -q` 和 `git diff --check`。**
- [ ] **步骤 5：在微信开发者工具和手机预览验证邀请卡片、成功弹层、订单通知卡片落地路径，提交 `feat(分享): 完成厨师通知与邀请流程`。**
