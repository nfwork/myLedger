<template>
  <div class="auth-page">
    <div class="brand">
      <div class="logo">◇</div>
      <h1>myLedger</h1>
      <p>简单记账，清晰收支</p>
    </div>

    <form class="card form-card" @submit.prevent="submit">
      <div class="field">
        <label for="u">用户名</label>
        <input id="u" v-model.trim="username" autocomplete="username" required minlength="4" placeholder="至少 4 个字符" />
      </div>
      <div class="field">
        <label for="p">密码</label>
        <input id="p" v-model="password" type="password" autocomplete="current-password" required minlength="6" placeholder="至少 6 位" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <button type="submit" class="btn btn-primary full" :disabled="loading">
        {{ loading ? '登录中…' : '登录' }}
      </button>
      <RouterLink class="sub" to="/register">还没有账号？去注册</RouterLink>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const toast = useToast()

const username = ref('')
const password = ref('')
const loading = ref(false)
const err = ref('')

/** 将接口/网络错误转成登录页可读、温和的提示 */
function friendlyLoginMessage(e) {
  const raw = String(
    (typeof e?.response?.data === 'string' ? e.response.data : e?.response?.data?.message || e?.response?.data?.detail) ||
      e?.message ||
      '',
  ).trim()
  const status = e?.response?.status
  const code = e?.code

  if (code === 'ECONNABORTED' || raw.includes('超时')) {
    return '等待时间有点长，请稍后再试一次'
  }
  if (!e?.response && (code === 'ERR_NETWORK' || raw === 'Network Error')) {
    return '当前连不上服务器，请检查网络或确认服务已开启'
  }
  if (status === 401 || /用户名或密码|账号或密码|未授权|Unauthorized/i.test(raw)) {
    return '账号或密码不对，请核对后再试；新用户可先注册'
  }
  if (/缺少.*username|缺少.*password|username 或 password/i.test(raw)) {
    return '请填写用户名和密码'
  }
  if (raw && raw.length <= 160 && !/^Request failed with status code \d+$/i.test(raw)) {
    return raw
  }
  return '暂时无法登录，请稍后再试'
}

async function submit() {
  err.value = ''
  loading.value = true
  try {
    await auth.login({ username: username.value, password: password.value })
    toast.show('欢迎回来', 'success')
    const redir = route.query.redirect
    router.replace(typeof redir === 'string' && redir.startsWith('/') ? redir : '/dashboard')
  } catch (e) {
    err.value = friendlyLoginMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100dvh;
  padding: 2.5rem 1.25rem 2rem;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  max-width: 420px;
  margin: 0 auto;
}
.brand {
  text-align: center;
  margin-bottom: 1.75rem;
  color: var(--text);
}
.logo {
  width: 3.25rem;
  height: 3.25rem;
  margin: 0 auto 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: linear-gradient(145deg, #14b8a6, #0f766e);
  color: #ecfdf5;
  font-size: 1.75rem;
  font-weight: 300;
  box-shadow: var(--shadow-lg);
}
.brand h1 {
  margin: 0;
  font-size: 1.65rem;
  font-weight: 800;
  letter-spacing: -0.02em;
}
.brand p {
  margin: 0.35rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
}
.form-card {
  padding: 1.35rem 1.25rem 1.5rem;
}
.full {
  width: 100%;
  margin-top: 0.25rem;
}
.err {
  color: var(--expense);
  font-size: 0.85rem;
  margin: 0 0 0.75rem;
}
.sub {
  display: block;
  text-align: center;
  margin-top: 1.1rem;
  font-size: 0.88rem;
  text-decoration: none;
  color: var(--muted);
  font-weight: 600;
}
</style>
