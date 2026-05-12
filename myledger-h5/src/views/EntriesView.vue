<template>
  <div class="entries">
    <section class="month-bar card">
      <button type="button" class="nav-btn" :disabled="searchAllMonths" @click="shift(-1)">‹</button>
      <div class="range-switch" aria-label="流水查询范围">
        <button
          type="button"
          class="range-btn"
          :class="{ on: !searchAllMonths }"
          @click="searchAllMonths = false"
        >
          {{ yearMonth }}
        </button>
        <button
          type="button"
          class="range-btn all"
          :class="{ on: searchAllMonths }"
          @click="searchAllMonths = true"
        >
          全部
        </button>
      </div>
      <button type="button" class="nav-btn" :disabled="searchAllMonths" @click="shift(1)">›</button>
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
                  :placeholder="searchAllMonths ? '搜索全部流水备注…' : '搜索当月备注…'"
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
        </div>
      </div>
    </div>

    <p v-if="loading" class="muted">加载中…</p>
    <template v-else>
      <ul class="day-list">
        <li v-for="group in groupedRows" :key="group.dateKey" class="day-card card">
          <div class="day-head">
            <strong>{{ group.dateLabel }}</strong>
            <span class="day-totals">
              <span v-if="group.incomeText" class="daily-total in">{{ group.incomeText }}</span>
              <span v-if="group.expenseText" class="daily-total ex">{{ group.expenseText }}</span>
            </span>
          </div>
          <RouterLink
            v-for="row in group.rows"
            :key="row.id"
            class="entry-row"
            :to="`/entry/${row.id}/edit`"
          >
            <span class="entry-initial" :class="row.entry_type === 'income' ? 'in' : 'ex'">
              {{ entryInitial(row) }}
            </span>
            <span class="row-main">
              <span class="cat">{{ row.category_name || '—' }}</span>
              <span v-if="rowSub(row)" class="sub">{{ rowSub(row) }}</span>
            </span>
            <span class="amt" :class="row.entry_type === 'income' ? 'in' : 'ex'">
              {{ row.entry_type === 'income' ? '+' : '−' }}{{ formatMoney(row.amount) }}
            </span>
          </RouterLink>
        </li>
        <li v-if="!rows.length" class="empty card">暂无流水</li>
      </ul>

      <div v-if="total > 0" class="load-footer" aria-label="加载更多">
        <button
          v-if="hasMore"
          type="button"
          class="load-more-btn"
          :disabled="loadingMore"
          @click="loadMore"
        >
          <span v-if="loadingMore" class="load-spinner" aria-hidden="true"></span>
          {{ loadingMore ? '加载中…' : `查看更多 (已加载 ${rows.length}/${total})` }}
        </button>
        <p v-else class="load-end">已显示全部 {{ total }} 条流水</p>
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
const searchAllMonths = ref(false)
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

const groupedRows = computed(() => {
  const groups = []
  const byDate = new Map()
  for (const row of rows.value) {
    const dateKey = String(row.entry_date || '').slice(0, 10)
    if (!byDate.has(dateKey)) {
      const group = {
        dateKey,
        dateLabel: formatDateDisplay(dateKey) || '未设置日期',
        income: 0,
        expense: 0,
        rows: [],
      }
      byDate.set(dateKey, group)
      groups.push(group)
    }
    const group = byDate.get(dateKey)
    const amount = Number(row.amount) || 0
    if (row.entry_type === 'income') group.income += amount
    else group.expense += amount
    group.rows.push(row)
  }
  return groups.map((group) => ({
    ...group,
    incomeText: group.income > 0 ? `收 ${formatMoney(group.income)}` : '',
    expenseText: group.expense > 0 ? `支 ${formatMoney(group.expense)}` : '',
  }))
})

function shift(d) {
  if (searchAllMonths.value) return
  yearMonth.value = shiftYearMonth(yearMonth.value, d)
}

function entryInitial(row) {
  return String(row.category_name || '—').slice(0, 1)
}

function rowSub(row) {
  return [row.account_name, row.remark].filter((v) => String(v || '').trim()).join(' · ')
}

