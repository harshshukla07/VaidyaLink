import type { AppointmentStatus } from '../api/types'

const classMap: Record<AppointmentStatus, string> = {
  PENDING: 'badge-pending',
  CONFIRMED: 'badge-confirmed',
  CANCELLED: 'badge-cancelled',
  COMPLETED: 'badge-completed',
}

export function StatusBadge({ status }: { status: AppointmentStatus }) {
  return <span className={`badge ${classMap[status]}`}>{status}</span>
}
