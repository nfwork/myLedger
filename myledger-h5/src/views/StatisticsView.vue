<template>
  <div class="stats-page">
    <section class="hero card">
      <p class="hero-title">月 × 分类（自然年）</p>
      <p class="hero-desc">
        行为月份、列为分类；按<strong>自然年</strong>汇总，默认当前年。查看<strong>今年</strong>时只展示到<strong>本月</strong>，往年仍为全年 12 个月。切换类型查看支出或收入分布。
      </p>
    </section>

    <div class="toolbar card">
      <div class="scope-row">
        <label for="stats-acc" class="scope-lbl">资金账户</label>
        <select id="stats-acc" v-model="accountPick" class="scope-select">
          <option value="">全部账户</option>
          <option v-for="a in accountsForFilter" :key="a.id" :value="String(a.id)">{{ a.name }}</option>
        </select>
      </div>
      <div class="type-tabs" role="tablist" aria-label="收支类型">
        <button
          type="button"
          role="tab"
          :aria-selected="entryType === 'expense'"
          :class="{ on: entryType === 'expense' }"
          @click="entryType = 'expense'"
        >
          支出
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="entryType === 'income'"
          :class="{ on: entryType === 'income' }"
          @click="entryType = 'income'"
        >
          收入
        </button>
      </div>
      <div class="year-bar">
        <button type="button" class="nav-btn" @click="shiftYear(-1)" aria-label="上一年">‹</button>
        <div class="ym-block">
          <span class="ym-label">统计区间</span>
          <span class="ym">{{ selectedYear }}年</span>
        </div>
        <button type="button" class="nav-btn" @click="shiftYear(1)" aria-label="下一年">›</button>
      </div>
    </div>

    <div v-if="loading" class="loading card" aria-busy="true">
      <span class="loading-dot" />
      <span>加载中…</span>
    </div>
    <div v-else-if="!matrix.cols.length" class="empty card">
      该自然年暂无{{ entryType === 'expense' ? '支出' : '收入' }}数据
    </div>
    <div v-else class="table-shell card">
      <div class="scroll-x">
        <table class="matrix" :class="entryType">
          <thead>
            <tr>
              <th class="sticky-col corner" scope="col">月份</th>
              <th
                v-for="c in matrix.cols"
                :key="c.id"
                class="col-cat"
                scope="col"
                :title="c.name"
              >
                <span class="cat-head">{{ c.name }}</span>
              </th>
              <th class="total-col head-total" scope="col">合计</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="(row, ri) in matrix.rows"
              :key="row.ym"
              :class="{ zebra: ri % 2 === 1, 'is-current-month-row': isCurrentMonthYm(row.ym) }"
            >
              <th class="sticky-col row-month" scope="row" :title="formatYearMonthLabel(row.ym)">{{ row.monthLabel }}</th>
              <td
                v-for="(v, j) in row.amounts"
                :key="matrix.cols[j]?.id + '-' + row.ym"
                class="num"
                :class="{ 'is-zero': !v }"
              >
                {{ cellMoney(v) }}
              </td>
              <td class="num total-col">{{ cellMoney(row.rowTotal) }}</td>
            </tr>
          </tbody>
          <tfoot v-if="matrix.cols.length">
            <tr class="foot-row">
              <th class="sticky-col foot-lbl" scope="row">分类合计</th>
              <td
                v-for="(v, j) in matrix.colTotals"
                :key="'ft-' + matrix.cols[j]?.id"
                class="num foot-num"
              >
                {{ cellMoney(v) }}
              </td>
              <td class="num total-col foot-num">{{ cellMoney(matrix.grand) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { fetchMonthCategoryMatrix } from '@/api'
import {
  scopeAccountId,
  accountsForFilter,
  ensureFilterAccounts,
  setScopeAccountId,
  accountFilterParams,
} from '@/composables/accountFilterScope'
import { currentYearMonth, formatMoney, formatYearMonthLabel } from '@/utils/format'
import { useToast } from '@/composables/useToast'

const toast = useToast()

const selectedYear = ref(new Date().getFullYear())
const entryType = ref('expense')
const loading = ref(false)
const flatRows = ref([])

const accountPick = computed({
  get: () => (scopeAccountId.value == null ? '' : String(scopeAccountId.value)),
  set: (v) => setScopeAccountId(v === '' || v == null ? null : v),
})

const monthsInSelectedYear = computed(() => visibleMonthsForStats(selectedYear.value))

const matrix = computed(() => buildMatrix(flatRows.value, monthsInSelectedYear.value))

function monthsOfCalendarYear(year) {
  const y = String(year)
  return Array.from({ length: 12 }, (_, i) => `${y}-${String(i + 1).padStart(2, '0')}`)
}

/** 往年：1–12 月；今年：1 月～本月；未来年：整年（便于占位，一般无数据） */
function visibleMonthsForStats(year) {
  const full = monthsOfCalendarYear(year)
  const y = Number(year)
  const now = new Date()
  const cy = now.getFullYear()
  const cm = now.getMonth() + 1
  if (y < cy) return full
  if (y > cy) return full
  return full.slice(0, cm)
}

function shiftYear(delta) {
  selectedYear.value += delta
}

function monthLabelFromYm(ym) {
  const mo = Number(String(ym).split('-')[1])
  return Number.isFinite(mo) ? `${mo}月` : String(ym)
}

/** 行=自然月，列=分类（按当前展示区间内各分类合计降序） */
function buildMatrix(flat, monthsList) {
  const catMap = new Map()
  for (const r of flat) {
    const id = Number(r.category_id ?? r.categoryId)
    if (!Number.isFinite(id)) continue
    const name = r.category_name ?? r.categoryName ?? '—'
    const ym = String(r.bill_month ?? r.billMonth ?? '')
    const amt = Number(r.total_amount ?? r.totalAmount ?? 0) || 0
    if (!catMap.has(id)) catMap.set(id, { id, name, byMonth: {} })
    const cur = catMap.get(id).byMonth[ym] || 0
    catMap.get(id).byMonth[ym] = cur + amt
  }

  const cats = [...catMap.values()]
    .map((c) => {
      const total = monthsList.reduce((s, ym) => s + Number(c.byMonth[ym] || 0), 0)
      return { id: c.id, name: c.name, byMonth: c.byMonth, total }
    })
    .sort((a, b) => b.total - a.total)

  const cols = cats.map((c) => ({ id: c.id, name: c.name }))

  const rows = monthsList.map((ym) => {
    const amounts = cats.map((c) => Number(c.byMonth[ym] || 0))
    const rowTotal = amounts.reduce((s, v) => s + v, 0)
    return { ym, monthLabel: monthLabelFromYm(ym), amounts, rowTotal }
  })

  const colTotals = cats.map((_, j) => rows.reduce((s, row) => s + row.amounts[j], 0))
  const grand = colTotals.reduce((s, v) => s + v, 0)

  return { rows, cols, colTotals, grand }
}

function isCurrentMonthYm(ym) {
  return selectedYear.value === new Date().getFullYear() && ym === currentYearMonth()
}

function cellMoney(n) {
  const v = Number(n) || 0
  if (v === 0) return '—'
  return formatMoney(v)
}

async function load() {
  loading.value = true
  const y = selectedYear.value
  const list = visibleMonthsForStats(y)
  if (!list.length) {
    flatRows.value = []
    loading.value = false
    return
  }
  const from = list[0]
  const to = list[list.length - 1]
  try {
    await ensureFilterAccounts()
    const fp = accountFilterParams()
    flatRows.value = await fetchMonthCategoryMatrix(
      {
        year_month_from: from,
        year_month_to: to,
        entry_type: entryType.value,
      },
      fp,
    )
  } catch (e) {
    toast.show(e?.message || '加载失败', 'error')
    flatRows.value = []
  } finally {
    loading.value = false
  }
}

watch([selectedYear, entryType, scopeAccountId], load)
onMounted(load)
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 560px;
  margin: 0 auto;
}

.hero {
  padding: 1rem 1.1rem 1.05rem;
  border: 1px solid rgb(13 148 136 / 0.12);
  background: linear-gradient(145deg, rgb(255 255 255 / 0.95), rgb(240 253 250 / 0.9));
}
.hero-title {
  margin: 0 0 0.35rem;
  font-size: 1rem;
  font-weight: 800;
  letter-spacing: 0.02em;
  color: var(--primary-dark);
}
.hero-desc {
  margin: 0;
  font-size: 0.8rem;
  line-height: 1.55;
  color: var(--muted);
}
.hero-desc strong {
  color: var(--text);
  font-weight: 700;
}

.toolbar {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 0.75rem 0.85rem 0.85rem;
}
.scope-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
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
  padding: 0.52rem 2rem 0.52rem 0.65rem;
  border-radius: 12px;
  border: 1px solid var(--line);
  font-weight: 600;
  font-size: 0.88rem;
  background-color: #fff;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='14' viewBox='0 0 24 24' fill='none' stroke='%2364748b' stroke-width='2.2' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpath d='M6 9l6 6 6-6'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 0.55rem center;
  background-size: 0.8rem;
  -webkit-appearance: none;
  appearance: none;
}
.type-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 0.25rem;
  gap: 0.25rem;
  border-radius: 14px;
  background: rgb(15 23 42 / 0.05);
}
.type-tabs button {
  border: none;
  border-radius: 11px;
  padding: 0.55rem 0.5rem;
  font-size: 0.88rem;
  font-weight: 800;
  color: var(--muted);
  background: transparent;
  cursor: pointer;
  transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;
}
.type-tabs button.on {
  color: var(--primary-dark);
  background: #fff;
  box-shadow: 0 1px 3px rgb(15 23 42 / 0.08);
}

