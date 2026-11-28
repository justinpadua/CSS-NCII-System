import { useCallback, useEffect, useMemo, useState } from 'react'
import { studentApiRequest } from './studentAuth.js'
import SafeMarkdown from './SafeMarkdown.jsx'
import YouTubeVideoPlayers from './YouTubeVideoPlayers.jsx'
import { completedStageCount, overallProgressPercent } from './studentDashboardMetrics.js'
import { isMatchingOptionDisabled, matchingSelectionsWithChange } from './matchingQuestionState.js'

const stageTitles = {
  PRE_TEST: 'Pre-test', DEFINITIONS: 'Definition of Terms', EXPLANATION: 'Explanation',
  EXAMPLES: 'Examples', VIDEO_LESSONS: 'Video Lessons', ACTIVITY: 'Activity', POST_TEST: 'Discussion Post-test',
}
const stageOrder = Object.keys(stageTitles)
const assessmentType = { PRE_TEST: 'PRE_TEST', ACTIVITY: 'ACTIVITY', POST_TEST: 'DISCUSSION_POST_TEST' }
const statusLabel = { LOCKED: 'Locked', AVAILABLE: 'Available', IN_PROGRESS: 'In Progress', COMPLETED: 'Completed', PASSED: 'Passed', FAILED: 'Attempts used' }

function statusStyle(status) {
  if (status === 'COMPLETED' || status === 'PASSED') return 'bg-emerald-100 text-emerald-800'
  if (status === 'IN_PROGRESS' || status === 'AVAILABLE') return 'bg-sky-100 text-sky-900'
  return 'bg-slate-100 text-slate-600'
}

function errorText(error) {
  if (error.status === 401 || error.status === 403) return 'Your Student session ended. Please log in again.'
  return error.message || 'The learning request could not be completed. Please try again.'
}

