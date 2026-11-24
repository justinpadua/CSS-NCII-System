import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createAnswerChoice, createAssessmentQuestion, createDiscussionAssessment, createGeneralizedAssessment,
  deleteAnswerChoice, deleteAssessmentQuestion, listAssessmentCompetencies, listAssessmentDiscussions,
  listAssessmentQuestions, listDiscussionAssessments, listGeneralizedAssessments,
  updateAnswerChoice, updateAssessment, updateAssessmentQuestion,
} from './assessmentApi.js'
import { getCorrectChoiceIndexes, updateCorrectChoice } from './assessmentQuestionState.js'

const DISCUSSION_TYPES = [
  { code: 'PRE_TEST', label: 'Pre-test' },
  { code: 'ACTIVITY', label: 'Activity' },
  { code: 'DISCUSSION_POST_TEST', label: 'Discussion Post-test' },
]
const GENERALIZED_TYPE = 'GENERALIZED_POST_TEST'
const QUESTION_TYPES = ['MULTIPLE_CHOICE', 'TRUE_FALSE', 'MULTIPLE_SELECT']

function messageFor(error) {
  if (error.status === 401) return 'Your Admin session has expired. Please log in again.'
  return error.message || 'The request could not be completed. Please try again.'
}

function blankQuestion(order = 1) {
  return {
    id: null,
    questionType: 'MULTIPLE_CHOICE',
    questionText: '',
    points: '1',
    displayOrder: String(order),
    choices: [
      { id: null, choiceText: '', displayOrder: '1', correct: true },
      { id: null, choiceText: '', displayOrder: '2', correct: false },
    ],
    correctValue: true,
  }
}

