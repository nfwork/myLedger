import { http } from './http'

/**
 * dbfound HTTP：POST JSON，路径与后端 model 一致，形如 `/模块/文件名.query!名称`、`.execute!名称`
 * @see myledger-api/src/main/resources/model/
 */
export function unwrapDbfound(data) {
  if (data && data.success === false) {
    const msg = data.message || data.msg || '请求失败'
    throw new Error(msg)
  }
  return data
}

export function getDbfoundList(data) {
  const d = unwrapDbfound(data)
  return d.datas ?? d.data?.datas ?? []
}

export function getDbfoundTotal(data) {
  const d = unwrapDbfound(data)
  const t = d.total_counts ?? d.totalCounts
  return typeof t === 'number' ? t : -1
}

export async function dbfoundPost(path, body = {}) {
  const { data } = await http.post(path, body, {
    headers: { 'Content-Type': 'application/json' },
  })
  if (data != null && typeof data === 'object' && 'success' in data) {
    unwrapDbfound(data)
  }
  return data
}
