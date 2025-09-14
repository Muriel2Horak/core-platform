import axios from 'axios'
import { LoginData, User } from './types'

export const api = axios.create({
  withCredentials: true,
})

export const login = async (loginData: LoginData): Promise<User> => {
  const response = await api.post<User>('/api/auth/login', loginData)
  return response.data
}

export const logout = async (): Promise<void> => {
  await api.post('/api/auth/logout')
}

export async function apiFetch(
  input: RequestInfo | URL,
  init: RequestInit = {},
): Promise<Response> {
  const headers = new Headers(init.headers || {})
  return fetch(input, { ...init, headers, credentials: 'include' })
}

export async function apiGetJson<T = unknown>(url: string, init: RequestInit = {}): Promise<T> {
  const res = await apiFetch(url, init)
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  const ct = res.headers.get('content-type') || ''
  return ct.includes('application/json')
    ? ((await res.json()) as T)
    : ((await res.text()) as unknown as T)
}
