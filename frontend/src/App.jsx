import { useAuth } from 'react-oidc-context'
import Login from './pages/Login'
import Dashboard from './pages/Dashboard'

function Centered({ children }) {
  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 text-slate-600">
      {children}
    </div>
  )
}

export default function App() {
  const auth = useAuth()

  if (auth.isLoading) return <Centered>Loading…</Centered>
  if (auth.error) {
    return (
      <Centered>
        <div className="text-red-600 max-w-md text-center px-4">
          Authentication error: {auth.error.message}
        </div>
      </Centered>
    )
  }
  // Not signed in → the login screen gates the whole app (protected-route behaviour)
  if (!auth.isAuthenticated) return <Login />
  return <Dashboard />
}
