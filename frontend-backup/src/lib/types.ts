export interface LoginData {
  username: string
  password: string
}

export interface User {
  id: string
  username: string
  firstName: string
  lastName: string
  email: string
}

export type UserProfile = {
  preferred_username?: string
  given_name?: string
  family_name?: string
  email?: string
  roles?: string[]
  realm_access?: { roles?: string[] }
  resource_access?: Record<string, { roles?: string[] }>
}
