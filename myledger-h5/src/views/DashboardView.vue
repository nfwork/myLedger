<template>
  <div class="dash">
    <section class="month-bar card">
      <button type="button" class="nav-btn" @click="shift(-1)" aria-label="上一月">‹</button>
      <div class="ym">{{ yearMonth }}</div>
      <button type="button" class="nav-btn" @click="shift(1)" aria-label="下一月">›</button>
    </section>

    <div class="scope card">
      <label for="dash-acc" class="scope-lbl">资金账户</label>
      <select id="dash-acc" v-model="accountPick" class="scope-select">
        <option value="">全部账户</option>
        <option v-for="a in accountsForFilter" :key="a.id" :value="String(a.id)">{{ a.name }}</option>
      </select>
    </div>

    <section class="totals">
      <div class="tile income">
        <span class="lbl">本月收入</span>
        <strong>{{ formatMoney(totals.income_total) }}</strong>
      </div>
      <div class="tile expense">
        <span class="lbl">本月支出</span>
        <strong>{{ formatMoney(totals.expense_total) }}</strong>
      </div>
    </section>

    <div class="balance card">
      <span>本月结余</span>
      <strong :class="balance >= 0 ? 'pos' : 'neg'">{{ formatMoney(balance) }}</strong>
    </div>

    <p v-if="loading" class="muted">加载中…</p>

    <template v-else>
      <section v-if="expenseCategoryRows.length" class="stat-card card">
        <div class="stat-head">
          <h2>支出分类</h2>
          <span class="stat-hint">占本月支出</span>
        </div>
        <ul class="bar-list">
          <li v-for="c in expenseCategoryRows" :key="'ex-' + (c.category_id ?? c.categoryId)">
            <div class="bar-line">
              <span class="bar-name">{{ c.category_name ?? c.categoryName }}</span>
              <span class="bar-amt ex">{{ formatMoney(catAmount(c)) }}</span>
            </div>
            <div class="bar-track" aria-hidden="true">
              <div class="bar-fill ex" :style="{ width: barPct(catAmount(c), expenseMonthTotal) + '%' }" />
            </div>
          </li>
        </ul>
        <p v-if="expenseTruncated" class="stat-more">仅展示金额前 {{ TOP_CATEGORIES }} 个支出分类</p>
      </section>

      <section v-if="incomeCategoryRows.length" class="stat-card card">
        <div class="stat-head">
          <h2>收入分类</h2>
          <span class="stat-hint">占本月收入</span>
        </div>
        <ul class="bar-list">
          <li v-for="c in incomeCategoryRows" :key="'in-' + (c.category_id ?? c.categoryId)">
            <div class="bar-line">
              <span class="bar-name">{{ c.category_name ?? c.categoryName }}</span>
              <span class="bar-amt in">{{ formatMoney(catAmount(c)) }}</span>
            </div>
            <div class="bar-track" aria-hidden="true">
              <div class="bar-fill in" :style="{ width: barPct(catAmount(c), incomeMonthTotal) + '%' }" />
            </div>
          </li>
        </ul>
        <p v-if="incomeTruncated" class="stat-more">仅展示金额前 {{ TOP_CATEGORIES }} 个收入分类</p>
      </section>

      <section class="section">
        <div class="section-head">
          <h2>最近流水</h2>
          <RouterLink to="/entries" class="link">全部</RouterLink>
        </div>
        <ul class="mini-list">
          <li v-for="row in recent" :key="row.id" class="mini-item">
            <div class="mini-main">
              <div class="t">{{ row.category_name || '—' }}</div>
              <div class="d">
                {{ formatDateDisplay(row.entry_date) }}
                <span v-if="row.account_name" class="acc"> · {{ row.account_name }}</span>
                <span v-if="row.remark" class="rm"> · {{ row.remark }}</span>
              </div>
            </div>
            <div class="amt" :class="row.entry_type === 'income' ? 'in' : 'ex'">
              {{ row.entry_type === 'income' ? '+' : '−' }}{{ formatMoney(row.amount) }}
            </div>
          </li>
          <li v-if="!recent.length" class="empty">本月还没有记账</li>
        </ul>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { fetchMonthTotals, fetchEntryList, fetchCategoryTotals } from '@/api'
import {
  scopeAccountId,
  accountsForFilter,
  ensureFilterAccounts,
  setScopeAccountId,
  accountFilterParams,
} from '@/composables/accountFilterScope'
import {
  currentYearMonth,
  shiftYearMonth,
  formatMoney,
  formatDateDisplay,
} from '@/utils/format'

const TOP_CATEGORIES = 8

const yearMonth = ref(currentYearMonth())
const loading = ref(true)

const accountPick = computed({
  get: () => (scopeAccountId.value == null ? '' : String(scopeAccountId.value)),
  set: (v) => setScopeAccountId(v === '' || v == null ? null : v),
})
const totals = ref({ income_total: 0, expense_total: 0 })
const recent = ref([])
const expenseCats = ref([])
const incomeCats = ref([])

const balance = computed(() => {
  const i = Number(totals.value.income_total) || 0
  const e = Number(totals.value.expense_total) || 0
  return i - e
})

function catAmount(row) {
  return Number(row?.total_amount ?? row?.totalAmount ?? 0) || 0
}

const expenseMonthTotal = computed(() =>
  expenseCats.value.reduce((s, r) => s + catAmount(r), 0)
)
const incomeMonthTotal = computed(() => incomeCats.value.reduce((s, r) => s + catAmount(r), 0))

const expenseCategoryRows = computed(() =>
  expenseCats.value.filter((r) => catAmount(r) > 0).slice(0, TOP_CATEGORIES)
)

