<template>
  <div class="account-page">
    <p class="tip card">
      资金账户表示钱所在的位置（现金、银行卡、支付宝等）；记一笔必选账户。概览、统计、流水列表可通过顶部筛选按账户查看。
    </p>

    <form class="add card" @submit.prevent="add">
      <div class="add-fields">
        <input v-model.trim="newName" type="text" placeholder="新账户名称" maxlength="64" required />
        <input v-model.number="newSort" type="number" placeholder="排序（数字越小越靠前）" />
      </div>
      <button type="submit" class="btn btn-primary" :disabled="adding">添加</button>
    </form>

    <p v-if="loading" class="muted">加载中…</p>
    <ul v-else class="list">
      <li v-for="a in rows" :key="a.id" class="item" :class="{ 'is-editing': editId === a.id }">
        <template v-if="editId === a.id">
          <div class="edit-row">
            <input v-model.trim="editName" type="text" maxlength="64" />
            <input v-model.number="editSort" type="number" placeholder="排序" />
            <div class="edit-actions">
              <button type="button" class="btn btn-primary sm" :disabled="saving" @click="saveEdit(a.id)">保存</button>
              <button type="button" class="btn btn-ghost sm" @click="cancelEdit">取消</button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="meta">
            <strong>{{ a.name }}</strong>
            <span class="hint">
              排序 {{ a.sort_order }}
              <span v-if="isDefaultAccount(a)" class="def">默认</span>
            </span>
          </div>
          <div class="actions">
            <button v-if="!isDefaultAccount(a)" type="button" class="linkish" @click="makeDefault(a.id)">设为默认</button>
            <button type="button" class="rename" @click="startEdit(a)">改名</button>
            <button
              v-if="!isDefaultAccount(a)"
              type="button"
              class="del"
              @click="remove(a)"
            >
              删除
            </button>
          </div>
        </template>
      </li>
      <li v-if="!rows.length" class="empty">暂无资金账户</li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import {
  fetchAccountList,
  createAccount,
  updateAccount,
  setDefaultAccount,
  deleteAccount,
} from '@/api'
import { refreshFilterAccounts } from '@/composables/accountFilterScope'
import { useToast } from '@/composables/useToast'

const toast = useToast()
const loading = ref(true)
const adding = ref(false)
const saving = ref(false)
const rows = ref([])
const newName = ref('')
const newSort = ref(100)
const editId = ref(null)
const editName = ref('')
const editSort = ref(100)

function isDefaultAccount(a) {
  return a.is_default === 1 || a.is_default === true
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchAccountList()
  } catch {
    rows.value = []
  } finally {
    loading.value = false
  }
}

async function add() {
  adding.value = true
  try {
    await createAccount({
      name: newName.value,
      sort_order: Number.isFinite(newSort.value) ? newSort.value : 100,
    })
    newName.value = ''
    newSort.value = 100
    toast.show('已添加账户', 'success')
    await load()
    await refreshFilterAccounts()
  } catch (e) {
    toast.show(e?.message || '添加失败', 'error')
  } finally {
    adding.value = false
  }
}

function startEdit(a) {
  editId.value = a.id
  editName.value = a.name || ''
  editSort.value = a.sort_order ?? 100
}

function cancelEdit() {
  editId.value = null
  editName.value = ''
}

async function saveEdit(id) {
  if (!editName.value) {
    toast.show('名称不能为空', 'error')
    return
  }
  saving.value = true
  try {
    await updateAccount({
      id,
      name: editName.value,
      sort_order: Number.isFinite(editSort.value) ? editSort.value : undefined,
    })
    toast.show('已保存', 'success')
    cancelEdit()
    await load()
    await refreshFilterAccounts()
  } catch (e) {
    toast.show(e?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

async function makeDefault(id) {
  try {
    await setDefaultAccount({ id })
    toast.show('已设为默认账户', 'success')
    await load()
    await refreshFilterAccounts()
  } catch (e) {
    toast.show(e?.message || '操作失败', 'error')
  }
}

async function remove(a) {
  if (!confirm(`删除资金账户「${a.name}」？（无流水且非默认才可删）`)) return
  try {
    await deleteAccount({ id: a.id })
    toast.show('已删除', 'success')
    await load()
    await refreshFilterAccounts()
  } catch (e) {
    toast.show(e?.message || '删除失败', 'error')
  }
}

onMounted(load)
</script>

<style scoped>
.account-page {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
  max-width: 520px;
  margin: 0 auto;
}
.tip {
  margin: 0;
  padding: 0.75rem 1rem;
  font-size: 0.82rem;
  line-height: 1.45;
  color: var(--muted);
}
.tip strong {
  color: var(--text);
  font-weight: 700;
}
.add {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  padding: 0.65rem 0.65rem;
}
.add-fields {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}
.add input {
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid var(--line);
  padding: 0.55rem 0.65rem;
}
.muted {
  text-align: center;
  color: var(--muted);
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.item {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.55rem;
  padding: 0.85rem 1rem;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
}
.meta strong {
  display: block;
  font-size: 0.95rem;
}
.hint {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  margin-top: 0.15rem;
  font-size: 0.72rem;
  color: var(--muted);
}
.def {
  font-weight: 700;
  font-size: 0.68rem;
  padding: 0.12rem 0.4rem;
  border-radius: 6px;
  background: rgb(13 148 136 / 0.12);
  color: var(--primary-dark);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem;
  justify-content: flex-end;
}
.rename,
.linkish,
.del {
  border: none;
  font-weight: 700;
  font-size: 0.78rem;
  padding: 0.4rem 0.55rem;
  border-radius: 10px;
  cursor: pointer;
}
.rename {
  background: rgb(13 148 136 / 0.1);
  color: var(--primary-dark);
}
.linkish {
  background: rgb(59 130 246 / 0.1);
  color: rgb(29 78 216);
}
.del {
  background: rgb(225 29 72 / 0.08);
  color: var(--expense);
}
.item.is-editing .edit-row {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.edit-row input {
  width: 100%;
  box-sizing: border-box;
  border-radius: 12px;
  border: 1px solid var(--line);
  padding: 0.55rem 0.65rem;
}
.edit-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}
.sm {
  padding: 0.4rem 0.75rem;
  font-size: 0.82rem;
}
.empty {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--muted);
}
</style>
