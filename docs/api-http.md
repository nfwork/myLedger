# myLedger HTTP API（1.0）

**文档与 curl 示例统一使用的 API 根地址**：`http://192.168.0.156:8080`（与仓库 **README**、**H5 `.env.*`**、**Android `gradle.properties`** 一致）。

**`GET /api/health`**：Spring 健康检查（不经 dbfound）。其余业务接口主要为 **dbfound Model HTTP**：路径对应 `src/main/resources/model/**/*.xml`（不含 `.xml`），后缀 **`.query` / `.query!名称`** 为查询，**`.execute!名称`** 为执行。

**模块与路径**：`model/user/user.xml` → `/user/user.*`；`model/ledger_settings/account.xml`、`category.xml` → `/ledger_settings/account.*`、`/ledger_settings/category.*`；`model/bookkeeping/entry.xml` → `/bookkeeping/entry.*`；`model/report/summary.xml` → `/report/summary.*`。dbfound Model HTTP 已启用 **`dbfound.web.api-allow-urls`** 白名单，只有配置列出的路径可被 HTTP 访问；白名单内除下文「匿名白名单」以外路径一致：**须 Bearer**。**`user/user.query!login`**、**`user/user.query!findByIdForAuth`**、**`model/auth/refresh_token.xml`** 下接口仅供服务端 **`ModelExecutor`** 调用，不开放 dbfound HTTP；终端登录请用 **`POST /api/auth/login`**。

## 登录与鉴权（JWT + dbfound request 作用域）

1. **`POST /api/auth/login`**（JSON：`username`, `password`）→ 返回 **`access_token`**（JWT）、**`refresh_token`**（opaque，仅本次响应明文）、**`token_type`**（`Bearer`）、**`expires_in`**（秒）、以及 **`user_id` / `username` / `nickname`**。服务端**不再**依赖 HttpSession 作为 API 鉴权。**同一用户可多处同时登录**：每次登录在 **`ml_refresh_token`** 新增一行，**不会**按用户删除其它设备已有 refresh。
2. 受保护请求须带：**`Authorization: Bearer <access_token>`**。
3. **`BearerAuthFilter`**：无令牌或 JWT 无效/过期则 **401**：`{"success":false,"message":"未登录或访问令牌已过期"}`；通过后把 **`user_id`、`username`** 写入 **`HttpServletRequest` 的 attribute**（access JWT 不含昵称；昵称见登录/refresh 包体或 **`GET /api/auth/me`** 读库）。
4. dbfound 中 **`user_id` 为 `scope="request"`**，取自上述 attribute；**请求体不要传 `user_id`**。
5. **`POST /api/auth/refresh`**（JSON：`refresh_token`）→ 轮换 refresh（旧行删除）并返回新的 **`access_token` + `refresh_token`** 等，与登录响应结构一致。
6. **`POST /api/auth/logout`**（可选 JSON：`refresh_token`）→ 吊销对应 refresh 行；可不携带 access（匿名路径）。
7. **`GET /api/auth/me`** → 当前用户（需有效 **access** JWT）。

**刷新令牌存储**：表 **`ml_refresh_token`**（见 `docs/sql/init_schema.sql`）；库内存 **SHA-256 hex**，不存明文。

匿名仍可直接调 **`/user/user.execute!register`**；登录请用 **`POST /api/auth/login`**（勿依赖 `user.query!login` 的 dbfound HTTP）。

### Android / 其它原生客户端

- 登录后持久化 **`refresh_token`**（安全存储），**`access_token`** 可放内存；请求头固定带 Bearer。
- access 过期时用 **`/api/auth/refresh`** 换新；退出时 **`/api/auth/logout`** 并删除本地 refresh。

## 调用约定

- **方法**：推荐 **POST**，`Content-Type: application/json`。
- **参数**：**snake_case**（与 Jackson `SNAKE_CASE` 一致）。
- **匿名白名单**：`/api/health`、`/api/auth/login`、`/api/auth/logout`、`/api/auth/refresh`、`/user/user.execute!register`。

## 接口一览

