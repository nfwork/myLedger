import { http } from '../core/http'
import { clearTokens, getRefreshToken, setTokensFromApi } from './tokenStorage'
import { postRefreshRaw } from './refreshBare'

/**
 * JWT：`/api/auth/*`；业务 dbfound 见各模块 js。
 */
export async function login(payload) {
  const { data } = await http.post('/api/auth/login', payload, {
    headers: { 'Content-Type': 'application/json' },
  })
  return data
}

export async function logout() {
  const rt = getRefreshToken()
  const { data } = await http.post('/api/auth/logout', rt ? { refresh_token: rt } : {})
  clearTokens()
  return data
}

export async function fetchCurrentUser() {
  const { data } = await http.get('/api/auth/me')
  return data
}

/** 主动刷新（如路由 bootstrap）；仅当 `isUnrecoverableRefreshFailure(err)`（当前即 refresh 返回 401）时再 clearTokens */
export async function refreshTokens() {
  const rt = getRefreshToken()
  if (!rt) throw new Error('no refresh token')
  const body = await postRefreshRaw(rt)
  if (!body?.success || !body.data) {
    throw new Error(body?.message || '刷新失败')
  }
  setTokensFromApi(body.data)
}
