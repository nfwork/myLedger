import axios from 'axios'

/**
 * 是否应在 refresh 失败后清除本地令牌：仅当服务端对 `/api/auth/refresh` 返回 HTTP 401
 *（表示 refresh 未被接受，如过期、无效、已吊销）。
 * 403、参数校验失败、200 + success:false、网络/5xx 等一律 false，避免误删仍可能有效的 refresh。
 * @param {unknown} err
 */
export function isUnrecoverableRefreshFailure(err) {
  const st = /** @type {{ response?: { status?: number } }} */ (err)?.response?.status
  return st === 401
}

/**
 * 使用独立 axios，避免与 `http.js` 拦截器循环依赖；仅用于刷新 access。
 * @param {string} refreshToken
 */
export async function postRefreshRaw(refreshToken) {
  const base = import.meta.env.VITE_API_BASE || ''
  const { data } = await axios.post(
    `${base}/api/auth/refresh`,
    { refresh_token: refreshToken },
    {
      timeout: 20000,
      headers: { 'Content-Type': 'application/json' },
    },
  )
  return data
}
