import { useCallback, useEffect, useState } from 'react'
import { getAdminDashboardSummary } from './adminReportingApi.js'

function StatCard({ label, value, detail }) {
  return <div className="border-l-2 border-cyan-400 py-1 pl-3"><p className="text-sm text-slate-600">{label}</p><p className="mt-1 text-2xl font-semibold tracking-tight text-slate-900">{value}</p>{detail && <p className="mt-1 text-xs text-slate-500">{detail}</p>}</div>
}

function StatSection({ title, stats }) {
  return <section className="rounded-xl border border-slate-200 bg-white p-5 sm:p-6">
    <h2 className="mb-4 text-base font-semibold text-slate-900">{title}</h2>
    <div className="grid gap-x-6 gap-y-5 sm:grid-cols-2 xl:grid-cols-3">{stats.map(stat => <StatCard key={stat.label} {...stat} />)}</div>
  </section>
}

export default function AdminDashboard({ onSessionExpired }) {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setSummary(await getAdminDashboardSummary())
    } catch (requestError) {
      if (requestError.status === 401) onSessionExpired()
      else setError(requestError.status >= 400 && requestError.status < 500
        ? requestError.message : 'Unable to load dashboard statistics. Please try again.')
    } finally { setLoading(false) }
  }, [onSessionExpired])

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => { if (active) void load() }, 0)
    return () => { active = false; window.clearTimeout(timer) }
  }, [load])

  if (loading && !summary) return <section aria-live="polite" className="rounded-2xl border border-slate-200 bg-white p-8 text-slate-600">Loading dashboard statistics…</section>

  return <section aria-labelledby="dashboard-title" className="space-y-5">
    <div className="flex flex-wrap items-end justify-between gap-4 border-b border-slate-200 pb-5">
      <div><p className="text-xs font-semibold uppercase tracking-[0.14em] text-teal-800">CSS NC II · COC 1 and COC 2</p><h1 id="dashboard-title" className="mt-1 text-2xl font-semibold tracking-tight text-slate-950">Admin Dashboard</h1><p className="mt-1 max-w-2xl text-sm leading-6 text-slate-600">Overview of student accounts, assessments, and configured learning stages.</p></div>
      <button type="button" onClick={() => void load()} disabled={loading} className="rounded-md bg-teal-800 px-4 py-2 text-sm font-semibold text-white hover:bg-teal-900 disabled:opacity-60">{loading ? 'Refreshing…' : 'Refresh statistics'}</button>
    </div>
    {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><p>{error}</p><button type="button" onClick={() => void load()} className="mt-2 font-semibold underline">Try again</button></div>}
    {summary && <>
      {summary.students.total === 0 && summary.assessments.total === 0 && summary.learningContent.total === 0 && <p className="rounded-lg border border-slate-200 bg-white p-4 text-sm text-slate-700">No student, assessment, or learning-stage records are configured yet. The figures below reflect the current database.</p>}
      <StatSection title="Student accounts" stats={[
        { label: 'Registered students', value: summary.students.total, accent: 'blue' },
        { label: 'Active', value: summary.students.active },
        { label: 'Pending review', value: summary.students.pending, accent: 'amber' },
        { label: 'Rejected', value: summary.students.rejected, accent: 'amber' },
        { label: 'Deactivated', value: summary.students.deactivated, accent: 'blue' },
      ]} />
      <StatSection title="Assessments" stats={[
        { label: 'Total assessments', value: summary.assessments.total, accent: 'blue' },
        { label: 'Active / published', value: summary.assessments.active },
        { label: 'Draft / inactive', value: summary.assessments.draft, accent: 'amber' },
        { label: 'COC 1 assessments', value: summary.assessments.coc1, accent: 'blue' },
        { label: 'COC 2 assessments', value: summary.assessments.coc2, accent: 'blue' },
        { label: 'Discussion / Generalized', value: `${summary.assessments.discussion} / ${summary.assessments.generalizedPostTests}` },
      ]} />
      <StatSection title="Learning stages" stats={[
        { label: 'Total stages', value: summary.learningContent.total, accent: 'blue' },
        { label: 'Active / published', value: summary.learningContent.active },
        { label: 'Draft / inactive', value: summary.learningContent.draft, accent: 'amber' },
        { label: 'COC 1 stages', value: summary.learningContent.coc1, accent: 'blue' },
        { label: 'COC 2 stages', value: summary.learningContent.coc2, accent: 'blue' },
      ]} />
      <p className="text-xs text-slate-500">Recent activity is not shown because the current schema does not keep a complete activity history.</p>
    </>}
  </section>
}