export default function StudentLearningDashboard({ student, onSessionExpired, sessionError, onLogout, logoutBusy }) {
  const [dashboard, setDashboard] = useState(null)
  const [view, setView] = useState({ page: 'dashboard' })
  const [mobileNavOpen, setMobileNavOpen] = useState(false)
  const [expandedCompetencies, setExpandedCompetencies] = useState({})
  const [expandedDiscussions, setExpandedDiscussions] = useState({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [stageContent, setStageContent] = useState(null)
  const [attempt, setAttempt] = useState(null)
  const [questions, setQuestions] = useState([])
  const [answers, setAnswers] = useState({})
  const [assessmentResult, setAssessmentResult] = useState(null)
  const [assessmentBusy, setAssessmentBusy] = useState(false)
  const [generalizedStatus, setGeneralizedStatus] = useState(null)
  const [attemptHistory, setAttemptHistory] = useState([])

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      setDashboard(await studentApiRequest('/api/student/learning/dashboard'))
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
      else setError(errorText(requestError))
    } finally {
      setLoading(false)
    }
  }, [onSessionExpired])

  useEffect(() => {
    let cancelled = false
    studentApiRequest('/api/student/learning/dashboard')
      .then(data => { if (!cancelled) setDashboard(data) })
      .catch(requestError => {
        if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
        else if (!cancelled) setError(errorText(requestError))
      })
      .finally(() => { if (!cancelled) setLoading(false) })
    return () => { cancelled = true }
  }, [onSessionExpired])

  const selectedCompetency = useMemo(() => dashboard?.competencies.find(item => item.id === view.competencyId), [dashboard, view])
  const selectedDiscussion = selectedCompetency?.discussions.find(item => item.id === view.discussionId)
  const selectedStage = selectedDiscussion?.stages.find(item => item.stageCode === view.stageCode)

  useEffect(() => {
    if (view.page !== 'stage' || !dashboard || !selectedCompetency || !selectedDiscussion || !selectedStage) return undefined
    let cancelled = false
    async function loadStage() {
      setLoading(true)
      setError('')
      setStageContent(null)
      setAttempt(null)
      setQuestions([])
      setAnswers({})
      let assessmentRequestStarted = false
      try {
        const path = `/api/student/learning/competencies/${selectedCompetency.id}/discussions/${selectedDiscussion.id}/stages/${view.stageCode}/open`
        const content = await studentApiRequest(path, { method: 'POST' })
        if (cancelled) return
        setStageContent(content)
        if (assessmentType[view.stageCode] && view.stageStatus !== 'COMPLETED') {
          assessmentRequestStarted = true
          const summary = await studentApiRequest(`/api/student/assessments/discussions/${selectedDiscussion.id}/${assessmentType[view.stageCode]}/attempts`, { method: 'POST' })
          if (cancelled) return
          setAttempt(summary)
          const prompts = await studentApiRequest(`/api/student/assessments/attempts/${summary.attemptId}/questions`)
          const saved = await studentApiRequest(`/api/student/assessments/attempts/${summary.attemptId}/answers`)
          if (!cancelled) {
            setQuestions(prompts)
            setAnswers(Object.fromEntries(saved.map(row => [row.questionId, JSON.parse(row.responseData)])))
          }
        }
      } catch (requestError) {
        if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
        else if (!cancelled && assessmentRequestStarted && requestError.status === 404) {
          setError('The learning stage opened, but its assessment is not configured yet. An Admin must add an active assessment and questions in Assessment Management.')
        } else if (!cancelled && assessmentRequestStarted && requestError.status === 409) {
          setError('The learning stage opened, but its assessment is not ready. Ask an Admin to review its active status and questions.')
        } else if (!cancelled) setError(errorText(requestError))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadStage()
    return () => { cancelled = true }
  }, [view, dashboard, selectedCompetency, selectedDiscussion, selectedStage, onSessionExpired])

  useEffect(() => {
    if (view.page !== 'generalized' || !selectedCompetency) return undefined
    let cancelled = false
    async function loadStatus() {
      setLoading(true)
      setError('')
      setAttempt(null)
      setQuestions([])
      setAnswers({})
      try {
        const [status, history] = await Promise.all([
          studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/status`),
          studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/history`),
        ])
        if (!cancelled) { setGeneralizedStatus(status); setAttemptHistory(history) }
        if (status.hasInProgressAttempt) {
          const summary = await studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/attempts`, { method: 'POST' })
          if (cancelled) return
          setAttempt(summary)
          const prompts = await studentApiRequest(`/api/student/assessments/generalized-attempts/${summary.attemptId}/questions`)
          const saved = await studentApiRequest(`/api/student/assessments/generalized-attempts/${summary.attemptId}/answers`)
          if (!cancelled) {
            setQuestions(prompts)
            setAnswers(Object.fromEntries(saved.map(row => [row.questionId, JSON.parse(row.responseData)])))
          }
        }
      } catch (requestError) {
        if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
        else if (requestError.status === 404 && !cancelled) {
          setGeneralizedStatus(null)
          setAttemptHistory([])
        }
        else if (!cancelled) setError(errorText(requestError))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    loadStatus()
    return () => { cancelled = true }
  }, [view, selectedCompetency, onSessionExpired])

  async function openGeneralizedTest() {
    setAssessmentBusy(true)
    setError('')
    try {
      const summary = await studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/attempts`, { method: 'POST' })
      setAttempt(summary)
      setAssessmentResult(null)
      setQuestions(await studentApiRequest(`/api/student/assessments/generalized-attempts/${summary.attemptId}/questions`))
      const saved = await studentApiRequest(`/api/student/assessments/generalized-attempts/${summary.attemptId}/answers`)
      setAnswers(Object.fromEntries(saved.map(row => [row.questionId, JSON.parse(row.responseData)])))
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
      else setError(errorText(requestError))
    } finally { setAssessmentBusy(false) }
  }

  async function saveAnswer(question, value, generalized = false) {
    const responseData = JSON.stringify(value)
    const path = generalized
      ? `/api/student/assessments/generalized-attempts/${attempt.attemptId}/answers`
      : `/api/student/assessments/attempts/${attempt.attemptId}/answers`
    setAnswers(current => ({ ...current, [question.questionId]: value }))
    try {
      await studentApiRequest(path, { method: 'PUT', body: { questionId: question.questionId, responseData } })
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
      else setError(errorText(requestError))
    }
  }

  async function submitAttempt(generalized = false) {
    const typeSpecificQuestions = ['MATCHING_TYPE', 'ENUMERATION']
    if (!attempt || questions.some(question => answers[question.questionId] === undefined
      && !typeSpecificQuestions.includes(question.questionType))) {
      setError('Please answer every question before submitting.')
      return
    }
    setAssessmentBusy(true)
    setError('')
    try {
      const prefix = generalized ? 'generalized-attempts' : 'attempts'
      const answerPath = `/api/student/assessments/${prefix}/${attempt.attemptId}/answers`
      for (const question of questions) {
        const config = question.questionConfig ? JSON.parse(question.questionConfig) : {}
        const response = answers[question.questionId] ?? (question.questionType === 'MATCHING_TYPE'
          ? { matches: question.matchingPrompts.map((_, promptIndex) => ({ promptIndex, choiceId: null })) }
          : question.questionType === 'ENUMERATION'
            ? { answers: Array.from({ length: config.requiredAnswerCount }, () => '') }
            : undefined)
        await studentApiRequest(answerPath, {
          method: 'PUT',
          body: { questionId: question.questionId, responseData: JSON.stringify(response) },
        })
      }
      const result = await studentApiRequest(`/api/student/assessments/${prefix}/${attempt.attemptId}/submit`, { method: 'POST' })
      setAttempt(null)
      if (!generalized) setView(current => ({ ...current, stageStatus: 'COMPLETED' }))
      await loadDashboard()
      setAssessmentResult(result)
      if (generalized) {
        setGeneralizedStatus(await studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/status`))
        setAttemptHistory(await studentApiRequest(`/api/student/assessments/competencies/${selectedCompetency.id}/generalized-post-test/history`))
      }
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
      else setError(errorText(requestError))
    } finally { setAssessmentBusy(false) }
  }

  async function completeStage() {
    setAssessmentBusy(true)
    setError('')
    try {
      await studentApiRequest(`/api/student/learning/competencies/${selectedCompetency.id}/discussions/${selectedDiscussion.id}/stages/${view.stageCode}/complete`, { method: 'POST' })
      await loadDashboard()
      setView({ page: 'discussion', competencyId: selectedCompetency.id, discussionId: selectedDiscussion.id })
    } catch (requestError) {
      if (requestError.status === 401 || requestError.status === 403) onSessionExpired()
      else setError(errorText(requestError))
    } finally { setAssessmentBusy(false) }
  }

  function backToDiscussion() {
    setError('')
    setView({ page: 'discussion', competencyId: selectedCompetency.id, discussionId: selectedDiscussion.id })
  }

  function stageButton(discussion, stage, competency = selectedCompetency, compact = false) {
    const postTestInProgress = discussion.stages.some(row => row.stageCode === 'POST_TEST' && row.status === 'IN_PROGRESS')
    const locked = stage.status === 'LOCKED' || (postTestInProgress && stage.stageCode !== 'POST_TEST')
    return <button key={stage.stageCode} type="button" disabled={locked} onClick={() => { setError(''); setAssessmentResult(null); setView({ page: 'stage', competencyId: competency.id, discussionId: discussion.id, stageCode: stage.stageCode, stageStatus: stage.status }); setMobileNavOpen(false) }} className={compact
      ? `flex w-full items-center justify-between gap-2 rounded-lg px-3 py-2 text-left text-xs transition ${locked ? 'cursor-not-allowed text-slate-500' : 'text-slate-200 hover:bg-white/10 hover:text-white'} ${view.page === 'stage' && view.discussionId === discussion.id && view.stageCode === stage.stageCode ? 'bg-teal-400/15 text-teal-100' : ''}`
      : `flex w-full items-center justify-between gap-3 rounded-xl border p-4 text-left ${locked ? 'cursor-not-allowed border-slate-200 bg-slate-50 opacity-70' : 'border-slate-200 bg-white hover:border-teal-500 hover:shadow-sm'}`}>
      <span className="min-w-0"><span className={`block truncate font-semibold ${compact ? '' : 'text-slate-900'}`}>{stage.title}</span>{!compact && <span className="mt-1 block text-xs text-slate-500">Stage {stageOrder.indexOf(stage.stageCode) + 1} of 7</span>}</span>
      <span aria-label={locked ? 'Locked' : stage.status === 'COMPLETED' ? 'Completed' : undefined} className={`shrink-0 ${compact ? 'text-[11px]' : `rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyle(locked ? 'LOCKED' : stage.status)}`}`}>{locked ? compact ? '🔒' : 'Locked' : compact && stage.status === 'COMPLETED' ? '✓' : compact ? '›' : statusLabel[stage.status] ?? stage.status}</span>
    </button>
  }

  if (loading && !dashboard) return <main className="grid h-full min-h-0 place-items-center bg-slate-950 text-cyan-100">Loading your learning dashboard…</main>

  const progressPercent = overallProgressPercent(dashboard)
  const assessmentStats = [
    { code: 'PRE_TEST', label: 'Pre-tests completed' },
    { code: 'ACTIVITY', label: 'Activities submitted' },
    { code: 'POST_TEST', label: 'Post-tests completed' },
  ]

  return (
    <div className="relative flex h-full min-h-0 w-full overflow-hidden bg-slate-950 text-white" style={{ backgroundImage: 'radial-gradient(ellipse at 75% 0%, rgba(13,148,136,.24), transparent 42%), radial-gradient(ellipse at 10% 80%, rgba(8,145,178,.13), transparent 38%), linear-gradient(135deg, #071522 0%, #0b2231 52%, #092b31 100%)' }}>
      <div aria-hidden="true" className="pointer-events-none absolute inset-0 opacity-[0.12]" style={{ backgroundImage: 'linear-gradient(rgba(103,232,249,.16) 1px, transparent 1px), linear-gradient(90deg, rgba(103,232,249,.16) 1px, transparent 1px)', backgroundSize: '42px 42px', maskImage: 'linear-gradient(to bottom, black, transparent 90%)' }} />
      {mobileNavOpen && <button type="button" aria-label="Close course navigation" className="absolute inset-0 z-30 bg-slate-950/70 backdrop-blur-sm lg:hidden" onClick={() => setMobileNavOpen(false)} />}
      <aside aria-label="Student course navigation" className={`absolute inset-y-0 left-0 z-40 flex w-[min(19rem,88vw)] flex-col border-r border-cyan-200/10 bg-[#071722]/95 shadow-2xl backdrop-blur-xl transition-transform lg:relative lg:inset-auto lg:h-full lg:w-72 lg:shrink-0 lg:translate-x-0 ${mobileNavOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}`}>
        <div className="border-b border-white/10 px-5 py-5">
          <div className="flex items-center gap-3"><div className="grid size-12 shrink-0 place-items-center rounded-2xl border border-cyan-200/30 bg-gradient-to-br from-cyan-400 to-teal-500 text-lg font-black text-slate-950 shadow-lg shadow-cyan-950/40" aria-hidden="true">{(dashboard?.fullName || student.fullName || 'S').trim().charAt(0).toUpperCase()}</div><div className="min-w-0"><p className="truncate text-sm font-bold text-white">{dashboard?.fullName || student.fullName}</p><p className="mt-0.5 text-xs text-cyan-200">Student Account</p></div></div>
          <p className="mt-5 text-[11px] font-bold uppercase tracking-[.2em] text-teal-200">CSS NC II · Learning Portal</p>
        </div>
        <nav aria-label="Student dashboard and courses" className="min-h-0 flex-1 space-y-3 overflow-y-auto px-3 py-4">
          <button type="button" aria-current={view.page === 'dashboard' ? 'page' : undefined} onClick={() => { setView({ page: 'dashboard' }); setMobileNavOpen(false) }} className={`flex w-full items-center gap-3 rounded-xl px-3 py-3 text-left text-sm font-semibold transition ${view.page === 'dashboard' ? 'border border-cyan-300/20 bg-cyan-300/10 text-cyan-100 shadow-inner' : 'text-slate-300 hover:bg-white/5 hover:text-white'}`}><span aria-hidden="true">⌂</span> Dashboard</button>
          <div className="px-3 pt-2 text-[11px] font-bold uppercase tracking-[.2em] text-slate-500">Course navigation</div>
          {(dashboard?.competencies ?? []).map(competency => {
            const expanded = Boolean(expandedCompetencies[competency.id])
            return <section key={competency.id} className="overflow-hidden rounded-xl border border-white/10 bg-white/[.025]">
              <button type="button" aria-expanded={expanded} onClick={() => { setExpandedCompetencies(current => ({ ...current, [competency.id]: !current[competency.id] })); setView({ page: 'competency', competencyId: competency.id }); setMobileNavOpen(false) }} className="flex w-full items-center justify-between gap-2 px-3 py-3 text-left hover:bg-white/[.06]">
                <span className="min-w-0"><span className="block text-sm font-bold text-white">{competency.code.replace('COC', 'COC ')}</span><span className="mt-0.5 block truncate text-[11px] text-slate-400">{competency.completedDiscussions} of {competency.discussionCount} Discussions</span></span><span className="text-cyan-300" aria-hidden="true">{expanded ? '−' : '+'}</span>
              </button>
              {expanded && <div className="space-y-1 border-t border-white/10 px-2 py-2">{competency.discussions.map((discussion, index) => {
                const discussionExpanded = Boolean(expandedDiscussions[discussion.id])
                const discussionLocked = discussion.stages[0]?.status === 'LOCKED'
                return <div key={discussion.id} className="rounded-lg">
                  <button type="button" disabled={discussionLocked} aria-expanded={discussionExpanded} onClick={() => { setExpandedDiscussions(current => ({ ...current, [discussion.id]: !current[discussion.id] })); setView({ page: 'discussion', competencyId: competency.id, discussionId: discussion.id }); setMobileNavOpen(false) }} className={`flex w-full items-start justify-between gap-2 rounded-lg px-2 py-2 text-left text-xs ${discussionLocked ? 'cursor-not-allowed text-slate-600' : 'text-slate-200 hover:bg-white/[.06]'}`}>
                    <span className="min-w-0"><span className="block font-semibold"><span className="text-teal-300">{index + 1}.</span> {discussion.title}</span><span className="mt-1 block text-[10px] text-slate-500">{discussion.completedStages} / 7 stages complete</span></span><span className="shrink-0 text-cyan-300" aria-label={discussionLocked ? 'Locked' : undefined}>{discussionLocked ? '🔒' : discussionExpanded ? '−' : '+'}</span>
                  </button>
                  {discussionExpanded && <div className="ml-2 border-l border-cyan-200/15 pl-2">{discussion.stages.map(stage => stageButton(discussion, stage, competency, true))}</div>}
                </div>
              })}</div>}
            </section>
          })}
        </nav>
        <div className="border-t border-white/10 p-3"><button type="button" onClick={onLogout} disabled={logoutBusy} className="flex w-full items-center justify-center gap-2 rounded-xl border border-rose-300/20 bg-rose-400/[.07] px-4 py-3 text-sm font-semibold text-rose-100 transition hover:border-rose-200/40 hover:bg-rose-400/[.13] disabled:cursor-wait disabled:opacity-60">↪ {logoutBusy ? 'Logging out…' : 'Log out'}</button></div>
      </aside>
      <main className="relative z-10 flex h-full min-h-0 min-w-0 flex-1 flex-col overflow-hidden px-3 pb-9 pt-3 sm:px-5 sm:pb-10 sm:pt-4 xl:px-7">
        <div className={`mx-auto flex h-full min-h-0 w-full max-w-7xl flex-col gap-3 xl:gap-4 ${view.page === 'dashboard' ? 'overflow-hidden' : 'overflow-y-auto overscroll-contain'}`}>
        <div className="flex items-center justify-between gap-3 lg:hidden"><button type="button" aria-label="Open course navigation" aria-expanded={mobileNavOpen} onClick={() => setMobileNavOpen(true)} className="rounded-xl border border-white/15 bg-white/[.06] px-3 py-2 text-sm font-semibold text-cyan-100">☰ <span className="ml-1">Courses</span></button><span className="max-w-[60%] truncate text-right text-xs text-slate-400">{dashboard?.fullName || student.fullName}</span></div>
        {sessionError && <p role="alert" className="rounded-xl border border-amber-200/20 bg-amber-300/10 p-4 text-sm text-amber-100">{sessionError}</p>}
        {error && <p role="alert" className="rounded-xl border border-rose-300/25 bg-rose-400/10 p-4 text-sm text-rose-100">{error}</p>}
        {view.page !== 'dashboard' && <div className="flex shrink-0 flex-wrap items-center gap-2 text-sm">
          {!(view.page === 'stage' && view.stageCode === 'POST_TEST' && stageContent?.status === 'IN_PROGRESS') && <button type="button" onClick={() => { setView({ page: 'dashboard' }); setError('') }} className="font-semibold text-cyan-200 underline underline-offset-2">Dashboard</button>}
          {selectedCompetency && <><span className="text-slate-600">/</span>{view.page === 'stage' && view.stageCode === 'POST_TEST' && stageContent?.status === 'IN_PROGRESS' ? <span className="text-slate-300">{selectedCompetency.code}</span> : <button type="button" onClick={() => setView({ page: 'competency', competencyId: selectedCompetency.id })} className="font-semibold text-cyan-200 underline underline-offset-2">{selectedCompetency.code}</button>}</>}
          {selectedDiscussion && <><span className="text-slate-600">/</span><button type="button" onClick={backToDiscussion} className="max-w-[50vw] truncate font-semibold text-cyan-200 underline underline-offset-2">{selectedDiscussion.title}</button></>}
          {selectedStage && <><span className="text-slate-600">/</span><span className="text-slate-300">{selectedStage.title}</span></>}
        </div>}
        {loading && dashboard && <p role="status" className="rounded-xl border border-white/10 bg-white/[.06] p-4 text-sm text-slate-300">Loading learning data…</p>}

        {view.page === 'dashboard' && <div className="flex min-h-0 flex-1 flex-col gap-3 xl:gap-4">
          <section className="student-dashboard-overview grid shrink-0 gap-3 xl:gap-4 xl:grid-cols-[minmax(270px,.76fr)_minmax(0,1.45fr)]">
            <div className="student-dashboard-progress rounded-3xl border border-cyan-100/15 bg-white/[.07] p-4 shadow-xl shadow-slate-950/15 backdrop-blur-xl sm:p-5 xl:p-6">
              <p className="text-xs font-bold uppercase tracking-[.19em] text-cyan-200">Overall learning progress</p>
              <div className="student-dashboard-progress-layout mt-3 flex flex-wrap items-center gap-4 sm:flex-nowrap xl:mt-5 xl:gap-5">
                <div role="progressbar" aria-label="Overall learning progress" aria-valuemin="0" aria-valuemax="100" aria-valuenow={progressPercent} className="student-dashboard-progress-ring grid size-32 shrink-0 place-items-center rounded-full p-[9px] shadow-lg shadow-cyan-950/40 transition-all duration-700 xl:size-36" style={{ background: `conic-gradient(#22d3ee ${progressPercent}%, rgba(148,163,184,.22) ${progressPercent}% 100%)` }}>
                  <div className="grid size-full place-items-center rounded-full border border-white/10 bg-[#102a38] text-center"><span><span className="block text-4xl font-bold tracking-tight text-white">{progressPercent}%</span><span className="mt-1 block text-[10px] font-semibold uppercase tracking-widest text-slate-400">complete</span></span></div>
                </div>
                <div className="student-dashboard-progress-copy min-w-0"><p className="text-base font-semibold text-white xl:text-lg">Your learning journey</p><p className="student-dashboard-progress-description mt-1 text-xs leading-5 text-slate-300 xl:mt-2 xl:text-sm xl:leading-6">Progress follows completed Discussions across the active COC 1 and COC 2 courses.</p><p className="mt-2 text-[11px] text-slate-400 xl:mt-4 xl:text-xs">Based on {dashboard?.competencies.reduce((sum, item) => sum + item.discussionCount, 0) ?? 0} available Discussions</p></div>
              </div>
            </div>
            <div className="student-dashboard-welcome relative flex min-h-0 flex-col justify-center overflow-hidden rounded-3xl border border-teal-100/20 bg-gradient-to-br from-teal-800/50 via-cyan-900/35 to-slate-900/50 p-5 shadow-xl shadow-slate-950/20 backdrop-blur-xl sm:p-6">
              <div aria-hidden="true" className="absolute -right-16 -top-20 size-64 rounded-full border border-cyan-100/10" /><div aria-hidden="true" className="absolute -right-8 -top-12 size-48 rounded-full border border-cyan-100/10" />
              <div className="relative"><p className="text-[10px] font-bold uppercase tracking-[.2em] text-teal-200 xl:text-xs">Student dashboard</p><h1 className="mt-2 text-2xl font-bold tracking-tight text-white sm:text-3xl xl:text-4xl">Welcome, {dashboard?.fullName || student.fullName}</h1><p className="mt-2 max-w-xl text-xs leading-5 text-slate-200 xl:mt-3 xl:text-sm xl:leading-6">Continue your Computer Systems Servicing NC II learning across your available competencies.</p><div className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-[11px] text-slate-300 xl:mt-5 xl:text-xs"><span>Account <strong className="ml-1 text-white">{dashboard?.accountId || student.accountId}</strong></span><span>Program <strong className="ml-1 text-white">{dashboard?.program || student.program}</strong></span></div></div>
            </div>
          </section>
          <section aria-label="Assessment statistics" className="student-dashboard-stats shrink-0 rounded-3xl border border-cyan-100/15 bg-white/[.06] p-3 shadow-xl shadow-slate-950/10 backdrop-blur-xl sm:p-4"><div className="student-dashboard-stats-heading mb-2"><p className="text-[10px] font-bold uppercase tracking-[.18em] text-cyan-200">Learning statistics</p><h2 className="mt-0.5 text-sm font-bold text-white">Assessment activity</h2></div><div className="student-dashboard-stats-grid grid gap-2 sm:grid-cols-3">{assessmentStats.map(stat => <div key={stat.code} className="student-dashboard-stat-card flex min-h-14 items-center gap-3 rounded-xl border border-white/10 bg-slate-950/20 px-3 py-2"><div className="student-dashboard-stat-count grid size-11 shrink-0 place-items-center rounded-full border-4 border-cyan-300/35 bg-cyan-300/[.08] text-base font-bold text-white">{completedStageCount(dashboard, stat.code)}</div><p className="student-dashboard-stat-label text-xs font-semibold text-cyan-100">{stat.label}</p></div>)}</div></section>
          <section className="flex min-h-0 flex-1 flex-col">
            <div className="mb-2 shrink-0"><p className="text-[10px] font-bold uppercase tracking-[.18em] text-cyan-200">Your courses</p><h2 className="mt-0.5 text-lg font-bold text-white">Choose a competency</h2></div>
            {!dashboard?.competencies.length ? <div role="region" aria-label="Competencies" className="grid min-h-0 flex-1 place-items-center rounded-2xl border border-white/10 bg-white/[.06] p-5 text-center text-sm text-slate-300">No active competencies are available yet. Please contact your instructor.</div> : <div role="region" aria-label="Competencies" tabIndex={0} className="min-h-0 flex-1 overflow-y-auto overscroll-contain pr-1"><div className="grid gap-3 lg:grid-cols-2">{dashboard.competencies.map(competency => <button key={competency.id} type="button" onClick={() => { setExpandedCompetencies(current => ({ ...current, [competency.id]: true })); setView({ page: 'competency', competencyId: competency.id }) }} className="rounded-2xl border border-cyan-100/15 bg-white/[.07] p-4 text-left shadow-lg shadow-slate-950/10 backdrop-blur-xl transition hover:border-cyan-200/40 hover:bg-white/[.1] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-cyan-300 xl:p-5"><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-[.18em] text-teal-200">{competency.code.replace('COC', 'COC ')}</p><h3 className="mt-1 text-base font-bold text-white xl:text-lg">{competency.title}</h3></div><span className="rounded-full border border-cyan-200/15 bg-cyan-200/[.08] px-2.5 py-1 text-[11px] font-semibold text-cyan-100">{statusLabel[competency.status] ?? competency.status}</span></div><p className="mt-2 text-xs leading-5 text-slate-300 xl:mt-3 xl:text-sm xl:leading-6">{competency.description || 'Continue through the Discussions and learning stages in order.'}</p><div className="mt-3 flex justify-between gap-3 text-[11px] text-slate-300 xl:mt-4 xl:text-xs"><span>{competency.discussionCount} Discussions</span><span>{competency.completedDiscussions} completed · {competency.remainingDiscussions} remaining</span></div><div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-950/50"><div className="h-full rounded-full bg-gradient-to-r from-cyan-400 to-teal-300 transition-[width] duration-700" style={{ width: `${competency.completionPercent}%` }} /></div><p className="mt-1 text-right text-[11px] font-semibold text-cyan-200">{competency.completionPercent}% complete</p></button>)}</div></div>}
          </section>
        </div>}

        {view.page === 'competency' && selectedCompetency && <section className="space-y-4"><div className="rounded-2xl border border-slate-200 bg-white p-6"><p className="text-sm font-bold uppercase tracking-wider text-teal-700">{selectedCompetency.code.replace('COC', 'COC ')}</p><h2 className="mt-1 text-2xl font-bold">{selectedCompetency.title}</h2><p className="mt-2 text-slate-600">{selectedCompetency.description}</p><p className="mt-3 text-sm text-slate-600">{selectedCompetency.completedDiscussions} of {selectedCompetency.discussionCount} Discussions completed</p></div><div className="grid gap-3">{selectedCompetency.discussions.map((discussion, index) => <button key={discussion.id} type="button" disabled={discussion.stages[0]?.status === 'LOCKED'} onClick={() => setView({ page: 'discussion', competencyId: selectedCompetency.id, discussionId: discussion.id })} className="flex items-center justify-between gap-3 rounded-xl border border-slate-200 bg-white p-5 text-left hover:border-teal-500 disabled:cursor-not-allowed disabled:bg-slate-50 disabled:opacity-70"><span><span className="text-xs font-bold uppercase tracking-wider text-teal-700">Discussion {index + 1}</span><span className="mt-1 block font-semibold text-slate-900">{discussion.title}</span><span className="mt-1 block text-sm text-slate-600">{discussion.completedStages} of 7 stages complete</span></span><span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle(discussion.completed ? 'COMPLETED' : discussion.stages[0]?.status)}`}>{discussion.completed ? 'Completed' : statusLabel[discussion.stages[0]?.status] || 'Locked'}</span></button>)}</div>{selectedCompetency.discussions.length > 0 && selectedCompetency.discussions.every(discussion => discussion.completed) && <section className="rounded-2xl border border-teal-200 bg-teal-50 p-5"><h3 className="font-bold text-teal-950">Generalized Post-test</h3><p className="mt-1 text-sm text-teal-900">Complete the separate competency assessment to pass this competency. A score of 70% is required.</p><button type="button" onClick={() => setView({ page: 'generalized', competencyId: selectedCompetency.id })} className="mt-4 rounded-lg bg-teal-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-900">Open Generalized Post-test</button></section>}</section>}

        {view.page === 'discussion' && selectedCompetency && selectedDiscussion && <section className="space-y-4"><div className="rounded-2xl border border-slate-200 bg-white p-6"><p className="text-sm font-bold uppercase tracking-wider text-teal-700">{selectedCompetency.code.replace('COC', 'COC ')} · Discussion {selectedDiscussion.displayOrder}</p><h2 className="mt-1 text-2xl font-bold">{selectedDiscussion.title}</h2>{selectedDiscussion.description && <p className="mt-2 text-slate-600">{selectedDiscussion.description}</p>}<p className="mt-3 text-sm text-slate-600">Complete each stage before moving forward. You can revisit completed stages until you start the Discussion Post-test.</p></div><div className="grid gap-3">{selectedDiscussion.stages.map(stage => stageButton(selectedDiscussion, stage))}</div></section>}

        {view.page === 'stage' && selectedCompetency && selectedDiscussion && selectedStage && <section className="space-y-4"><article className="rounded-2xl border border-slate-200 bg-white p-5 sm:p-7"><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-bold uppercase tracking-wider text-teal-700">{selectedCompetency.code.replace('COC', 'COC ')} · {selectedDiscussion.title}</p><h2 className="mt-1 text-2xl font-bold">{stageContent?.title || selectedStage.title}</h2></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle(stageContent?.status || selectedStage.status)}`}>{statusLabel[stageContent?.status || selectedStage.status]}</span></div>
          {loading ? <p className="mt-6 text-slate-600">Loading this stage…</p> : stageContent?.hasContent || stageContent?.youtubeVideoIds?.length ? <div className="mt-6 space-y-5">
            {stageContent.stageCode === 'VIDEO_LESSONS' && <YouTubeVideoPlayers videoIds={stageContent.youtubeVideoIds} />}
            {stageContent.hasContent && <SafeMarkdown source={stageContent.contentBody} allowYoutubeVideos={stageContent.stageCode === 'VIDEO_LESSONS'} />}
          </div> : <p className="mt-6 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900">Learning content has not been configured for this stage yet. Please contact your instructor. The Markdown source files are kept separately and have not been published automatically.</p>}
        </article>
        {assessmentType[view.stageCode] && selectedStage.status !== 'COMPLETED' && attempt && !assessmentResult && <AssessmentForm title={view.stageCode === 'POST_TEST' ? 'Discussion Post-test' : stageTitles[view.stageCode]} questions={questions} answers={answers} onAnswer={saveAnswer} onSubmit={() => submitAttempt(false)} busy={assessmentBusy} />}
        {assessmentResult && <ResultCard result={assessmentResult} />}
        {!assessmentType[view.stageCode] && <div className="flex justify-end"><button type="button" onClick={completeStage} disabled={assessmentBusy || loading || !stageContent?.hasContent || selectedStage.status === 'COMPLETED'} className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:cursor-not-allowed disabled:opacity-50">{selectedStage.status === 'COMPLETED' ? 'Stage completed' : assessmentBusy ? 'Saving…' : 'Mark stage complete'}</button></div>}
        </section>}

        {view.page === 'generalized' && selectedCompetency && <section className="space-y-4"><div className="rounded-2xl border border-slate-200 bg-white p-6"><p className="text-sm font-bold uppercase tracking-wider text-teal-700">{selectedCompetency.code.replace('COC', 'COC ')}</p><h2 className="mt-1 text-2xl font-bold">Generalized Post-test</h2><p className="mt-2 text-sm leading-6 text-slate-600">This assessment covers the competency. The passing score is 70%. Three initial attempts are available; additional cycles require Admin authorization. Passed competency progress is preserved.</p>{generalizedStatus && <p className="mt-3 text-sm font-semibold text-slate-700">{generalizedStatus.passed ? 'Competency passed' : `${generalizedStatus.attemptsRemaining} of ${generalizedStatus.allowedAttempts} attempts available`}</p>}{!loading && !generalizedStatus && <p className="mt-3 text-sm text-slate-600">No published Generalized Post-test is available yet.</p>}</div>
          {generalizedStatus && !generalizedStatus.passed && !attempt && <button type="button" onClick={openGeneralizedTest} disabled={assessmentBusy || loading || generalizedStatus.attemptsRemaining <= 0} className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:opacity-50">{assessmentBusy ? 'Opening…' : generalizedStatus.hasInProgressAttempt ? 'Resume test' : 'Start test'}</button>}
          {attempt && !assessmentResult && <AssessmentForm title="Generalized Post-test" questions={questions} answers={answers} onAnswer={(question, value) => saveAnswer(question, value, true)} onSubmit={() => submitAttempt(true)} busy={assessmentBusy} />}
          {assessmentResult && <ResultCard result={assessmentResult} />}
          {attemptHistory.length > 0 && <div className="rounded-2xl border border-slate-200 bg-white p-5"><h3 className="font-bold">Previous attempts</h3><ul className="mt-3 space-y-2 text-sm text-slate-700">{attemptHistory.map(row => <li key={row.attemptId} className="flex justify-between gap-4"><span>Attempt {row.attemptNumber} · {row.status}</span><span>{row.percentage == null ? 'In progress' : `${row.percentage}%${row.passed ? ' · Passed' : ' · Not passed'}`}</span></li>)}</ul></div>}
        </section>}
        </div>
      </main>
    </div>
  )
}

export function AssessmentForm({ title, questions, answers, onAnswer, onSubmit, busy }) {
  return (
    <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 text-slate-900 sm:p-7">
      <div className="flex items-center justify-between gap-3">
        <h3 className="text-lg font-bold">{title}</h3>
        <span className="text-sm text-slate-500">{questions.length} questions</span>
      </div>
      {questions.map((question, index) => {
        const config = question.questionConfig ? JSON.parse(question.questionConfig) : {}
        return <fieldset key={question.questionId} className="rounded-xl border border-slate-200 p-4">
          <legend className="px-1 font-semibold text-slate-900">{index + 1}. {question.questionText}</legend>
          <div className="mt-3 space-y-2">
            {question.questionType === 'MATCHING_TYPE'
                ? <div className="space-y-3">{question.matchingPrompts.map((prompt, promptIndex) => {
                  const selected = answers[question.questionId]?.matches || []
                  const selectedId = selected.find(match => match.promptIndex === promptIndex)?.choiceId ?? ''
                  return <label key={`${question.questionId}-${promptIndex}`} className="grid gap-2 text-slate-900 sm:grid-cols-[minmax(0,1fr)_minmax(10rem,.8fr)] sm:items-center"><span>{prompt}</span><select value={selectedId} onChange={event => onAnswer(question, { matches: matchingSelectionsWithChange(selected, question.matchingPrompts.length, promptIndex, event.target.value) })} className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2"><option value="">Select a match</option>{question.choices.map(choice => <option key={choice.choiceId} value={choice.choiceId} disabled={isMatchingOptionDisabled(selected, promptIndex, choice.choiceId, selectedId)}>{choice.choiceText}</option>)}</select></label>
                })}</div>
                : question.questionType === 'ENUMERATION'
                  ? <div className="space-y-2">{Array.from({ length: config.requiredAnswerCount }, (_, answerIndex) => <label key={`${question.questionId}-${answerIndex}`} className="block text-sm text-slate-700">Answer {answerIndex + 1}<input type="text" value={answers[question.questionId]?.answers?.[answerIndex] ?? ''} onChange={event => { const current = answers[question.questionId]?.answers || Array.from({ length: config.requiredAnswerCount }, () => ''); onAnswer(question, { answers: current.map((value, itemIndex) => itemIndex === answerIndex ? event.target.value : value) }) }} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900" /></label>)}</div>
                  : question.questionType === 'MULTIPLE_SELECT'
              ? question.choices.map(choice => {
                const selected = answers[question.questionId]?.choiceIds || []
                return (
                  <label key={choice.choiceId} className="flex cursor-pointer items-start gap-3 rounded-lg p-2 text-slate-900 hover:bg-slate-50">
                    <input
                      type="checkbox"
                      checked={selected.includes(choice.choiceId)}
                      onChange={event => onAnswer(question, {
                        choiceIds: event.target.checked ? [...selected, choice.choiceId] : selected.filter(id => id !== choice.choiceId),
                      })}
                      className="mt-1 accent-teal-800"
                    />
                    <span>{choice.choiceText}</span>
                  </label>
                )
              })
              : question.questionType === 'TRUE_FALSE'
                ? [true, false].map(value => (
                  <label key={String(value)} className="flex cursor-pointer items-center gap-3 rounded-lg p-2 text-slate-900 hover:bg-slate-50">
                    <input
                      type="radio"
                      name={`q-${question.questionId}`}
                      checked={answers[question.questionId]?.value === value}
                      onChange={() => onAnswer(question, { value })}
                      className="accent-teal-800"
                    />
                    <span>{value ? 'True' : 'False'}</span>
                  </label>
                ))
                : question.choices.map(choice => (
                  <label key={choice.choiceId} className="flex cursor-pointer items-start gap-3 rounded-lg p-2 text-slate-900 hover:bg-slate-50">
                    <input
                      type="radio"
                      name={`q-${question.questionId}`}
                      checked={answers[question.questionId]?.choiceId === choice.choiceId}
                      onChange={() => onAnswer(question, { choiceId: choice.choiceId })}
                      className="mt-1 accent-teal-800"
                    />
                    <span>{choice.choiceText}</span>
                  </label>
                ))}
          </div>
        </fieldset>
      })}
      <button type="button" onClick={onSubmit} disabled={busy || !questions.length} className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:opacity-50">
        {busy ? 'Submitting…' : 'Submit assessment'}
      </button>
    </section>
  )
}

function ResultCard({ result }) {
  return <section role="status" className="rounded-2xl border border-teal-200 bg-teal-50 p-5"><h3 className="font-bold text-teal-950">Assessment submitted</h3><p className="mt-1 text-sm text-teal-900">Score: {result.percentage}% ({result.score} / {result.totalPossibleScore})</p>{typeof result.passed === 'boolean' && <p className="mt-1 text-sm font-semibold text-teal-950">{result.passed ? 'Competency passed.' : 'This attempt did not reach the 70% passing score.'}</p>}</section>
}
