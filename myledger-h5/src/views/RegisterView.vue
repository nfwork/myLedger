<template>
  <div class="auth-page">
    <div class="brand">
      <div class="logo">+</div>
      <h1>创建账号</h1>
      <p>注册后自动创建默认资金账户与常用分类</p>
    </div>

    <form class="card form-card" @submit.prevent="submit">
      <div class="field">
        <label for="u">用户名</label>
        <input id="u" v-model.trim="username" autocomplete="username" required minlength="4" placeholder="至少 4 个字符" />
      </div>
      <div class="field">
        <label for="p">密码</label>
        <input id="p" v-model="password" type="password" autocomplete="new-password" required minlength="6" placeholder="至少 6 位" />
      </div>
      <div class="field">
        <label for="p2">确认密码</label>
        <input id="p2" v-model="passwordConfirm" type="password" autocomplete="new-password" required minlength="6" placeholder="请再次输入密码" />
      </div>
      <div class="field">
        <label for="n">昵称（可选）</label>
        <input id="n" v-model.trim="nickname" autocomplete="nickname" placeholder="显示名称" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <button type="submit" class="btn btn-primary full" :disabled="loading">
        {{ loading ? '提交中…' : '注册' }}
      </button>
      <RouterLink class="sub" to="/login">已有账号？去登录</RouterLink>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerAccount } from '@/api'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()

const username = ref('')
const password = ref('')
const passwordConfirm = ref('')
const nickname = ref('')
const loading = ref(false)
const err = ref('')

async function submit() {
  err.value = ''
  if (password.value !== passwordConfirm.value) {
    err.value = '两次输入的密码不一致'
    toast.show(err.value, 'error')
    return
  }
  loading.value = true
  try {
    const body = { username: username.value, password: password.value }
    if (nickname.value) body.nickname = nickname.value
    await registerAccount(body)
    toast.show('注册成功，请登录', 'success')
    router.replace('/login')
  } catch (e) {
    err.value = e?.message || '注册失败'
    toast.show(err.value, 'error')
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
  max-width: 420px;
  margin: 0 auto;
}
.brand {
  text-align: center;
  margin-bottom: 1.75rem;
}
.logo {
  width: 3.25rem;
  height: 3.25rem;
  margin: 0 auto 0.75rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 20px;
  background: linear-gradient(145deg, #5eead4, #0d9488);
  color: #042f2e;
  font-size: 2rem;
  font-weight: 300;
  line-height: 1;
  box-shadow: var(--shadow-lg);
}
.brand h1 {
  margin: 0;
  font-size: 1.45rem;
  font-weight: 800;
}
.brand p {
  margin: 0.35rem 0 0;
  color: var(--muted);
  font-size: 0.88rem;
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
