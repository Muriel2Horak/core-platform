import { api } from './api'
import { User } from './types'
import useSWR from 'swr'

const fetcher = (url: string) => api.get(url).then((res) => res.data)

export function useUser() {
  const { data, error, mutate } = useSWR<User>('/api/auth/me', fetcher, {
    shouldRetryOnError: false,
    revalidateOnFocus: false,
  })

  const loading = !data && !error
  const loggedOut = !!(error && error.response && error.response.status === 401)

  return {
    loading,
    loggedOut,
    user: data,
    mutate,
  }
}
