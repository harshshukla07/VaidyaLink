import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { Doctor } from '../../api/types'
import { EmptyState, ListSkeleton } from '../../components/EmptyState'

export function Doctors() {
  const [params, setParams] = useSearchParams()
  const [specialties, setSpecialties] = useState<string[]>([])
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [speciality, setSpeciality] = useState(params.get('speciality') || '')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    api
      .getSpecialties()
      .then((list) => {
        setSpecialties(list)
        if (!speciality && list.length > 0) {
          setSpeciality(list[0])
        } else if (list.length === 0) {
          setLoading(false)
        }
      })
      .catch((err) => {
        setError(
          err instanceof ApiError ? err.message : 'Failed to load specialties',
        )
        setLoading(false)
      })
  }, [])

  useEffect(() => {
    if (!speciality) return
    setLoading(true)
    setError('')
    setParams({ speciality }, { replace: true })
    api
      .getDoctorsBySpecialty(speciality)
      .then(setDoctors)
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Failed to load doctors')
        setDoctors([])
      })
      .finally(() => setLoading(false))
  }, [speciality])

  return (
    <div className="page-enter">
      <header className="page-header">
        <h1>Find a doctor</h1>
        <p>Filter by specialty and book an open slot.</p>
      </header>

      <div className="filters">
        <div className="field">
          <label htmlFor="speciality">Specialty</label>
          <select
            id="speciality"
            value={speciality}
            onChange={(e) => setSpeciality(e.target.value)}
            disabled={specialties.length === 0}
          >
            {specialties.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      {loading ? (
        <ListSkeleton rows={4} />
      ) : doctors.length === 0 ? (
        <EmptyState
          title="No doctors in this specialty"
          description="Try another specialty, or start triage for a recommendation."
          action={
            <Link to="/chat" className="btn btn-primary btn-sm">
              Start triage
            </Link>
          }
        />
      ) : (
        <div className="list">
          {doctors.map((d, i) => (
            <div
              key={d.id}
              className="list-item"
              style={{ animationDelay: `${i * 0.04}s` }}
            >
              <div className="list-item-main">
                <h3>{d.name}</h3>
                <p>
                  {d.speciality} · {d.experience} years experience
                </p>
              </div>
              <Link to={`/book/${d.id}`} className="btn btn-primary btn-sm">
                Book appointment
              </Link>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
