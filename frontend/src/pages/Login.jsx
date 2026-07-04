import { useAuth } from 'react-oidc-context'

export default function Login() {
  const auth = useAuth()

  return (
    <div className="min-h-screen flex items-center justify-center bg-slate-50 px-4">
      <div className="bg-white shadow-lg rounded-2xl p-10 w-full max-w-md text-center">
        <div className="mx-auto w-12 h-12 rounded-xl bg-indigo-600 flex items-center justify-center text-white text-xl font-bold">
          A
        </div>
        <h1 className="mt-5 text-2xl font-semibold text-slate-800">AuditTrail</h1>
        <p className="mt-1 text-slate-500">Payment Gateway Compliance Console</p>

        <button
          onClick={() => auth.signinRedirect()}
          className="mt-8 w-full rounded-lg bg-indigo-600 px-4 py-3 text-white font-medium hover:bg-indigo-700 transition"
        >
          Sign in with WSO2
        </button>

        <p className="mt-4 text-xs text-slate-400">
          You'll be redirected to the Identity Server to authenticate.
        </p>
      </div>
    </div>
  )
}
