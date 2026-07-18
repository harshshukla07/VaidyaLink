import type {
  Appointment,
  AppointmentStatus,
  AuthUser,
  ChatReply,
  ChatSession,
  Doctor,
  DoctorRegisterPayload,
  Page,
  PatientRegisterPayload,
  SlotGeneratePayload,
} from './types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

class ApiError extends Error {
  status: number

  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

function getToken(): string | null {
  return localStorage.getItem('vl_token')
}

async function request<T>(
  path: string,
  options: RequestInit = {},
  auth = true,
): Promise<T> {
  const headers = new Headers(options.headers)

  if (!headers.has('Content-Type') && options.body) {
    headers.set('Content-Type', 'application/json')
  }

  if (auth) {
    const token = getToken()
    if (token) headers.set('Authorization', `Bearer ${token}`)
  }

  const res = await fetch(`${BASE_URL}${path}`, { ...options, headers })
  const text = await res.text()
  const data = text ? safeParse(text) : null

  if (!res.ok) {
    throw new ApiError(res.status, extractErrorMessage(data, res.status))
  }

  return data as T
}

function safeParse(text: string): unknown {
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function extractErrorMessage(data: unknown, status: number): string {
  if (typeof data === 'string' && data.trim()) return data

  if (data && typeof data === 'object') {
    const obj = data as Record<string, unknown>
    if (typeof obj.error === 'string' && obj.error.trim()) return obj.error
    if (typeof obj.message === 'string' && obj.message.trim()) return obj.message

    // Spring validation: { fieldName: "message", ... }
    const fieldMessages = Object.values(obj).filter(
      (v): v is string => typeof v === 'string' && v.trim().length > 0,
    )
    if (fieldMessages.length > 0) return fieldMessages[0]
  }

  return `Request failed (${status})`
}

export const api = {
  login(email: string, password: string) {
    return request<AuthUser>(
      '/api/auth/login',
      { method: 'POST', body: JSON.stringify({ email, password }) },
      false,
    )
  },

  registerPatient(payload: PatientRegisterPayload) {
    return request<string>(
      '/api/auth/register/patient',
      { method: 'POST', body: JSON.stringify(payload) },
      false,
    )
  },

  registerDoctor(payload: DoctorRegisterPayload) {
    return request<string>(
      '/api/auth/register/doctor',
      { method: 'POST', body: JSON.stringify(payload) },
      false,
    )
  },

  me() {
    return request<AuthUser>('/api/auth/me')
  },

  getChatSession() {
    return request<ChatSession>('/api/chat/session')
  },

  sendChatMessage(sessionId: number, messageText: string) {
    return request<ChatReply>('/api/chat/send', {
      method: 'POST',
      body: JSON.stringify({ sessionId, messageText }),
    })
  },

  getSpecialties() {
    return request<string[]>('/api/doctors/specialties')
  },

  getDoctorsBySpecialty(speciality: string) {
    return request<Doctor[]>(
      `/api/doctors?speciality=${encodeURIComponent(speciality)}`,
    )
  },

  getDoctor(id: number) {
    return request<Doctor>(`/api/doctors/${id}`)
  },

  getAvailableSlots(doctorId: number, date: string) {
    return request<string[]>(
      `/api/appointments/doctor/${doctorId}/available-slots?date=${date}`,
    )
  },

  bookAppointment(payload: {
    patientId: number
    doctorId: number
    appointmentDate: string
    appointmentTime: string
  }) {
    return request<Appointment>('/api/appointments/book', {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },

  getPatientAppointments(patientId: number, page = 0, size = 20) {
    return request<Page<Appointment>>(
      `/api/appointments/patient/${patientId}?page=${page}&size=${size}`,
    )
  },

  getUpcomingAppointments(patientId: number, page = 0, size = 10) {
    return request<Page<Appointment>>(
      `/api/appointments/patient/${patientId}/upcoming?page=${page}&size=${size}`,
    )
  },

  getDoctorAppointments(doctorId: number, date?: string, page = 0, size = 20) {
    const qs = new URLSearchParams({ page: String(page), size: String(size) })
    if (date) qs.set('date', date)
    return request<Page<Appointment>>(
      `/api/appointments/doctor/${doctorId}?${qs}`,
    )
  },

  searchDoctorAppointments(
    doctorId: number,
    query: string,
    page = 0,
    size = 20,
  ) {
    const qs = new URLSearchParams({
      query,
      page: String(page),
      size: String(size),
    })
    return request<Page<Appointment>>(
      `/api/appointments/doctor/${doctorId}/search?${qs}`,
    )
  },

  updateAppointmentStatus(appointmentId: number, status: AppointmentStatus) {
    return request<Appointment>(
      `/api/appointments/${appointmentId}/status?status=${status}`,
      { method: 'PATCH' },
    )
  },

  generateSlots(doctorId: number, payload: SlotGeneratePayload) {
    return request<string>(`/api/doctors/${doctorId}/slots/generate`, {
      method: 'POST',
      body: JSON.stringify(payload),
    })
  },
}

export { ApiError }
