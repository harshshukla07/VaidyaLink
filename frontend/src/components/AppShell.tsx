import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ApiStatusBanner } from './ApiStatusBanner'
import { PageTransition } from './PageTransition'

export function AppShell() {
  const { user, logout, isPatient, isDoctor } = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const location = useLocation()

  useEffect(() => {
    function onResize() {
      if (window.innerWidth > 760) setMenuOpen(false)
    }
    window.addEventListener('resize', onResize)
    return () => window.removeEventListener('resize', onResize)
  }, [])

  useEffect(() => {
    setMenuOpen(false)
  }, [location.pathname])

  function closeMenu() {
    setMenuOpen(false)
  }

  return (
    <div className="shell">
      <ApiStatusBanner />
      <header className="nav">
        <NavLink
          to={isDoctor ? '/doctor' : '/home'}
          className="nav-brand"
          onClick={closeMenu}
        >
          <span className="nav-cross" aria-hidden="true" />
          VaidyaLink
        </NavLink>

        <button
          type="button"
          className="nav-toggle"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((v) => !v)}
        >
          <span />
          <span />
          <span />
        </button>

        <nav className={`nav-links${menuOpen ? ' open' : ''}`}>
          {isPatient && (
            <>
              <NavLink to="/home" className={linkClass} end onClick={closeMenu}>
                Home
              </NavLink>
              <NavLink to="/chat" className={linkClass} onClick={closeMenu}>
                Triage
              </NavLink>
              <NavLink to="/doctors" className={linkClass} onClick={closeMenu}>
                Doctors
              </NavLink>
              <NavLink
                to="/appointments"
                className={linkClass}
                onClick={closeMenu}
              >
                Appointments
              </NavLink>
            </>
          )}
          {isDoctor && (
            <>
              <NavLink
                to="/doctor"
                className={linkClass}
                end
                onClick={closeMenu}
              >
                Schedule
              </NavLink>
              <NavLink
                to="/doctor/slots"
                className={linkClass}
                onClick={closeMenu}
              >
                Slots
              </NavLink>
            </>
          )}

          <div className="nav-user">
            <span className="nav-avatar" aria-hidden="true">
              {(user?.name || '?').charAt(0).toUpperCase()}
            </span>
            <span className="nav-user-text">
              {user?.name}
              <span className="muted"> · {isDoctor ? 'Doctor' : 'Patient'}</span>
            </span>
            <button
              type="button"
              className="btn btn-ghost btn-sm"
              onClick={() => {
                closeMenu()
                logout()
              }}
            >
              Sign out
            </button>
          </div>
        </nav>
      </header>

      <main className="shell-main">
        <PageTransition>
          <Outlet />
        </PageTransition>
      </main>

      <footer className="app-footer">
        Triage guidance supports care decisions and does not replace a medical
        diagnosis.
      </footer>
    </div>
  )
}

function linkClass({ isActive }: { isActive: boolean }) {
  return `nav-link${isActive ? ' active' : ''}`
}
