<template>
  <div class="stats-page">
    <section class="hero card">
      <p class="hero-title">月 × 分类</p>
      <p class="hero-desc">
        行为分类、列为最近 <strong>{{ WINDOW }}</strong> 个自然月（以结束月为准）；切换类型查看支出或收入分布。
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
      <div class="month-bar">
        <button type="button" class="nav-btn" @click="shiftWindow(-1)" aria-label="结束月上一月">‹</button>
        <div class="ym-block">
          <span class="ym-label">数据截止到</span>
          <span class="ym">{{ formatYearMonthLabel(windowEndYm) }}</span>
        </div>
        <button type="button" class="nav-btn" @click="shiftWindow(1)" aria-label="结束月下一月">›</button>
      </div>
    </div>

    <div v-if="loading" class="loading card" aria-busy="true">
      <span class="loading-dot" />
      <span>加载中…</span>
    </div>
    <div v-else class="table-shell card">
      <div class="scroll-x">
        <table class="matrix" :class="entryType">
          <thead>
            <tr>
              <th class="sticky-col corner" scope="col">分类</th>
              <th
                v-for="h in monthHeaders"
                :key="h.ym"
                class="col-ym"
                scope="col"
                :title="formatYearMonthLabel(h.ym)"
              >
                <span class="ym-compact">{{ formatYmCompact(h.ym) }}</span>
              </th>
              <th class="total-col head-total" scope="col">合计</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in matrix.rows" :key="row.id" :class="{ zebra: ri % 2 === 1 }">
              <th class="sticky-col cat-name" scope="row">{{ row.name }}</th>
              <td
                v-for="(v, j) in row.amounts"
                :key="monthKeys[j]"
                class="num"
                :class="{ 'is-zero': !v }"
              >
                {{ cellMoney(v) }}
              </td>
              <td class="num total-col">{{ cellMoney(row.rowTotal) }}</td>
            </tr>
          </tbody>
          <tfoot v-if="matrix.rows.length">
            <tr class="foot-row">
              <th class="sticky-col foot-lbl" scope="row">月度合计</th>
              <td v-for="(v, j) in matrix.colTotals" :key="'ft-' + monthKeys[j]" class="num foot-num">
                {{ cellMoney(v) }}
              </td>
              <td class="num total-col foot-num">{{ cellMoney(matrix.grand) }}</td>
            </tr>
          </tfoot>
        </table>
      </div>
      <p v-if="!matrix.rows.length" class="empty">该区间暂无{{ entryType === 'expense' ? '支出' : '收入' }}数据</p>
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
import {
  currentYearMonth,
  shiftYearMonth,
  formatMoney,
  formatYearMonthLabel,
  formatYmCompact,
} from '@/utils/format'

const WINDOW = 12

const windowEndYm = ref(currentYearMonth())
const entryType = ref('expense')
const loading = ref(false)
const flatRows = ref([])

const accountPick = computed({
  get: () => (scopeAccountId.value == null ? '' : String(scopeAccountId.value)),
  set: (v) => setScopeAccountId(v === '' || v == null ? null : v),
})

const months = computed(() => rangeYearMonthsAscending(windowEndYm.value, WINDOW))
const monthKeys = computed(() => months.value)

const monthHeaders = computed(() => months.value.map((ym) => ({ ym })))

const matrix = computed(() => buildMatrix(flatRows.value, months.value))

function rangeYearMonthsAscending(endYm, count) {
  const rev = []
  let ym = endYm
  for (let i = 0; i < count; i++) {
    rev.push(ym)
    ym = shiftYearMonth(ym, -1)
  }
  return rev.reverse()
}

function shiftWindow(delta) {
  windowEndYm.value = shiftYearMonth(windowEndYm.value, delta)
}

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
  const rows = [...catMap.values()].map((c) => {
    const amounts = monthsList.map((ym) => Number(c.byMonth[ym] || 0))
    const rowTotal = amounts.reduce((s, v) => s + v, 0)
    return { id: c.id, name: c.name, amounts, rowTotal }
  })
  rows.sort((a, b) => b.rowTotal - a.rowTotal)
  const colTotals = monthsList.map((_, j) => rows.reduce((s, row) => s + row.amounts[j], 0))
  const grand = colTotals.reduce((s, v) => s + v, 0)
  return { rows, colTotals, grand }
}

function cellMoney(n) {
  const v = Number(n) || 0
  if (v === 0) return '—'
  return formatMoney(v)
}

async function load() {
  loading.value = true
  const list = rangeYearMonthsAscending(windowEndYm.value, WINDOW)
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
  } catch {
    flatRows.value = []
  } finally {
    loading.value = false
  }
}

watch([windowEndYm, entryType, scopeAccountId], load)
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

.month-bar {
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
  padding: 0.55rem 0.45rem;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}
.matrix thead th {
  font-weight: 700;
  color: var(--muted);
  white-space: nowrap;
  background: rgb(248 250 252);
  border-bottom: 1px solid rgb(15 23 42 / 0.08);
}
.matrix thead .corner {
  border-top-left-radius: 0;
  vertical-align: bottom;
  padding-bottom: 0.5rem;
}
.matrix .col-ym {
  min-width: 3.5rem;
  text-align: right;
  padding-left: 0.35rem;
  padding-right: 0.35rem;
}
.ym-compact {
  font-family: ui-monospace, 'Cascadia Code', 'SF Mono', Menlo, monospace;
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  color: rgb(71 85 105);
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
  background: rgb(13 148 136 / 0.04);
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
  background: rgb(248 250 252);
}
.matrix tbody tr.zebra .sticky-col {
  background: rgb(248 250 252 / 0.85);
}
.matrix tbody tr:not(.zebra) .sticky-col {
  background: var(--surface);
}
.matrix tbody tr.zebra td:not(.sticky-col),
.matrix tbody tr.zebra th[scope='row'] {
  background: rgb(248 250 252 / 0.45);
}
.matrix tbody tr:hover .sticky-col {
  background: rgb(236 253 245 / 0.95);
}
.matrix tbody tr:hover td:not(.total-col),
.matrix tbody tr:hover th[scope='row'] {
  background: rgb(236 253 245 / 0.35);
}

.cat-name {
  font-weight: 700;
  font-size: 0.8rem;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.matrix tfoot .sticky-col,
.matrix tfoot td {
  border-bottom: none;
}
.foot-row th,
.foot-row td {
  border-top: 2px solid rgb(13 148 136 / 0.2);
  background: rgb(13 148 136 / 0.08);
  padding-top: 0.65rem;
  padding-bottom: 0.65rem;
}
.foot-row .sticky-col {
  z-index: 3;
  background: rgb(13 148 136 / 0.1);
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
  padding: 1.35rem 1rem 1.15rem;
  text-align: center;
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 500;
  border-top: 1px solid var(--line);
}
</style>
