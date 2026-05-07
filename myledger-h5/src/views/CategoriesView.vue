<template>
  <div class="cat-page">
    <div class="tabs card">
      <button type="button" :class="{ on: tab === 'expense' }" @click="tab = 'expense'">支出</button>
      <button type="button" :class="{ on: tab === 'income' }" @click="tab = 'income'">收入</button>
    </div>

    <section class="tip-box">
      <span class="tip-ico" aria-hidden="true">i</span>
      <p>收支分类用于标记每一笔账目的用途（如餐饮、交通、工资等）；合理的分类有助于在首页、统计和流水中更清晰地分析财务状况。</p>
    </section>

    <form class="add card" @submit.prevent="add">
      <div class="add-fields">
        <input v-model.trim="newName" type="text" placeholder="新分类名称" maxlength="32" required />
        <input v-model.number="newSort" type="number" placeholder="排序 (100)" />
      </div>
      <button type="submit" class="add-btn" :disabled="adding || !newName">
        <span aria-hidden="true">+</span>
        <strong>添加</strong>
      </button>
    </form>

    <p v-if="loading" class="muted">加载中…</p>
    <ul v-else class="list card">
      <li v-for="c in rows" :key="c.id" class="item" :class="{ 'is-editing': editId === c.id }">
        <template v-if="editId === c.id">
          <div class="edit-row">
            <input v-model.trim="editName" type="text" maxlength="32" />
            <input v-model.number="editSort" type="number" placeholder="排序" />
            <div class="edit-actions">
              <button type="button" class="btn btn-primary sm" :disabled="saving" @click="saveEdit(c.id)">保存</button>
              <button type="button" class="btn btn-ghost sm" @click="cancelEdit">取消</button>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="item-main">
            <div class="meta">
              <strong>{{ c.name }}</strong>
              <span class="hint">排序 {{ c.sort_order }}</span>
            </div>
            <div class="actions">
              <button type="button" class="icon-btn" aria-label="编辑" @click="startEdit(c)">
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path fill="currentColor" d="M4 17.5V20h2.5L17.1 9.4l-2.5-2.5L4 17.5zM19 7.5a1 1 0 000-1.4L17.9 5a1 1 0 00-1.4 0l-.8.8 2.5 2.5.8-.8z"/>
                </svg>
              </button>
              <button type="button" class="icon-btn danger" aria-label="删除" @click="askDelete(c)">
                <svg viewBox="0 0 24 24" width="18" height="18" aria-hidden="true">
                  <path fill="currentColor" d="M7 21a2 2 0 01-2-2V8h14v11a2 2 0 01-2 2H7zM9 4h6l1 2h4v2H4V6h4l1-2z"/>
                </svg>
              </button>
            </div>
          </div>
        </template>
      </li>
      <li v-if="!rows.length" class="empty">暂无分类</li>
    </ul>

    <div v-if="deleteTarget" class="dialog-mask" role="dialog" aria-modal="true" aria-labelledby="delete-category-title">
      <div class="dialog-card card">
        <h2 id="delete-category-title">确认删除</h2>
        <p>确定要删除分类「{{ deleteTarget.name }}」吗？</p>
        <div class="dialog-actions">
          <button type="button" class="btn btn-ghost" @click="deleteTarget = null">取消</button>
          <button type="button" class="btn btn-danger solid" @click="del">删除</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { fetchCategoryList, createCategory, updateCategory, deleteCategory } from '@/api'
import { useToast } from '@/composables/useToast'

const tab = ref('expense')
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
const toast = useToast()

async function load() {
  loading.value = true
  try {
    rows.value = await fetchCategoryList({ type: tab.value })
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
    await createCategory({
      name: newName.value,
      type: tab.value,
      sort_order: Number.isFinite(newSort.value) ? newSort.value : 100,
    })
    newName.value = ''
    newSort.value = 100
    toast.show('已添加', 'success')
    await load()
  } catch (e) {
    toast.show(e?.message || '添加失败', 'error')
  } finally {
    adding.value = false
  }
}

function startEdit(c) {
  editId.value = c.id
  editName.value = c.name || ''
  editSort.value = c.sort_order ?? 100
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
    await updateCategory({
      id,
      name: editName.value,
      sort_order: Number.isFinite(editSort.value) ? editSort.value : undefined,
    })
    toast.show('已保存', 'success')
    cancelEdit()
    await load()
  } catch (e) {
    toast.show(e?.message || '保存失败', 'error')
  } finally {
    saving.value = false
  }
}

function askDelete(c) {
  deleteTarget.value = c
}

async function del() {
  const c = deleteTarget.value
  if (!c) return
  try {
    await deleteCategory({ id: c.id })
    toast.show('已删除', 'success')
    deleteTarget.value = null
    await load()
  } catch (e) {
    toast.show(e?.message || '删除失败', 'error')
  }
}

watch(tab, load)
onMounted(load)
</script>

<style scoped>
.cat-page {
  display: flex;
  flex-direction: column;
  gap: 0.9rem;
  max-width: 520px;
  margin: 0 auto;
}
.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 0.25rem;
  gap: 0.25rem;
  background: rgb(15 23 42 / 0.05);
  box-shadow: none;
}
.tabs button {
  border: none;
  border-radius: 11px;
  padding: 0.62rem;
  font-weight: 800;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
.tabs button.on {
  background: var(--surface);
  color: var(--primary-dark);
  box-shadow: 0 1px 5px rgb(15 23 42 / 0.08);
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
  gap: 0.5rem;
  padding: 0.65rem 0.65rem;
  align-items: stretch;
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
.meta strong {
  display: block;
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
.actions {
  display: flex;
  align-items: center;
  gap: 0.25rem;
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
