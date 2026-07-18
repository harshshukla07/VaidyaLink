import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from 'react'

type ToastKind = 'success' | 'error' | 'info'

interface ToastItem {
  id: number
  kind: ToastKind
  message: string
}

interface ToastContextValue {
  push: (message: string, kind?: ToastKind) => void
}

const ToastContext = createContext<ToastContextValue | null>(null)

export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([])

  const push = useCallback((message: string, kind: ToastKind = 'success') => {
    const id = Date.now() + Math.random()
    setItems((prev) => [...prev, { id, kind, message }])
  }, [])

  function dismiss(id: number) {
    setItems((prev) => prev.filter((t) => t.id !== id))
  }

  return (
    <ToastContext.Provider value={{ push }}>
      {children}
      <div className="toast-stack" aria-live="polite" aria-relevant="additions">
        {items.map((t) => (
          <ToastCard key={t.id} item={t} onDone={() => dismiss(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastCard({
  item,
  onDone,
}: {
  item: ToastItem
  onDone: () => void
}) {
  useEffect(() => {
    const timer = window.setTimeout(onDone, 3200)
    return () => window.clearTimeout(timer)
  }, [onDone])

  return (
    <div className={`toast toast-${item.kind}`} role="status">
      <span>{item.message}</span>
      <button type="button" className="toast-close" onClick={onDone} aria-label="Dismiss">
        ×
      </button>
    </div>
  )
}

export function useToast() {
  const ctx = useContext(ToastContext)
  if (!ctx) throw new Error('useToast must be used within ToastProvider')
  return ctx
}
