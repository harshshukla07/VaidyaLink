import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { Appointment } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { StatusBadge } from '../../components/StatusBadge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { EmptyState, ListSkeleton } from '../../components/EmptyState'
import { useToast } from '../../components/Toast'
import { displayDate, displayTime } from '../../utils/format'

export function Appointments() {
  const { user } = useAuth()
  const { push } = useToast()
  const [items, setItems] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [pendingCancel, setPendingCancel] = useState<Appointment | null>(null)

  async function load() {
    if (!user) return
    setLoading(true)
    try {
      const page = await api.getPatientAppointments(user.id)
      setItems(page.content || [])
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : 'Failed to load appointments',
      )
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
  }, [user?.id])

  async function confirmCancel() {
    if (!pendingCancel) return
    const id = pendingCancel.appointmentId
    setBusyId(id)
    setError('')
    try {
      await api.updateAppointmentStatus(id, 'CANCELLED')
      setPendingCancel(null)
      push('Appointment cancelled')
      await load()
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Could not cancel')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="page-enter">
      <header className="page-header">
        <h1>Your appointments</h1>
        <p>Upcoming and past visits in one place.</p>
      </header>

      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {loading ? (
        <ListSkeleton rows={4} />
      ) : items.length === 0 ? (
        <EmptyState
          title="No appointments yet"
          description="Start with symptom triage or browse doctors by specialty."
          action={
            <>
              <Link to="/chat" className="btn btn-primary btn-sm">
                Start triage
              </Link>
              <Link to="/doctors" className="btn btn-ghost btn-sm">
                Browse doctors
              </Link>
            </>
          }
        />
      ) : (
        <div className="list">
          {items.map((a) => (
            <div key={a.appointmentId} className="list-item">
              <div className="list-item-main">
                <h3>{a.doctorName}</h3>
                <p>
                  {displayDate(a.appointmentDate)} ·{' '}
                  {displayTime(String(a.appointmentTime))}
                </p>
                <div style={{ marginTop: '0.45rem' }}>
                  <StatusBadge status={a.status} />
                </div>
              </div>
              {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                <div className="appt-actions">
                  <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    disabled={busyId === a.appointmentId}
                    onClick={() => setPendingCancel(a)}
                  >
                    Cancel
                  </button>
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pendingCancel)}
        title="Cancel appointment?"
        message={
          pendingCancel
            ? `Cancel your visit with ${pendingCancel.doctorName} on ${displayDate(pendingCancel.appointmentDate)} at ${displayTime(String(pendingCancel.appointmentTime))}?`
            : ''
        }
        confirmLabel="Cancel visit"
        danger
        busy={busyId === pendingCancel?.appointmentId}
        onCancel={() => setPendingCancel(null)}
        onConfirm={confirmCancel}
      />
    </div>
  )
}
