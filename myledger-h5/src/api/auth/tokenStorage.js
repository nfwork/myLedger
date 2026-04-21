const ACCESS = 'myledger.access_token'
const REFRESH = 'myledger.refresh_token'

/** @param {string} v */
export function setAccessToken(v) {
  if (v) localStorage.setItem(ACCESS, v)
  else localStorage.removeItem(ACCESS)
}

/** @param {string} v */
export function setRefreshToken(v) {
  if (v) localStorage.setItem(REFRESH, v)
  else localStorage.removeItem(REFRESH)
}

/**
 * 写入登录或 `/api/auth/refresh` 返回的令牌。
 * @param {Record<string, unknown>} data
 */
export function setTokensFromApi(data) {
  if (!data || typeof data !== 'object') return
  const at = /** @type {string|undefined} */ (data.access_token)
  const rt = /** @type {string|undefined} */ (data.refresh_token)
  if (at) setAccessToken(at)
  if (rt) setRefreshToken(rt)
}

export function clearTokens() {
  localStorage.removeItem(ACCESS)
  localStorage.removeItem(REFRESH)
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS) || ''
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH) || ''
}
