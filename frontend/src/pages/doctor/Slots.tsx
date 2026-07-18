import { useMemo, useState, type FormEvent } from 'react'
import { api, ApiError } from '../../api/client'
import { useAuth } from '../../auth/AuthContext'
import { useToast } from '../../components/Toast'
import { todayIso } from '../../utils/format'

function friendlySlotError(message: string) {
  if (/past date/i.test(message)) {
    return 'Slots cannot be generated for a past date'
  }
  return message
}

function estimateSlotCount(start: string, end: string, duration: number) {
  const [sh, sm] = start.split(':').map(Number)
  const [eh, em] = end.split(':').map(Number)
  const mins = eh * 60 + em - (sh * 60 + sm)
  if (mins <= 0 || duration < 10) return 0
  return Math.floor(mins / duration)
}

export function Slots() {
  const { user } = useAuth()
  const { push } = useToast()
  const [date, setDate] = useState(todayIso())
  const [start, setStart] = useState('09:00')
  const [end, setEnd] = useState('13:00')
  const [duration, setDuration] = useState(20)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const estimate = useMemo(
    () => estimateSlotCount(start, end, duration),
    [start, end, duration],
  )

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!user) return
    setError('')
    setSuccess('')

    if (date < todayIso()) {
      setError('Slots cannot be generated for a past date')
      return
    }

    setSubmitting(true)
    try {
      const msg = await api.generateSlots(user.id, {
        date,
        shiftStartTime: `${start}:00`,
        shiftEndTime: `${end}:00`,
        durationInMinutes: duration,
      })
      const countMatch =
        typeof msg === 'string' ? msg.match(/Generated\s+(\d+)\s+slots/i) : null
      const text = countMatch
        ? `Success! Generated ${countMatch[1]} slots`
        : 'Success! Slots generated.'
      setSuccess(text)
      push(text)
    } catch (err) {
      const raw =
        err instanceof ApiError ? err.message : 'Could not generate slots'
      setError(friendlySlotError(raw))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div style={{ maxWidth: 520 }} className="page-enter">
      <header className="page-header">
        <h1>Generate slots</h1>
        <p>Open your calendar for a shift so patients can book.</p>
      </header>

      {error && (
        <div className="error-banner" style={{ marginBottom: '1rem' }}>
          {error}
        </div>
      )}
      {success && (
        <div className="success-banner" style={{ marginBottom: '1rem' }}>
          {success}
        </div>
      )}

      <form className="panel form" onSubmit={onSubmit}>
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
        <div className="form-row two">
          <div className="field">
            <label htmlFor="start">Shift start</label>
            <input
              id="start"
              type="time"
              required
              value={start}
              onChange={(e) => setStart(e.target.value)}
            />
          </div>
          <div className="field">
            <label htmlFor="end">Shift end</label>
            <input
              id="end"
              type="time"
              required
              value={end}
              onChange={(e) => setEnd(e.target.value)}
            />
          </div>
        </div>
        <div className="field">
          <label htmlFor="duration">Slot duration (minutes)</label>
          <input
            id="duration"
            type="number"
            min={10}
            required
            value={duration}
            onChange={(e) => setDuration(Number(e.target.value))}
          />
        </div>

        <p className="muted small">
          {estimate > 0
            ? `About ${estimate} slots will be created for this shift.`
            : 'Check that end time is after start time.'}
        </p>

        <button className="btn btn-primary" disabled={submitting || estimate < 1}>
          {submitting ? 'Generating…' : 'Generate slots'}
        </button>
      </form>
    </div>
  )
}
