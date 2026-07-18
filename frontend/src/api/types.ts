export type Role = 'ROLE_PATIENT' | 'ROLE_DOCTOR'

export type AppointmentStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED' | 'COMPLETED'

export interface AuthUser {
  token?: string
  role: Role
  id: number
  name: string
  email: string
}

export interface Doctor {
  id: number
  name: string
  email: string
  speciality: string
  experience: number
}

export interface ChatMessage {
  id: number
  senderType: string
  messageText: string
}

export interface ChatSession {
  sessionId: number
  existingMessages: ChatMessage[]
}

export interface ChatReply {
  aiReply: string
  triageComplete: boolean
  recommendedSpecialty: string | null
  recommendedDoctors: Doctor[] | null
}

export interface Appointment {
  appointmentId: number
  patientId: number
  patientName: string
  doctorId: number
  doctorName: string
  appointmentDate: string
  appointmentTime: string
  status: AppointmentStatus
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  last: boolean
}

export interface PatientRegisterPayload {
  name: string
  email: string
  mobile: string
  gender: string
  age: number
  password: string
}

export interface DoctorRegisterPayload {
  name: string
  email: string
  speciality: string
  experience: number
  password: string
}

export interface SlotGeneratePayload {
  date: string
  shiftStartTime: string
  shiftEndTime: string
  durationInMinutes: number
}