function AssessmentManagement({ onSessionExpired }) {
  const [competencies, setCompetencies] = useState([])
  const [competencyId, setCompetencyId] = useState('')
  const [discussions, setDiscussions] = useState([])
  const [discussionId, setDiscussionId] = useState('')
  const [scope, setScope] = useState('discussion')
  const [assessments, setAssessments] = useState([])
  const [assessmentId, setAssessmentId] = useState('')
  const [questions, setQuestions] = useState([])
  const [questionDraft, setQuestionDraft] = useState(null)
  const [assessmentDraft, setAssessmentDraft] = useState(null)
  const [createType, setCreateType] = useState('PRE_TEST')
  const [createTitle, setCreateTitle] = useState('')
  const [createInstructions, setCreateInstructions] = useState('')
  const [createActive, setCreateActive] = useState(false)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')

  const competency = competencies.find(item => String(item.id) === String(competencyId)) ?? null
  const discussion = discussions.find(item => String(item.id) === String(discussionId)) ?? null
  const assessment = assessments.find(item => String(item.id) === String(assessmentId)) ?? null
  const usedTypes = useMemo(() => new Set(assessments.map(item => item.assessmentType)), [assessments])

  const fail = useCallback((requestError) => {
    setError(messageFor(requestError))
    if (requestError.status === 401) onSessionExpired()
  }, [onSessionExpired])

  const loadCompetencies = useCallback(async () => {
    setLoading(true)
    try {
      const items = await listAssessmentCompetencies()
      setCompetencies(items)
      setCompetencyId(current => items.some(item => String(item.id) === String(current))
        ? current : String(items.find(item => item.code === 'COC1')?.id ?? items[0]?.id ?? ''))
    } catch (requestError) {
      fail(requestError)
    } finally {
      setLoading(false)
    }
  }, [fail])

  const loadDiscussions = useCallback(async () => {
    if (!competencyId) { setDiscussions([]); setDiscussionId(''); return }
    try {
      const items = await listAssessmentDiscussions(competencyId)
      setDiscussions(items)
      setDiscussionId(current => items.some(item => String(item.id) === String(current))
        ? current : String(items[0]?.id ?? ''))
    } catch (requestError) { fail(requestError) }
  }, [competencyId, fail])

  const loadAssessments = useCallback(async () => {
    if (!competencyId || (scope === 'discussion' && !discussionId)) {
      setAssessments([]); setAssessmentId(''); setQuestions([]); return
    }
    setLoading(true)
    try {
      const items = scope === 'generalized'
        ? await listGeneralizedAssessments(competencyId)
        : await listDiscussionAssessments(competencyId, discussionId)
      setAssessments(items)
      setAssessmentId(current => items.some(item => String(item.id) === String(current)) ? current : '')
      setQuestions([])
      setQuestionDraft(null)
      setAssessmentDraft(null)
    } catch (requestError) { fail(requestError) } finally { setLoading(false) }
  }, [competencyId, discussionId, fail, scope])

  const loadQuestions = useCallback(async () => {
    if (!assessmentId) { setQuestions([]); return }
    try {
      setLoading(true)
      setQuestions(await listAssessmentQuestions(assessmentId))
    } catch (requestError) { fail(requestError) } finally { setLoading(false) }
  }, [assessmentId, fail])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadCompetencies() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadCompetencies])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadDiscussions() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadDiscussions])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadAssessments() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadAssessments])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadQuestions() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadQuestions])

  async function run(operation, success) {
    setBusy(true); setError(''); setNotice('')
    try { await operation(); if (success) setNotice(success) } catch (requestError) { fail(requestError) }
    finally { setBusy(false) }
  }

  async function saveAssessment(event) {
    event.preventDefault()
    if (!assessment) return
    await run(async () => {
      const saved = await updateAssessment(assessment.id, {
        title: assessmentDraft?.title ?? assessment.title,
        instructions: assessmentDraft?.instructions ?? assessment.instructions ?? '',
        active: assessmentDraft?.active ?? assessment.active,
      })
      setAssessments(items => items.map(item => item.id === saved.id ? saved : item))
      setAssessmentDraft(null)
    }, 'Assessment settings saved.')
  }

  async function createAssessment(event) {
    event.preventDefault()
    if (!competencyId || (scope === 'discussion' && !discussionId)) return
    const title = createTitle.trim()
    await run(async () => {
      const payload = { assessmentType: createType, title, instructions: createInstructions.trim() || null, active: createActive }
      const created = scope === 'generalized'
        ? await createGeneralizedAssessment(competencyId, payload)
        : await createDiscussionAssessment(competencyId, discussionId, payload)
      setAssessments(items => [...items, created])
      setAssessmentId(String(created.id))
      if (scope === 'discussion' && createType !== GENERALIZED_TYPE) {
        setCreateType(missingTypes.find(type => type !== createType) ?? createType)
      }
      setCreateTitle(''); setCreateInstructions(''); setCreateActive(false)
    }, 'Assessment created.')
  }

  function editQuestion(question) {
    setQuestionDraft({
      id: question.id,
      questionType: question.questionType,
      questionText: question.questionText,
      points: String(question.points),
      displayOrder: String(question.displayOrder),
      choices: question.choices.map(choice => ({ ...choice, displayOrder: String(choice.displayOrder) })),
      correctValue: question.correctValue ?? true,
    })
    setError(''); setNotice('')
  }

  async function saveQuestion(event) {
    event.preventDefault()
    if (!assessment || !questionDraft) return
    const order = Number(questionDraft.displayOrder)
    const points = Number(questionDraft.points)
    const common = {
      questionType: questionDraft.questionType,
      questionText: questionDraft.questionText.trim(),
      points,
      displayOrder: order,
    }
    await run(async () => {
      if (!questionDraft.id) {
        const isBoolean = questionDraft.questionType === 'TRUE_FALSE'
        const created = await createAssessmentQuestion(assessment.id, {
          ...common,
          choices: isBoolean ? [] : questionDraft.choices.map(choice => ({
            choiceText: choice.choiceText.trim(), displayOrder: Number(choice.displayOrder),
          })),
          correctChoiceIndexes: isBoolean ? [] : getCorrectChoiceIndexes(questionDraft.choices),
          correctValue: isBoolean ? questionDraft.correctValue : null,
        })
        setQuestions(items => [...items, created].sort((a, b) => a.displayOrder - b.displayOrder))
        setQuestionDraft(null)
      } else {
        const saved = await updateAssessmentQuestion(assessment.id, questionDraft.id, {
          ...common,
          correctChoiceIds: questionDraft.questionType === 'TRUE_FALSE' ? [] : questionDraft.choices
            .filter(choice => choice.correct).map(choice => choice.id),
          correctValue: questionDraft.questionType === 'TRUE_FALSE' ? questionDraft.correctValue : null,
        })
        setQuestions(items => items.map(item => item.id === saved.id ? saved : item)
          .sort((a, b) => a.displayOrder - b.displayOrder))
      }
    }, 'Question and scoring settings saved.')
  }

  async function saveChoice(choice) {
    if (!questionDraft?.id) return
    const body = { choiceText: choice.choiceText.trim(), displayOrder: Number(choice.displayOrder) }
    await run(async () => {
      if (choice.id) await updateAnswerChoice(questionDraft.id, choice.id, body)
      else await createAnswerChoice(questionDraft.id, body)
      const choices = await listAssessmentQuestions(assessment.id)
      setQuestions(choices)
      const fresh = choices.find(item => item.id === questionDraft.id)
      if (fresh) editQuestion(fresh)
    }, choice.id ? 'Answer choice saved.' : 'Answer choice added.')
  }

  async function removeChoice(choice) {
    if (!questionDraft?.id || !window.confirm('Delete this answer choice? Any student attempt history prevents deleting choices from this assessment.')) return
    await run(async () => {
      await deleteAnswerChoice(questionDraft.id, choice.id)
      const refreshed = await listAssessmentQuestions(assessment.id)
      setQuestions(refreshed)
      const fresh = refreshed.find(item => item.id === questionDraft.id)
      if (fresh) editQuestion(fresh)
    }, 'Answer choice deleted.')
  }

  async function removeQuestion(question) {
    if (!window.confirm('Delete this question? Questions cannot be deleted once an attempt exists for this assessment.')) return
    await run(async () => {
      await deleteAssessmentQuestion(assessment.id, question.id)
      setQuestions(items => items.filter(item => item.id !== question.id))
      if (questionDraft?.id === question.id) setQuestionDraft(null)
    }, 'Question deleted.')
  }

  const setDraft = (key, value) => setQuestionDraft(current => ({ ...current, [key]: value }))
  const updateDraftChoice = (index, patch) => setQuestionDraft(current => ({
    ...current,
    choices: current.choices.map((choice, itemIndex) => itemIndex === index ? { ...choice, ...patch } : choice),
  }))
  const updateDraftCorrectChoice = (index, checked) => setQuestionDraft(current => ({
    ...current,
    choices: updateCorrectChoice(current.choices, index, checked, current.questionType),
  }))

  const missingTypes = scope === 'generalized'
    ? (usedTypes.has(GENERALIZED_TYPE) ? [] : [GENERALIZED_TYPE])
    : DISCUSSION_TYPES.filter(type => !usedTypes.has(type.code)).map(type => type.code)

  return (
    <section className="space-y-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">Assessment Management</h1>
        <p className="mt-2 text-sm text-slate-600">Manage Discussion assessments and competency Generalized Post-tests, questions, answer choices, and points.</p>
      </div>

      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {notice && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}

      <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-3">
        <label className="text-sm font-semibold text-slate-700">COC<select value={competencyId} onChange={event => { setCompetencyId(event.target.value); setDiscussionId(''); setAssessmentId('') }} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal">
          {competencies.map(item => <option key={item.id} value={item.id}>{item.code === 'COC1' ? 'COC 1' : item.code === 'COC2' ? 'COC 2' : item.title}</option>)}
        </select></label>
        <label className="text-sm font-semibold text-slate-700">Assessment scope<select value={scope} onChange={event => { setScope(event.target.value); setAssessmentId('') }} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal">
          <option value="discussion">Discussion assessment</option><option value="generalized">Generalized Post-test</option>
        </select></label>
        {scope === 'discussion' ? <label className="text-sm font-semibold text-slate-700">Discussion<select value={discussionId} onChange={event => { setDiscussionId(event.target.value); setAssessmentId('') }} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal">
          {discussions.map(item => <option key={item.id} value={item.id}>{item.displayOrder}. {item.title}</option>)}
        </select></label> : <div className="flex items-center rounded-lg bg-sky-50 px-3 text-sm text-sky-950">Passing threshold: <strong className="ml-1">70%</strong> · retake limits remain system-managed</div>}
      </div>

      {loading && <p className="rounded-xl bg-white p-4 text-sm text-slate-600">Loading assessment data…</p>}
      {!loading && !competencies.length && <p className="rounded-xl bg-amber-50 p-4 text-sm text-amber-900">Create COC 1 and COC 2 in Course Structure before configuring assessments.</p>}
      {!loading && competency && scope === 'discussion' && !discussion && <p className="rounded-xl bg-slate-50 p-4 text-sm text-slate-600">Create a Discussion before configuring its assessments.</p>}

      {!loading && competency && (scope === 'generalized' || discussion) && <>
        <div className="grid gap-5 lg:grid-cols-[minmax(16rem,0.8fr)_minmax(0,1.5fr)]">
          <section className="space-y-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div><h2 className="text-lg font-bold text-slate-900">Assessments</h2><p className="mt-1 text-sm text-slate-600">Each scope may have one of each approved assessment type.</p></div>
            <div className="space-y-2">
              {assessments.map(item => <button key={item.id} type="button" onClick={() => setAssessmentId(String(item.id))} className={`w-full rounded-lg border p-3 text-left ${String(item.id) === String(assessmentId) ? 'border-teal-500 bg-teal-50' : 'border-slate-200 hover:bg-slate-50'}`}>
                <span className="block font-semibold text-slate-900">{item.title}</span><span className="mt-1 block text-xs text-slate-600">{item.assessmentType.replaceAll('_', ' ')} · {item.active ? 'Active / Published' : 'Draft / Inactive'}</span>
              </button>)}
              {!assessments.length && <p className="rounded-lg bg-slate-50 p-3 text-sm text-slate-600">No assessment configured for this scope.</p>}
            </div>
            {missingTypes.length > 0 && <form onSubmit={createAssessment} className="space-y-3 border-t border-slate-200 pt-4">
              <h3 className="font-semibold text-slate-800">Create assessment</h3>
              <label className="block text-sm font-medium text-slate-700">Type<select value={createType} onChange={event => { setCreateType(event.target.value); setCreateTitle('') }} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2">
                {missingTypes.map(type => <option key={type} value={type}>{type.replaceAll('_', ' ')}</option>)}
              </select></label>
              <label className="block text-sm font-medium text-slate-700">Title<input required maxLength={200} value={createTitle} onChange={event => setCreateTitle(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
              <label className="block text-sm font-medium text-slate-700">Instructions<textarea rows="3" value={createInstructions} onChange={event => setCreateInstructions(event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
              <label className="flex items-start gap-2 text-sm text-slate-700"><input type="checkbox" checked={createActive} onChange={event => setCreateActive(event.target.checked)} className="mt-1" /><span><span className="font-medium">Activate after creation</span><span className="block text-xs text-slate-500">Active assessments are available to eligible students immediately. Leave unchecked to save as a draft.</span></span></label>
              <button disabled={busy} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Create assessment</button>
            </form>}
          </section>

          <section className="space-y-4">
            {assessment ? <>
              <form onSubmit={saveAssessment} className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3"><div><p className="text-xs font-semibold uppercase tracking-wide text-teal-700">{assessment.assessmentType.replaceAll('_', ' ')}</p><h2 className="text-lg font-bold">Assessment settings</h2></div>{assessment.passPercentage != null && <span className="rounded-full bg-amber-100 px-3 py-1 text-xs font-semibold text-amber-900">Fixed pass mark: {assessment.passPercentage}%</span>}</div>
                <label className="block text-sm font-medium text-slate-700">Title<input maxLength={200} value={assessmentDraft?.title ?? assessment.title} onChange={event => setAssessmentDraft(current => ({ ...current, title: event.target.value }))} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
                <label className="block text-sm font-medium text-slate-700">Instructions<textarea rows="2" value={assessmentDraft?.instructions ?? assessment.instructions ?? ''} onChange={event => setAssessmentDraft(current => ({ ...current, instructions: event.target.value }))} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
                <label className="flex items-center gap-2 text-sm font-medium text-slate-700"><input type="checkbox" checked={assessmentDraft?.active ?? assessment.active} onChange={event => setAssessmentDraft(current => ({ ...current, active: event.target.checked }))} /> Assessment is active and available to eligible students</label>
                <button disabled={busy} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Save settings</button>
              </form>

              <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-bold text-slate-900">Questions</h2><p className="text-sm text-slate-600">Question points determine the assessment score. Total: {questions.reduce((sum, item) => sum + Number(item.points), 0).toFixed(2)} points.</p></div><button type="button" onClick={() => { setQuestionDraft(blankQuestion(questions.length + 1)); setNotice(''); setError('') }} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white">Add question</button></div>
                <ol className="mt-4 space-y-2">{questions.map(question => <li key={question.id} className="flex items-center justify-between gap-3 rounded-lg border border-slate-200 p-3"><button type="button" onClick={() => editQuestion(question)} className="min-w-0 flex-1 text-left"><span className="text-xs font-semibold text-teal-800">{question.displayOrder}. {question.points} pt · {question.questionType.replaceAll('_', ' ')}</span><span className="block truncate font-medium text-slate-900">{question.questionText}</span></button><button type="button" onClick={() => void removeQuestion(question)} className="rounded-md px-3 py-2 text-sm font-semibold text-red-700 hover:bg-red-50">Delete</button></li>)}</ol>
                {!questions.length && <p className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-600">No questions yet.</p>}
              </div>

              {questionDraft && <form onSubmit={saveQuestion} className="space-y-4 rounded-2xl border border-teal-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between gap-3"><h2 className="text-lg font-bold">{questionDraft.id ? 'Edit question' : 'New question'}</h2><button type="button" onClick={() => setQuestionDraft(null)} className="text-sm font-semibold text-slate-600 underline">Close</button></div>
                <div className="grid gap-3 sm:grid-cols-3">
                  <label className="text-sm font-medium text-slate-700">Question type<select disabled={Boolean(questionDraft.id)} value={questionDraft.questionType} onChange={event => setDraft('questionType', event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 disabled:bg-slate-100">{QUESTION_TYPES.map(type => <option key={type}>{type}</option>)}</select></label>
                  <label className="text-sm font-medium text-slate-700">Points<input type="number" min="0" max="999999.99" step="0.01" required value={questionDraft.points} onChange={event => setDraft('points', event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
                  <label className="text-sm font-medium text-slate-700">Order<input type="number" min="0" required value={questionDraft.displayOrder} onChange={event => setDraft('displayOrder', event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>
                </div>
                {questionDraft.id && <p className="text-xs text-slate-500">Question type is fixed after creation to avoid changing how saved attempts are interpreted.</p>}
                <label className="block text-sm font-medium text-slate-700">Question prompt<textarea required rows="3" value={questionDraft.questionText} onChange={event => setDraft('questionText', event.target.value)} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label>

                {questionDraft.questionType === 'TRUE_FALSE' ? <fieldset className="flex gap-5 text-sm"><legend className="mb-2 font-semibold">Correct answer</legend>{[true, false].map(value => <label key={String(value)} className="flex items-center gap-2"><input type="radio" name="correctValue" checked={questionDraft.correctValue === value} onChange={() => setDraft('correctValue', value)} />{value ? 'True' : 'False'}</label>)}</fieldset> : <fieldset className="space-y-3"><legend className="font-semibold">Answer choices · mark correct answer{questionDraft.questionType === 'MULTIPLE_SELECT' ? 's' : ''}</legend>
                  {questionDraft.choices.map((choice, index) => <div key={choice.id ?? `new-${index}`} className="grid gap-2 rounded-lg border border-slate-200 p-3 sm:grid-cols-[auto_minmax(0,1fr)_6rem_auto_auto] sm:items-center">
                    <input aria-label={`Choice ${index + 1} is correct`} type={questionDraft.questionType === 'MULTIPLE_SELECT' ? 'checkbox' : 'radio'} name="correct-choice" checked={Boolean(choice.correct)} onChange={event => updateDraftCorrectChoice(index, event.target.checked)} />
                    <input required maxLength={50000} aria-label={`Choice ${index + 1} text`} placeholder={`Choice ${index + 1}`} value={choice.choiceText} onChange={event => updateDraftChoice(index, { choiceText: event.target.value })} className="rounded-lg border border-slate-300 px-3 py-2 text-sm" />
                    <input required type="number" min="0" aria-label={`Choice ${index + 1} order`} value={choice.displayOrder} onChange={event => updateDraftChoice(index, { displayOrder: event.target.value })} className="rounded-lg border border-slate-300 px-2 py-2 text-sm" />
                    {questionDraft.id && <button type="button" onClick={() => void saveChoice(choice)} className="rounded-md border border-slate-300 px-3 py-2 text-xs font-semibold">Save choice</button>}
                    {questionDraft.id && <button type="button" onClick={() => void removeChoice(choice)} className="text-xs font-semibold text-red-700">Delete</button>}
                  </div>)}
                  {!questionDraft.id && <button type="button" onClick={() => setQuestionDraft(current => ({ ...current, choices: [...current.choices, { id: null, choiceText: '', displayOrder: String(current.choices.length + 1), correct: false }] }))} className="rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold">Add another choice</button>}
                  {questionDraft.id && <button type="button" onClick={() => setQuestionDraft(current => ({ ...current, choices: [...current.choices, { id: null, choiceText: '', displayOrder: String(current.choices.length + 1), correct: false }] }))} className="rounded-md border border-slate-300 px-3 py-2 text-sm font-semibold">Add choice</button>}
                </fieldset>}
                <button disabled={busy} className="rounded-lg bg-teal-800 px-5 py-2.5 text-sm font-semibold text-white disabled:opacity-60">{busy ? 'Saving…' : questionDraft.id ? 'Save question and answer key' : 'Create question and answer choices'}</button>
              </form>}
            </> : <p className="rounded-2xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Select an assessment or create one for this scope.</p>}
          </section>
        </div>
      </>}
    </section>
  )
}

export default AssessmentManagement
