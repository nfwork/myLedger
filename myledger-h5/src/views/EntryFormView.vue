<template>
  <div class="form-page">
    <p v-if="booting" class="muted">加载中…</p>
    <form v-else class="card form" @submit.prevent="submit">
      <div class="field">
        <label>类型</label>
        <div class="toggle">
          <button type="button" :class="{ on: form.entry_type === 'expense' }" @click="setType('expense')">支出</button>
          <button type="button" :class="{ on: form.entry_type === 'income' }" @click="setType('income')">收入</button>
        </div>
      </div>

      <div class="field">
        <label for="amt">金额</label>
        <input id="amt" v-model.number="form.amount" type="number" step="0.01" min="0.01" required placeholder="0.00" />
      </div>

      <div class="field">
        <label for="dt">日期</label>
        <input id="dt" v-model="form.entry_date" type="date" required />
      </div>

      <div class="field">
        <label for="acc">资金账户</label>
        <select id="acc" v-model.number="form.account_id" required>
          <option v-for="a in accounts" :key="a.id" :value="a.id">{{ a.name }}{{ isDefaultAccount(a) ? '（默认）' : '' }}</option>
        </select>
      </div>

      <div class="field">
        <label for="cat">分类</label>
        <select id="cat" v-model.number="form.category_id" required>
          <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
        </select>
      </div>

      <div class="field">
        <label for="rm">备注<span class="req">（必填）</span></label>
        <textarea
          id="rm"
          v-model.trim="form.remark"
          rows="2"
          maxlength="512"
          required
          placeholder="简要说明这笔账，便于日后按备注查找"
        />
      </div>

      <p v-if="err" class="err">{{ err }}</p>

      <button type="submit" class="btn btn-primary full" :disabled="saving">
        {{ saving ? '保存中…' : isEdit ? '保存修改' : '保存' }}
      </button>
      <button v-if="isEdit" type="button" class="btn btn-danger full outline" :disabled="saving" @click="showDeleteConfirm = true">
        删除本条
      </button>
    </form>

    <div v-if="showPostSaveChoice" class="dialog-mask" role="dialog" aria-modal="true" aria-labelledby="post-save-title">
      <div class="dialog-card card">
        <h2 id="post-save-title">记账成功</h2>
        <p>这笔流水已保存。要继续记账，还是返回流水页？</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-ghost" @click="continueEntry">继续记账</button>
          <button type="button" class="btn btn-primary" @click="backToEntries">返回流水页</button>
        </div>
      </div>
    </div>

    <div v-if="showDeleteConfirm" class="dialog-mask" role="dialog" aria-modal="true" aria-labelledby="delete-title">
      <div class="dialog-card card">
        <h2 id="delete-title">确认删除</h2>
        <p>确定删除这条流水？</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-ghost" :disabled="saving" @click="showDeleteConfirm = false">取消</button>
          <button type="button" class="btn btn-danger solid" :disabled="saving" @click="remove">
            {{ saving ? '删除中…' : '删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  fetchAccountList,
  fetchCategoryList,
  fetchEntryById,
  createEntry,
  updateEntry,
  deleteEntry,
} from '@/api'
import { useToast } from '@/composables/useToast'

const route = useRoute()
const router = useRouter()
const toast = useToast()

const isEdit = computed(() => !!route.params.id)
const booting = ref(true)
const saving = ref(false)
const err = ref('')
const accounts = ref([])
const categories = ref([])
const showPostSaveChoice = ref(false)
const showDeleteConfirm = ref(false)

const form = reactive({
  entry_type: 'expense',
  amount: null,
  entry_date: new Date().toISOString().slice(0, 10),
  account_id: null,
  category_id: null,
  remark: '',
})

function isDefaultAccount(a) {
  return a.is_default === 1 || a.is_default === true
}

async function loadAccounts() {
  accounts.value = await fetchAccountList()
  const def = accounts.value.find((a) => isDefaultAccount(a))
  if (!form.account_id) {
    form.account_id = (def || accounts.value[0])?.id ?? null
  }
}

async function loadCategories() {
  categories.value = await fetchCategoryList({ type: form.entry_type })
  if (!categories.value.find((c) => c.id === form.category_id)) {
    form.category_id = categories.value[0]?.id ?? null
  }
}

async function setType(t) {
  if (form.entry_type === t) return
  form.entry_type = t
  await loadCategories()
}

async function loadEntry() {
  if (!isEdit.value) return
  const row = await fetchEntryById(route.params.id)
  if (!row) throw new Error('记录不存在')
  form.entry_type = row.entry_type
  form.amount = Number(row.amount)
  form.entry_date = String(row.entry_date).slice(0, 10)
  form.account_id = row.account_id
  form.category_id = row.category_id
  form.remark = row.remark || ''
}

onMounted(async () => {
  err.value = ''
  booting.value = true
  try {
    await loadAccounts()
    if (isEdit.value) {
      await loadEntry()
    }
    await loadCategories()
  } catch (e) {
    err.value = e?.message || '加载失败'
    toast.show(err.value, 'error')
  } finally {
    booting.value = false
  }
})

async function submit() {
  err.value = ''
  const amount = Number(form.amount)
  if (form.amount == null || form.amount === '') {
    err.value = '请输入金额'
    toast.show(err.value, 'error')
    return
  }
  if (!Number.isFinite(amount) || amount < 0.01) {
    err.value = '请输入有效金额'
    toast.show(err.value, 'error')
    return
  }
  const dateErr = validateEntryDate(form.entry_date)
  if (dateErr) {
    err.value = dateErr
    toast.show(err.value, 'error')
    return
  }
  if (form.category_id == null || form.account_id == null) {
    err.value = '请选择资金账户与分类'
    toast.show(err.value, 'error')
    return
  }
  if (!form.remark || !String(form.remark).trim()) {
    err.value = '请填写备注'
    toast.show(err.value, 'error')
    return
  }
  saving.value = true
  try {
    const body = {
      account_id: form.account_id,
      category_id: form.category_id,
      entry_type: form.entry_type,
      amount,
      entry_date: form.entry_date,
      remark: form.remark.trim(),
    }
    if (isEdit.value) {
      body.id = Number(route.params.id)
      await updateEntry(body)
      toast.show('已保存', 'success')
      router.push('/entries')
    } else {
      await createEntry(body)
      showPostSaveChoice.value = true
    }
  } catch (e) {
    err.value = e?.message || '保存失败'
    toast.show(err.value, 'error')
  } finally {
    saving.value = false
  }
}

function continueEntry() {
  form.amount = null
  form.remark = ''
  err.value = ''
  showPostSaveChoice.value = false
}

function backToEntries() {
  showPostSaveChoice.value = false
  toast.show('已记账', 'success')
  router.push('/entries')
}

async function remove() {
  saving.value = true
  try {
    await deleteEntry(route.params.id)
    toast.show('已删除', 'success')
    router.push('/entries')
  } catch (e) {
    toast.show(e?.message || '删除失败', 'error')
  } finally {
    saving.value = false
    showDeleteConfirm.value = false
  }
}

function validateEntryDate(value) {
  const s = String(value || '').trim()
  if (!s) return '请填写日期'
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(s)
  if (!match) return '请输入有效日期'
  const [, yy, mm, dd] = match
  const d = new Date(Number(yy), Number(mm) - 1, Number(dd))
  if (
    d.getFullYear() !== Number(yy) ||
    d.getMonth() + 1 !== Number(mm) ||
    d.getDate() !== Number(dd)
  ) {
    return '请输入有效日期'
  }
  return ''
}
</script>

<style scoped>
.form-page {
  max-width: 520px;
  margin: 0 auto;
}
.muted {
  text-align: center;
  color: var(--muted);
}
.form {
  padding: 1.15rem 1.1rem 1.5rem;
}
.toggle {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.5rem;
}
.toggle button {
  padding: 0.55rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: var(--bg);
  font-weight: 700;
  color: var(--muted);
  cursor: pointer;
}
.toggle button.on {
  border-color: rgb(13 148 136 / 0.45);
  background: rgb(13 148 136 / 0.12);
  color: var(--primary-dark);
}
.full {
  width: 100%;
  margin-top: 0.35rem;
}
.outline {
  margin-top: 0.65rem;
  background: transparent !important;
  color: var(--expense) !important;
  border: 1px solid rgb(225 29 72 / 0.35) !important;
  box-shadow: none !important;
}
.err {
  color: var(--expense);
  font-size: 0.85rem;
}
.req {
  font-weight: 600;
  color: var(--expense);
  font-size: 0.82rem;
}
.btn-danger {
  background: transparent;
}
.btn-danger.solid {
  background: rgb(225 29 72 / 0.92);
  color: #fff;
  box-shadow: 0 6px 20px rgb(225 29 72 / 0.2);
}
.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 120;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1.25rem;
  background: rgb(15 23 42 / 0.32);
  backdrop-filter: blur(3px);
}
.dialog-card {
  width: min(100%, 22rem);
  padding: 1.15rem 1.1rem 1rem;
}
.dialog-card h2 {
  margin: 0;
  font-size: 1.05rem;
  font-weight: 800;
}
.dialog-card p {
  margin: 0.55rem 0 1rem;
  color: var(--muted);
  font-size: 0.92rem;
  line-height: 1.55;
}
.dialog-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.65rem;
}
</style>
