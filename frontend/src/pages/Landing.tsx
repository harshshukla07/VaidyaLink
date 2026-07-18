import { Link, Navigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

const careAreas = [
  {
    title: 'AI-assisted triage',
    text: 'A guided clinical conversation that helps clarify your symptoms before you meet a doctor.',
  },
  {
    title: 'Specialty direction',
    text: 'Thoughtful routing toward the medical specialty best suited to your presentation.',
  },
  {
    title: 'Physician appointments',
    text: 'Book a confirmed slot with an available doctor when you are ready to be seen.',
  },
]

export function Landing() {
  const { user, loading, isDoctor } = useAuth()

  if (!loading && user) {
    return <Navigate to={isDoctor ? '/doctor' : '/home'} replace />
  }

  return (
    <div className="landing">
      <section className="hero">
        <div className="hero-media" aria-hidden="true">
          <img
            className="hero-photo"
            src="https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=2400&q=80"
            alt=""
          />
          <div className="hero-shade" />
          <div className="hero-vignette" />
        </div>

        <header className="hero-nav animate-fade">
          <Link to="/" className="hero-mark" aria-label="VaidyaLink home">
            <span className="hero-cross" aria-hidden="true" />
            <span>VaidyaLink</span>
          </Link>
          <Link to="/login" className="btn btn-ghost hero-nav-btn">
            Sign in
          </Link>
        </header>

        <div className="hero-body">
          <p className="hero-brand animate-rise">VaidyaLink</p>
          <h1 className="animate-rise delay-1">
            Specialist care, guided from the first symptom.
          </h1>
          <p className="lede animate-rise delay-2">
            A calm, clinical pathway for patients seeking the right physician —
            with assessment support and secure appointment booking.
          </p>
          <div className="hero-actions animate-rise delay-3">
            <Link to="/register" className="btn btn-primary">
              Create an account
            </Link>
            <Link to="/login" className="btn btn-ghost">
              Sign in
            </Link>
          </div>
        </div>

        <svg
          className="hero-pulse"
          viewBox="0 0 1200 80"
          preserveAspectRatio="none"
          aria-hidden="true"
        >
          <path
            className="hero-pulse-line"
            d="M0 40 H180 L200 40 L220 12 L240 68 L260 40 H420 L440 40 L455 22 L470 58 L485 40 H700 L720 40 L740 8 L760 72 L780 40 H1200"
          />
        </svg>
      </section>

      <section className="landing-care">
        <div className="landing-care-inner">
          <header className="landing-care-header">
            <h2>How we support your care</h2>
            <p>
              Built for clarity at every step — from first concern to confirmed
              visit.
            </p>
          </header>

          <ul className="landing-care-list">
            {careAreas.map((item) => (
              <li key={item.title}>
                <h3>{item.title}</h3>
                <p>{item.text}</p>
              </li>
            ))}
          </ul>
        </div>
      </section>

      <footer className="landing-footer">
        Built with ❤️ by Harsh Shukla
      </footer>
    </div>
  )
}
