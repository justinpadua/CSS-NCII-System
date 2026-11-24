import { requestAdminApi } from './adminAuth.js'

export function getAdminDashboardSummary() {
  return requestAdminApi('/api/admin/dashboard')
}

export function getStudentProgressPrograms() {
  return requestAdminApi('/api/admin/student-progress/filters/programs')
}

export function getStudentProgressPage({ search = '', program = '', status = '', page = 0, size = 20 }) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (search.trim()) params.set('search', search.trim())
  if (program) params.set('program', program)
  if (status) params.set('status', status)
  return requestAdminApi(`/api/admin/student-progress?${params.toString()}`)
}

export function getStudentProgress(studentId) {
  return requestAdminApi(`/api/admin/student-progress/${studentId}`)
}
