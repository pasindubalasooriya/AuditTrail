// Thin wrapper around the AuditTrail backend. Callers pass the OIDC access_token as
// the bearer credential (see Dashboard.jsx).

const BASE = 'http://localhost:8080/api/audit'

async function request(path, token, options = {}) {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
  })

  if (!res.ok) {
    let body
    try {
      body = await res.json()
    } catch {
      body = { error: res.statusText }
    }
    const err = new Error(body.error || `Request failed (${res.status})`)
    err.status = res.status
    throw err
  }

  if (res.status === 204) return null
  return res.json()
}

export const api = {
  getEvents: (token) => request('/events', token),
  getHighRisk: (token) => request('/high-risk', token),
  getDashboard: (token) => request('/dashboard', token),
  search: (token, body) => request('/search', token, { method: 'POST', body: JSON.stringify(body) }),
  recordRefund: (token, body) => request('/refund', token, { method: 'POST', body: JSON.stringify(body) }),
  recordFraudFlag: (token, body) => request('/fraud-flag', token, { method: 'POST', body: JSON.stringify(body) }),
}
