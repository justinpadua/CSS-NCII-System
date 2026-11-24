import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createCompetency, createDiscussion, createTopic, initializeCourseStructure, listCompetencies, listDiscussions, listTopics,
  setCompetencyStatus, setDiscussionStatus, setTopicStatus,
  updateCompetency, updateDiscussion, updateTopic,
} from './courseStructureApi.js'

const emptyForm = { title: '', description: '', displayOrder: '' }

function messageFor(error) {
  if (error.status === 401) return 'Your Admin session has expired. Please log in again.'
  return error.message || 'The request could not be completed. Please try again.'
}

function CourseStructureManagement({ initialCode, onSessionExpired }) {
  const [competencies, setCompetencies] = useState([])
  const [selectedCode, setSelectedCode] = useState(initialCode)
  const [discussions, setDiscussions] = useState([])
  const [selectedDiscussionId, setSelectedDiscussionId] = useState(null)
  const [topics, setTopics] = useState([])
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [editingCompetency, setEditingCompetency] = useState(false)
  const [creatingCompetency, setCreatingCompetency] = useState(false)
  const [discussionForm, setDiscussionForm] = useState(null)
  const [topicForm, setTopicForm] = useState(null)

  const selectedCompetency = useMemo(
    () => competencies.find(item => item.code === selectedCode) ?? null,
    [competencies, selectedCode],
  )
  const selectedDiscussion = discussions.find(item => item.id === selectedDiscussionId) ?? null

  const run = useCallback(async (operation, successMessage) => {
    setBusy(true)
    setError('')
    setNotice('')
    try {
      await operation()
      if (successMessage) setNotice(successMessage)
    } catch (requestError) {
      setError(messageFor(requestError))
      if (requestError.status === 401) onSessionExpired()
    } finally {
      setBusy(false)
    }
  }, [onSessionExpired])

  const loadCompetencies = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const items = await listCompetencies()
      setCompetencies(items)
      if (!items.some(item => item.code === selectedCode)) {
        setSelectedCode(items.find(item => item.code === initialCode)?.code ?? items[0]?.code ?? initialCode)
      }
    } catch (requestError) {
      setError(messageFor(requestError))
      if (requestError.status === 401) onSessionExpired()
    } finally {
      setLoading(false)
    }
  }, [initialCode, onSessionExpired, selectedCode])

  const loadDiscussions = useCallback(async () => {
    if (!selectedCompetency) {
      setDiscussions([])
      setTopics([])
      setSelectedDiscussionId(null)
      return
    }
    try {
      const items = await listDiscussions(selectedCompetency.id)
      setDiscussions(items)
      if (!items.some(item => item.id === selectedDiscussionId)) {
        setSelectedDiscussionId(items[0]?.id ?? null)
      }
    } catch (requestError) {
      setError(messageFor(requestError))
      if (requestError.status === 401) onSessionExpired()
    }
  }, [onSessionExpired, selectedCompetency, selectedDiscussionId])

  const loadTopics = useCallback(async () => {
    if (!selectedCompetency || !selectedDiscussion) {
      setTopics([])
      return
    }
    try {
      setTopics(await listTopics(selectedCompetency.id, selectedDiscussion.id))
    } catch (requestError) {
      setError(messageFor(requestError))
      if (requestError.status === 401) onSessionExpired()
    }
  }, [onSessionExpired, selectedCompetency, selectedDiscussion])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadCompetencies() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadCompetencies])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadDiscussions() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadDiscussions])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadTopics() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadTopics])

  async function submitCompetency(event) {
    event.preventDefault()
    await run(async () => {
      await updateCompetency(selectedCompetency.id, {
        code: selectedCompetency.code,
        title: event.currentTarget.title.value.trim(),
        description: event.currentTarget.description.value.trim(),
        displayOrder: Number(event.currentTarget.displayOrder.value),
      })
      setEditingCompetency(false)
      await loadCompetencies()
    }, 'Competency updated.')
  }

  async function createSelectedCompetency(event) {
    event.preventDefault()
    const form = event.currentTarget
    await run(async () => {
      await createCompetency({
        code: selectedCode,
        title: form.title.value.trim(),
        description: form.description.value.trim(),
        displayOrder: Number(form.displayOrder.value),
      })
      setCreatingCompetency(false)
      await loadCompetencies()
    }, `${selectedCode} created.`)
  }

  async function initializeDocumentedStructure() {
    await run(async () => {
      const result = await initializeCourseStructure()
      await loadCompetencies()
      setNotice(`Course structure is ready: ${result.competencyCount} competencies, ${result.discussionCount} Discussions, ${result.topicCount} topics, and ${result.learningStageCount} stage records. Add and review learning content separately before students use it.`)
    }, '')
  }

  async function submitDiscussion(event) {
    event.preventDefault()
    const form = event.currentTarget
    const body = { title: form.title.value.trim(), description: form.description.value.trim(), displayOrder: Number(form.displayOrder.value) }
    await run(async () => {
      const result = discussionForm?.id
        ? await updateDiscussion(selectedCompetency.id, discussionForm.id, body)
        : await createDiscussion(selectedCompetency.id, body)
      setDiscussionForm(null)
      await loadDiscussions()
      if (result?.id) setSelectedDiscussionId(result.id)
    }, discussionForm?.id ? 'Discussion updated.' : 'Discussion created with its seven learning-stage records.')
  }

  async function submitTopic(event) {
    event.preventDefault()
    const form = event.currentTarget
    const body = { title: form.title.value.trim(), description: form.description.value.trim(), displayOrder: Number(form.displayOrder.value) }
    await run(async () => {
      if (topicForm?.id) await updateTopic(selectedCompetency.id, selectedDiscussion.id, topicForm.id, body)
      else await createTopic(selectedCompetency.id, selectedDiscussion.id, body)
      setTopicForm(null)
      await loadTopics()
    }, topicForm?.id ? 'Topic updated.' : 'Topic created.')
  }

  async function changeStatus(label, update, reload) {
    if (!window.confirm(`${update.active ? 'Activate' : 'Deactivate'} this ${label}?`)) return
    await run(async () => { await update.action(); await reload() }, `${label} status updated.`)
  }

  function beginDiscussion(item = null) {
    setDiscussionForm(item ? { ...item } : { ...emptyForm })
    setError('')
    setNotice('')
  }

  function beginTopic(item = null) {
    setTopicForm(item ? { ...item } : { ...emptyForm })
    setError('')
    setNotice('')
  }

  if (loading) return <section className="rounded-2xl border border-slate-200 bg-white p-6 text-slate-600">Loading course structure…</section>

  return (
    <section className="space-y-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Course setup</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">{selectedCode === 'COC1' ? 'COC 1' : 'COC 2'} structure</h1>
        <p className="mt-2 text-sm text-slate-600">Manage competencies, ordered Discussions, and their topics. Order values must be unique within each parent.</p>
      </div>

      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {notice && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}

      <div className="flex flex-wrap items-center gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
        <label className="text-sm font-semibold text-slate-700" htmlFor="course-code">Course</label>
        <select id="course-code" value={selectedCode} onChange={event => { setSelectedCode(event.target.value); setSelectedDiscussionId(null); setDiscussionForm(null); setTopicForm(null) }} className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm">
          <option value="COC1">COC 1</option><option value="COC2">COC 2</option>
        </select>
        <button type="button" onClick={() => run(loadCompetencies, '')} disabled={busy} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-sky-950 hover:bg-slate-50 disabled:opacity-60">Refresh</button>
      </div>

      {!selectedCompetency ? (
        <div className="rounded-2xl border border-amber-200 bg-amber-50 p-5 shadow-sm">
          <h2 className="font-bold text-amber-950">Set up {selectedCode === 'COC1' ? 'COC 1' : 'COC 2'}</h2>
          <p className="mt-1 text-sm text-amber-900">No course record exists yet. Initialize both COC structures from the documented course and learning-content organization, or create this record manually.</p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button type="button" onClick={initializeDocumentedStructure} disabled={busy} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Initialize COC 1 and COC 2 structures</button>
            <button type="button" onClick={() => setCreatingCompetency(value => !value)} disabled={busy} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-sky-950 disabled:opacity-60">{creatingCompetency ? 'Cancel manual setup' : 'Create this course record manually'}</button>
          </div>
          {creatingCompetency && <form onSubmit={createSelectedCompetency} className="mt-4 grid gap-3 border-t border-amber-300 pt-4 sm:grid-cols-2">
            <label className="text-sm font-medium text-slate-800">Title<input name="title" required maxLength={200} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label>
            <label className="text-sm font-medium text-slate-800">Display order<input name="displayOrder" type="number" min="0" max="4294967295" required defaultValue="0" className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label>
            <label className="text-sm font-medium text-slate-800 sm:col-span-2">Description<textarea name="description" rows="2" className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label>
            <button disabled={busy} className="w-fit rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Create course record</button>
          </form>}
        </div>
      ) : (
        <>
          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div><h2 className="text-lg font-bold text-slate-900">{selectedCompetency.title}</h2><p className="mt-1 text-sm text-slate-600">{selectedCompetency.description || 'No description provided.'} · Order {selectedCompetency.displayOrder}</p></div>
              <div className="flex gap-2">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${selectedCompetency.active ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'}`}>{selectedCompetency.active ? 'Active' : 'Inactive'}</span>
                <button type="button" onClick={() => setEditingCompetency(value => !value)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-sky-950">{editingCompetency ? 'Cancel edit' : 'Edit'}</button>
                <button type="button" disabled={busy} onClick={() => changeStatus('competency', { active: !selectedCompetency.active, action: () => setCompetencyStatus(selectedCompetency.id, !selectedCompetency.active) }, loadCompetencies)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-semibold text-sky-950 disabled:opacity-60">{selectedCompetency.active ? 'Deactivate' : 'Activate'}</button>
              </div>
            </div>
            {editingCompetency && <form key={selectedCompetency.id} onSubmit={submitCompetency} className="mt-4 grid gap-3 border-t border-slate-100 pt-4 sm:grid-cols-2"><label className="text-sm font-medium">Title<input name="title" required maxLength={200} defaultValue={selectedCompetency.title} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label><label className="text-sm font-medium">Display order<input name="displayOrder" type="number" min="0" max="4294967295" required defaultValue={selectedCompetency.displayOrder} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label><label className="text-sm font-medium sm:col-span-2">Description<textarea name="description" rows="2" defaultValue={selectedCompetency.description || ''} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2" /></label><button disabled={busy} className="w-fit rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Save competency</button></form>}
          </section>

          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center justify-between gap-3"><div><h2 className="text-lg font-bold text-slate-900">Discussions</h2><p className="text-sm text-slate-600">Ordered within this competency</p></div><button type="button" onClick={() => beginDiscussion()} className="rounded-lg bg-teal-800 px-3 py-2 text-sm font-semibold text-white hover:bg-teal-900">Add Discussion</button></div>
              {discussionForm && <form onSubmit={submitDiscussion} className="mt-4 space-y-3 rounded-xl bg-slate-50 p-4"><h3 className="font-semibold text-slate-900">{discussionForm.id ? 'Edit Discussion' : 'New Discussion'}</h3><label className="block text-sm font-medium">Title<input name="title" required maxLength={250} defaultValue={discussionForm.title} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><label className="block text-sm font-medium">Description<textarea name="description" rows="2" defaultValue={discussionForm.description || ''} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><label className="block text-sm font-medium">Display order<input name="displayOrder" type="number" min="0" max="4294967295" required defaultValue={discussionForm.displayOrder} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><div className="flex gap-2"><button disabled={busy} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Save</button><button type="button" onClick={() => setDiscussionForm(null)} className="rounded-lg border border-slate-300 px-4 py-2 text-sm">Cancel</button></div></form>}
              {discussions.length === 0 ? <p className="mt-5 rounded-lg bg-slate-50 p-4 text-sm text-slate-600">No Discussions have been added.</p> : <ol className="mt-4 divide-y divide-slate-100">{discussions.map(item => <li key={item.id} className={`py-3 ${item.id === selectedDiscussionId ? 'rounded-lg bg-sky-50 px-3' : ''}`}><div className="flex items-start justify-between gap-3"><button type="button" onClick={() => setSelectedDiscussionId(item.id)} className="min-w-0 text-left"><span className="block font-semibold text-slate-900">{item.displayOrder}. {item.title}</span><span className="mt-1 block text-xs text-slate-500">{item.active ? 'Active' : 'Inactive'}{item.description ? ` · ${item.description}` : ''}</span></button><div className="flex shrink-0 gap-2"><button type="button" onClick={() => beginDiscussion(item)} className="text-sm font-semibold text-teal-800 underline">Edit</button><button type="button" onClick={() => changeStatus('Discussion', { active: !item.active, action: () => setDiscussionStatus(selectedCompetency.id, item.id, !item.active) }, loadDiscussions)} className="text-sm font-semibold text-teal-800 underline">{item.active ? 'Deactivate' : 'Activate'}</button></div></div></li>)}</ol>}
            </section>

            <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex items-center justify-between gap-3"><div><h2 className="text-lg font-bold text-slate-900">Topics</h2><p className="text-sm text-slate-600">{selectedDiscussion ? `For ${selectedDiscussion.title}` : 'Select a Discussion to manage its topics'}</p></div><button type="button" disabled={!selectedDiscussion} onClick={() => beginTopic()} className="rounded-lg bg-teal-800 px-3 py-2 text-sm font-semibold text-white hover:bg-teal-900 disabled:opacity-50">Add Topic</button></div>
              {topicForm && <form onSubmit={submitTopic} className="mt-4 space-y-3 rounded-xl bg-slate-50 p-4"><h3 className="font-semibold text-slate-900">{topicForm.id ? 'Edit topic' : 'New topic'}</h3><label className="block text-sm font-medium">Title<input name="title" required maxLength={250} defaultValue={topicForm.title} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><label className="block text-sm font-medium">Description<textarea name="description" rows="2" defaultValue={topicForm.description || ''} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><label className="block text-sm font-medium">Display order<input name="displayOrder" type="number" min="0" max="4294967295" required defaultValue={topicForm.displayOrder} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" /></label><div className="flex gap-2"><button disabled={busy} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">Save</button><button type="button" onClick={() => setTopicForm(null)} className="rounded-lg border border-slate-300 px-4 py-2 text-sm">Cancel</button></div></form>}
              {!selectedDiscussion ? <p className="mt-5 rounded-lg bg-slate-50 p-4 text-sm text-slate-600">Choose a Discussion to view its topics.</p> : topics.length === 0 ? <p className="mt-5 rounded-lg bg-slate-50 p-4 text-sm text-slate-600">No topics have been added to this Discussion.</p> : <ol className="mt-4 divide-y divide-slate-100">{topics.map(item => <li key={item.id} className="py-3"><div className="flex items-start justify-between gap-3"><div><span className="font-semibold text-slate-900">{item.displayOrder}. {item.title}</span><span className="mt-1 block text-xs text-slate-500">{item.active ? 'Active' : 'Inactive'}{item.description ? ` · ${item.description}` : ''}</span></div><div className="flex shrink-0 gap-2"><button type="button" onClick={() => beginTopic(item)} className="text-sm font-semibold text-teal-800 underline">Edit</button><button type="button" onClick={() => changeStatus('topic', { active: !item.active, action: () => setTopicStatus(selectedCompetency.id, selectedDiscussion.id, item.id, !item.active) }, loadTopics)} className="text-sm font-semibold text-teal-800 underline">{item.active ? 'Deactivate' : 'Activate'}</button></div></div></li>)}</ol>}
            </section>
          </div>
        </>
      )}
    </section>
  )
}

export default CourseStructureManagement
