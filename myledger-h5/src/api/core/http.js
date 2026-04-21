import axios from 'axios'
import { clearTokens, getAccessToken, getRefreshToken, setTokensFromApi } from '../auth/tokenStorage'
import { isUnrecoverableRefreshFailure, postRefreshRaw } from '../auth/refreshBare'

/** 全局 Axios：baseURL、超时；鉴权为 Bearer（不再依赖 Cookie Session） */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000,
  withCredentials: false,
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

/** @type {Promise<void> | null} */
let refreshChain = null

function runRefreshChain() {
  if (!refreshChain) {
    refreshChain = (async () => {
      const rt = getRefreshToken()
      if (!rt) throw new Error('no refresh token')
      const body = await postRefreshRaw(rt)
      if (!body?.success || !body.data) {
        throw new Error(body?.message || '刷新失败')
      }
      setTokensFromApi(body.data)
    })().finally(() => {
      refreshChain = null
    })
  }
  return refreshChain
}

http.interceptors.request.use((config) => {
  const url = String(config.url || '')
  if (url.includes('/api/auth/refresh')) {
    return config
  }
  const token = getAccessToken()
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const res = error.response
    const cfg = /** @type {import('axios').InternalAxiosRequestConfig & { _authRetry?: boolean }} */ (
      error.config || {}
    )

    if (res?.status === 401 && !cfg._authRetry && shouldTryRefresh(cfg)) {
      cfg._authRetry = true
      try {
        await runRefreshChain()
        return http(cfg)
      } catch (refreshErr) {
        if (isUnrecoverableRefreshFailure(refreshErr)) {
          clearTokens()
        }
      }
    }

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

/**
 * @param {import('axios').InternalAxiosRequestConfig} cfg
 */
function shouldTryRefresh(cfg) {
  const url = String(cfg.url || '')
  if (url.includes('/api/auth/login') || url.includes('/api/auth/refresh')) {
    return false
  }
  return !!getRefreshToken()
}
