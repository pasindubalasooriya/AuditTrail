import { useState } from 'react'
import { api } from '../api/client'

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="text-xs text-slate-500">{label}</span>
      {children}
    </label>
  )
}

const inputCls =
  'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500'

function Result({ result }) {
  if (!result) return null
  return (
    <p className={`mt-3 text-sm ${result.ok ? 'text-green-600' : 'text-red-600'}`}>
      {result.message}
    </p>
  )
}

function RefundForm({ token }) {
  const [form, setForm] = useState({ transactionId: '', merchantId: '', amount: '', notes: '' })
  const [result, setResult] = useState(null)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setResult(null)
    try {
      const saved = await api.recordRefund(token, {
        transactionId: form.transactionId,
        merchantId: form.merchantId,
        amount: form.amount === '' ? null : Number(form.amount),
        notes: form.notes || null,
      })
      setResult({ ok: true, message: `Recorded refund #${saved.id} by ${saved.performedBy}` })
      setForm({ transactionId: '', merchantId: '', amount: '', notes: '' })
    } catch (err) {
      setResult({ ok: false, message: err.message })
    }
  }

  return (
    <form onSubmit={submit} className="bg-white rounded-xl border border-slate-200 p-5">
      <h3 className="font-medium text-slate-800">Record a refund</h3>
      <div className="mt-4 grid grid-cols-2 gap-3">
        <Field label="Transaction ID">
          <input className={inputCls} value={form.transactionId} onChange={set('transactionId')} required />
        </Field>
        <Field label="Merchant ID">
          <input className={inputCls} value={form.merchantId} onChange={set('merchantId')} required />
        </Field>
        <Field label="Amount">
          <input className={inputCls} type="number" step="0.01" min="0" value={form.amount} onChange={set('amount')} required />
        </Field>
        <Field label="Notes">
          <input className={inputCls} value={form.notes} onChange={set('notes')} />
        </Field>
      </div>
      <button className="mt-4 rounded-lg bg-indigo-600 px-4 py-2 text-white text-sm font-medium hover:bg-indigo-700">
        Submit refund
      </button>
      <Result result={result} />
    </form>
  )
}

function FraudFlagForm({ token }) {
  const [form, setForm] = useState({ transactionId: '', merchantId: '', riskLevel: 'HIGH', notes: '' })
  const [result, setResult] = useState(null)
  const set = (k) => (e) => setForm({ ...form, [k]: e.target.value })

  const submit = async (e) => {
    e.preventDefault()
    setResult(null)
    try {
      const saved = await api.recordFraudFlag(token, {
        transactionId: form.transactionId,
        merchantId: form.merchantId || null,
        riskLevel: form.riskLevel,
        notes: form.notes || null,
      })
      setResult({ ok: true, message: `Flagged ${saved.transactionId} (${saved.riskLevel})` })
      setForm({ transactionId: '', merchantId: '', riskLevel: 'HIGH', notes: '' })
    } catch (err) {
      setResult({ ok: false, message: err.message })
    }
  }

  return (
    <form onSubmit={submit} className="bg-white rounded-xl border border-slate-200 p-5">
      <h3 className="font-medium text-slate-800">Flag a fraudulent transaction</h3>
      <div className="mt-4 grid grid-cols-2 gap-3">
        <Field label="Transaction ID">
          <input className={inputCls} value={form.transactionId} onChange={set('transactionId')} required />
        </Field>
        <Field label="Merchant ID">
          <input className={inputCls} value={form.merchantId} onChange={set('merchantId')} />
        </Field>
        <Field label="Risk level">
          <select className={inputCls} value={form.riskLevel} onChange={set('riskLevel')}>
            <option>LOW</option>
            <option>MEDIUM</option>
            <option>HIGH</option>
            <option>CRITICAL</option>
          </select>
        </Field>
        <Field label="Notes">
          <input className={inputCls} value={form.notes} onChange={set('notes')} />
        </Field>
      </div>
      <button className="mt-4 rounded-lg bg-amber-600 px-4 py-2 text-white text-sm font-medium hover:bg-amber-700">
        Flag fraud
      </button>
      <Result result={result} />
    </form>
  )
}

export default function Actions({ token }) {
  return (
    <section>
      <h2 className="text-xs font-semibold text-slate-500 uppercase tracking-wide mb-3">Actions</h2>
      <div className="grid md:grid-cols-2 gap-4">
        <RefundForm token={token} />
        <FraudFlagForm token={token} />
      </div>
    </section>
  )
}
