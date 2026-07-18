import { useEffect, useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { Appointment, Doctor } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { useToast } from '../../components/Toast'
import { ListSkeleton } from '../../components/EmptyState'
import { displayDate, displayTime, todayIso } from '../../utils/format'

function normalizeTime(t: string) {
  if (/^\d{2}:\d{2}$/.test(t)) return `${t}:00`
  return t
}

export function Book() {
  const { doctorId } = useParams()
  const { user } = useAuth()
  const { push } = useToast()
  const id = Number(doctorId)

  const [doctor, setDoctor] = useState<Doctor | null>(null)
  const [date, setDate] = useState(todayIso())
  const [slots, setSlots] = useState<string[]>([])
  const [selected, setSelected] = useState('')
  const [loading, setLoading] = useState(true)
  const [slotsLoading, setSlotsLoading] = useState(false)
  const [booking, setBooking] = useState(false)
  const [error, setError] = useState('')
  const [booked, setBooked] = useState<Appointment | null>(null)

  useEffect(() => {
    if (!id) return
    api
      .getDoctor(id)
      .then(setDoctor)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Doctor not found')
      })
      .finally(() => setLoading(false))
  }, [id])

  useEffect(() => {
    if (!id || !date || booked) return
    setSelected('')
    setError('')
    setSlotsLoading(true)
    api
      .getAvailableSlots(id, date)
      .then(setSlots)
      .catch((err) => {
        setSlots([])
        setError(err instanceof ApiError ? err.message : 'Could not load slots')
      })
      .finally(() => setSlotsLoading(false))
  }, [id, date, booked])

  async function onBook(e: FormEvent) {
    e.preventDefault()
    if (!user || !selected) return
    setBooking(true)
    setError('')
    try {
      const appointment = await api.bookAppointment({
        patientId: user.id,
        doctorId: id,
        appointmentDate: date,
        appointmentTime: normalizeTime(selected),
      })
      setBooked(appointment)
      push('Appointment booked successfully')
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Booking failed')
    } finally {
      setBooking(false)
    }
  }

  if (loading) {
    return (
      <div style={{ maxWidth: 560 }}>
        <header className="page-header">
          <h1>Book appointment</h1>
        </header>
        <ListSkeleton rows={2} />
      </div>
    )
  }

  if (!doctor) {
    return (
      <div className="stack">
        <div className="error-banner">{error || 'Doctor not found'}</div>
        <Link to="/doctors" className="btn btn-ghost">
          Back to doctors
        </Link>
      </div>
    )
  }

  if (booked) {
    return (
      <div style={{ maxWidth: 560 }} className="page-enter">
        <header className="page-header">
          <h1>Appointment confirmed</h1>
          <p>Your visit has been scheduled.</p>
        </header>
        <div className="panel stack booking-summary">
          <div>
            <p className="muted small">Doctor</p>
            <p className="summary-value">{booked.doctorName || doctor.name}</p>
          </div>
          <div>
            <p className="muted small">Specialty</p>
            <p className="summary-value">{doctor.speciality}</p>
          </div>
          <div>
            <p className="muted small">When</p>
            <p className="summary-value">
              {displayDate(booked.appointmentDate)} ·{' '}
              {displayTime(String(booked.appointmentTime))}
            </p>
          </div>
          <div>
            <p className="muted small">Status</p>
            <p className="summary-value">{booked.status}</p>
          </div>
          <div className="hero-actions" style={{ marginTop: '0.5rem' }}>
            <Link to="/appointments" className="btn btn-primary">
              View appointments
            </Link>
            <Link to="/doctors" className="btn btn-ghost">
              Browse doctors
            </Link>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: 560 }} className="page-enter">
      <header className="page-header">
        <h1>Book with {doctor.name}</h1>
        <p>
          {doctor.speciality} · {doctor.experience} years experience
        </p>
      </header>

      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}

      <form className="panel form" onSubmit={onBook}>
        <div className="field">
          <label htmlFor="date">Date</label>
          <input
            id="date"
            type="date"
            min={todayIso()}
            required
            value={date}
            onChange={(e) => setDate(e.target.value)}
          />
        </div>

        <div className="field">
          <label>Available times</label>
          {slotsLoading ? (
            <p className="muted small">Checking availability…</p>
          ) : slots.length === 0 ? (
            <p className="muted small">No open slots for this date. Try another day.</p>
          ) : (
            <div className="slot-grid" role="listbox" aria-label="Available times">
              {slots.map((slot) => (
                <button
                  key={slot}
                  type="button"
                  role="option"
                  aria-selected={selected === slot}
                  className={`slot${selected === slot ? ' selected' : ''}`}
                  onClick={() => setSelected(slot)}
                >
                  {displayTime(slot)}
                </button>
              ))}
            </div>
          )}
        </div>

        {selected && (
          <p className="muted small">
            Selected: {displayDate(date)} at {displayTime(selected)}
          </p>
        )}

        <button className="btn btn-primary" disabled={!selected || booking}>
          {booking ? 'Booking…' : 'Confirm appointment'}
        </button>
      </form>
    </div>
  )
}
