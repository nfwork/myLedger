<template>
  <div class="account-page">
    <section class="tip-box">
      <span class="tip-ico" aria-hidden="true">i</span>
      <p>资金账户表示钱所在的位置（现金、银行卡、支付宝等）；记一笔必选账户。概览、统计、流水列表可通过顶部筛选按账户查看。</p>
    </section>

    <form class="add card" @submit.prevent="add">
      <div class="add-fields">
        <input v-model.trim="newName" type="text" placeholder="新账户名称" maxlength="64" required />
        <input v-model.number="newSort" type="number" placeholder="排序 (100)" />
      </div>
      <button type="submit" class="add-btn" :disabled="adding || !newName">
        <span aria-hidden="true">+</span>
        <strong>添加</strong>
      </button>
    </form>

    <p v-if="loading" class="muted">加载中…</p>
    <ul v-else class="list card">
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
          <div class="item-main">
            <div class="meta">
              <div class="name-line">
                <strong>{{ a.name }}</strong>
                <span v-if="isDefaultAccount(a)" class="def">默认</span>
              </div>
              <span class="hint">排序 {{ a.sort_order }}</span>
            </div>
            <div class="actions">
              <button v-if="!isDefaultAccount(a)" type="button" class="pill-action" @click="makeDefault(a.id)">设默认</button>
              <button type="button" class="icon-btn" aria-label="改名" @click="startEdit(a)">
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path fill="currentColor" d="M4 17.5V20h2.5L17.1 9.4l-2.5-2.5L4 17.5zM19 7.5a1 1 0 000-1.4L17.9 5a1 1 0 00-1.4 0l-.8.8 2.5 2.5.8-.8z"/>
                </svg>
              </button>
              <button
                v-if="!isDefaultAccount(a)"
                type="button"
                class="icon-btn danger"
                aria-label="删除"
                @click="askRemove(a)"
              >
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path fill="currentColor" d="M7 21a2 2 0 01-2-2V8h14v11a2 2 0 01-2 2H7zM9 4h6l1 2h4v2H4V6h4l1-2z"/>
                </svg>
              </button>
            </div>
          </div>
        </template>
      </li>
      <li v-if="!rows.length" class="empty">暂无资金账户</li>
    </ul>

    <div v-if="deleteTarget" class="dialog-mask" role="dialog" aria-modal="true" aria-labelledby="delete-account-title">
      <div class="dialog-card card">
        <h2 id="delete-account-title">确认删除</h2>
        <p>确定要删除账户「{{ deleteTarget.name }}」吗？</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-ghost" @click="deleteTarget = null">取消</button>
          <button type="button" class="btn btn-danger solid" @click="remove">删除</button>
        </div>
      </div>
    </div>
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
const deleteTarget = ref(null)

function isDefaultAccount(a) {
  return a.is_default === 1 || a.is_default === true
}

async function load() {
  loading.value = true
  try {
    rows.value = await fetchAccountList()
  } catch (e) {
    toast.show(e?.message || '加载失败', 'error')
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

function askRemove(a) {
  deleteTarget.value = a
}

async function remove() {
  const a = deleteTarget.value
  if (!a) return
  try {
    await deleteAccount({ id: a.id })
    toast.show('已删除', 'success')
    deleteTarget.value = null
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
  gap: 0.9rem;
  max-width: 520px;
  margin: 0 auto;
}
.tip-box {
  display: flex;
  align-items: flex-start;
  gap: 0.65rem;
  margin: 0;
  padding: 0.75rem 0.85rem;
  border-radius: 14px;
  border: 1px solid rgb(13 148 136 / 0.15);
  background: rgb(13 148 136 / 0.06);
}
.tip-box p {
  margin: 0;
  font-size: 0.82rem;
  line-height: 1.55;
  color: rgb(15 118 110 / 0.82);
}
.tip-ico {
  flex: 0 0 auto;
  width: 1.15rem;
  height: 1.15rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-top: 0.08rem;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  font-size: 0.78rem;
  font-weight: 900;
}
.add {
  display: flex;
  align-items: stretch;
  gap: 0.5rem;
  padding: 0.65rem;
}
.add-fields {
  flex: 1;
  min-width: 0;
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
.add-btn {
  width: 4.4rem;
  border: none;
  border-radius: 12px;
  background: var(--primary);
  color: #fff;
  font-weight: 800;
  cursor: pointer;
  box-shadow: 0 6px 20px rgb(13 148 136 / 0.25);
}
.add-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
  box-shadow: none;
}
.add-btn span {
  display: block;
  font-size: 1.25rem;
  line-height: 1;
}
.add-btn strong {
  display: block;
  margin-top: 0.2rem;
  font-size: 0.82rem;
}
.muted {
  text-align: center;
  color: var(--muted);
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  overflow: hidden;
}
.item {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  padding: 0.85rem 1rem;
}
.item + .item {
  border-top: 1px solid rgb(13 148 136 / 0.1);
}
.item-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
}
.meta {
  min-width: 0;
  flex: 1;
}
.name-line {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  min-width: 0;
}
.meta strong {
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hint {
  display: block;
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
  align-items: center;
  gap: 0.25rem;
}
.pill-action {
  border: none;
  border-radius: 8px;
  padding: 0.34rem 0.5rem;
  background: rgb(59 130 246 / 0.1);
  color: rgb(29 78 216);
  font-size: 0.72rem;
  font-weight: 800;
  cursor: pointer;
}
.icon-btn {
  width: 2rem;
  height: 2rem;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 10px;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
.icon-btn.danger {
  color: var(--expense);
  opacity: 0.72;
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
