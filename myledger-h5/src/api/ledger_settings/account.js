import { dbfoundPost, unwrapDbfound, getDbfoundList } from '../core/dbfound'

/** @see model ledger_settings/account.xml — `ledger_settings/account` */

export async function fetchAccountList() {
  const data = await dbfoundPost('/ledger_settings/account.query!listByUser', {})
  unwrapDbfound(data)
  return getDbfoundList(data)
}

export async function fetchDefaultAccount() {
  const data = await dbfoundPost('/ledger_settings/account.query!getDefault', {})
  unwrapDbfound(data)
  return getDbfoundList(data)[0] ?? null
}

export async function createAccount(body) {
  const data = await dbfoundPost('/ledger_settings/account.execute!add', body)
  unwrapDbfound(data)
  return data
}

export async function updateAccount(body) {
  const data = await dbfoundPost('/ledger_settings/account.execute!update', body)
  unwrapDbfound(data)
  return data
}

export async function setDefaultAccount(body) {
  const data = await dbfoundPost('/ledger_settings/account.execute!setDefault', body)
  unwrapDbfound(data)
  return data
}

export async function deleteAccount(body) {
  const data = await dbfoundPost('/ledger_settings/account.execute!delete', body)
  unwrapDbfound(data)
  return data
}
