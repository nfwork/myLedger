import axios from 'axios'

/** 全局 Axios：baseURL、超时、携带 Cookie（Session） */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000,
  withCredentials: true,
})

/**
 * 从 Spring / dbfound 等 JSON 错误体中取出可读文案（HTTP 非 2xx 时 axios 不会走业务 unwrap）
 * @param {unknown} data
 * @returns {string}
 */
export function pickHttpErrorMessage(data) {
  if (data == null) return ''
  if (typeof data === 'string') {
    const t = data.trim()
    return t.length > 0 && t.length < 2000 ? t : ''
  }
  if (typeof data !== 'object') return ''
  const o = /** @type {Record<string, unknown>} */ (data)
  const tryStr = (v) => (typeof v === 'string' && v.trim() ? v.trim() : '')
  let m = tryStr(o.message)
  if (!m) m = tryStr(o.msg)
  if (!m) m = tryStr(o.error_description)
  if (!m && typeof o.error === 'string') m = tryStr(o.error)
  if (!m) m = tryStr(o.detail)
  return m
}

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const res = error.response
    const fromBody = pickHttpErrorMessage(res?.data)
    if (fromBody) {
      const next = new Error(fromBody)
      next.response = res
      return Promise.reject(next)
    }
    if (error.code === 'ECONNABORTED') {
      return Promise.reject(new Error('请求超时，请稍后重试'))
    }
    return Promise.reject(error)
  },
)
