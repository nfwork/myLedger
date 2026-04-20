<template>
  <div class="profile">
    <div class="hero card">
      <div class="avatar">{{ initials }}</div>
      <div>
        <h2>{{ user?.username || '—' }}</h2>
        <p class="nick">{{ user?.nickname || '未设置昵称' }}</p>
      </div>
    </div>

    <form class="card block" @submit.prevent="saveNick">
      <div class="field">
        <label for="nick">昵称</label>
        <input id="nick" v-model.trim="nickname" maxlength="64" placeholder="显示名称" />
      </div>
      <button type="submit" class="btn btn-primary full" :disabled="saving">{{ saving ? '保存中…' : '保存昵称' }}</button>
    </form>

    <div class="card block links">
      <RouterLink to="/accounts" class="row">资金账户 <span class="chev">›</span></RouterLink>
      <RouterLink to="/categories" class="row">分类管理 <span class="chev">›</span></RouterLink>
      <RouterLink to="/password" class="row">修改密码 <span class="chev">›</span></RouterLink>
    </div>

    <button type="button" class="btn btn-ghost full logout" @click="doLogout">退出登录</button>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { updateProfile } from '@/api'
import { useToast } from '@/composables/useToast'

const auth = useAuth()
const router = useRouter()
const toast = useToast()

const user = computed(() => auth.user.value)
const nickname = ref('')
const saving = ref(false)

const initials = computed(() => {
  const n = user.value?.username || '?'
  return String(n).slice(0, 1).toUpperCase()
})

watch(
  () => user.value,
  (u) => {
    nickname.value = u?.nickname || ''
  },
  { immediate: true },
)

onMounted(async () => {
  await auth.fetchMe()
})

async function saveNick() {
  saving.value = true
  try {
    await updateProfile({ nickname: nickname.value })
    await auth.fetchMe()
    toast.show('昵称已更新', 'success')
  } catch (e) {
    toast.show(e?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function doLogout() {
  await auth.logout()
  toast.show('已退出', 'info')
  router.replace('/login')
}
</script>

<style scoped>
.profile {
  max-width: 520px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.hero {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1.1rem 1rem;
}
.avatar {
  width: 3.5rem;
  height: 3.5rem;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.35rem;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(145deg, #2dd4bf, #0f766e);
  box-shadow: var(--shadow);
}
.hero h2 {
  margin: 0;
  font-size: 1.15rem;
  font-weight: 800;
}
.nick {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.88rem;
}
.block {
  padding: 1rem 1.05rem 1.15rem;
}
.full {
  width: 100%;
}
.links .row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 0.25rem;
  font-weight: 700;
  text-decoration: none;
  color: var(--text);
}
.links .row + .row {
  border-top: 1px solid var(--line);
}
.chev {
  color: var(--muted);
  font-size: 1.25rem;
}
.logout {
  width: 100%;
  border: 1px solid var(--line);
}
</style>
