import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './contexts/AuthContext'
import LoginPage from './pages/LoginPage'
import BoardListPage from './pages/BoardListPage'
import KanbanBoardPage from './pages/KanbanBoardPage'
import TaskDetailPage from './pages/TaskDetailPage'
import RemindersPage from './pages/RemindersPage'

function ProtectedRoute({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <div className="min-h-screen bg-surface" />
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppRoutes() {
  const { user, loading } = useAuth()
  if (loading) return <div className="min-h-screen bg-surface" />

  return (
    <Routes>
      <Route path="/login" element={user ? <Navigate to="/" replace /> : <LoginPage />} />
      <Route path="/" element={<ProtectedRoute><BoardListPage /></ProtectedRoute>} />
      <Route path="/board/:boardId" element={<ProtectedRoute><KanbanBoardPage /></ProtectedRoute>} />
      <Route path="/board/:boardId/task/:taskId" element={<ProtectedRoute><TaskDetailPage /></ProtectedRoute>} />
      <Route path="/reminders" element={<ProtectedRoute><RemindersPage /></ProtectedRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
