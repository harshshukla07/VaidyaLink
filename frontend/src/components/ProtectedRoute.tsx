import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../api/types'

export function ProtectedRoute({
  children,
  role,
}: {
  children: ReactNode
  role?: Role
}) {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <div className="loading-screen">Loading…</div>
  }

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }

  if (role && user.role !== role) {
    return (
      <Navigate
        to={user.role === 'ROLE_DOCTOR' ? '/doctor' : '/home'}
        replace
      />
    )
  }

  return children
}
