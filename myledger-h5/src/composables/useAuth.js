import { ref, computed } from 'vue'
import { login as apiLogin, logout as apiLogout, fetchCurrentUser, refreshTokens } from '@/api'
import { isUnrecoverableRefreshFailure } from '@/api/auth/refreshBare'
import { clearTokens, getAccessToken, getRefreshToken, setTokensFromApi } from '@/api/auth/tokenStorage'

const user = ref(null)
const bootstrapped = ref(false)
/** @type {Promise<void> | null} */
let bootstrapPromise = null

function setUserFromLoginData(data) {
  if (!data || typeof data !== 'object') {
    user.value = null
    return
  }
  const id = data.user_id
  user.value =
    id != null
      ? {
          user_id: id,
          username: data.username,
          nickname: data.nickname,
        }
      : null
}

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
    if (!bootstrapPromise) {
      bootstrapPromise = (async () => {
        const access = getAccessToken()
        const refresh = getRefreshToken()
        if (!access && !refresh) {
          return
        }

        try {
          if (access && (await fetchMe())) {
            return
          }

          if (refresh) {
            await refreshTokens()
            await fetchMe()
          }
        } catch (e) {
          if (isUnrecoverableRefreshFailure(e)) {
            clearTokens()
          }
          user.value = null
        } finally {
          bootstrapped.value = true
        }
      })().finally(() => {
        bootstrapPromise = null
      })
    }
    await bootstrapPromise
  }

  async function doLogin(payload) {
    const res = await apiLogin(payload)
    if (!res?.success) {
      throw new Error(res?.message || '暂时无法登录')
    }
    setTokensFromApi(res.data)
    setUserFromLoginData(res.data)
    return res.data
  }

  async function doLogout() {
    try {
      await apiLogout()
    } finally {
      clearTokens()
      user.value = null
      bootstrapped.value = false
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
