import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api, ApiError } from '../../api/client'
import type { ChatMessage, Doctor } from '../../api/types'

interface UiMessage {
  key: string
  senderType: 'PATIENT' | 'AI' | string
  messageText: string
}

const STARTERS = [
  'I have had a persistent headache for 3 days',
  'I feel chest tightness when I walk upstairs',
  'My child has fever and a sore throat',
]

export function Chat() {
  const [sessionId, setSessionId] = useState<number | null>(null)
  const [messages, setMessages] = useState<UiMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(true)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const [specialty, setSpecialty] = useState<string | null>(null)
  const [doctors, setDoctors] = useState<Doctor[]>([])
  const [complete, setComplete] = useState(false)
  const bottomRef = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLInputElement>(null)

  useEffect(() => {
    api
      .getChatSession()
      .then((session) => {
        setSessionId(session.sessionId)
        setMessages(
          (session.existingMessages || []).map((m: ChatMessage) => ({
            key: `m-${m.id}`,
            senderType: m.senderType,
            messageText: m.messageText,
          })),
        )
      })
      .catch((err) => {
        setError(err instanceof ApiError ? err.message : 'Could not open chat')
      })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, sending])

  useEffect(() => {
    if (!loading && !complete) inputRef.current?.focus()
  }, [loading, complete, sending])

  async function sendText(text: string) {
    if (!sessionId || !text.trim() || sending || complete) return

    setInput('')
    setError('')
    setSending(true)
    setMessages((prev) => [
      ...prev,
      { key: `local-${Date.now()}`, senderType: 'PATIENT', messageText: text },
    ])

    try {
      const reply = await api.sendChatMessage(sessionId, text)
      setMessages((prev) => [
        ...prev,
        {
          key: `ai-${Date.now()}`,
          senderType: 'AI',
          messageText: reply.aiReply,
        },
      ])
      if (reply.triageComplete) {
        setComplete(true)
        setSpecialty(reply.recommendedSpecialty)
        setDoctors(reply.recommendedDoctors || [])
      }
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Message failed')
    } finally {
      setSending(false)
    }
  }

  async function onSend(e: FormEvent) {
    e.preventDefault()
    await sendText(input.trim())
  }

  if (loading) {
    return <div className="loading-screen">Opening your triage session…</div>
  }

  return (
    <div className="stack-lg page-enter">
      <header className="page-header">
        <h1>Symptom triage</h1>
        <p>
          Describe what you&apos;re experiencing. We&apos;ll guide you toward
          the right specialty, then you can book.
        </p>
      </header>

      {error && <div className="error-banner">{error}</div>}

      {complete && (
        <div className="success-banner chat-complete-banner">
          Triage complete
          {specialty ? ` — ${specialty} recommended.` : '.'} Choose a doctor
          below or{' '}
          <Link to="/doctors" style={{ fontWeight: 600, color: 'inherit' }}>
            browse all
          </Link>
          .
        </div>
      )}

      <div className="chat-layout">
        <div className="panel chat-panel">
          <div className="chat-messages">
            {messages.length === 0 && !sending && (
              <div className="chat-empty">
                <p>Start with a short description of your main concern.</p>
                <div className="starter-row">
                  {STARTERS.map((s) => (
                    <button
                      key={s}
                      type="button"
                      className="starter-chip"
                      disabled={!sessionId || sending}
                      onClick={() => sendText(s)}
                    >
                      {s}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {messages.map((m) => (
              <div
                key={m.key}
                className={`bubble ${
                  m.senderType === 'PATIENT' || m.senderType === 'USER'
                    ? 'user'
                    : 'ai'
                }`}
              >
                {m.messageText}
              </div>
            ))}
            {sending && (
              <div className="bubble ai typing" aria-label="Assistant is typing">
                <span />
                <span />
                <span />
              </div>
            )}
            <div ref={bottomRef} />
          </div>

          <form className="chat-composer" onSubmit={onSend}>
            <input
              ref={inputRef}
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder={
                complete
                  ? 'Triage complete — you can book a doctor now'
                  : 'Describe your symptoms…'
              }
              disabled={sending || !sessionId || complete}
              aria-label="Message"
            />
            <button
              className="btn btn-primary"
              disabled={sending || !input.trim() || !sessionId || complete}
            >
              Send
            </button>
          </form>
        </div>

        <aside className="panel stack">
          <div>
            <h2 style={{ fontSize: '1.15rem' }}>Recommendation</h2>
            <p className="muted small" style={{ marginTop: '0.35rem' }}>
              {complete
                ? 'Matching doctors for your recommended specialty.'
                : 'Keep answering until a specialty can be recommended.'}
            </p>
            {specialty && <span className="specialty-pill">{specialty}</span>}
          </div>

          {doctors.length > 0 ? (
            <div className="list">
              {doctors.map((d) => (
                <div
                  key={d.id}
                  className="list-item panel-tight"
                  style={{ padding: '0.85rem' }}
                >
                  <div className="list-item-main">
                    <h3>{d.name}</h3>
                    <p>
                      {d.speciality} · {d.experience} yrs
                    </p>
                  </div>
                  <Link to={`/book/${d.id}`} className="btn btn-accent btn-sm">
                    Book
                  </Link>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted small">
              Or{' '}
              <Link
                to="/doctors"
                style={{ color: 'var(--teal)', fontWeight: 500 }}
              >
                browse all doctors
              </Link>{' '}
              by specialty.
            </p>
          )}
        </aside>
      </div>
    </div>
  )
}
