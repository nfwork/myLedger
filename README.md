# myLedger

个人记账应用：记录收支、分类与统计，便于回顾与长期维护。

## 仓库结构

| 目录 | 说明 |
| --- | --- |
| [myledger-api/](myledger-api/) | 后端：**Spring Boot 3.2** + **dbfound**（`dbfound-spring-boot-starter`），MySQL；业务在 **`resources/model/`**（`user/`、`ledger_settings/`、`bookkeeping/`、`report/`） |
| [myledger-h5/](myledger-h5/) | 前端：**Vue 3** + **Vite 2**；**`src/api/`** 与后端 model 目录对齐；dbfound 路径须写**完整字面量**（见 `.cursor/rules/myledger-h5-api-urls.mdc`） |

后续可增加其它客户端，与当前 **HTTP API** 共用契约。

## 技术栈（摘要）

- **后端**：Java 17、Spring Boot、dbfound、MySQL。
- **前端**：Vue 3、Vue Router、Axios；**Node** 版本以 **`myledger-h5/package.json`** / **`.nvmrc`** 为准（**Vite 2** 与低版本 Node 的约束见该目录说明）。

## 本地运行

### 数据库

新环境在 MySQL 中建库并授权后，执行 **`docs/sql/init_schema.sql`**；脚本内 **`USE …`** 须与 **`myledger-api`** 里 JDBC 库名一致。说明见 **[docs/业务归档.md](docs/业务归档.md)**。

### 后端

```bash
cd myledger-api
mvn spring-boot:run
```

默认 **8080**。开发期 Maven 默认带 Spring **`dev`** profile（`application-dev.yml`，含 model 热加载等）；生产请使用 **`SPRING_PROFILES_ACTIVE=prod`**（或至少非 `dev`）。**IDEA** 运行主类时工作目录建议为 **`myledger-api`** 模块根。

健康检查：`GET http://127.0.0.1:8080/api/health`。登录：`POST /api/auth/login`（返回 JWT，业务请求 `Authorization: Bearer`）；业务接口与路径见 **[docs/api-http.md](docs/api-http.md)**。数据库需含 **`ml_refresh_token`** 表（见 `docs/sql/init_schema.sql`）。

### H5

```bash
cd myledger-h5
npm install
npm run dev
```

默认 **http://127.0.0.1:5173**；**`vite.config.cjs`** 已 **`server.host: true`**，局域网可访问。开发期 **`VITE_API_BASE` 留空** 走代理，避免跨域。生产在 **`myledger-h5/.env.production`** 配置 **`VITE_API_BASE`**。

若 **`npm install` 报 engines**：将 Node 升到 `package.json` 要求后再试。Windows 下 **esbuild** 异常可先结束 **node** 进程后删 **`node_modules`** 重装，或使用项目内的 **`npm run esbuild:cache`**（见 `package.json` 脚本说明）。

## 文档

| 文档 | 说明 |
| --- | --- |
| [docs/业务归档.md](docs/业务归档.md) | **1.0 业务与数据域、脚本索引、鉴权摘要**（归档主文档） |
| [docs/api-http.md](docs/api-http.md) | HTTP 路径与参数 |
| [docs/版本说明.md](docs/版本说明.md) | 版本与重大变更记录 |
| [.cursor/rules/](.cursor/rules/) | Cursor 项目规则 |

## 协作约定

重大变更（模型、API、部署）请更新 **`docs/版本说明.md`**，并同步 **`README.md`** 或 **`docs/业务归档.md`** 相关段落。细则见 **`.cursor/rules/project-maintenance-record.mdc`**。

## 许可证

待定。
