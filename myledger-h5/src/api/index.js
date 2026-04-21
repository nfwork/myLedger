/**
 * API 统一出口（与后端对齐）：
 * - `core/` — Axios、dbfound 通用封装
 * - `auth/tokens.js` — JWT（`/api/auth/*`）
 * - `health/status.js` — 存活（`/api/health`）
 * - `user/`、`ledger_settings/`（资金账户、分类）、`bookkeeping/`、`report/` — 与 `myledger-api/.../resources/model/` 一致
 *
 * 推荐：`import { login, fetchEntryList } from '@/api'`
 */

export { http } from './core/http'
export { unwrapDbfound, getDbfoundList, getDbfoundTotal, dbfoundPost } from './core/dbfound'

export * from './auth/tokens'
export * from './health/status'

export * from './user/user'
export * from './ledger_settings/account'
export * from './ledger_settings/category'
export * from './bookkeeping/entry'
export * from './report/summary'
