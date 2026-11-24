import { useCallback, useEffect, useState } from 'react'
import { getStudentProgress, getStudentProgressPage, getStudentProgressPrograms } from './adminReportingApi.js'
import { attemptResultText, progressPercentage, submittedAttemptCount } from './adminReportingView.js'

const STATUS_OPTIONS = ['ACTIVE', 'PENDING', 'REJECTED', 'DEACTIVATED']
const STAGE_LABELS = {
  PRE_TEST: 'Pre-test', DEFINITIONS: 'Definitions', EXPLANATION: 'Explanation',
  EXAMPLES: 'Examples', VIDEO_LESSONS: 'Video lessons', ACTIVITY: 'Activity', POST_TEST: 'Discussion Post-test',
}
const ASSESSMENT_LABELS = {
  PRE_TEST: 'Pre-test', ACTIVITY: 'Activity assessment', DISCUSSION_POST_TEST: 'Discussion Post-test',
  GENERALIZED_POST_TEST: 'Generalized Post-test',
}
const PAGE_SIZE = 20

function ProgressBar({ completed, total }) {
  const percentage = progressPercentage(completed, total)
  return <div className="min-w-32"><div className="flex justify-between gap-2 text-xs text-slate-600"><span>{completed}/{total} stages</span><span>{percentage}%</span></div><div className="mt-1 h-2 overflow-hidden rounded-full bg-slate-200"><div className="h-full rounded-full bg-teal-700" style={{ width: `${percentage}%` }} /></div></div>
}

function AttemptRow({ attempt }) {
  return <li className="flex flex-wrap justify-between gap-x-4 gap-y-1 border-t border-slate-100 py-2 text-sm"><span>Attempt {attempt.attemptNumber} · {attempt.status.replaceAll('_', ' ')}</span><span className="text-slate-600">{attemptResultText(attempt)}</span></li>
}

function AssessmentCard({ assessment }) {
  return <div className="rounded-lg border border-slate-200 p-3">
    <div className="flex flex-wrap items-center justify-between gap-2"><p className="font-semibold text-slate-800">{ASSESSMENT_LABELS[assessment.assessmentType] || assessment.assessmentType}</p>
      {assessment.configured ? <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${assessment.active ? 'bg-emerald-100 text-emerald-800' : 'bg-amber-100 text-amber-800'}`}>{assessment.active ? 'Active' : 'Draft / inactive'}</span> : <span className="text-xs text-slate-500">Not configured</span>}
    </div>
    {assessment.configured && <><p className="mt-1 text-xs text-slate-600">{assessment.title} · {submittedAttemptCount(assessment.attempts)} submitted attempt(s)</p>{assessment.attempts.length > 0 && <ul className="mt-2">{assessment.attempts.map(attempt => <AttemptRow key={attempt.id} attempt={attempt} />)}</ul>}</>}
  </div>
}

function CompetencyDetail({ competency }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
    <div className="flex flex-wrap items-start justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-teal-700">{competency.code}</p><h3 className="mt-1 text-xl font-bold text-slate-900">{competency.title}</h3></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${competency.generalizedPostTestPassed ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}`}>{competency.generalizedPostTestPassed ? 'Competency passed' : competency.status.replaceAll('_', ' ')}</span></div>
    <div className="mt-4 grid gap-3 text-sm sm:grid-cols-3"><p><span className="font-semibold">Discussions:</span> {competency.completedDiscussions} of {competency.totalDiscussions} completed</p><p><span className="font-semibold">Stages:</span> {competency.completedStages} of {competency.totalStages} completed</p><p><span className="font-semibold">Generalized Post-test:</span> {competency.generalizedAttemptsUsed} submitted · {competency.generalizedAttemptsRemaining} attempts remaining</p></div>
    <div className="mt-4"><h4 className="mb-2 font-semibold text-slate-800">Generalized Post-test attempts</h4><AssessmentCard assessment={competency.generalizedPostTest} /></div>
    <div className="mt-5 space-y-3"><h4 className="font-semibold text-slate-800">Discussion breakdown</h4>
      {competency.discussions.length === 0 && <p className="text-sm text-slate-600">No Discussions are configured for this competency.</p>}
      {competency.discussions.map((discussion, index) => <details key={discussion.id} className="rounded-xl border border-slate-200" open={index === 0}>
        <summary className="flex cursor-pointer list-none flex-wrap items-center justify-between gap-3 p-4"><span><span className="block text-xs font-bold uppercase tracking-wider text-teal-700">Discussion {discussion.displayOrder}</span><span className="mt-1 block font-semibold text-slate-900">{discussion.title}</span></span><span className={`rounded-full px-3 py-1 text-xs font-semibold ${discussion.completed ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}`}>{discussion.completed ? 'Completed' : `${discussion.completedStages} of 7 stages complete`}</span></summary>
        <div className="space-y-4 border-t border-slate-100 p-4">
          <div><h5 className="mb-2 text-sm font-semibold text-slate-800">Learning stages</h5><ul className="grid gap-2 sm:grid-cols-2">{discussion.stages.map(stage => <li key={stage.stageCode} className="flex justify-between gap-2 rounded-lg bg-slate-50 px-3 py-2 text-sm"><span>{STAGE_LABELS[stage.stageCode] || stage.stageCode}</span><span className={stage.status === 'COMPLETED' ? 'font-semibold text-emerald-700' : 'text-slate-600'}>{stage.status.replaceAll('_', ' ')}</span></li>)}</ul></div>
          <div><h5 className="mb-2 text-sm font-semibold text-slate-800">Discussion assessments</h5><div className="grid gap-2 lg:grid-cols-3">{discussion.assessments.map(assessment => <AssessmentCard key={assessment.assessmentType} assessment={assessment} />)}</div></div>
        </div>
      </details>)}
    </div>
  </section>
}

