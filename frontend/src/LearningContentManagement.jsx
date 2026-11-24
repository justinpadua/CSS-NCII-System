import { useCallback, useEffect, useMemo, useState } from 'react'
import { listContentCompetencies, listDiscussionStages, previewDiscussionStageSource, updateDiscussionStage } from './learningContentApi.js'
import { listDiscussions } from './courseStructureApi.js'

const STAGES = [
  { code: 'PRE_TEST', title: 'Pre-test' },
  { code: 'DEFINITIONS', title: 'Definitions' },
  { code: 'EXPLANATION', title: 'Explanation' },
  { code: 'EXAMPLES', title: 'Examples' },
  { code: 'VIDEO_LESSONS', title: 'Video Lessons' },
  { code: 'ACTIVITY', title: 'Activity' },
  { code: 'POST_TEST', title: 'Post-test' },
]

function errorMessage(error) {
  if (error.status === 401) return 'Your Admin session has expired. Please log in again.'
  return error.message || 'The request could not be completed. Please try again.'
}

function LearningContentManagement({ onSessionExpired }) {
  const [competencies, setCompetencies] = useState([])
  const [selectedCode, setSelectedCode] = useState('COC1')
  const [discussions, setDiscussions] = useState([])
  const [selectedDiscussionId, setSelectedDiscussionId] = useState('')
  const [stages, setStages] = useState([])
  const [selectedStageCode, setSelectedStageCode] = useState('PRE_TEST')
  const [draftTitle, setDraftTitle] = useState(null)
  const [draftBody, setDraftBody] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [sourcePreview, setSourcePreview] = useState(null)
  const [sourceLoading, setSourceLoading] = useState(false)

  const competency = useMemo(() => competencies.find(item => item.code === selectedCode) ?? null,
    [competencies, selectedCode])
  const selectedDiscussion = discussions.find(item => String(item.id) === String(selectedDiscussionId)) ?? null
  const selectedStage = stages.find(item => item.stageCode === selectedStageCode) ?? null

  const handleError = useCallback((requestError) => {
    setError(errorMessage(requestError))
    if (requestError.status === 401) onSessionExpired()
  }, [onSessionExpired])

  const loadCompetencies = useCallback(async () => {
    try {
      const items = await listContentCompetencies()
      setCompetencies(items)
      if (!items.some(item => item.code === selectedCode)) {
        setSelectedCode(items.find(item => item.code === 'COC1')?.code ?? items[0]?.code ?? 'COC1')
      }
    } catch (requestError) {
      handleError(requestError)
    }
  }, [handleError, selectedCode])

  const loadDiscussionsForCompetency = useCallback(async () => {
    if (!competency) {
      setDiscussions([])
      setSelectedDiscussionId('')
      setStages([])
      return
    }
    try {
      const items = await listDiscussions(competency.id)
      setDiscussions(items)
      if (!items.some(item => String(item.id) === String(selectedDiscussionId))) {
        setSelectedDiscussionId(items[0] ? String(items[0].id) : '')
      }
    } catch (requestError) {
      handleError(requestError)
    }
  }, [competency, handleError, selectedDiscussionId])

  const loadStages = useCallback(async () => {
    if (!competency || !selectedDiscussion) {
      setStages([])
      return
    }
    setLoading(true)
    try {
      const items = await listDiscussionStages(competency.id, selectedDiscussion.id)
      setStages(items)
    } catch (requestError) {
      handleError(requestError)
    } finally {
      setLoading(false)
    }
  }, [competency, handleError, selectedDiscussion])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadCompetencies() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadCompetencies])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadDiscussionsForCompetency() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadDiscussionsForCompetency])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadStages() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadStages])

  async function saveStage(event) {
    event.preventDefault()
    if (!competency || !selectedDiscussion || !selectedStage) return
    setBusy(true)
    setError('')
    setNotice('')
    try {
      const updated = await updateDiscussionStage(competency.id, selectedDiscussion.id, selectedStage.stageCode, {
        title: (draftTitle ?? selectedStage.title).trim(),
        contentBody: draftBody ?? selectedStage.contentBody,
      })
      setStages(items => items.map(item => item.stageCode === updated.stageCode ? updated : item))
      setDraftTitle(null)
      setDraftBody(null)
      setSourcePreview(null)
      setNotice(`${selectedStage.title} content saved.`)
    } catch (requestError) {
      handleError(requestError)
    } finally {
      setBusy(false)
    }
  }

  const setCourse = event => {
    setSelectedCode(event.target.value)
    setSelectedDiscussionId('')
    setStages([])
    setDraftTitle(null)
    setDraftBody(null)
    setSourcePreview(null)
    setError('')
    setNotice('')
  }

  return (
    <section className="space-y-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">Learning Content</h1>
        <p className="mt-2 text-sm text-slate-600">Choose a COC competency, Discussion, and stage to edit its instructional content.</p>
      </div>

      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {notice && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}

      <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-3">
        <label className="block text-sm font-semibold text-slate-700">COC
          <select value={selectedCode} onChange={setCourse} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal text-slate-900">
            <option value="COC1">COC 1</option><option value="COC2">COC 2</option>
          </select>
        </label>
        <div className="text-sm font-semibold text-slate-700">Competency
          <div className="mt-1 flex min-h-11 items-center rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 font-normal text-slate-900">
            {competency ? competency.title : 'No competency record configured'}
          </div>
          <span className="mt-1 block text-xs font-normal text-slate-500">In the current schema, each COC is stored as its competency record.</span>
        </div>
        <label className="block text-sm font-semibold text-slate-700">Discussion
          <select value={selectedDiscussionId} onChange={event => { setSelectedDiscussionId(event.target.value); setDraftTitle(null); setDraftBody(null); setSourcePreview(null); setNotice('') }} disabled={!competency || discussions.length === 0} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal text-slate-900">
            {discussions.length === 0 ? <option value="">No Discussions available</option> : discussions.map(item => <option key={item.id} value={item.id}>{item.displayOrder}. {item.title}</option>)}
          </select>
        </label>
      </div>

      {!competency ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">No {selectedCode} competency record exists. Create it in Course Structure before adding learning content.</div>
      ) : !selectedDiscussion ? (
        <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Select a Discussion with its existing course structure before editing stages.</div>
      ) : loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Loading the seven learning stages…</div>
      ) : stages.length !== STAGES.length ? (
        <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">This Discussion does not have a complete set of seven learning-stage records. The API did not create or duplicate stage rows.</div>
      ) : (
        <div className="grid gap-5 lg:grid-cols-[minmax(15rem,0.8fr)_minmax(0,1.6fr)]">
          <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="px-2 text-lg font-bold text-slate-900">Seven stages</h2>
            <ol className="mt-3 space-y-1">
              {STAGES.map((stage, index) => {
                const item = stages.find(value => value.stageCode === stage.code)
                return (
                  <li key={stage.code}>
                    <button type="button" onClick={() => { setSelectedStageCode(stage.code); setDraftTitle(null); setDraftBody(null); setSourcePreview(null); setNotice('') }} className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-3 text-left ${selectedStageCode === stage.code ? 'bg-teal-50 text-teal-950 ring-1 ring-teal-200' : 'text-slate-700 hover:bg-slate-50'}`}>
                      <span><span className="font-semibold">{index + 1}. {stage.title}</span><span className="mt-0.5 block text-xs text-slate-500">{item?.active ? 'Active' : 'Inactive'}</span></span>
                      <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${item?.hasContent ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>{item?.hasContent ? 'Configured' : 'Empty'}</span>
                    </button>
                  </li>
                )
              })}
            </ol>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            {!selectedStage ? <p className="text-sm text-slate-600">Choose a learning stage to edit.</p> : (
              <form onSubmit={saveStage} className="space-y-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-teal-700">{selectedStage.stageCode} · Stage {selectedStage.displayOrder} of 7</p>
                  <h2 className="mt-1 text-xl font-bold text-slate-900">Edit {selectedStage.title}</h2>
                  <p className="mt-1 text-sm text-slate-600">The stage code and position are fixed to preserve the Discussion learning flow.</p>
                </div>
                <div className="rounded-xl border border-sky-200 bg-sky-50 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div><h3 className="font-semibold text-sky-950">Existing Markdown source</h3><p className="mt-1 text-xs leading-5 text-sky-900">Preview is read-only. Review it, load it into the editor, then explicitly save to publish. Existing stage content is never replaced.</p></div>
                    <button type="button" disabled={sourceLoading || sourcePreview?.existingContentPreserved} onClick={async () => {
                      setSourceLoading(true)
                      setError('')
                      try {
                        setSourcePreview(await previewDiscussionStageSource(competency.id, selectedDiscussion.id, selectedStage.stageCode))
                      } catch (requestError) {
                        handleError(requestError)
                      } finally {
                        setSourceLoading(false)
                      }
                    }} className="rounded-lg border border-sky-300 bg-white px-3 py-2 text-sm font-semibold text-sky-950 disabled:opacity-60">{sourceLoading ? 'Loading preview…' : 'Preview source Markdown'}</button>
                  </div>
                  {sourcePreview && <div className="mt-3 space-y-3 border-t border-sky-200 pt-3">
                    <p className="text-xs text-sky-950"><span className="font-semibold">Source:</span> {sourcePreview.sourcePath}</p>
                    <p role="status" className="text-sm text-sky-950">{sourcePreview.message}</p>
                    {sourcePreview.assessmentItemsRemoved && <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-950">Assessment questions and answer keys are not included in this student-visible stage draft. Use Admin Assessment Management to review and enter them.</p>}
                    {sourcePreview.stageBodyPreview && <pre className="max-h-72 overflow-auto whitespace-pre-wrap rounded-lg border border-sky-200 bg-white p-3 text-xs leading-5 text-slate-800">{sourcePreview.stageBodyPreview}</pre>}
                    {sourcePreview.canLoadIntoEditor && <button type="button" disabled={draftBody !== null} onClick={() => { setDraftBody(sourcePreview.stageBodyPreview); setNotice('Source loaded as an unsaved draft. Save the stage to publish it.') }} className="rounded-lg bg-teal-800 px-3 py-2 text-sm font-semibold text-white disabled:opacity-60">{draftBody !== null ? 'Draft already loaded or edited' : 'Load reviewed source into editor'}</button>}
                  </div>}
                </div>
                <label className="block text-sm font-semibold text-slate-700">Stage title<input value={draftTitle ?? selectedStage.title} onChange={event => setDraftTitle(event.target.value)} required maxLength={200} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 font-normal text-slate-900" /></label>
                <label className="block text-sm font-semibold text-slate-700">Content body
                  <textarea value={draftBody ?? selectedStage.contentBody} onChange={event => setDraftBody(event.target.value)} rows={14} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-3 font-mono text-sm font-normal text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" placeholder="Write instructional text here. Markdown image and video references are supported." />
                </label>
                <div className="rounded-lg bg-sky-50 p-3 text-xs leading-5 text-sky-950">
                  <p className="font-semibold">Text and media references</p>
                  <p>Use plain text or Markdown. For an image: <code>![Alt text](https://…)</code>. For a video: <code>[Watch video](https://…)</code>. Media files stay outside the database; the stage body stores the reference.</p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <button type="submit" disabled={busy} className="rounded-lg bg-teal-800 px-5 py-2.5 text-sm font-semibold text-white hover:bg-teal-900 disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : 'Save stage content'}</button>
                  <span className="text-xs text-slate-500">{selectedStage.hasContent ? 'This stage has content configured.' : 'This stage is currently empty.'}</span>
                </div>
              </form>
            )}
          </section>
        </div>
      )}
    </section>
  )
}

export default LearningContentManagement
