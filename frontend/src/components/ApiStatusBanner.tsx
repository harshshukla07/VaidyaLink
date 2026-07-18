import { useEffect, useState } from 'react'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

async function probeApi() {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), 4000)
  try {
    const res = await fetch(`${BASE_URL}/v3/api-docs`, {
      method: 'GET',
      signal: controller.signal,
    })
    return res.ok || (res.status >= 200 && res.status < 500)
  } catch {
    return false
  } finally {
    window.clearTimeout(timer)
  }
}

export function ApiStatusBanner() {
  const [online, setOnline] = useState(true)
  const [checked, setChecked] = useState(false)

  useEffect(() => {
    let active = true

    async function check() {
      const ok = await probeApi()
      if (active) {
        setOnline(ok)
        setChecked(true)
      }
    }

    check()
    const id = window.setInterval(check, 20000)
    const onOnline = () => check()
    window.addEventListener('online', onOnline)
    window.addEventListener('offline', () => {
      if (active) {
        setOnline(false)
        setChecked(true)
      }
    })

    return () => {
      active = false
      window.clearInterval(id)
      window.removeEventListener('online', onOnline)
    }
  }, [])

  if (!checked || online) return null

  return (
    <div className="api-banner" role="alert">
      Can&apos;t reach the VaidyaLink server. Check that the backend is running on
      port 8080.
    </div>
  )
}