function listQueryBody(start) {
  const body = {
    start,
    limit: PAGE_SIZE,
  }
  if (!searchAllMonths.value) body.year_month = yearMonth.value
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

watch([yearMonth, entryType, scopeAccountId, searchAllMonths], load)
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
  gap: 0.75rem;
}
.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.25rem 0.5rem;
  gap: 0.5rem;
}
.nav-btn {
  flex: 0 0 auto;
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 12px;
  background: rgb(13 148 136 / 0.1);
  color: var(--primary-dark);
  font-size: 1.35rem;
  cursor: pointer;
}
.nav-btn:disabled {
  cursor: not-allowed;
  color: rgb(100 116 139 / 0.45);
  background: rgb(13 148 136 / 0.05);
}
.range-switch {
  flex: 1;
  min-width: 0;
  display: flex;
  gap: 0.45rem;
  padding: 0 0.1rem;
}
.range-btn {
  flex: 1;
  min-width: 0;
  height: 2.15rem;
  border: none;
  border-radius: 12px;
  background: transparent;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 750;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.range-btn.all {
  flex: 0.72;
}
.range-btn.on {
  background: rgb(13 148 136 / 0.12);
  color: var(--primary-dark);
  font-weight: 850;
}
.filters {
  padding: 0.5rem 0.875rem;
}
.filter-toolbar {
  --filter-label-w: 2rem;
  --filter-row-gap: 0.5rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.filter-row {
  display: flex;
  align-items: center;
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
  font-size: 0.75rem;
  font-weight: 800;
  color: var(--muted);
}
.filter-select {
  display: block;
  box-sizing: border-box;
  width: 100%;
  max-width: 100%;
  min-width: 0;
  height: 2.15rem;
  margin: 0;
  padding: 0 2rem 0 0.7rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  color: var(--text);
  font-weight: 600;
  font-size: 0.88rem;
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
  min-height: 2.15rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  background: linear-gradient(180deg, #fff 0%, var(--bg) 100%);
  padding: 0 0.35rem 0 0.6rem;
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
  padding: 0.45rem 0.25rem;
  font-size: 0.88rem;
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
  width: 1.8rem;
  height: 1.8rem;
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
.muted {
  color: var(--muted);
  text-align: center;
}
.day-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}
.day-card {
  overflow: hidden;
}
.day-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  padding: 0.55rem 0.9rem;
  background: rgb(15 23 42 / 0.02);
}
.day-head strong {
  font-size: 0.9rem;
  font-weight: 800;
}
.day-totals {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  min-width: 0;
}
.daily-total {
  flex-shrink: 0;
  font-size: 0.72rem;
  font-weight: 800;
}
.daily-total.in {
  color: var(--income);
}
.daily-total.ex {
  color: var(--expense);
}
.entry-row {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.78rem 1rem;
  text-decoration: none;
  color: inherit;
}
.entry-row + .entry-row {
  border-top: 1px solid rgb(13 148 136 / 0.1);
}
.entry-initial {
  flex: 0 0 auto;
  width: 2.5rem;
  height: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 800;
}
.entry-initial.in {
  background: rgb(5 150 105 / 0.1);
  color: var(--income);
}
.entry-initial.ex {
  background: rgb(225 29 72 / 0.1);
  color: var(--expense);
}
.row-main {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.cat {
  font-weight: 700;
  font-size: 0.95rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  margin-top: 0.2rem;
  font-size: 0.78rem;
  color: var(--muted);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.amt {
  flex: 0 0 auto;
  font-weight: 800;
  font-size: 1rem;
  text-align: right;
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
  justify-content: center;
  align-items: center;
  padding: 0.15rem 0 0.35rem;
}
.load-more-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.35rem;
  width: 100%;
  min-height: 2.75rem;
  padding: 0.7rem 1rem;
  border-radius: 12px;
  border: none;
  background: transparent;
  color: var(--primary);
  font-size: 0.82rem;
  font-weight: 800;
  cursor: pointer;
  transition: background 0.15s ease, opacity 0.15s ease;
}
.load-more-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}
.load-more-btn:not(:disabled):active {
  background: rgb(13 148 136 / 0.06);
}
.load-spinner {
  width: 0.9rem;
  height: 0.9rem;
  border: 2px solid rgb(13 148 136 / 0.22);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}
.load-end {
  margin: 0;
  width: 100%;
  padding: 1rem 0;
  text-align: center;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--muted);
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
