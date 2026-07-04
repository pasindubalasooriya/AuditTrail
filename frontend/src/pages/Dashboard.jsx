import { useAuth } from 'react-oidc-context'
import NavBar from '../components/NavBar'
import SummaryCards from '../components/SummaryCards'
import Actions from '../components/Actions'
import HighRiskPanel from '../components/HighRiskPanel'
import EventsTable from '../components/EventsTable'

// WSO2 emits roles as either a JSON array (org-audience roles) or a
// comma-delimited string (application-audience roles) depending on config.
function parseRoles(profile) {
  const r = profile?.roles
  if (!r) return []
  if (Array.isArray(r)) return r.map((s) => String(s).trim()).filter(Boolean)
  return String(r).split(',').map((s) => s.trim()).filter(Boolean)
}

export default function Dashboard() {
  const auth = useAuth()
  const profile = auth.user?.profile || {}
  // The access_token (not the id_token) is the correct bearer credential for API
  // calls; WSO2 is configured to carry the same roles claim on it.
  const token = auth.user?.access_token
  const username = profile.sub
  const roles = parseRoles(profile)
  const isAnalyst = roles.includes('FRAUD_ANALYST')
  const isOfficer = roles.includes('COMPLIANCE_OFFICER')

  return (
    <div className="min-h-screen bg-slate-50">
      <NavBar username={username} roles={roles} onLogout={() => auth.removeUser()} />
      <main className="max-w-6xl mx-auto px-4 py-8 space-y-10">
        {/* Compliance officers see the aggregate summary */}
        {isOfficer && <SummaryCards token={token} />}

        {/* Fraud analysts get the action forms (record refund / flag fraud) */}
        {isAnalyst && <Actions token={token} />}

        {/* Shared reads */}
        <HighRiskPanel token={token} />
        <EventsTable token={token} />
      </main>
    </div>
  )
}
