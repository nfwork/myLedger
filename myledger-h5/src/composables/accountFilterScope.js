import { ref } from 'vue'
import { fetchAccountList } from '@/api'

const STORAGE_KEY = 'myledger_scope_account_id'

/** 与概览 / 统计 / 流水列表共享：按资金账户筛选（null 表示全部） */
export const scopeAccountId = ref(null)
export const accountsForFilter = ref([])

let loaded = false

function readStored() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    if (s == null || s === '') {
      scopeAccountId.value = null
      return
    }
    const n = Number(s)
    scopeAccountId.value = Number.isFinite(n) ? n : null
  } catch {
    scopeAccountId.value = null
  }
}

function writeStored() {
  try {
    if (scopeAccountId.value == null) localStorage.removeItem(STORAGE_KEY)
    else localStorage.setItem(STORAGE_KEY, String(scopeAccountId.value))
  } catch {
    /* ignore */
  }
}

function validateScopeAgainstList() {
  if (scopeAccountId.value == null) return
  const ok = accountsForFilter.value.some((a) => a.id === scopeAccountId.value)
  if (!ok) {
    scopeAccountId.value = null
    writeStored()
  }
}

export async function ensureFilterAccounts() {
  if (loaded) return
  readStored()
  accountsForFilter.value = await fetchAccountList()
  loaded = true
  validateScopeAgainstList()
}

export async function refreshFilterAccounts() {
  accountsForFilter.value = await fetchAccountList()
  loaded = true
  validateScopeAgainstList()
}

export function setScopeAccountId(v) {
  if (v === '' || v == null) {
    scopeAccountId.value = null
  } else {
    const n = Number(v)
    scopeAccountId.value = Number.isFinite(n) ? n : null
  }
  writeStored()
}

export function accountFilterParams() {
  return scopeAccountId.value == null ? {} : { account_id: scopeAccountId.value }
}
