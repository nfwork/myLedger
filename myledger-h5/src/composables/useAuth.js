import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, fetchCurrentUser } from '@/api'

const user = ref(null)
const bootstrapped = ref(false)

export function useAuth() {
  const isLoggedIn = computed(() => !!user.value?.user_id)

  async function fetchMe() {
    try {
      const res = await fetchCurrentUser()
      if (res?.success && res.data) {
        user.value = res.data
        return true
      }
    } catch {
      user.value = null
    }
    return false
  }

  async function bootstrap() {
    if (bootstrapped.value) return
    bootstrapped.value = true
    await fetchMe()
  }

  async function doLogin(payload) {
    const res = await apiLogin(payload)
    if (!res?.success) {
      throw new Error(res?.message || '登录失败')
    }
    user.value = res.data
    return res.data
  }

  async function doLogout() {
    try {
      await apiLogout()
    } finally {
      user.value = null
    }
  }

  return {
    user,
    isLoggedIn,
    fetchMe,
    bootstrap,
    login: doLogin,
    logout: doLogout,
  }
}
