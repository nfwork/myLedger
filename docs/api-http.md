# myLedger HTTP API（1.0）

**`GET /api/health`**：Spring 健康检查（不经 dbfound）。其余业务接口主要为 **dbfound Model HTTP**：路径对应 `src/main/resources/model/**/*.xml`（不含 `.xml`），后缀 **`.query` / `.query!名称`** 为查询，**`.execute!名称`** 为执行。

**模块与路径**：`model/user/user.xml` → `/user/user.*`；`model/ledger_settings/account.xml`、`category.xml` → `/ledger_settings/account.*`、`/ledger_settings/category.*`；`model/bookkeeping/entry.xml` → `/bookkeeping/entry.*`；`model/report/summary.xml` → `/report/summary.*`。

## 登录与 Session

1. **`POST /api/auth/login`**（JSON：`username`, `password`）→ HttpSession；响应 `Set-Cookie: JSESSIONID=...`。
2. 后续请求携带 Cookie（axios：`withCredentials: true`）。
3. **`LoginSessionFilter`**：无 `user_id` 则 **401**：`{"success":false,"message":"未登录或会话已过期"}`。
4. dbfound 中 **`user_id` 为 `scope="session"`**，请求体**不要传 `user_id`**。
5. **`GET /api/auth/me`**、**`POST /api/auth/logout`**。

匿名仍可直接调 **`/user/user.query!login`**、**`/user/user.execute!register`**（不写 Session）；H5 推荐 **`/api/auth/login`**。

## 调用约定

- **方法**：推荐 **POST**，`Content-Type: application/json`。
- **参数**：**snake_case**（与 Jackson `SNAKE_CASE` 一致）。
- **匿名白名单**：`/api/health`、`/api/auth/login`、`/api/auth/logout`、`/user/user.query!login`、`/user/user.execute!register`。

## 接口一览

### Spring

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/health` | 存活 |
| POST | `/api/auth/login` | body：`username`, `password` |
| POST | `/api/auth/logout` | 注销 |
| GET | `/api/auth/me` | 当前用户（需 Session） |

### `user/user`（dbfound）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/user/user.query!login` | 匿名；仅校验，不写 Session |
| query | `/user/user.query!getById` | 需登录 |
| execute | `/user/user.execute!register` | 匿名；注册并初始化默认账户与分类 |
| execute | `/user/user.execute!updateProfile` | `nickname` |
| execute | `/user/user.execute!changePassword` | `old_password`, `new_password` |

### `ledger_settings/account`（需登录）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/ledger_settings/account.query!listByUser` | 列表 |
| query | `/ledger_settings/account.query!getDefault` | 默认账户 |
| execute | `/ledger_settings/account.execute!add` | `name`，可选 `sort_order` |
| execute | `/ledger_settings/account.execute!update` | `id`, `name`，可选 `sort_order` |
| execute | `/ledger_settings/account.execute!setDefault` | `id` |
| execute | `/ledger_settings/account.execute!delete` | `id`（默认账户不可删；有流水不可删） |

### `ledger_settings/category`（需登录）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/ledger_settings/category.query!list` | 可选 `type`：`income` / `expense` |
| execute | `/ledger_settings/category.execute!add` | `name`, `type`，可选 `sort_order` |
| execute | `/ledger_settings/category.execute!update` | `id`, `name`，可选 `sort_order` |
| execute | `/ledger_settings/category.execute!delete` | `id` |

### `bookkeeping/entry`（需登录）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/bookkeeping/entry.query!list` | 分页：`start`（从 0）、`limit`，默认 `pagerSize=20`；响应含 `total_counts`。可选：`account_id`, `category_id`, `entry_type`, `year_month`, `entry_date_from`, `entry_date_to`, `remark_keyword`；行含 `account_name` |
| query | `/bookkeeping/entry.query!getById` | `id` |
| execute | `/bookkeeping/entry.execute!add` | **`account_id`**, `category_id`, `entry_type`, `amount`, `entry_date`, **`remark`（必填，trim 非空）** |
| execute | `/bookkeeping/entry.execute!update` | 同上 + `id` |
| execute | `/bookkeeping/entry.execute!delete` | `id` |

### `report/summary`（需登录）

| 类型 | 路径 | 说明 |
|------|------|------|
| query | `/report/summary.query!monthTotals` | `year_month`（`YYYY-MM`）；可选 `account_id` |
| query | `/report/summary.query!byCategory` | `year_month`, `entry_type`；可选 `account_id` |
| query | `/report/summary.query!matrixByMonthCategory` | `year_month_from`, `year_month_to`, `entry_type`；可选 `account_id`；返回 **`bill_month`**、`category_id`, `category_name`, `total_amount` |
| query | `/report/summary.query!byDay` | `year_month`；可选 `account_id`；`day_date`, `income_amount`, `expense_amount` |

## 响应结构

dbfound：**ResponseObject** JSON。Spring **`/api/auth/*`**：**ApiResponse**（`success`, `message?`, `data?`）；登录与 `/me` 的 `data` 含 `user_id`、`username`、`nickname`。

## curl 示例（Windows，带 Cookie）

```bash
curl -s -c jar.txt -X POST http://127.0.0.1:8080/api/auth/login ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"demo\",\"password\":\"123456\"}"

curl -s -b jar.txt -X POST http://127.0.0.1:8080/ledger_settings/account.query!listByUser ^
  -H "Content-Type: application/json" ^
  -d "{}"
```

（Linux/macOS 将 `^` 换为行末 `\`。）