.year-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}
.ym-block {
  flex: 1;
  text-align: center;
  min-width: 0;
}
.ym-label {
  display: block;
  font-size: 0.68rem;
  font-weight: 600;
  color: var(--muted);
  letter-spacing: 0.02em;
  margin-bottom: 0.12rem;
}
.ym {
  font-weight: 800;
  font-size: 1.02rem;
  letter-spacing: 0.03em;
  color: var(--text);
}
.nav-btn {
  flex-shrink: 0;
  width: 2.5rem;
  height: 2.5rem;
  border: none;
  border-radius: 12px;
  background: rgb(13 148 136 / 0.1);
  color: var(--primary-dark);
  font-size: 1.35rem;
  line-height: 1;
  cursor: pointer;
  transition: background 0.15s ease;
}
.nav-btn:active {
  background: rgb(13 148 136 / 0.18);
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 1.75rem 1rem;
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--muted);
}
.loading-dot {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 0.9s ease-in-out infinite alternate;
}
@keyframes pulse {
  from {
    opacity: 0.35;
    transform: scale(0.92);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.table-shell {
  padding: 0;
  overflow: hidden;
}
.scroll-x {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  max-width: 100%;
}
.matrix {
  width: max-content;
  min-width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 0.8rem;
}
.matrix th,
.matrix td {
  padding: 0.72rem 0.5rem;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}
.matrix thead th {
  font-weight: 700;
  color: var(--muted);
  white-space: nowrap;
  background: #f8fafc;
  border-bottom: 1px solid rgb(15 23 42 / 0.08);
}
.matrix thead .corner {
  border-top-left-radius: 0;
  vertical-align: bottom;
  padding-bottom: 0.58rem;
}
.matrix .col-cat {
  min-width: 4rem;
  max-width: 7rem;
  text-align: right;
  padding-left: 0.35rem;
  padding-right: 0.35rem;
}
.cat-head {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 0.72rem;
  font-weight: 700;
  color: rgb(71 85 105);
  text-align: right;
}
.matrix .head-total {
  text-align: right;
  font-size: 0.75rem;
  color: rgb(51 65 85);
}

.matrix .num {
  text-align: right;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  font-size: 0.78rem;
  font-weight: 600;
  letter-spacing: 0.01em;
}
.matrix .num.is-zero {
  color: rgb(148 163 184);
  font-weight: 500;
}
.matrix.expense tbody .num:not(.is-zero),
.matrix.expense tfoot .num {
  color: var(--expense);
}
.matrix.income tbody .num:not(.is-zero),
.matrix.income tfoot .num {
  color: var(--income);
}

.matrix .total-col {
  font-weight: 800;
  border-left: 1px solid rgb(13 148 136 / 0.15);
  background: #f0f9f8;
}
.matrix tbody td.num:not(.total-col) {
  background: #fff;
}
.matrix tbody td.total-col {
  background: #f0f9f8;
}
.matrix tbody tr.zebra td.num:not(.total-col) {
  background: #f1f5f9;
}
.matrix tbody tr.is-current-month-row td.num:not(.total-col) {
  background: #daf2ec;
}
.matrix tbody tr.is-current-month-row.zebra td.num:not(.total-col) {
  background: #cfe9e2;
}
.matrix tbody tr:hover td.num:not(.total-col) {
  background: #ecfdf5;
}
.matrix tbody tr.is-current-month-row:hover td.num:not(.total-col) {
  background: #c5ebe3;
}
.matrix tbody tr.zebra td.total-col {
  background: #e6f3f0;
}
.matrix tbody tr:hover td.total-col {
  background: #d5ede8;
}
.sticky-col {
  position: sticky;
  left: 0;
  z-index: 2;
  text-align: left;
  min-width: 5.75rem;
  max-width: 7.5rem;
  background: var(--surface);
  box-shadow: 6px 0 14px -4px rgb(15 23 42 / 0.12);
}
.matrix thead .sticky-col {
  z-index: 4;
  background: #f8fafc;
}
.matrix tbody tr.zebra .sticky-col {
  background: #f1f5f9;
}
.matrix tbody tr:not(.zebra) .sticky-col {
  background: var(--surface);
}
.matrix tbody tr.is-current-month-row .sticky-col {
  background: #daf2ec;
}
.matrix tbody tr.is-current-month-row.zebra .sticky-col {
  background: #cfe9e2;
}
.matrix tbody tr:hover .sticky-col {
  background: #ecfdf5;
}
.matrix tbody tr.is-current-month-row:hover .sticky-col {
  background: #c5ebe3;
}

.row-month {
  font-weight: 700;
  font-size: 0.8rem;
  color: var(--text);
  font-variant-numeric: tabular-nums;
}

.matrix tfoot .sticky-col,
.matrix tfoot td {
  border-bottom: none;
}
.foot-row th,
.foot-row td {
  border-top: 2px solid rgb(13 148 136 / 0.2);
  background: #e6f4f2;
  padding-top: 0.78rem;
  padding-bottom: 0.78rem;
}
.foot-row .sticky-col {
  z-index: 3;
  background: #e6f4f2;
  box-shadow: 6px 0 14px -4px rgb(15 23 42 / 0.1);
}
.foot-lbl {
  font-weight: 800;
  font-size: 0.8rem;
  color: var(--text);
}
.foot-num {
  font-weight: 800;
  font-size: 0.8rem;
}

.empty {
  margin: 0;
  padding: 1.75rem 1.15rem;
  text-align: center;
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 500;
}
</style>
