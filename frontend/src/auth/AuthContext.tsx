import {
  createContext,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'
import { api } from '../api/client'
import type { AuthUser, Role } from '../api/types'

interface AuthContextValue {
  user: AuthUser | null
  loading: boolean
  login: (email: string, password: string) => Promise<AuthUser>
  logout: () => void
  isPatient: boolean
  isDoctor: boolean
}

const AuthContext = createContext<AuthContextValue | null>(null)

const STORAGE_KEY = 'vl_auth'

function persist(user: AuthUser | null) {
  if (!user?.token) {
    localStorage.removeItem(STORAGE_KEY)
    localStorage.removeItem('vl_token')
    return
  }
  localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  localStorage.setItem('vl_token', user.token)
}

function readStored(): AuthUser | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as AuthUser
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => readStored())
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const stored = readStored()
    if (!stored?.token) {
      setLoading(false)
      return
    }

    api
      .me()
      .then((me) => {
        const next = { ...me, token: stored.token }
        setUser(next)
        persist(next)
      })
      .catch(() => {
        setUser(null)
        persist(null)
      })
      .finally(() => setLoading(false))
  }, [])

  async function login(email: string, password: string) {
    const auth = await api.login(email, password)
    setUser(auth)
    persist(auth)
    return auth
  }

  function logout() {
    setUser(null)
    persist(null)
  }

  const role = user?.role as Role | undefined

  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        logout,
        isPatient: role === 'ROLE_PATIENT',
        isDoctor: role === 'ROLE_DOCTOR',
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
