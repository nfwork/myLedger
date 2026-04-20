import { http } from '../core/http'

/**
 * 登录态（HttpSession）：`/api/auth/*`
 * 与 dbfound `user/user` 区分：此处写 Cookie，业务资料/改密见 `user/user.js`
 */
export async function login(payload) {
  const { data } = await http.post('/api/auth/login', payload, {
    headers: { 'Content-Type': 'application/json' },
  })
  return data
}

export async function logout() {
  const { data } = await http.post('/api/auth/logout')
  return data
}

export async function fetchCurrentUser() {
  const { data } = await http.get('/api/auth/me')
  return data
}
