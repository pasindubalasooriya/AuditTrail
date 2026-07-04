import { useEffect, useState } from 'react'
import { api } from '../api/client'

const CARDS = [
  { key: 'totalEvents', label: 'Total Events', accent: 'text-slate-700' },
  { key: 'totalRefunds', label: 'Refunds', accent: 'text-blue-600' },
  { key: 'totalFraudFlags', label: 'Fraud Flags', accent: 'text-amber-600' },
  { key: 'highRiskEvents', label: 'High-Risk', accent: 'text-red-600' },
]

export default function SummaryCards({ token }) {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.getDashboard(token).then(setData).catch((e) => setError(e.message))
  }, [token])

  return (
    <section>
      <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">Summary</h2>
      {error && <p className="text-red-600 text-sm">{error}</p>}
      {!error && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {CARDS.map((c) => (
            <div key={c.key} className="bg-white rounded-xl border border-slate-200 p-5">
              <div className="text-sm text-slate-500">{c.label}</div>
              <div className={`mt-2 text-3xl font-semibold ${c.accent}`}>
                {data ? data[c.key] : '—'}
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
