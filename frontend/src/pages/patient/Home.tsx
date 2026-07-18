import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { Appointment } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { StatusBadge } from '../../components/StatusBadge'
import { ListSkeleton } from '../../components/EmptyState'
import { displayDate, displayTime, greetingForNow } from '../../utils/format'

export function PatientHome() {
  const { user } = useAuth()
  const [upcoming, setUpcoming] = useState<Appointment | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!user) return
    setLoading(true)
    api
      .getUpcomingAppointments(user.id, 0, 1)
      .then((page) => {
        setUpcoming(page.content?.[0] ?? null)
      })
      .catch((err) => {
        setError(
          err instanceof ApiError ? err.message : 'Could not load your home',
        )
      })
      .finally(() => setLoading(false))
  }, [user?.id])

  return (
    <div className="page-enter stack-lg">
      <header className="page-header home-header">
        <p className="eyebrow">{greetingForNow(user?.name)}</p>
        <h1>Your care workspace</h1>
        <p>Continue triage, find a doctor, or review your next visit.</p>
      </header>

      {error && <div className="error-banner">{error}</div>}

      <section className="home-next panel">
        <div className="home-next-label">Next appointment</div>
        {loading ? (
          <ListSkeleton rows={1} />
        ) : upcoming ? (
          <div className="home-next-body">
            <div>
              <h2>{upcoming.doctorName}</h2>
              <p className="muted">
                {displayDate(upcoming.appointmentDate)} ·{' '}
                {displayTime(String(upcoming.appointmentTime))}
              </p>
              <div style={{ marginTop: '0.55rem' }}>
                <StatusBadge status={upcoming.status} />
              </div>
            </div>
            <Link to="/appointments" className="btn btn-primary btn-sm">
              View details
            </Link>
          </div>
        ) : (
          <div className="home-next-empty">
            <p>No upcoming visits yet.</p>
            <div className="home-next-actions">
              <Link to="/chat" className="btn btn-primary btn-sm">
                Start triage
              </Link>
              <Link to="/doctors" className="btn btn-ghost btn-sm">
                Browse doctors
              </Link>
            </div>
          </div>
        )}
      </section>

      <section className="home-actions">
        <Link to="/chat" className="home-action">
          <span className="home-action-kicker">Step 1</span>
          <strong>Symptom triage</strong>
          <span>Describe how you feel and get specialty guidance.</span>
        </Link>
        <Link to="/doctors" className="home-action">
          <span className="home-action-kicker">Step 2</span>
          <strong>Find a doctor</strong>
          <span>Browse specialists and open appointment slots.</span>
        </Link>
        <Link to="/appointments" className="home-action">
          <span className="home-action-kicker">Step 3</span>
          <strong>Your visits</strong>
          <span>Track status and manage upcoming appointments.</span>
        </Link>
      </section>
    </div>
  )
}
