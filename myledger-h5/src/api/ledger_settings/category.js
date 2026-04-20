import { dbfoundPost, unwrapDbfound, getDbfoundList } from '../core/dbfound'

/** @see model ledger_settings/category.xml — `ledger_settings/category` */

export async function fetchCategoryList(params = {}) {
  const data = await dbfoundPost('/ledger_settings/category.query!list', params)
  unwrapDbfound(data)
  return getDbfoundList(data)
}

export async function createCategory(body) {
  const data = await dbfoundPost('/ledger_settings/category.execute!add', body)
  unwrapDbfound(data)
  return data
}

export async function deleteCategory(body) {
  const data = await dbfoundPost('/ledger_settings/category.execute!delete', body)
  unwrapDbfound(data)
  return data
}
