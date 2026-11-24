import { useCallback, useEffect, useMemo, useState } from 'react'
import { createStudent, getCompetencies, getStudents, grantStudentRetake, resetStudentPassword, reviewStudent, updateStudent, updateStudentNotes, updateStudentStatus } from './studentApi.js'

const emptyStudent = { fullName: '', program: '', initialPassword: '' }
function safeMessage(error, fallback) { return [400, 404, 409].includes(error.status) ? error.message : fallback }

function StudentManagement({ onSessionExpired }) {
  const [students, setStudents] = useState([])
  const [competencies, setCompetencies] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [search, setSearch] = useState('')
  const [formMode, setFormMode] = useState(null)
  const [selectedStudent, setSelectedStudent] = useState(null)
  const [draft, setDraft] = useState(emptyStudent)
  const [fieldError, setFieldError] = useState('')
  const [formError, setFormError] = useState('')
  const [notice, setNotice] = useState('')
  const [saving, setSaving] = useState(false)
  const [busyId, setBusyId] = useState(null)
  const [retakeCompetency, setRetakeCompetency] = useState('')

  const loadStudents = useCallback(async () => {
    setLoading(true)
    setLoadError('')
    try {
      const records = await getStudents()
      if (!Array.isArray(records)) throw new Error('Unexpected student response.')
      setStudents(records)
    } catch (error) {
      if (error.status === 401) onSessionExpired()
      else setLoadError(safeMessage(error, 'Unable to load student records. Please try again.'))
    } finally { setLoading(false) }
  }, [onSessionExpired])

  useEffect(() => {
    let active = true
    const timer = window.setTimeout(() => {
      if (!active) return
      void loadStudents()
      getCompetencies().then(result => { if (active) setCompetencies(result) }).catch(() => {
        if (active) setCompetencies([])
      })
    }, 0)
    return () => {
      active = false
      window.clearTimeout(timer)
    }
  }, [loadStudents])

  const filteredStudents = useMemo(() => {
    const query = search.trim().toLocaleLowerCase()
    return query ? students.filter(student => [student.accountId, student.fullName, student.program]
      .some(value => value.toLocaleLowerCase().includes(query))) : students
  }, [search, students])

  function openForm(mode, student = null) {
    setSelectedStudent(student)
    setFormMode(mode)
    setFieldError('')
    setFormError('')
    if (mode === 'create') setDraft(emptyStudent)
    else if (mode === 'edit') setDraft({ fullName: student.fullName, program: student.program })
    else if (mode === 'notes') setDraft({ adminNotes: student.adminNotes || '' })
    else if (mode === 'password') setDraft({ newPassword: '' })
  }

  async function submitForm(event) {
    event.preventDefault()
    let message = ''
    if (['create', 'edit'].includes(formMode)) {
      if (!draft.fullName?.trim()) message = 'Full Name is required.'
      else if (draft.fullName.trim().length > 200) message = 'Full Name must be 200 characters or fewer.'
      else if (!draft.program?.trim()) message = 'Program is required.'
      else if (draft.program.trim().length > 150) message = 'Program must be 150 characters or fewer.'
    }
    if (formMode === 'create' && (!draft.initialPassword || draft.initialPassword.length < 8)) message = 'Initial password must contain at least 8 characters.'
    if (formMode === 'password' && (!draft.newPassword || draft.newPassword.length < 8)) message = 'New password must contain at least 8 characters.'
    setFieldError(message)
    if (message) return
    setSaving(true)
    setFormError('')
    setNotice('')
    try {
      if (formMode === 'create') {
        const result = await createStudent({ ...draft, fullName: draft.fullName.trim(), program: draft.program.trim() })
        setNotice(`Account ${result.accountId} was created as PENDING. Give its initial password to the student through a trusted channel.${result.possibleDuplicate ? ' The record is flagged for duplicate review.' : ''}`)
      } else if (formMode === 'edit') {
        await updateStudent(selectedStudent.id, { fullName: draft.fullName.trim(), program: draft.program.trim() })
        setNotice('Student information updated.')
      } else if (formMode === 'notes') {
        await updateStudentNotes(selectedStudent.id, draft.adminNotes)
        setNotice('Private Admin notes saved.')
      } else if (formMode === 'password') {
        await resetStudentPassword(selectedStudent.id, draft.newPassword)
        setNotice('Student password reset. Existing Student sessions were invalidated.')
      }
      setFormMode(null)
      setSelectedStudent(null)
      await loadStudents()
    } catch (error) {
      if (error.status === 401) onSessionExpired()
      else setFormError(safeMessage(error, 'Unable to save this change. Please try again.'))
    } finally { setSaving(false) }
  }

  async function runAction(student, label, action, confirmation) {
    if (confirmation && !window.confirm(confirmation)) return
    setBusyId(student.id)
    setLoadError('')
    setNotice('')
    try {
      const result = await action()
      setNotice(label === 'retake' ? `Additional attempts authorized: ${result.attemptsGranted}.` : `Student ${label} successfully.`)
      await loadStudents()
    } catch (error) {
      if (error.status === 401) onSessionExpired()
      else setLoadError(safeMessage(error, `Unable to ${label} this student. Please try again.`))
    } finally { setBusyId(null) }
  }

  function review(student, decision) {
    const action = decision === 'APPROVE' ? 'approve' : 'reject'
    runAction(student, action, () => reviewStudent(student.id, decision), `${action} ${student.fullName}'s registration?`)
  }

  function setStatus(student, status) {
    const action = status === 'ACTIVE' ? 'reactivate' : 'deactivate'
    runAction(student, action, () => updateStudentStatus(student.id, status), `${action} ${student.fullName}? Their learning and assessment history will be preserved.`)
  }

  function grantRetake(student) {
    if (!retakeCompetency) { setLoadError('Select a competency before granting additional attempts.'); return }
    runAction(student, 'retake', () => grantStudentRetake(student.id, Number(retakeCompetency)), `Grant three additional attempts to ${student.fullName}?`)
  }

  return (
    <section aria-labelledby="students-heading" className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div><p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p><h1 id="students-heading" className="mt-1 text-2xl font-bold text-slate-900">Student Management</h1><p className="mt-2 max-w-2xl text-sm leading-6 text-slate-600">Review registrations and manage student accounts without deleting their learning history.</p></div>
        <button type="button" onClick={() => openForm('create')} className="rounded-lg bg-teal-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-900">Add Student</button>
      </div>
      {notice && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-800">{notice}</p>}
      {loadError && <div role="alert" className="flex flex-col gap-3 rounded-lg border border-red-200 bg-red-50 p-4 text-sm text-red-800 sm:flex-row sm:items-center sm:justify-between"><p>{loadError}</p><button type="button" onClick={loadStudents} className="self-start font-semibold underline sm:self-auto">Refresh</button></div>}
      {formMode && <form onSubmit={submitForm} noValidate className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
        <div className="flex items-start justify-between gap-3"><div><h2 className="text-lg font-bold text-slate-900">{formMode === 'create' ? 'Create Pending Student Account' : formMode === 'edit' ? 'Edit Student' : formMode === 'notes' ? 'Private Admin Notes' : 'Reset Student Password'}</h2><p className="mt-1 text-sm text-slate-600">{selectedStudent?.accountId || 'A generated Account ID will be assigned.'}</p></div><button type="button" disabled={saving} onClick={() => setFormMode(null)} className="rounded-md px-2 py-1 text-sm text-slate-600 hover:bg-slate-100">Cancel</button></div>
        {formError && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{formError}</p>}
        {fieldError && <p role="alert" className="mt-4 text-sm text-red-700">{fieldError}</p>}
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {['create', 'edit'].includes(formMode) && <>
            <StudentField id="student-full-name" label="Full Name" value={draft.fullName || ''} onChange={value => setDraft(current => ({ ...current, fullName: value }))} />
            <StudentField id="student-program" label="Program" value={draft.program || ''} onChange={value => setDraft(current => ({ ...current, program: value }))} />
          </>}
          {formMode === 'create' && <StudentField id="initial-password" type="password" label="Initial Password (at least 8 characters)" value={draft.initialPassword} onChange={value => setDraft(current => ({ ...current, initialPassword: value }))} />}
          {formMode === 'password' && <StudentField id="new-password" type="password" label="New Password (at least 8 characters)" value={draft.newPassword} onChange={value => setDraft(current => ({ ...current, newPassword: value }))} />}
          {formMode === 'notes' && <label htmlFor="admin-notes" className="block text-sm font-medium text-slate-700 md:col-span-2">Private notes<textarea id="admin-notes" maxLength={65535} rows={4} value={draft.adminNotes} onChange={event => setDraft(current => ({ ...current, adminNotes: event.target.value }))} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>}
        </div>
        <button type="submit" disabled={saving} className="mt-5 rounded-lg bg-teal-800 px-4 py-2.5 text-sm font-semibold text-white hover:bg-teal-900 disabled:opacity-60">{saving ? 'Saving…' : 'Save'}</button>
      </form>}
      <div className="rounded-2xl border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-200 p-4 sm:flex-row sm:items-center sm:justify-between sm:p-5"><div><h2 className="font-bold text-slate-900">Student Records</h2><p className="mt-1 text-sm text-slate-600">Search by Account ID, Full Name, or Program.</p></div><label className="block w-full sm:max-w-sm"><span className="sr-only">Search students</span><input type="search" value={search} onChange={event => setSearch(event.target.value)} placeholder="Search student accounts…" className="w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label></div>
        <div className="overflow-x-auto"><table className="w-full min-w-[920px] border-collapse text-left text-sm"><thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-600"><tr><th className="px-4 py-3">Account ID</th><th className="px-4 py-3">Full Name</th><th className="px-4 py-3">Program</th><th className="px-4 py-3">Status</th><th className="px-4 py-3">Actions</th></tr></thead><tbody className="divide-y divide-slate-100">
          {loading ? <tr><td colSpan="5" className="px-4 py-10 text-center text-slate-600">Loading student records…</td></tr> : filteredStudents.length === 0 ? <tr><td colSpan="5" className="px-4 py-10 text-center text-slate-600">{loadError ? 'Student records are unavailable until loading succeeds.' : students.length ? 'No students match your search.' : 'No student records yet.'}</td></tr> : filteredStudents.map(student => <tr key={student.id} className="align-top hover:bg-slate-50/70">
            <td className="whitespace-nowrap px-4 py-3 font-medium">{student.accountId}</td><td className="px-4 py-3">{student.fullName}{student.possibleDuplicate && <span className="mt-1 block text-xs font-semibold text-amber-700">Possible duplicate — review</span>}</td><td className="px-4 py-3">{student.program}</td>
            <td className="px-4 py-3"><span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${student.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : student.status === 'PENDING' ? 'bg-amber-100 text-amber-800' : student.status === 'REJECTED' ? 'bg-rose-100 text-rose-800' : 'bg-slate-200 text-slate-700'}`}>{student.status}</span></td>
            <td className="px-4 py-3"><div className="flex max-w-xl flex-wrap gap-2">
              <button type="button" onClick={() => openForm('edit', student)} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold">Edit</button>
              {(student.status === 'PENDING' || student.status === 'REJECTED') && <><button type="button" disabled={busyId === student.id} onClick={() => review(student, 'APPROVE')} className="rounded-md bg-emerald-700 px-3 py-1.5 text-xs font-semibold text-white disabled:opacity-50">Approve</button><button type="button" disabled={busyId === student.id} onClick={() => review(student, 'REJECT')} className="rounded-md border border-rose-300 px-3 py-1.5 text-xs font-semibold text-rose-800 disabled:opacity-50">Reject</button></>}
              {student.status === 'ACTIVE' && <button type="button" disabled={busyId === student.id} onClick={() => setStatus(student, 'DEACTIVATED')} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold">Deactivate</button>}
              {student.status === 'DEACTIVATED' && <button type="button" disabled={busyId === student.id} onClick={() => setStatus(student, 'ACTIVE')} className="rounded-md border border-emerald-300 px-3 py-1.5 text-xs font-semibold text-emerald-800">Reactivate</button>}
              <button type="button" onClick={() => openForm('notes', student)} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold">Notes</button><button type="button" onClick={() => openForm('password', student)} className="rounded-md border border-slate-300 px-3 py-1.5 text-xs font-semibold">Reset Password</button>
              {student.status === 'ACTIVE' && competencies.length > 0 && <div className="flex gap-1"><select aria-label={`Competency for ${student.accountId} retake`} value={retakeCompetency} onChange={event => setRetakeCompetency(event.target.value)} className="max-w-32 rounded-md border border-slate-300 px-2 py-1 text-xs"><option value="">COC</option>{competencies.map(item => <option key={item.id} value={item.id}>{item.code}</option>)}</select><button type="button" disabled={busyId === student.id || !retakeCompetency} onClick={() => grantRetake(student)} className="rounded-md border border-slate-300 px-2 py-1.5 text-xs font-semibold disabled:opacity-50">Grant retake</button></div>}
            </div></td></tr>)}
        </tbody></table></div>
        {!loading && !loadError && students.length > 0 && <p className="border-t border-slate-100 px-4 py-3 text-xs text-slate-500">Showing {filteredStudents.length} of {students.length} students.</p>}
      </div>
    </section>
  )
}

function StudentField({ id, label, type = 'text', value, onChange }) {
  return <label htmlFor={id} className="block text-sm font-medium text-slate-700">{label}<input id={id} type={type} required value={value} onChange={event => onChange(event.target.value)} className="mt-1 block w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" /></label>
}

export default StudentManagement
