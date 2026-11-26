import { useCallback, useEffect, useMemo, useState } from 'react'
import { studentApiRequest } from './studentAuth.js'
import SafeMarkdown from './SafeMarkdown.jsx'
import YouTubeVideoPlayers from './YouTubeVideoPlayers.jsx'

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

export default function StudentLearningDashboard({ student, onSessionExpired, sessionError }) {
  const [dashboard, setDashboard] = useState(null)
  const [view, setView] = useState({ page: 'dashboard' })
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
    if (!attempt || questions.some(question => answers[question.questionId] === undefined)) {
      setError('Please answer every question before submitting.')
      return
    }
    setAssessmentBusy(true)
    setError('')
    try {
      const prefix = generalized ? 'generalized-attempts' : 'attempts'
      const answerPath = `/api/student/assessments/${prefix}/${attempt.attemptId}/answers`
      for (const question of questions) {
        await studentApiRequest(answerPath, {
          method: 'PUT',
          body: { questionId: question.questionId, responseData: JSON.stringify(answers[question.questionId]) },
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

  function stageButton(discussion, stage) {
    const postTestInProgress = discussion.stages.some(row => row.stageCode === 'POST_TEST' && row.status === 'IN_PROGRESS')
    const locked = stage.status === 'LOCKED' || (postTestInProgress && stage.stageCode !== 'POST_TEST')
    return <button key={stage.stageCode} type="button" disabled={locked} onClick={() => { setError(''); setAssessmentResult(null); setView({ page: 'stage', competencyId: selectedCompetency.id, discussionId: discussion.id, stageCode: stage.stageCode, stageStatus: stage.status }) }} className={`flex w-full items-center justify-between gap-3 rounded-xl border p-4 text-left ${locked ? 'cursor-not-allowed border-slate-200 bg-slate-50 opacity-70' : 'border-slate-200 bg-white hover:border-teal-500 hover:shadow-sm'}`}>
      <span><span className="block font-semibold text-slate-900">{stage.title}</span><span className="mt-1 block text-xs text-slate-500">Stage {stageOrder.indexOf(stage.stageCode) + 1} of 7</span></span>
      <span className={`shrink-0 rounded-full px-2.5 py-1 text-xs font-semibold ${statusStyle(locked ? 'LOCKED' : stage.status)}`}>{locked ? 'Locked' : statusLabel[stage.status] ?? stage.status}</span>
    </button>
  }

  if (loading && !dashboard) return <main className="grid min-h-[calc(100svh-144px)] place-items-center bg-slate-100 pb-10 text-slate-700 sm:min-h-[calc(100svh-104px)]">Loading your learning dashboard…</main>

  return (
    <div className="min-h-[calc(100svh-144px)] bg-slate-100 pb-10 text-slate-900 sm:min-h-[calc(100svh-104px)]">
      <main className="mx-auto max-w-7xl space-y-5 p-4 sm:p-6 lg:p-8">
        {sessionError && <p role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">{sessionError}</p>}
        {error && <p role="alert" className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">{error}</p>}
        <div className="flex flex-wrap items-center gap-2 text-sm">
          {!(view.page === 'stage' && view.stageCode === 'POST_TEST' && stageContent?.status === 'IN_PROGRESS') && <button type="button" onClick={() => { setView({ page: 'dashboard' }); setError('') }} className="font-semibold text-teal-800 underline underline-offset-2">Dashboard</button>}
          {selectedCompetency && <><span className="text-slate-400">/</span>{view.page === 'stage' && view.stageCode === 'POST_TEST' && stageContent?.status === 'IN_PROGRESS' ? <span className="text-slate-600">{selectedCompetency.code}</span> : <button type="button" onClick={() => setView({ page: 'competency', competencyId: selectedCompetency.id })} className="font-semibold text-teal-800 underline underline-offset-2">{selectedCompetency.code}</button>}</>}
          {selectedDiscussion && <><span className="text-slate-400">/</span><button type="button" onClick={backToDiscussion} className="font-semibold text-teal-800 underline underline-offset-2">{selectedDiscussion.title}</button></>}
          {selectedStage && <><span className="text-slate-400">/</span><span className="text-slate-600">{selectedStage.title}</span></>}
        </div>
        {loading && dashboard && <p role="status" className="rounded-xl bg-white p-4 text-sm text-slate-600">Loading learning data…</p>}

        {view.page === 'dashboard' && <>
          <section className="rounded-2xl bg-gradient-to-r from-sky-950 to-teal-900 p-6 text-white shadow-sm sm:p-8"><p className="text-sm font-semibold uppercase tracking-wider text-teal-200">Welcome, {dashboard?.fullName || student.fullName}</p><h2 className="mt-2 text-2xl font-bold">Continue your CSS NC II learning</h2><p className="mt-2 text-sm text-sky-100">Choose a competency to view its Discussions, lessons, assessments, and saved progress.</p><p className="mt-4 text-sm text-sky-100">Account ID: <span className="font-semibold text-white">{dashboard?.accountId || student.accountId}</span> <span className="mx-2">·</span> {dashboard?.program || student.program}</p></section>
          {!dashboard?.competencies.length ? <section className="rounded-2xl border border-slate-200 bg-white p-6 text-slate-600">No active competencies are available yet. Please contact your instructor.</section> : <section className="grid gap-4 lg:grid-cols-2">{dashboard.competencies.map(competency => <button key={competency.id} type="button" onClick={() => setView({ page: 'competency', competencyId: competency.id })} className="rounded-2xl border border-slate-200 bg-white p-6 text-left shadow-sm transition hover:border-teal-500 hover:shadow-md"><div className="flex items-start justify-between gap-4"><div><p className="text-sm font-bold uppercase tracking-wider text-teal-700">{competency.code.replace('COC', 'COC ')}</p><h3 className="mt-2 text-xl font-bold text-slate-900">{competency.title}</h3></div><span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyle(competency.status)}`}>{statusLabel[competency.status] ?? competency.status}</span></div><p className="mt-3 text-sm leading-6 text-slate-600">{competency.description || 'Continue through the Discussions and learning stages in order.'}</p><div className="mt-5 flex justify-between text-sm text-slate-600"><span>{competency.discussionCount} Discussions</span><span>{competency.completedDiscussions} completed · {competency.remainingDiscussions} remaining</span></div><div className="mt-2 h-2 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-teal-700" style={{ width: `${competency.completionPercent}%` }} /></div><p className="mt-2 text-right text-xs font-semibold text-teal-800">{competency.completionPercent}% complete</p></button>)}</section>}
        </>}

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
      </main>
    </div>
  )
}

function AssessmentForm({ title, questions, answers, onAnswer, onSubmit, busy }) {
  return <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 sm:p-7"><div className="flex items-center justify-between gap-3"><h3 className="text-lg font-bold">{title}</h3><span className="text-sm text-slate-500">{questions.length} questions</span></div>{questions.map((question, index) => <fieldset key={question.questionId} className="rounded-xl border border-slate-200 p-4"><legend className="px-1 font-semibold text-slate-900">{index + 1}. {question.questionText}</legend><div className="mt-3 space-y-2">{question.questionType === 'MULTIPLE_SELECT' ? question.choices.map(choice => { const selected = answers[question.questionId]?.choiceIds || []; return <label key={choice.choiceId} className="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-slate-50"><input type="checkbox" checked={selected.includes(choice.choiceId)} onChange={event => onAnswer(question, { choiceIds: event.target.checked ? [...selected, choice.choiceId] : selected.filter(id => id !== choice.choiceId) })} className="mt-1 accent-teal-800" /><span>{choice.choiceText}</span></label> }) : question.questionType === 'TRUE_FALSE' ? [true, false].map(value => <label key={String(value)} className="flex cursor-pointer items-center gap-3 rounded-lg p-2 hover:bg-slate-50"><input type="radio" name={`q-${question.questionId}`} checked={answers[question.questionId]?.value === value} onChange={() => onAnswer(question, { value })} className="accent-teal-800" /><span>{value ? 'True' : 'False'}</span></label>) : question.choices.map(choice => <label key={choice.choiceId} className="flex cursor-pointer items-start gap-3 rounded-lg p-2 hover:bg-slate-50"><input type="radio" name={`q-${question.questionId}`} checked={answers[question.questionId]?.choiceId === choice.choiceId} onChange={() => onAnswer(question, { choiceId: choice.choiceId })} className="mt-1 accent-teal-800" /><span>{choice.choiceText}</span></label>)}</div></fieldset>)}<button type="button" onClick={onSubmit} disabled={busy || !questions.length} className="rounded-lg bg-teal-800 px-5 py-2.5 font-semibold text-white hover:bg-teal-900 disabled:opacity-50">{busy ? 'Submitting…' : 'Submit assessment'}</button></section>
}

function ResultCard({ result }) {
  return <section role="status" className="rounded-2xl border border-teal-200 bg-teal-50 p-5"><h3 className="font-bold text-teal-950">Assessment submitted</h3><p className="mt-1 text-sm text-teal-900">Score: {result.percentage}% ({result.score} / {result.totalPossibleScore})</p>{typeof result.passed === 'boolean' && <p className="mt-1 text-sm font-semibold text-teal-950">{result.passed ? 'Competency passed.' : 'This attempt did not reach the 70% passing score.'}</p>}</section>
}
