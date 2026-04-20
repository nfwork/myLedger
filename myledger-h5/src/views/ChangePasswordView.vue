<template>
  <div class="pwd">
    <form class="card form" @submit.prevent="submit">
      <div class="field">
        <label for="o">当前密码</label>
        <input id="o" v-model="oldPassword" type="password" autocomplete="current-password" required />
      </div>
      <div class="field">
        <label for="n">新密码</label>
        <input id="n" v-model="newPassword" type="password" autocomplete="new-password" required minlength="6" />
      </div>
      <div class="field">
        <label for="n2">确认新密码</label>
        <input id="n2" v-model="newPassword2" type="password" autocomplete="new-password" required minlength="6" />
      </div>
      <p v-if="err" class="err">{{ err }}</p>
      <button type="submit" class="btn btn-primary full" :disabled="saving">{{ saving ? '提交中…' : '确认修改' }}</button>
    </form>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { changePassword } from '@/api'
import { useToast } from '@/composables/useToast'

const router = useRouter()
const toast = useToast()

const oldPassword = ref('')
const newPassword = ref('')
const newPassword2 = ref('')
const err = ref('')
const saving = ref(false)

async function submit() {
  err.value = ''
  if (newPassword.value !== newPassword2.value) {
    err.value = '两次输入的新密码不一致'
    return
  }
  saving.value = true
  try {
    await changePassword({
      old_password: oldPassword.value,
      new_password: newPassword.value,
    })
    toast.show('密码已更新', 'success')
    router.replace('/profile')
  } catch (e) {
    err.value = e?.message || '修改失败'
    toast.show(err.value, 'error')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.pwd {
  max-width: 480px;
  margin: 0 auto;
}
.form {
  padding: 1.15rem 1.1rem 1.5rem;
}
.full {
  width: 100%;
}
.err {
  color: var(--expense);
  font-size: 0.85rem;
}
</style>
