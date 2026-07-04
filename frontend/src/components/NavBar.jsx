export default function NavBar({ username, roles, onLogout }) {
  return (
    <header className="bg-white border-b border-slate-200">
      <div className="max-w-6xl mx-auto px-4 h-16 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold">
            A
          </div>
          <span className="text-lg font-semibold text-slate-800">AuditTrail</span>
          <span className="text-slate-300">/</span>
          <span className="text-sm text-slate-500">Compliance Console</span>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-right">
            <div className="text-sm font-medium text-slate-700">{username}</div>
            <div className="text-xs text-slate-400">{roles.join(', ') || 'no roles'}</div>
          </div>
          <button
            onClick={onLogout}
            className="text-sm rounded-lg border border-slate-300 px-3 py-1.5 text-slate-600 hover:bg-slate-50"
          >
            Sign out
          </button>
        </div>
      </div>
    </header>
  )
}
