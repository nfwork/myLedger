import { dbfoundPost, unwrapDbfound, getDbfoundList, getDbfoundTotal } from '../core/dbfound'

/** @see model bookkeeping/entry.xml — `bookkeeping/entry` */

export async function fetchEntryList(params = {}) {
  const data = await dbfoundPost('/bookkeeping/entry.query!list', params)
  unwrapDbfound(data)
  return getDbfoundList(data)
}

/**
 * 分页流水列表（dbfound 内置 `start`、`limit`；响应 `total_counts`）
 * @returns {{ rows: any[], total: number }}
 */
export async function fetchEntryListPage(params = {}) {
  const data = await dbfoundPost('/bookkeeping/entry.query!list', params)
  unwrapDbfound(data)
  const rows = getDbfoundList(data)
  const rawTotal = getDbfoundTotal(data)
  const total = rawTotal >= 0 ? rawTotal : rows.length
  return { rows, total }
}

export async function fetchEntryById(id) {
  const data = await dbfoundPost('/bookkeeping/entry.query!getById', { id: Number(id) })
  unwrapDbfound(data)
  return getDbfoundList(data)[0] ?? null
}

export async function createEntry(body) {
  const data = await dbfoundPost('/bookkeeping/entry.execute!add', body)
  unwrapDbfound(data)
  return data
}

export async function updateEntry(body) {
  const data = await dbfoundPost('/bookkeeping/entry.execute!update', body)
  unwrapDbfound(data)
  return data
}

export async function deleteEntry(id) {
  const data = await dbfoundPost('/bookkeeping/entry.execute!delete', { id: Number(id) })
  unwrapDbfound(data)
  return data
}
