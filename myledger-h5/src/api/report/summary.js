import { dbfoundPost, unwrapDbfound, getDbfoundList } from '../core/dbfound'

/** @see model report/summary.xml — `report/summary` */

export async function fetchMonthTotals(yearMonth, extra = {}) {
  const data = await dbfoundPost('/report/summary.query!monthTotals', { year_month: yearMonth, ...extra })
  unwrapDbfound(data)
  const row = getDbfoundList(data)[0] || {}
  return {
    income_total: Number(row.income_total ?? 0),
    expense_total: Number(row.expense_total ?? 0),
  }
}

export async function fetchCategoryTotals(yearMonth, entryType, extra = {}) {
  const data = await dbfoundPost('/report/summary.query!byCategory', {
    year_month: yearMonth,
    entry_type: entryType,
    ...extra,
  })
  unwrapDbfound(data)
  return getDbfoundList(data)
}

/** 月 × 分类 二维明细（区间内按月、按分类汇总）；行含 `bill_month`（`YYYY-MM`，避免与 MySQL `YEAR_MONTH` 关键字冲突） @see summary.xml `matrixByMonthCategory` */
export async function fetchMonthCategoryMatrix({ year_month_from, year_month_to, entry_type }, extra = {}) {
  const data = await dbfoundPost('/report/summary.query!matrixByMonthCategory', {
    year_month_from,
    year_month_to,
    entry_type,
    ...extra,
  })
  unwrapDbfound(data)
  return getDbfoundList(data)
}
