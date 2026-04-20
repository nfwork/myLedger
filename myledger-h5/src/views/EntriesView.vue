<template>
  <div class="entries">
    <section class="month-bar card">
      <button type="button" class="nav-btn" @click="shift(-1)">‹</button>
      <div class="ym">{{ yearMonth }}</div>
      <button type="button" class="nav-btn" @click="shift(1)">›</button>
    </section>

    <div class="filters card">
      <div class="filter-toolbar">
        <div class="filter-row">
          <span id="lbl-entry-type" class="filter-lbl">类型</span>
          <div class="filter-select-wrap">
            <select v-model="entryType" class="filter-select" aria-labelledby="lbl-entry-type">
              <option value="">全部</option>
              <option value="income">收入</option>
              <option value="expense">支出</option>
            </select>
          </div>
        </div>

        <div class="filter-row">
          <span id="lbl-acc-filter" class="filter-lbl">账户</span>
          <div class="filter-select-wrap">
            <select id="acc-filter" v-model="accountPick" class="filter-select" aria-labelledby="lbl-acc-filter">
              <option value="">全部</option>
              <option v-for="a in accountsForFilter" :key="a.id" :value="String(a.id)">{{ a.name }}</option>
            </select>
          </div>
        </div>

        <div class="remark-filter">
          <div class="filter-row">
            <span id="lbl-remark" class="filter-lbl">备注</span>
            <div class="search-field">
              <div class="search-shell">
                <span class="search-ico" aria-hidden="true">
                  <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none">
                    <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="2" />
                    <path d="M20 20 16.65 16.65" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
                  </svg>
                </span>
                <input
                  v-model.trim="remarkKeyword"
                  class="search-input"
                  type="search"
                  enterkeyhint="search"
                  maxlength="128"
                  placeholder="搜索备注…"
                  autocomplete="off"
                  aria-labelledby="lbl-remark"
                />
                <button
                  v-if="remarkKeyword"
                  type="button"
                  class="search-clear"
                  aria-label="清空备注筛选"
                  @click.stop="remarkKeyword = ''"
                >
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none">
                    <path
                      d="M18 6 6 18M6 6l12 12"
                      stroke="currentColor"
                      stroke-width="2.2"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
              </div>
            </div>
          </div>
          <p class="search-caption">仅在当前月份流水中，对备注做模糊匹配</p>
        </div>
      </div>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <template v-else>
      <ul class="list">
        <li v-for="row in rows" :key="row.id">
          <RouterLink class="row" :to="`/entry/${row.id}/edit`">
            <div>
              <div class="top">
                <span class="pill" :class="row.entry_type === 'income' ? 'pill-income' : 'pill-expense'">
                  {{ row.entry_type === 'income' ? '收入' : '支出' }}
                </span>
                <span class="cat">{{ row.category_name }}</span>
              </div>
              <div class="sub">
                {{ formatDateDisplay(row.entry_date) }}
                <span v-if="row.account_name" class="acc"> · {{ row.account_name }}</span>
                <span v-if="row.remark"> · {{ row.remark }}</span>
              </div>
            </div>
            <div class="amt" :class="row.entry_type === 'income' ? 'in' : 'ex'">
              {{ row.entry_type === 'income' ? '+' : '−' }}{{ formatMoney(row.amount) }}
            </div>
          </RouterLink>
        </li>
        <li v-if="!rows.length" class="empty">暂无流水</li>
      </ul>

      <div v-if="total > 0" class="load-footer card" aria-label="加载更多">
        <p class="load-count">已显示 {{ rows.length }} / {{ total }} 条</p>
        <button
          v-if="hasMore"
          type="button"
          class="load-more-btn"
          :disabled="loadingMore"
          @click="loadMore"
        >
          {{ loadingMore ? '加载中…' : '加载更多' }}
        </button>
        <p v-else class="load-end">已加载全部</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { fetchEntryListPage } from '@/api'
import {
  scopeAccountId,
  accountsForFilter,
  ensureFilterAccounts,
  setScopeAccountId,
  accountFilterParams,
} from '@/composables/accountFilterScope'
import { currentYearMonth, shiftYearMonth, formatMoney, formatDateDisplay } from '@/utils/format'
import { useToast } from '@/composables/useToast'

const PAGE_SIZE = 20
const toast = useToast()

const yearMonth = ref(currentYearMonth())
const entryType = ref('')
const remarkKeyword = ref('')
const total = ref(0)
const loading = ref(true)
const loadingMore = ref(false)
const rows = ref([])
let remarkSearchTimer = 0

const hasMore = computed(() => total.value > 0 && rows.value.length < total.value)

const accountPick = computed({
  get: () => (scopeAccountId.value == null ? '' : String(scopeAccountId.value)),
  set: (v) => setScopeAccountId(v === '' || v == null ? null : v),
})

function shift(d) {
  yearMonth.value = shiftYearMonth(yearMonth.value, d)
}

function listQueryBody(start) {
  const body = {
    year_month: yearMonth.value,
    start,
    limit: PAGE_SIZE,
  }
  if (entryType.value) body.entry_type = entryType.value
  const kw = remarkKeyword.value.trim()
  if (kw) body.remark_keyword = kw
  Object.assign(body, accountFilterParams())
  return body
}

async function load() {
  loading.value = true
  try {
    await ensureFilterAccounts()
    const { rows: list, total: n } = await fetchEntryListPage(listQueryBody(0))
    rows.value = list
    total.value = n
  } catch (e) {
    toast.show(e?.message || '加载失败', 'error')
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!hasMore.value || loadingMore.value || loading.value) return
  loadingMore.value = true
  try {
    const { rows: list, total: n } = await fetchEntryListPage(listQueryBody(rows.value.length))
    rows.value = rows.value.concat(list)
    total.value = n
  } catch (e) {
    toast.show(e?.message || '加载更多失败', 'error')
  } finally {
    loadingMore.value = false
  }
}

