import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { api, ApiError } from '../api/client'
import { useAuth } from '../auth/AuthContext'

type RoleChoice = 'patient' | 'doctor'

export function Register() {
  const { user, loading, isDoctor } = useAuth()
  const navigate = useNavigate()
  const [role, setRole] = useState<RoleChoice>('patient')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [mobile, setMobile] = useState('')
  const [gender, setGender] = useState('Other')
  const [age, setAge] = useState('')
  const [speciality, setSpeciality] = useState('')
  const [experience, setExperience] = useState('')

  if (!loading && user) {
    return <Navigate to={isDoctor ? '/doctor' : '/home'} replace />
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      if (role === 'patient') {
        await api.registerPatient({
          name: name.trim(),
          email: email.trim(),
          mobile: mobile.trim(),
          gender,
          age: Number(age),
          password,
        })
      } else {
        await api.registerDoctor({
          name: name.trim(),
          email: email.trim(),
          speciality: speciality.trim(),
          experience: Number(experience),
          password,
        })
      }
      navigate('/login', { state: { registered: true } })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Registration failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="panel auth-card" style={{ width: 'min(480px, 100%)' }}>
        <Link to="/" className="brand">
          VaidyaLink
        </Link>
        <h1>Create account</h1>
        <p className="sub">Join as a patient or doctor — one clean path either way.</p>

        <div className="role-tabs">
          <button
            type="button"
            className={`role-tab${role === 'patient' ? ' active' : ''}`}
            onClick={() => setRole('patient')}
          >
            Patient
          </button>
          <button
            type="button"
            className={`role-tab${role === 'doctor' ? ' active' : ''}`}
            onClick={() => setRole('doctor')}
          >
            Doctor
          </button>
        </div>

        {error && <div className="error-banner">{error}</div>}

        <form className="form" onSubmit={onSubmit}>
          <div className="field">
            <label htmlFor="name">Full name</label>
            <input
              id="name"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </div>

          {role === 'patient' ? (
            <>
              <div className="form-row two">
                <div className="field">
                  <label htmlFor="mobile">Mobile (10 digits)</label>
                  <input
                    id="mobile"
                    inputMode="numeric"
                    pattern="[0-9]{10}"
                    required
                    value={mobile}
                    onChange={(e) => setMobile(e.target.value)}
                  />
                </div>
                <div className="field">
                  <label htmlFor="age">Age</label>
                  <input
                    id="age"
                    type="number"
                    min={0}
                    required
                    value={age}
                    onChange={(e) => setAge(e.target.value)}
                  />
                </div>
              </div>
              <div className="field">
                <label htmlFor="gender">Gender</label>
                <select
                  id="gender"
                  value={gender}
                  onChange={(e) => setGender(e.target.value)}
                >
                  <option>Female</option>
                  <option>Male</option>
                  <option>Other</option>
                </select>
              </div>
            </>
          ) : (
            <div className="form-row two">
              <div className="field">
                <label htmlFor="speciality">Speciality</label>
                <input
                  id="speciality"
                  required
                  placeholder="e.g. Cardiology"
                  value={speciality}
                  onChange={(e) => setSpeciality(e.target.value)}
                />
              </div>
              <div className="field">
                <label htmlFor="experience">Experience (years)</label>
                <input
                  id="experience"
                  type="number"
                  min={0}
                  required
                  value={experience}
                  onChange={(e) => setExperience(e.target.value)}
                />
              </div>
            </div>
          )}

          <div className="field">
            <label htmlFor="password">Password</label>
            <div className="password-field">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                required
                minLength={6}
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
            {submitting ? 'Creating…' : 'Create account'}
          </button>
        </form>

        <p className="auth-footer">
          Already registered? <Link to="/login">Sign in</Link>
        </p>
      </div>
    </div>
  )
}
