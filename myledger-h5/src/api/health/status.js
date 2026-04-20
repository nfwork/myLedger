import { http } from '../core/http'

/** 应用存活检查：`GET /api/health`（不经 dbfound） */
export async function fetchHealth() {
  const { data } = await http.get('/api/health')
  return data
}
