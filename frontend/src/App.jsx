import { useCallback, useEffect, useRef, useState } from 'react'
import { getCurrentAdmin, isAdminSessionEnded, loginAdmin, logoutAdmin } from './adminAuth.js'
import { getCurrentStudent, isStudentSessionEnded, loginStudent, logoutStudent, registerStudent } from './studentAuth.js'
import { restoreSession } from './sessionRestore.js'
import StudentManagement from './StudentManagement.jsx'
import CourseStructureManagement from './CourseStructureManagement.jsx'
import LearningContentManagement from './LearningContentManagement.jsx'
import AssessmentManagement from './AssessmentManagement.jsx'
import StudentLearningDashboard from './StudentLearningDashboard.jsx'
import AdminDashboard from './AdminDashboard.jsx'
import StudentProgress from './StudentProgress.jsx'
import SystemBrandingLayout from './SystemBranding.jsx'

const navigationItems = [
  { id: 'dashboard', label: 'Dashboard' },
  { id: 'students', label: 'Students' },
  { id: 'coc-1', label: 'COC 1' },
  { id: 'coc-2', label: 'COC 2' },
  { id: 'learning-content', label: 'Learning Content' },
  { id: 'assessments', label: 'Assessments' },
  { id: 'student-progress', label: 'Student Progress' },
]

