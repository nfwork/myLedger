import axios from 'axios'

/** 全局 Axios：baseURL、超时、携带 Cookie（Session） */
export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 20000,
  withCredentials: true,
})
