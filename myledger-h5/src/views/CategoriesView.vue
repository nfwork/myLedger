<template>
  <div class="cat-page">
    <div class="tabs card">
      <button type="button" :class="{ on: tab === 'expense' }" @click="tab = 'expense'">支出</button>
      <button type="button" :class="{ on: tab === 'income' }" @click="tab = 'income'">收入</button>
    </div>

    <form class="add card" @submit.prevent="add">
      <input v-model.trim="newName" type="text" placeholder="新分类名称" maxlength="32" required />
      <button type="submit" class="btn btn-primary" :disabled="adding">添加</button>
    </form>

    <p v-if="loading" class="muted">加载中…</p>
    <ul v-else class="list">
      <li v-for="c in rows" :key="c.id" class="item">
        <div>
          <strong>{{ c.name }}</strong>
          <span class="hint">排序 {{ c.sort_order }}</span>
        </div>
        <button type="button" class="del" @click="del(c)">删除</button>
      </li>
      <li v-if="!rows.length" class="empty">暂无分类</li>
    </ul>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { fetchCategoryList, createCategory, deleteCategory } from '@/api'
import { useToast } from '@/composables/useToast'

const tab = ref('expense')
const loading = ref(true)
const adding = ref(false)
const rows = ref([])
const newName = ref('')
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
      sort_order: 100,
    })
    newName.value = ''
    toast.show('已添加', 'success')
    await load()
  } catch (e) {
    toast.show(e?.message || '添加失败', 'error')
  } finally {
    adding.value = false
  }
}

async function del(c) {
  if (!confirm(`删除分类「${c.name}」？`)) return
  try {
    await deleteCategory({ id: c.id })
    toast.show('已删除', 'success')
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
  gap: 0.85rem;
}
.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 0.35rem;
  gap: 0.35rem;
}
.tabs button {
  border: none;
  border-radius: 12px;
  padding: 0.55rem;
  font-weight: 800;
  background: transparent;
  color: var(--muted);
  cursor: pointer;
}
.tabs button.on {
  background: rgb(13 148 136 / 0.12);
  color: var(--primary-dark);
}
.add {
  display: flex;
  gap: 0.5rem;
  padding: 0.65rem 0.65rem;
  align-items: stretch;
}
.add input {
  flex: 1;
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
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
}
.hint {
  display: block;
  margin-top: 0.15rem;
  font-size: 0.72rem;
  color: var(--muted);
}
.del {
  border: none;
  background: rgb(225 29 72 / 0.1);
  color: var(--expense);
  font-weight: 700;
  font-size: 0.8rem;
  padding: 0.4rem 0.65rem;
  border-radius: 10px;
  cursor: pointer;
}
.empty {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--muted);
}
</style>