watch([yearMonth, entryType, scopeAccountId], load)
watch(remarkKeyword, () => {
  if (remarkSearchTimer) clearTimeout(remarkSearchTimer)
  remarkSearchTimer = setTimeout(() => {
    remarkSearchTimer = 0
    load()
  }, 350)
})
onMounted(load)
</script>

<style scoped>
.entries {
  display: flex;
  flex-direction: column;
  gap: 0.85rem;
}
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.65rem 0.5rem;
}
.nav-btn {
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 12px;
  background: rgb(13 148 136 / 0.1);
  color: var(--primary-dark);
  font-size: 1.35rem;
  cursor: pointer;
}
.ym {
  font-weight: 800;
  letter-spacing: 0.04em;
}
.filters {
  padding: 0.85rem 1rem 0.95rem;
}
.filter-toolbar {
  --filter-label-w: 2.35rem;
  --filter-row-gap: 0.65rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.filter-row {
  display: flex;
  align-items: stretch;
  gap: var(--filter-row-gap);
}
.filter-select-wrap {
  flex: 1;
  min-width: 0;
  max-width: 100%;
}
.remark-filter {
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
}
.search-field {
  flex: 1;
  min-width: 0;
}
.search-field .search-shell {
  width: 100%;
}
.filter-lbl {
  flex: 0 0 var(--filter-label-w);
  align-self: center;
  font-size: 0.78rem;
  font-weight: 800;
  letter-spacing: 0.03em;
  color: var(--muted);
}
.filter-select {
  display: block;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  margin: 0;
  padding: 0.55rem 2.15rem 0.55rem 0.7rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  color: var(--text);
  font-weight: 600;
  font-size: 1rem;
  line-height: 1.35;
  outline: none;
  -webkit-appearance: none;
  appearance: none;
  background-color: rgb(255 255 255);
  background-image: linear-gradient(180deg, #fff 0%, rgb(248 250 250) 100%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat, no-repeat;
  background-position: 0 0, right 0.6rem center;
  background-size: auto, 0.85rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.filter-select:focus {
  border-color: rgb(13 148 136 / 0.42);
  box-shadow: 0 0 0 3px rgb(20 184 166 / 0.14);
}
.search-shell {
  position: relative;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  border-radius: 14px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, #fff 0%, var(--bg) 100%);
  padding: 0.2rem 0.45rem 0.2rem 0.65rem;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
.search-shell:focus-within {
  border-color: rgb(13 148 136 / 0.42);
  box-shadow: 0 0 0 3px rgb(20 184 166 / 0.14);
}
.search-ico {
  display: flex;
  flex-shrink: 0;
  color: rgb(100 116 139 / 0.88);
}
.search-input {
  flex: 1;
  min-width: 0;
  border: none;
  background: transparent;
  padding: 0.52rem 0.25rem;
  font-size: 0.92rem;
  font-weight: 500;
  color: var(--text);
  outline: none;
}
.search-input::placeholder {
  color: rgb(100 116 139 / 0.62);
  font-weight: 400;
}
.search-input::-webkit-search-cancel-button {
  -webkit-appearance: none;
  appearance: none;
  display: none;
}
.search-clear {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  margin: 0 -0.1rem 0 0;
  border: none;
  border-radius: 11px;
  background: rgb(15 23 42 / 0.05);
  color: rgb(71 85 105 / 0.9);
  cursor: pointer;
  transition: background 0.12s ease, color 0.12s ease;
}
.search-clear:active {
  background: rgb(15 23 42 / 0.1);
  color: var(--text);
}
.search-caption {
  margin: 0;
  padding: 0 0 0 calc(var(--filter-label-w) + var(--filter-row-gap));
  font-size: 0.72rem;
  line-height: 1.4;
  color: var(--muted);
  font-weight: 500;
}
.muted {
  color: var(--muted);
  text-align: center;
}
.list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
}
.row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1rem;
  border-radius: var(--radius);
  background: var(--surface);
  border: 1px solid var(--line);
  text-decoration: none;
  color: inherit;
  box-shadow: var(--shadow);
}
.top {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}
.cat {
  font-weight: 700;
  font-size: 0.95rem;
}
.sub {
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--muted);
}
.acc {
  font-weight: 600;
  color: rgb(100 116 139 / 0.92);
}
.amt {
  font-weight: 800;
  font-size: 1rem;
}
.amt.in {
  color: var(--income);
}
.amt.ex {
  color: var(--expense);
}
.empty {
  text-align: center;
  padding: 2rem 1rem;
  color: var(--muted);
  font-size: 0.9rem;
}
.load-footer {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 0.55rem;
  padding: 0.75rem 1rem 0.85rem;
  margin-top: 0.15rem;
}
.load-count {
  margin: 0;
  text-align: center;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--muted);
}
.load-more-btn {
  width: 100%;
  padding: 0.62rem 1rem;
  border-radius: 12px;
  border: 1px solid rgb(13 148 136 / 0.35);
  background: rgb(13 148 136 / 0.08);
  color: var(--primary-dark);
  font-size: 0.92rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.15s ease, opacity 0.15s ease;
}
.load-more-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.load-more-btn:not(:disabled):active {
  background: rgb(13 148 136 / 0.16);
}
.load-end {
  margin: 0;
  text-align: center;
  font-size: 0.78rem;
  font-weight: 600;
  color: var(--muted);
}
</style>
