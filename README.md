# 超大一碗饭

「超大一碗饭」是一款面向多人聚餐场景的微信点菜小程序。主厨可以创建餐桌、维护菜单并处理订单，食客可以加入餐桌、选菜和下单。

小程序以原创角色「饭宝」为视觉核心，采用蓝白奶油色轻二次元界面。项目当前只保留微信小程序前端与 FastAPI 后端，Android 客户端已停止维护并移除。

## 主要功能

- **餐桌协作：** 创建餐桌、切换餐桌、复制邀请码，以及通过微信卡片直接邀请好友加入。
- **点菜流程：** 分类浏览、搜索、随机推荐、调味选择、分餐桌购物车、人数与备注。
- **订单流程：** 提交订单、查看进度与历史订单、主厨更新制作状态、订单汇总与营业统计。
- **饭宝分享卡片：** 好友邀请卡片和订单通知卡片使用不同插画与落地路径。
- **通知厨师：** 下单成功后可手动把饭宝订单卡片分享给厨师；厨师点击后进入对应餐桌的订单处理页。
- **菜单管理：** 分类、菜品、供应状态、调味配置和图片上传。

> 微信平台限制：当前小程序账号尚未完成微信认证，微信会暂时拦截「分享给好友」能力。代码、卡片和落地流程已经完成；认证通过后重新预览或上传即可启用。复制邀请码不受该限制。

## 当前代码范围

- **微信小程序：** 包含登录、餐桌创建与邀请、菜单浏览、搜索、购物车、订单、个人中心、菜单管理、订单汇总和饭宝分享卡片。
- **FastAPI 后端：** 提供认证、用户、餐桌、分类、菜品、订单和图片上传接口。
- **数据与部署：** 本地开发默认使用 SQLite；可配置 Turso、Cloudinary 和 Railway。

当前代码已通过自动化测试和微信开发者工具编译验证。微信登录需要配置小程序凭据；图片云存储需要配置 Cloudinary；正式发布仍需在微信公众平台完成认证、审核与发布。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 前端 | 微信小程序原生 JavaScript、WXML、WXSS |
| 后端 | Python、FastAPI、SQLAlchemy、Pydantic |
| 数据库 | SQLite（本地）或 Turso（可选） |
| 图片存储 | Cloudinary（可选） |
| 部署 | Railway |

## 项目结构

```text
meal-app/
├── miniprogram/             # 微信小程序
│   ├── components/          # 通用组件
│   ├── pages/               # 页面
│   ├── utils/               # 请求、认证、分享与本地存储工具
│   ├── images/share/        # 饭宝好友邀请与订单通知卡片
│   ├── tests/               # 小程序纯逻辑测试
│   ├── app.js
│   ├── app.json
│   └── app.wxss
├── backend/                 # FastAPI 后端
│   ├── routers/             # API 路由
│   ├── tests/               # 自动化测试
│   ├── main.py
│   ├── models.py
│   └── schemas.py
├── docs/
│   ├── architecture.md      # 当前架构说明
│   └── superpowers/         # 历史设计规格与实施计划
├── railway.toml
└── requirements.txt
```

## 本地开发

### 1. 启动后端

需要 Python 3.10 或更高版本。

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
cd backend
Copy-Item .env.example .env
python seed_data.py
python main.py
```

后端默认监听 `http://localhost:8000`：

- 健康检查：`GET /api/health`
- OpenAPI 文档：`http://localhost:8000/docs`

Linux 或 macOS 环境可以使用对应的虚拟环境激活命令，其余步骤相同。

### 2. 打开微信小程序

1. 使用微信开发者工具导入 `miniprogram/`。
2. 检查 `miniprogram/project.config.json` 中的 AppID 是否属于当前开发账号。
3. 默认服务器地址为 `https://meal.xiaofengj.xyz`。本地联调如需切换后端，可临时修改 `miniprogram/utils/storage.js` 中的 `DEFAULT_SERVER_URL`；提交前必须恢复正式 HTTPS 域名。
4. 若使用微信登录，在后端 `.env` 中配置 `WX_APPID` 与 `WX_SECRET`。

正式发布时，需要把 `https://meal.xiaofengj.xyz` 同时加入微信公众平台的 `request`、`uploadFile` 和 `downloadFile` 合法域名。

## 当前微信版本

- **AppID：** `wxbe5689fe47ab71a6`
- **最新开发版本：** `2026.07.13.2`
- **后端健康检查：** `https://meal.xiaofengj.xyz/api/health`
- **状态：** 开发版本已上传；正式用户使用前仍需提交审核并发布。

## 后端配置

环境变量示例位于 `backend/.env.example`。主要配置如下：

| 变量 | 用途 |
| --- | --- |
| `DATABASE_URL` | 显式指定数据库连接地址 |
| `TURSO_DATABASE_URL`、`TURSO_AUTH_TOKEN` | Turso 数据库配置 |
| `CLOUDINARY_CLOUD_NAME`、`CLOUDINARY_API_KEY`、`CLOUDINARY_API_SECRET` | 图片存储配置 |
| `SECRET_KEY` | 服务端密钥，生产环境必须修改 |
| `WX_APPID`、`WX_SECRET` | 微信小程序登录配置 |

未配置远程数据库时，后端使用本地 `backend/meal_app.db`。该文件仅用于本地开发，不应提交到版本库。

## 部署

仓库根目录的 `railway.toml` 已定义 Railway 启动命令与 `/api/health` 健康检查。部署前至少需要：

1. 在 Railway 配置生产环境变量。
2. 使用持久化数据库，避免依赖临时文件系统保存业务数据。
3. 将 `SECRET_KEY` 替换为不可预测的随机值。
4. 按需配置微信登录和 Cloudinary。
5. 将部署后的 HTTPS 地址配置到小程序，并完成接口与业务流程验收。

## 测试与静态检查

```powershell
# 后端测试
python -m pytest backend/tests

# Python 语法检查
python -m compileall -q backend

# 小程序 JavaScript 语法检查
Get-ChildItem miniprogram -Recurse -Filter *.js |
  ForEach-Object { node --check $_.FullName }

# 小程序逻辑测试
node --test miniprogram/tests/*.test.js
```

当前验证基线：

- 小程序逻辑测试：10 项通过。
- 后端测试：56 项通过，Windows 环境下 1 项 libSQL 原生扩展测试跳过。
- 微信开发者工具：编译错误为 0。
- 小程序代码包：约 482 KB。

## 当前限制

- 微信登录依赖有效的小程序凭据与网络环境；未配置时需要使用仓库现有的账号登录流程进行本地联调。
- 微信分享依赖小程序账号完成微信认证；未认证时平台会拦截好友分享，但邀请码仍可复制使用。
- Cloudinary 未配置时，图片上传能力不可用于生产环境。
- SQLite 适合本地开发；多实例部署应使用合适的持久化数据库。
- 提交正式版前仍需进行手机端分享、邀请加入和订单处理的最终验收。

更详细的组件边界与数据流请参阅 [架构说明](docs/architecture.md)。