export default function StudentProgress({ onSessionExpired }) {
  const [pageData, setPageData] = useState(null)
  const [programs, setPrograms] = useState([])
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [program, setProgram] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [selectedId, setSelectedId] = useState(null)
  const [detail, setDetail] = useState(null)
  const [loading, setLoading] = useState(true)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailError, setDetailError] = useState('')

  useEffect(() => {
    const timer = window.setTimeout(() => setSearch(searchDraft.trim()), 250)
    return () => window.clearTimeout(timer)
  }, [searchDraft])

  const loadPage = useCallback(async () => {
    setLoading(true)
    setError('')
    try { setPageData(await getStudentProgressPage({ search, program, status, page, size: PAGE_SIZE })) }
    catch (requestError) {
      if (requestError.status === 401) onSessionExpired()
      else setError(requestError.status >= 400 && requestError.status < 500 ? requestError.message : 'Unable to load student progress. Please try again.')
    } finally { setLoading(false) }
  }, [onSessionExpired, page, program, search, status])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadPage() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadPage])

  useEffect(() => {
    let active = true
    getStudentProgressPrograms().then(items => { if (active) setPrograms(items) }).catch(requestError => {
      if (active && requestError.status === 401) onSessionExpired()
    })
    return () => { active = false }
  }, [onSessionExpired])

  const selectStudent = useCallback(async (studentId) => {
    setSelectedId(studentId)
    setDetail(null)
    setDetailError('')
    setDetailLoading(true)
    try { setDetail(await getStudentProgress(studentId)) }
    catch (requestError) {
      if (requestError.status === 401) onSessionExpired()
      else setDetailError(requestError.status === 404 ? 'This student record could not be found.' : 'Unable to load this student’s progress. Please try again.')
    } finally { setDetailLoading(false) }
  }, [onSessionExpired])

  function resetPage(update) { setPage(0); update() }

  return <section aria-labelledby="student-progress-title" className="space-y-5">
    <div><p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p><h1 id="student-progress-title" className="mt-1 text-2xl font-bold text-slate-900">Student Progress</h1><p className="mt-2 text-sm text-slate-600">Review recorded stage completion and submitted assessment history for each student.</p></div>
    <div className="grid gap-3 rounded-2xl border border-slate-200 bg-white p-4 shadow-sm md:grid-cols-3">
      <label className="text-sm font-medium text-slate-700">Search name or Account ID<input type="search" value={searchDraft} onChange={event => resetPage(() => setSearchDraft(event.target.value))} placeholder="Search students" className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
      <label className="text-sm font-medium text-slate-700">Program<select value={program} onChange={event => resetPage(() => setProgram(event.target.value))} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"><option value="">All programs</option>{programs.map(item => <option key={item} value={item}>{item}</option>)}</select></label>
      <label className="text-sm font-medium text-slate-700">Account status<select value={status} onChange={event => resetPage(() => setStatus(event.target.value))} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2"><option value="">All statuses</option>{STATUS_OPTIONS.map(item => <option key={item} value={item}>{item}</option>)}</select></label>
    </div>
    {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"><p>{error}</p><button type="button" onClick={() => void loadPage()} className="mt-2 font-semibold underline">Try again</button></div>}
    <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
      {loading && !pageData ? <p aria-live="polite" className="p-6 text-sm text-slate-600">Loading student progress…</p> : !pageData || pageData.content.length === 0 ? <p className="p-6 text-sm text-slate-600">{loading ? 'Updating results…' : 'No students match these filters.'}</p> : <>
        <div className="overflow-x-auto"><table className="min-w-full divide-y divide-slate-200 text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-600"><tr><th className="px-4 py-3">Student</th><th className="px-4 py-3">Program</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">COC 1 progress</th><th className="px-4 py-3">COC 2 progress</th><th className="px-4 py-3">Detail</th></tr></thead><tbody className="divide-y divide-slate-100">{pageData.content.map(row => <tr key={row.id} className={selectedId === row.id ? 'bg-teal-50/60' : ''}><td className="whitespace-nowrap px-4 py-3"><span className="block font-semibold text-slate-900">{row.fullName}</span><span className="text-xs text-slate-600">{row.accountId}</span></td><td className="whitespace-nowrap px-4 py-3 text-slate-700">{row.program}</td><td className="whitespace-nowrap px-4 py-3"><span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold">{row.status}</span></td><td className="px-4 py-3"><ProgressBar completed={row.coc1.completedStages} total={row.coc1.totalStages} /></td><td className="px-4 py-3"><ProgressBar completed={row.coc2.completedStages} total={row.coc2.totalStages} /></td><td className="px-4 py-3"><button type="button" onClick={() => void selectStudent(row.id)} className="rounded-lg border border-teal-800 px-3 py-2 text-xs font-semibold text-teal-900 hover:bg-teal-50">View progress</button></td></tr>)}</tbody></table></div>
        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 px-4 py-3 text-sm text-slate-600"><span>{pageData.totalElements} student(s) · Page {pageData.totalPages ? pageData.page + 1 : 0} of {pageData.totalPages}</span><div className="flex gap-2"><button type="button" disabled={loading || pageData.page <= 0} onClick={() => setPage(value => Math.max(0, value - 1))} className="rounded-lg border border-slate-300 px-3 py-2 disabled:opacity-50">Previous</button><button type="button" disabled={loading || pageData.page + 1 >= pageData.totalPages} onClick={() => setPage(value => value + 1)} className="rounded-lg border border-slate-300 px-3 py-2 disabled:opacity-50">Next</button></div></div>
      </>}
    </div>
    {selectedId && <section aria-labelledby="student-detail-heading" className="space-y-4">
      {detailLoading && <p aria-live="polite" className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Loading student detail…</p>}
      {detailError && <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{detailError}</p>}
      {detail && <><div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><h2 id="student-detail-heading" className="text-xl font-bold text-slate-900">{detail.student.fullName}</h2><p className="mt-1 text-sm text-slate-600">{detail.student.accountId} · {detail.student.program} · {detail.student.status}</p></div>{detail.competencies.map(competency => <CompetencyDetail key={competency.id} competency={competency} />)}</>}
    </section>}
  </section>
}
