import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { AppShell } from './components/AppShell'
import { ProtectedRoute } from './components/ProtectedRoute'
import { ToastProvider } from './components/Toast'
import { Landing } from './pages/Landing'
import { Login } from './pages/Login'
import { Register } from './pages/Register'
import { PatientHome } from './pages/patient/Home'
import { Chat } from './pages/patient/Chat'
import { Doctors } from './pages/patient/Doctors'
import { Book } from './pages/patient/Book'
import { Appointments } from './pages/patient/Appointments'
import { DoctorDashboard } from './pages/doctor/Dashboard'
import { Slots } from './pages/doctor/Slots'

export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<Landing />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />

          <Route
            element={
              <ProtectedRoute>
                <AppShell />
              </ProtectedRoute>
            }
          >
            <Route
              path="/home"
              element={
                <ProtectedRoute role="ROLE_PATIENT">
                  <PatientHome />
                </ProtectedRoute>
              }
            />
            <Route
              path="/chat"
              element={
                <ProtectedRoute role="ROLE_PATIENT">
                  <Chat />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctors"
              element={
                <ProtectedRoute role="ROLE_PATIENT">
                  <Doctors />
                </ProtectedRoute>
              }
            />
            <Route
              path="/book/:doctorId"
              element={
                <ProtectedRoute role="ROLE_PATIENT">
                  <Book />
                </ProtectedRoute>
              }
            />
            <Route
              path="/appointments"
              element={
                <ProtectedRoute role="ROLE_PATIENT">
                  <Appointments />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor"
              element={
                <ProtectedRoute role="ROLE_DOCTOR">
                  <DoctorDashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/doctor/slots"
              element={
                <ProtectedRoute role="ROLE_DOCTOR">
                  <Slots />
                </ProtectedRoute>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
      </ToastProvider>
    </AuthProvider>
  )
}