function App() {
  const [admin, setAdmin] = useState(null)
  const [student, setStudent] = useState(null)
  const sessionStateRef = useRef({ admin, student })
  const [checkingSession, setCheckingSession] = useState(true)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [studentAccountId, setStudentAccountId] = useState('')
  const [studentPassword, setStudentPassword] = useState('')
  const [registrationMode, setRegistrationMode] = useState(false)
  const [registrationFullName, setRegistrationFullName] = useState('')
  const [registrationProgram, setRegistrationProgram] = useState('')
  const [registrationPassword, setRegistrationPassword] = useState('')
  const [registrationResult, setRegistrationResult] = useState(null)
  const [loginMode, setLoginMode] = useState('student')
  const [error, setError] = useState('')
  const [studentLoginError, setStudentLoginError] = useState('')
  const [sessionCheckError, setSessionCheckError] = useState('')
  const [busy, setBusy] = useState(false)
  const [activePage, setActivePage] = useState('dashboard')
  const [sidebarOpen, setSidebarOpen] = useState(false)

  useEffect(() => {
    sessionStateRef.current = { admin, student }
  }, [admin, student])

  const checkSession = useCallback(async (showExpiredMessage = false) => {
    try {
      const session = await restoreSession(sessionStateRef.current, { getCurrentAdmin, getCurrentStudent })
      setAdmin(session.admin)
      setStudent(session.student)
      setSessionCheckError('')
      if (!session.admin && !session.student && showExpiredMessage) {
        if (sessionStateRef.current.student) {
          setStudentLoginError('Your Student session has expired. Please log in again.')
          setLoginMode('student')
        } else if (sessionStateRef.current.admin) {
          setLoginMode('admin')
          setError('Your Admin session has expired. Please log in again.')
        }
      }
    } catch (sessionError) {
      setAdmin(null)
      setStudent(null)
      setSessionCheckError(sessionError.status
        ? 'Unable to verify the current session. Please try again.'
        : 'Unable to reach the server to check your session. Please try again when the connection is available.')
    } finally {
      setCheckingSession(false)
    }
  }, [])

  const handleAdminSessionExpired = useCallback(() => {
    setAdmin(null)
    setStudent(null)
    setPassword('')
    setLoginMode('admin')
    setError('Your Admin session has expired. Please log in again.')
  }, [])

  useEffect(() => {
    const timer = window.setTimeout(() => checkSession(), 0)
    return () => window.clearTimeout(timer)
  }, [checkSession])

  useEffect(() => {
    if (!admin && !student) return undefined
    const handleVisibility = () => {
      if (document.visibilityState === 'visible') checkSession(true)
    }
    document.addEventListener('visibilitychange', handleVisibility)
    return () => document.removeEventListener('visibilitychange', handleVisibility)
  }, [admin, student, checkSession])

  async function handleLogin(event) {
    event.preventDefault()
    setBusy(true)
    setError('')
    try {
      const authenticatedAdmin = await loginAdmin(username.trim(), password)
      setAdmin(authenticatedAdmin)
      setStudent(null)
      setPassword('')
      setActivePage('dashboard')
    } catch (loginError) {
      setError(loginError.status === 401
        ? 'Invalid username or password.'
        : loginError.message || 'Unable to log in. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  function selectLoginMode(mode) {
    setLoginMode(mode)
    setError('')
    setStudentLoginError('')
  }

  async function handleStudentLogin(event) {
    event.preventDefault()
    setBusy(true)
    setStudentLoginError('')
    try {
      const result = await loginStudent({
        accountId: studentAccountId.trim(),
        password: studentPassword,
      })
      setStudent(result.student)
      setAdmin(null)
      setStudentPassword('')
      setActivePage('dashboard')
    } catch (loginError) {
      setStudentLoginError(loginError.status === 401
        ? loginError.message || 'Invalid Account ID or password, or the account is not active.'
        : loginError.status === 400
          ? loginError.message || 'Enter your Account ID and password.'
          : loginError.message || 'Unable to log in. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  async function handleStudentRegistration(event) {
    event.preventDefault()
    setBusy(true)
    setStudentLoginError('')
    setRegistrationResult(null)
    try {
      const result = await registerStudent({
        fullName: registrationFullName.trim(),
        program: registrationProgram.trim(),
        password: registrationPassword,
      })
      setRegistrationResult(result)
      setRegistrationPassword('')
      setRegistrationFullName('')
      setRegistrationProgram('')
    } catch (registrationError) {
      setStudentLoginError(registrationError.message || 'Unable to submit registration. Please try again.')
    } finally {
      setBusy(false)
    }
  }

  function openRegistration() {
    setRegistrationMode(true)
    setRegistrationResult(null)
    setStudentLoginError('')
  }

  function backToStudentLogin() {
    setRegistrationMode(false)
    setRegistrationResult(null)
    setStudentLoginError('')
  }

  async function handleLogout() {
    setBusy(true)
    setError('')
    try {
      await logoutAdmin()
    } catch (logoutError) {
      if (logoutError.status !== 401) {
        setError(logoutError.message || 'Unable to log out. Please try again.')
        setBusy(false)
        return
      }
    }

    try {
      const sessionEnded = await isAdminSessionEnded()
      if (!sessionEnded) {
        setError('The server session is still active. Please try logging out again.')
        setBusy(false)
        return
      }
    } catch {
      setError('Unable to verify logout. Please try again; your session may still be active.')
      setBusy(false)
      return
    }

    setAdmin(null)
    setStudent(null)
    setPassword('')
    setSidebarOpen(false)
    setBusy(false)
  }

  async function handleStudentLogout() {
    setBusy(true)
    setStudentLoginError('')
    try {
      await logoutStudent()
      const sessionEnded = await isStudentSessionEnded()
      if (!sessionEnded) {
        setStudentLoginError('The Student session is still active. Please try logging out again.')
        return
      }
      setStudent(null)
      setStudentAccountId('')
      setStudentPassword('')
      setLoginMode('student')
    } catch (logoutError) {
      if (logoutError.status === 401) {
        setStudent(null)
        setLoginMode('student')
      } else {
        setStudentLoginError(logoutError.message || 'Unable to log out. Please try again.')
      }
    } finally {
      setBusy(false)
    }
  }

  const handleStudentSessionExpired = useCallback(() => {
    setStudent(null)
    setStudentAccountId('')
    setStudentPassword('')
    setLoginMode('student')
    setStudentLoginError('Your Student session has expired. Please log in again.')
  }, [])

  if (checkingSession) {
    return (
      <SystemBrandingLayout headerContent={<div className="text-sm font-semibold text-sky-950">CSS NC II Learning System <span className="ml-2 font-normal text-slate-600">Checking session…</span></div>}>
        <main className="grid min-h-[calc(100svh-144px)] place-items-center bg-slate-100 pb-10 text-slate-700 sm:min-h-[calc(100svh-104px)]">Checking your session…</main>
      </SystemBrandingLayout>
    )
  }

  if (student) {
    return (
      <SystemBrandingLayout
        studentMode
        headerContent={(
          <div>
            <p className="truncate text-xs font-semibold text-cyan-200 sm:text-sm">CSS NC II Learning System</p>
            <p className="truncate text-[10px] font-semibold text-slate-200 sm:text-xs">Student Learning Dashboard</p>
          </div>
        )}
      >
        <StudentLearningDashboard student={student} onSessionExpired={handleStudentSessionExpired} sessionError={sessionCheckError} onLogout={handleStudentLogout} logoutBusy={busy} />
      </SystemBrandingLayout>
    )
  }

  if (admin) {
    const currentPage = navigationItems.find(item => item.id === activePage) ?? navigationItems[0]
    return (
      <SystemBrandingLayout
        headerContent={(
          <div className="flex flex-wrap items-center justify-between gap-2">
            <div className="flex min-w-0 items-center gap-3">
              <button
                type="button"
                aria-label="Open navigation menu"
                aria-expanded={sidebarOpen}
                onClick={() => setSidebarOpen(true)}
                className="grid size-10 shrink-0 place-items-center rounded-lg border border-slate-200 text-sky-950 hover:bg-slate-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-700 lg:hidden"
              >
                <span aria-hidden="true" className="text-xl leading-none">☰</span>
              </button>
              <div className="min-w-0">
                <p className="text-xs font-semibold uppercase tracking-wider text-teal-700">Admin Workspace</p>
                <h2 className="truncate text-base font-bold text-slate-900 sm:text-lg">{currentPage.label}</h2>
              </div>
            </div>
            <div className="flex items-center gap-2 sm:gap-3">
              <p className="hidden max-w-56 truncate text-sm font-medium text-slate-600 sm:block" title={admin.username}>{admin.username}</p>
              <button type="button" onClick={handleLogout} disabled={busy} className="shrink-0 rounded-lg border border-slate-300 px-3 py-2 text-xs font-semibold text-sky-950 hover:bg-slate-50 disabled:cursor-wait disabled:opacity-60 sm:text-sm">{busy ? 'Signing out…' : 'Log out'}</button>
            </div>
          </div>
        )}
      >
      <div className="min-h-[calc(100svh-144px)] bg-slate-100 pb-10 text-slate-900 sm:min-h-[calc(100svh-104px)]">
        {sidebarOpen && (
          <button
            type="button"
            aria-label="Close navigation menu"
            className="fixed inset-x-0 bottom-0 top-[112px] z-30 bg-slate-950/50 sm:top-[72px] lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
        )}

        <aside className={`fixed bottom-0 left-0 top-[112px] z-40 flex w-72 flex-col bg-gradient-to-b from-sky-950 to-teal-950 text-white shadow-xl transition-transform duration-200 sm:top-[72px] lg:translate-x-0 ${sidebarOpen ? 'translate-x-0' : '-translate-x-full'}`}>
          <div className="border-b border-white/10 px-6 py-6">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-teal-200">CSS NC II</p>
            <h1 className="mt-1 text-xl font-bold">Admin Workspace</h1>
          </div>

          <nav aria-label="Admin navigation" className="flex-1 space-y-1 px-3 py-5">
            {navigationItems.map(item => {
              const selected = activePage === item.id
              return (
                <button
                  key={item.id}
                  type="button"
                  aria-current={selected ? 'page' : undefined}
                  onClick={() => {
                    setActivePage(item.id)
                    setSidebarOpen(false)
                  }}
                  className={`w-full rounded-lg px-4 py-3 text-left text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-200 ${selected ? 'bg-teal-700 text-white shadow-sm' : 'text-sky-100 hover:bg-white/10 hover:text-white'}`}
                >
                  {item.label}
                </button>
              )
            })}
          </nav>

        </aside>

        <div className="min-h-[calc(100svh-144px)] pb-10 lg:pl-72 sm:min-h-[calc(100svh-104px)]">
          <main className="mx-auto w-full max-w-7xl p-4 sm:p-6 lg:p-8">
            {error && <p role="alert" className="mb-5 rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}
            {activePage === 'students' ? (
              <StudentManagement onSessionExpired={handleAdminSessionExpired} />
            ) : activePage === 'student-progress' ? (
              <StudentProgress onSessionExpired={handleAdminSessionExpired} />
            ) : activePage === 'assessments' ? (
              <AssessmentManagement onSessionExpired={handleAdminSessionExpired} />
            ) : activePage === 'learning-content' ? (
              <LearningContentManagement onSessionExpired={handleAdminSessionExpired} />
            ) : activePage === 'coc-1' || activePage === 'coc-2' ? (
              <CourseStructureManagement
                key={activePage}
                initialCode={activePage === 'coc-1' ? 'COC1' : 'COC2'}
                onSessionExpired={handleAdminSessionExpired}
              />
            ) : activePage === 'dashboard' ? (
              <AdminDashboard onSessionExpired={handleAdminSessionExpired} />
            ) : (
              <section aria-labelledby="placeholder-title" className="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-8">
                <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p>
                <h1 id="placeholder-title" className="mt-2 text-2xl font-bold text-slate-900">{currentPage.label}</h1>
                <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">
                  This section is not implemented yet. Its management tools will appear here when the required backend endpoints are available.
                </p>
              </section>
            )}
          </main>
        </div>
      </div>
      </SystemBrandingLayout>
    )
  }

  return (
    <SystemBrandingLayout
      headerContent={(
        <div>
          <p className="text-sm font-semibold text-teal-700">CSS NC II Learning System</p>
          <p className="text-xs font-semibold text-slate-700 sm:text-sm">{loginMode === 'admin' ? 'Admin Login' : registrationMode ? 'Student Registration' : 'Student Login'}</p>
        </div>
      )}
    >
    <main
      className="grid min-h-[calc(100svh-152px)] place-items-center bg-slate-900 p-4 pb-10 sm:min-h-[calc(100svh-112px)]"
      style={{
        backgroundImage: 'linear-gradient(rgba(8, 35, 61, 0.48), rgba(8, 35, 61, 0.58)), url("/assets/login-background.jpg")',
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
      }}
    >
      <section className="w-full max-w-md rounded-2xl border border-white/30 bg-white/95 p-6 shadow-2xl sm:p-8">
        {loginMode === 'student' ? (
          <section aria-labelledby="student-login-heading">
            <h1 id="student-login-heading" className="text-center text-2xl font-bold text-slate-900">CSS NC II Learning System</h1>
            {registrationMode ? (
              <>
                <h2 className="mt-6 text-lg font-semibold text-slate-800">Create Student Account</h2>
                {registrationResult && <div role="status" className="mt-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900"><p>{registrationResult.message}</p><p className="mt-1 font-semibold">Your Account ID: {registrationResult.accountId}</p>{registrationResult.possibleDuplicate && <p className="mt-1">A similar registration was flagged for Admin review.</p>}</div>}
                {studentLoginError && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{studentLoginError}</p>}
                <form onSubmit={handleStudentRegistration} className="mt-4 space-y-4">
                  <label className="block text-sm font-medium text-slate-700">Full Name<input autoComplete="name" required maxLength={200} value={registrationFullName} onChange={event => setRegistrationFullName(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
                  <label className="block text-sm font-medium text-slate-700">Program<input required maxLength={150} value={registrationProgram} onChange={event => setRegistrationProgram(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
                  <label className="block text-sm font-medium text-slate-700">Password (at least 8 characters)<input type="password" autoComplete="new-password" required minLength={8} maxLength={128} value={registrationPassword} onChange={event => setRegistrationPassword(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
                  <button type="submit" disabled={busy} className="w-full rounded-lg bg-teal-800 px-4 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:cursor-wait disabled:opacity-60">{busy ? 'Submitting…' : 'Register'}</button>
                </form>
                <button type="button" onClick={backToStudentLogin} className="mt-4 w-full text-center text-sm font-semibold text-teal-800 underline underline-offset-4">Back to Student Login</button>
              </>
            ) : (
              <>
                <h2 className="mt-6 text-lg font-semibold text-slate-800">Login:</h2>
                {studentLoginError && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-700">{studentLoginError}</p>}
                {sessionCheckError && <p role="alert" className="mt-4 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">{sessionCheckError}</p>}
                <form onSubmit={handleStudentLogin} className="mt-4 space-y-4">
                  <label className="block text-sm font-medium text-slate-700">Account ID<input autoComplete="username" required value={studentAccountId} onChange={event => setStudentAccountId(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
                  <label className="block text-sm font-medium text-slate-700">Password<input type="password" autoComplete="current-password" required value={studentPassword} onChange={event => setStudentPassword(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
                  <button type="submit" disabled={busy} className="w-full rounded-lg bg-teal-800 px-4 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:cursor-wait focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-800">{busy ? 'Logging in…' : 'Log In'}</button>
                </form>
                <button type="button" onClick={openRegistration} className="mt-4 w-full text-center text-sm font-semibold text-teal-800 underline underline-offset-4">Create Student Account</button>
                <button type="button" onClick={() => selectLoginMode('admin')} className="mt-3 w-full text-center text-sm font-semibold text-teal-800 underline underline-offset-4 hover:text-teal-950">Log in as Admin?</button>
              </>
            )}
          </section>
        ) : (
          <section aria-labelledby="admin-login-heading">
            <h1 id="admin-login-heading" className="text-center text-2xl font-bold text-slate-900">Admin Login</h1>

            {(error || sessionCheckError) && <p role="alert" className="mt-5 rounded-lg bg-red-50 p-3 text-sm text-red-700">{error || sessionCheckError}</p>}

            <form onSubmit={handleLogin} className="mt-6 space-y-4">
              <label className="block text-sm font-medium text-slate-700">
                Username
                <input
                  autoComplete="username"
                  required
                  value={username}
                  onChange={event => setUsername(event.target.value)}
                  className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
                />
              </label>
              <label className="block text-sm font-medium text-slate-700">
                Password
                <input
                  type="password"
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={event => setPassword(event.target.value)}
                  className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20"
                />
              </label>
              <button
                type="submit"
                disabled={busy}
                className="w-full rounded-lg bg-teal-800 px-4 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:cursor-wait disabled:opacity-60 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-800"
              >
                {busy ? 'Logging in…' : 'Log In'}
              </button>
            </form>
            <button
              type="button"
              onClick={() => selectLoginMode('student')}
              className="mt-4 w-full text-center text-sm font-semibold text-teal-800 underline underline-offset-4 hover:text-teal-950 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-800"
            >
              Back to Student Login
            </button>
          </section>
        )}
      </section>
    </main>
    </SystemBrandingLayout>
  )
}

export default App
