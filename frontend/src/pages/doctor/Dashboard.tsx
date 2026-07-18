import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { Appointment, AppointmentStatus } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { StatusBadge } from '../../components/StatusBadge'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { EmptyState, ListSkeleton } from '../../components/EmptyState'
import { useToast } from '../../components/Toast'
import {
  displayDate,
  displayTime,
  greetingForNow,
  todayIso,
} from '../../utils/format'

interface PendingAction {
  id: number
  status: AppointmentStatus
  label: string
  patientName: string
}

export function DoctorDashboard() {
  const { user } = useAuth()
  const { push } = useToast()
  const [date, setDate] = useState(todayIso())
  const [query, setQuery] = useState('')
  const [items, setItems] = useState<Appointment[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [busyId, setBusyId] = useState<number | null>(null)
  const [pending, setPending] = useState<PendingAction | null>(null)

  async function load(search = query) {
    if (!user) return
    setLoading(true)
    setError('')
    try {
      const page = search.trim()
        ? await api.searchDoctorAppointments(user.id, search.trim())
        : await api.getDoctorAppointments(user.id, date)
      setItems(page.content || [])
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to load schedule')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (!user) return
    const delay = query.trim() ? 350 : 0
    const handle = window.setTimeout(() => {
      load(query)
    }, delay)
    return () => window.clearTimeout(handle)
  }, [user?.id, date, query])

  async function applyStatus() {
    if (!pending) return
    setBusyId(pending.id)
    setError('')
    try {
      await api.updateAppointmentStatus(pending.id, pending.status)
      push(`Marked as ${pending.status.toLowerCase()}`)
      setPending(null)
      await load(query)
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Update failed')
    } finally {
      setBusyId(null)
    }
  }

  const pendingCount = items.filter((a) => a.status === 'PENDING').length

  return (
    <div className="page-enter">
      <header className="page-header home-header">
        <p className="eyebrow">{greetingForNow(user?.name)}</p>
        <h1>Schedule</h1>
        <p>Review visits, search patients, and update status.</p>
      </header>

      {!loading && !query.trim() && (
        <p className="schedule-meta muted small">
          {items.length} appointment{items.length === 1 ? '' : 's'} on{' '}
          {displayDate(date)}
          {pendingCount > 0 ? ` · ${pendingCount} pending` : ''}
        </p>
      )}

      <div className="filters">
        <div className="field">
          <label htmlFor="date">Date</label>
          <input
            id="date"
            type="date"
            value={date}
            onChange={(e) => {
              setQuery('')
              setDate(e.target.value)
            }}
          />
        </div>
        <div className="field" style={{ flex: 2 }}>
          <label htmlFor="query">Search patient</label>
          <input
            id="query"
            placeholder="Type a patient name…"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      </div>

      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      {loading ? (
        <ListSkeleton rows={4} />
      ) : items.length === 0 ? (
        <EmptyState
          title={query.trim() ? 'No matching patients' : 'No appointments'}
          description={
            query.trim()
              ? 'Try another name, or clear the search.'
              : 'Generate slots so patients can book visits on this day.'
          }
          action={
            !query.trim() ? (
              <Link to="/doctor/slots" className="btn btn-primary btn-sm">
                Generate slots
              </Link>
            ) : (
              <button
                type="button"
                className="btn btn-ghost btn-sm"
                onClick={() => setQuery('')}
              >
                Clear search
              </button>
            )
          }
        />
      ) : (
        <div className="list">
          {items.map((a) => (
            <div key={a.appointmentId} className="list-item">
              <div className="list-item-main">
                <h3>{a.patientName}</h3>
                <p>
                  {displayDate(a.appointmentDate)} ·{' '}
                  {displayTime(String(a.appointmentTime))}
                </p>
                <div style={{ marginTop: '0.45rem' }}>
                  <StatusBadge status={a.status} />
                </div>
              </div>
              <div className="appt-actions">
                {a.status === 'PENDING' && (
                  <button
                    type="button"
                    className="btn btn-primary btn-sm"
                    disabled={busyId === a.appointmentId}
                    onClick={() =>
                      setPending({
                        id: a.appointmentId,
                        status: 'CONFIRMED',
                        label: 'Confirm',
                        patientName: a.patientName,
                      })
                    }
                  >
                    Confirm
                  </button>
                )}
                {(a.status === 'PENDING' || a.status === 'CONFIRMED') && (
                  <>
                    <button
                      type="button"
                      className="btn btn-ghost btn-sm"
                      disabled={busyId === a.appointmentId}
                      onClick={() =>
                        setPending({
                          id: a.appointmentId,
                          status: 'COMPLETED',
                          label: 'Complete',
                          patientName: a.patientName,
                        })
                      }
                    >
                      Complete
                    </button>
                    <button
                      type="button"
                      className="btn btn-danger btn-sm"
                      disabled={busyId === a.appointmentId}
                      onClick={() =>
                        setPending({
                          id: a.appointmentId,
                          status: 'CANCELLED',
                          label: 'Cancel',
                          patientName: a.patientName,
                        })
                      }
                    >
                      Cancel
                    </button>
                  </>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmDialog
        open={Boolean(pending)}
        title={`${pending?.label} appointment?`}
        message={
          pending
            ? `${pending.label} the visit for ${pending.patientName}?`
            : ''
        }
        confirmLabel={pending?.label || 'Confirm'}
        danger={pending?.status === 'CANCELLED'}
        busy={busyId === pending?.id}
        onCancel={() => setPending(null)}
        onConfirm={applyStatus}
      />
    </div>
  )
}