### Spring

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 存活 |
| POST | `/api/auth/login` | body：`username`, `password` → 见上文 |
| POST | `/api/auth/refresh` | body：`refresh_token` |
| POST | `/api/auth/logout` | 可选 body：`refresh_token` |
| GET | `/api/auth/me` | 当前用户（需 `Authorization: Bearer`） |

### `user/user`（dbfound）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/user/user.query!login` | 服务端内部；与 `/api/auth/login` 共用校验 SQL，不在 dbfound HTTP 白名单 |
| query | `/user/user.query!getById` | 需 Bearer |
| execute | `/user/user.execute!register` | 匿名；注册并初始化默认账户与分类 |
| execute | `/user/user.execute!updateProfile` | `nickname` |
| execute | `/user/user.execute!changePassword` | `old_password`, `new_password` |

### `ledger_settings/account`（需 Bearer）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/ledger_settings/account.query!listByUser` | 列表 |
| query | `/ledger_settings/account.query!getDefault` | 默认账户 |
| execute | `/ledger_settings/account.execute!add` | `name`，可选 `sort_order` |
| execute | `/ledger_settings/account.execute!update` | `id`, `name`，可选 `sort_order` |
| execute | `/ledger_settings/account.execute!setDefault` | `id` |
| execute | `/ledger_settings/account.execute!delete` | `id`（默认账户不可删；有流水不可删） |

### `ledger_settings/category`（需 Bearer）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/ledger_settings/category.query!list` | 可选 `type`：`income` / `expense` |
| execute | `/ledger_settings/category.execute!add` | `name`, `type`，可选 `sort_order` |
| execute | `/ledger_settings/category.execute!update` | `id`, `name`，可选 `sort_order` |
| execute | `/ledger_settings/category.execute!delete` | `id` |

### `bookkeeping/entry`（需 Bearer）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/bookkeeping/entry.query!list` | 分页：`start`（从 0）、`limit`，默认 `pagerSize=20`；响应含 `total_counts`。可选：`account_id`, `category_id`, `entry_type`, `year_month`, `entry_date_from`, `entry_date_to`, `remark_keyword`；行含 `account_name` |
| query | `/bookkeeping/entry.query!getById` | `id` |
| execute | `/bookkeeping/entry.execute!add` | **`account_id`**, `category_id`, `entry_type`, `amount`, `entry_date`, **`remark`（必填，trim 非空）** |
| execute | `/bookkeeping/entry.execute!update` | 同上 + `id` |
| execute | `/bookkeeping/entry.execute!delete` | `id` |

### `report/summary`（需 Bearer）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/report/summary.query!monthTotals` | `year_month`（`YYYY-MM`）；可选 `account_id` |
| query | `/report/summary.query!byCategory` | `year_month`, `entry_type`；可选 `account_id` |
| query | `/report/summary.query!matrixByMonthCategory` | `year_month_from`, `year_month_to`, `entry_type`；可选 `account_id`；返回 **`bill_month`**、`category_id`, `category_name`, `total_amount` |
| query | `/report/summary.query!byDay` | `year_month`；可选 `account_id`；`day_date`, `income_amount`, `expense_amount` |

## 响应结构

dbfound：**ResponseObject** JSON。Spring **`/api/auth/*`**：**ApiResponse**（`success`, `message?`, `data?`）。  
登录与 refresh 的 `data`：`user_id`, `username`, `nickname`, `access_token`, `refresh_token`, `token_type`, `expires_in`。  
`/me` 的 `data`：`user_id`, `username`, `nickname`。

## curl 示例（Bearer）

```bash
curl -s -X POST http://192.168.0.156:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"demo\",\"password\":\"123456\"}"

REM 将响应中的 access_token 填入下方 AT
set AT=eyJ...

curl -s -X POST http://192.168.0.156:8080/ledger_settings/account.query!listByUser ^
  -H "Content-Type: application/json" ^
  -H "Authorization: Bearer %AT%" ^
  -d "{}"
```

（Linux/macOS 将 `^` 换为行末 `\`，环境变量写法按 shell 调整。）
