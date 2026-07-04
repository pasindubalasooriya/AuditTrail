import { useEffect, useState } from 'react'
import { api } from '../api/client'

const riskColor = (r) =>
  r === 'CRITICAL' ? 'bg-red-100 text-red-700' : r === 'HIGH' ? 'bg-amber-100 text-amber-700' : 'bg-slate-100 text-slate-600'

export default function HighRiskPanel({ token }) {
  const [events, setEvents] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.getHighRisk(token).then(setEvents).catch((e) => setError(e.message))
  }, [token])

  return (
    <section>
      <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
        High-Risk Fraud Flags
      </h2>
      <div className="bg-white rounded-xl border border-slate-200 divide-y divide-slate-100">
        {error && <p className="p-5 text-sm text-red-600">{error}</p>}
        {!error && events && events.length === 0 && (
          <p className="p-5 text-sm text-slate-400">No high-risk events.</p>
        )}
        {!error && !events && <p className="p-5 text-sm text-slate-400">Loading…</p>}
        {events &&
          events.map((e) => (
            <div key={e.id} className="p-4 flex items-center justify-between">
              <div>
                <div className="text-sm font-medium text-slate-800">{e.transactionId || '—'}</div>
                <div className="text-xs text-slate-400">
                  by {e.performedBy} · {new Date(e.performedAt).toLocaleString()}
                </div>
              </div>
              <span className={`text-xs font-medium px-2 py-1 rounded ${riskColor(e.riskLevel)}`}>
                {e.riskLevel}
              </span>
            </div>
          ))}
      </div>
    </section>
  )
}
