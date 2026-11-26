export async function restoreSession(currentSession, { getCurrentAdmin, getCurrentStudent }) {
  if (currentSession.admin) {
    const admin = await getCurrentAdmin()
    if (admin) return { admin, student: null }

    const student = await getCurrentStudent()
    return { admin: null, student }
  }

  const student = await getCurrentStudent()
  if (student) return { admin: null, student }

  const admin = await getCurrentAdmin()
  return { admin, student: null }
}