const incomeCategoryRows = computed(() =>
  incomeCats.value.filter((r) => catAmount(r) > 0).slice(0, TOP_CATEGORIES)
)

const expenseTruncated = computed(
  () => expenseCats.value.filter((r) => catAmount(r) > 0).length > TOP_CATEGORIES
)
const incomeTruncated = computed(
  () => incomeCats.value.filter((r) => catAmount(r) > 0).length > TOP_CATEGORIES
)

function barPct(part, whole) {
  if (!whole || whole <= 0 || !part) return 0
  return Math.min(100, Math.round((part / whole) * 1000) / 10)
}

function shift(d) {
  yearMonth.value = shiftYearMonth(yearMonth.value, d)
}

async function load() {
  loading.value = true
  const ym = yearMonth.value
  try {
    await ensureFilterAccounts()
    const fp = accountFilterParams()
    const [t, rows, ex, inc] = await Promise.all([
      fetchMonthTotals(ym, fp),
      fetchEntryList({ year_month: ym, start: 0, limit: 8, ...fp }),
      fetchCategoryTotals(ym, 'expense', fp),
      fetchCategoryTotals(ym, 'income', fp),
    ])
    totals.value = t
    recent.value = rows
    expenseCats.value = ex
    incomeCats.value = inc
  } catch {
    totals.value = { income_total: 0, expense_total: 0 }
    recent.value = []
    expenseCats.value = []
    incomeCats.value = []
  } finally {
    loading.value = false
  }
}

watch(yearMonth, load)
watch(scopeAccountId, load)
onMounted(load)
</script>

<style scoped>
.dash {
  display: flex;
  flex-direction: column;
  gap: 1rem;
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
  line-height: 1;
  cursor: pointer;
}
.ym {
  font-weight: 800;
  font-size: 1.05rem;
  letter-spacing: 0.04em;
}
.scope {
  display: flex;
  align-items: center;
  gap: 0.65rem;
  padding: 0.65rem 1rem;
}
.scope-lbl {
  flex: 0 0 auto;
  font-size: 0.78rem;
  font-weight: 800;
  color: var(--muted);
}
.scope-select {
  flex: 1;
  min-width: 0;
  padding: 0.55rem 2rem 0.55rem 0.7rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  font-weight: 600;
  font-size: 0.92rem;
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.6rem center;
  background-size: 0.85rem;
  -webkit-appearance: none;
  appearance: none;
}
.acc {
  font-weight: 600;
  color: rgb(100 116 139 / 0.95);
}
.totals {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0.75rem;
}
.tile {
  border-radius: var(--radius);
  padding: 1rem 1.1rem;
  color: #fff;
  box-shadow: var(--shadow);
}
.tile.income {
  background: linear-gradient(135deg, #34d399, #059669);
}
.tile.expense {
  background: linear-gradient(135deg, #fb7185, #e11d48);
}
.tile .lbl {
  display: block;
  font-size: 0.75rem;
  opacity: 0.9;
  font-weight: 600;
}
.tile strong {
  font-size: 1.2rem;
  font-weight: 800;
}
.balance {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.85rem 1.1rem;
  font-size: 0.9rem;
  color: var(--muted);
  font-weight: 600;
}
.balance strong {
  font-size: 1.15rem;
  color: var(--text);
}
.balance .pos {
  color: var(--income);
}
.balance .neg {
  color: var(--expense);
}
.stat-card {
  padding: 0.85rem 1rem 1rem;
}
.stat-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.65rem;
}
.stat-head h2 {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 800;
}
.stat-hint {
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--muted);
}
.bar-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.65rem;
}
.bar-line {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}
.bar-name {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--text);
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bar-amt {
  flex-shrink: 0;
  font-size: 0.82rem;
  font-weight: 800;
}
.bar-amt.in {
  color: var(--income);
}
.bar-amt.ex {
  color: var(--expense);
}
.bar-track {
  height: 0.45rem;
  border-radius: 999px;
  background: rgb(15 23 42 / 0.06);
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  min-width: 0;
  transition: width 0.25s ease;
}
.bar-fill.in {
  background: linear-gradient(90deg, #34d399, #059669);
}
.bar-fill.ex {
  background: linear-gradient(90deg, #fb7185, #e11d48);
}
.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 0.65rem;
}
.section-head h2 {
  margin: 0;
  font-size: 1rem;
  font-weight: 800;
}
.link {
  font-size: 0.82rem;
  font-weight: 700;
  text-decoration: none;
}
.muted {
  color: var(--muted);
  font-size: 0.88rem;
  text-align: center;
}
.mini-list {
  list-style: none;
  margin: 0;
  padding: 0;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--line);
  background: var(--surface);
}
.mini-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid var(--line);
}
.mini-item:last-child {
  border-bottom: none;
}
.mini-main {
  min-width: 0;
  flex: 1;
}
.t {
  font-weight: 700;
  font-size: 0.92rem;
}
.d {
  font-size: 0.75rem;
  color: var(--muted);
  margin-top: 0.15rem;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.rm {
  font-weight: 500;
}
.amt {
  flex-shrink: 0;
  font-weight: 800;
  font-size: 0.95rem;
}
.amt.in {
  color: var(--income);
}
.amt.ex {
  color: var(--expense);
}
.empty {
  padding: 1.25rem 1rem;
  text-align: center;
  color: var(--muted);
  font-size: 0.88rem;
}
.stat-more {
  margin: 0.4rem 0 0;
  font-size: 0.72rem;
  font-weight: 600;
  color: var(--muted);
  text-align: center;
}
</style>
