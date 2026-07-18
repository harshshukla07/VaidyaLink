import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function Login() {
  const { user, login, loading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const state = location.state as {
    registered?: boolean
    from?: string
  } | null
  const registered = Boolean(state?.registered)
  const from = state?.from
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  function homeFor(role: string) {
    return role === 'ROLE_DOCTOR' ? '/doctor' : '/home'
  }

  if (!loading && user) {
    return <Navigate to={from || homeFor(user.role)} replace />
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      const auth = await login(email.trim(), password)
      const fallback = homeFor(auth.role)
      const target =
        from && !from.startsWith('/login') && !from.startsWith('/register')
          ? from
          : fallback
      navigate(target)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Unable to sign in')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="panel auth-card">
        <Link to="/" className="brand">
          VaidyaLink
        </Link>
        <h1>Welcome back</h1>
        <p className="sub">Sign in to continue to your care workspace.</p>

        {registered && (
          <div className="success-banner" style={{ marginBottom: '1rem' }}>
            Account created. Sign in to continue.
          </div>
        )}
        {error && <div className="error-banner">{error}</div>}

        <form className="form" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              autoComplete="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                autoComplete="current-password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((v) => !v)}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>
          <button className="btn btn-primary btn-block" disabled={submitting}>
            {submitting ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <p className="auth-footer">
          New here? <Link to="/register">Create an account</Link>
        </p>
      </div>
    </div>
  )
}
