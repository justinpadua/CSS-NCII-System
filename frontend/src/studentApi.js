import { requestAdminApi } from './adminAuth.js'

const STUDENTS_PATH = '/api/admin/students'

export function getStudents() {
  return requestAdminApi(STUDENTS_PATH)
}

export function createStudent(student) {
  return requestAdminApi(STUDENTS_PATH, { method: 'POST', body: student })
}

export function updateStudent(id, student) {
  return requestAdminApi(`${STUDENTS_PATH}/${id}`, { method: 'PUT', body: student })
}

export function getPendingStudents() {
  return requestAdminApi(`${STUDENTS_PATH}/pending`)
}

export function reviewStudent(id, decision) {
  return requestAdminApi(`${STUDENTS_PATH}/${id}/review`, { method: 'PATCH', body: { decision } })
}

export function updateStudentStatus(id, status) {
  return requestAdminApi(`${STUDENTS_PATH}/${id}/status`, {
    method: 'PATCH',
    body: { status },
  })
}

export function updateStudentNotes(id, adminNotes) {
  return requestAdminApi(`${STUDENTS_PATH}/${id}/notes`, { method: 'PUT', body: { adminNotes } })
}

export function resetStudentPassword(id, newPassword) {
  return requestAdminApi(`${STUDENTS_PATH}/${id}/password-reset`, { method: 'POST', body: { newPassword } })
}

export function grantStudentRetake(studentId, competencyId) {
  return requestAdminApi(`/api/admin/students/${studentId}/competencies/${competencyId}/retake-authorizations`, { method: 'POST' })
}

export function getCompetencies() {
  return requestAdminApi('/api/admin/competencies')
}
