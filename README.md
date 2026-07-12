# 超大一碗饭

「超大一碗饭」是一款面向多人聚餐场景的点菜应用。主厨可以创建餐桌、维护菜单并查看订单，食客可以通过邀请码加入餐桌、选菜和下单。

项目当前只保留两个运行组件：微信小程序前端与 FastAPI 后端。

## 当前代码范围

- **微信小程序：** 包含登录、餐桌创建与加入、菜单浏览、搜索、购物车、订单、个人中心、菜单管理和订单汇总页面。
- **FastAPI 后端：** 提供认证、用户、餐桌、分类、菜品、订单和图片上传接口。
- **数据与部署：** 本地开发默认使用 SQLite；可配置 Turso、Cloudinary 和 Railway。

上述内容描述仓库中的现有代码范围，不代表所有交互和生产环境配置均已验收。微信登录需要配置小程序凭据；图片云存储需要配置 Cloudinary；正式发布前仍需完成微信开发者工具联调和端到端验收。

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
│   ├── utils/               # 请求、认证与本地存储工具
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
3. 当前小程序没有服务器地址配置界面。本地联调时，需要临时修改 `miniprogram/utils/storage.js` 中的 `DEFAULT_SERVER_URL`：开发者工具可按调试环境设置地址；真机不可使用 `localhost`，应填写开发机的局域网地址，并确保手机与开发机处于同一局域网。提交或发布前必须恢复为正式 HTTPS 域名。
4. 若使用微信登录，在后端 `.env` 中配置 `WX_APPID` 与 `WX_SECRET`。

正式发布时，应将后端域名加入微信公众平台的合法请求域名，并启用 HTTPS。

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
```

## 当前限制

- 微信登录依赖有效的小程序凭据与网络环境；未配置时需要使用仓库现有的账号登录流程进行本地联调。
- Cloudinary 未配置时，图片上传能力不可用于生产环境。
- SQLite 适合本地开发；多实例部署应使用合适的持久化数据库。
- 小程序界面、权限边界和完整下单流程仍需在微信开发者工具及真机环境中持续验收。

更详细的组件边界与数据流请参阅 [架构说明](docs/architecture.md)。
