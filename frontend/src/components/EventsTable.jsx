import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'

const inputCls =
  'rounded-lg border border-slate-300 px-3 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500'

const PAGE_SIZE = 10

export default function EventsTable({ token }) {
  const [filters, setFilters] = useState({ actionType: '', riskLevel: '', performedBy: '' })
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setError(null)
    api
      .search(token, {
        actionType: filters.actionType || null,
        riskLevel: filters.riskLevel || null,
        performedBy: filters.performedBy || null,
        page,
        size: PAGE_SIZE,
      })
      .then(setData)
      .catch((e) => setError(e.message))
  }, [token, filters, page])

  useEffect(() => {
    load()
  }, [load])

  const set = (k) => (e) => {
    setPage(0)
    setFilters({ ...filters, [k]: e.target.value })
  }

  const rows = data?.content ?? []
  const totalPages = data?.totalPages ?? 0
  const totalElements = data?.totalElements ?? 0

  return (
    <section>
      <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">
        Audit Events
      </h2>

      {/* Filters */}
      <div className="flex flex-wrap gap-2 mb-3">
        <select className={inputCls} value={filters.actionType} onChange={set('actionType')}>
          <option value="">All actions</option>
          <option value="REFUND">REFUND</option>
          <option value="FRAUD_FLAG">FRAUD_FLAG</option>
          <option value="MERCHANT_ONBOARD">MERCHANT_ONBOARD</option>
        </select>
        <select className={inputCls} value={filters.riskLevel} onChange={set('riskLevel')}>
          <option value="">Any risk</option>
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
        <input
          className={inputCls}
          placeholder="performed by…"
          value={filters.performedBy}
          onChange={set('performedBy')}
        />
      </div>

      <div className="bg-white rounded-xl border border-slate-200 overflow-hidden">
        {error && <p className="p-5 text-sm text-red-600">{error}</p>}
        {!error && (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500 text-xs uppercase">
                <tr>
                  <th className="text-left px-4 py-3">ID</th>
                  <th className="text-left px-4 py-3">Action</th>
                  <th className="text-left px-4 py-3">By</th>
                  <th className="text-left px-4 py-3">Transaction</th>
                  <th className="text-left px-4 py-3">Amount</th>
                  <th className="text-left px-4 py-3">Risk</th>
                  <th className="text-left px-4 py-3">When</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {rows.map((e) => (
                  <tr key={e.id}>
                    <td className="px-4 py-3 text-slate-400">{e.id}</td>
                    <td className="px-4 py-3 font-medium text-slate-700">{e.actionType}</td>
                    <td className="px-4 py-3">{e.performedBy}</td>
                    <td className="px-4 py-3">{e.transactionId || '—'}</td>
                    <td className="px-4 py-3">{e.amount != null ? e.amount : '—'}</td>
                    <td className="px-4 py-3">{e.riskLevel || '—'}</td>
                    <td className="px-4 py-3 text-slate-400">
                      {new Date(e.performedAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
                {rows.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-4 py-6 text-center text-slate-400">
                      No matching events.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        <div className="flex items-center justify-between px-4 py-3 border-t border-slate-100 text-sm text-slate-500">
          <span>{totalElements} events</span>
          <div className="flex items-center gap-2">
            <button
              className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Prev
            </button>
            <span>
              Page {totalPages === 0 ? 0 : page + 1} / {totalPages}
            </span>
            <button
              className="rounded border border-slate-300 px-2 py-1 disabled:opacity-40"
              onClick={() => setPage((p) => p + 1)}
              disabled={page + 1 >= totalPages}
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </section>
  )
}
