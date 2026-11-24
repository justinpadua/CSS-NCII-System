const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function readResponse(response) {
  if (response.status === 204) return null
  if (!response.ok) {
    const fallback = response.status === 401
      ? 'Invalid Account ID or password, or the account is not active.'
      : response.status === 404
        ? 'The requested learning item or assessment is not configured yet.'
        : response.status === 409
          ? 'This learning stage or assessment is not ready yet.'
          : response.status >= 500
            ? 'The server could not load this learning item. Please try again or contact your instructor.'
            : `The Student request could not be completed (HTTP ${response.status}). Please try again.`
    let message = fallback
    try {
      const body = await response.json()
      if (typeof body.message === 'string') message = body.message
      else if (typeof body.detail === 'string') message = body.detail
    } catch {
      // Keep the safe default for non-JSON responses.
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }
  return response.json()
}

async function getCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/api/student/auth/csrf`, { credentials: 'include' })
  return readResponse(response)
}

async function postWithCsrf(path, body) {
  const csrf = await getCsrfToken()
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
  })
  return readResponse(response)
}

export async function studentApiRequest(path, { method = 'GET', body } = {}) {
  const options = { method, credentials: 'include', headers: {} }
  if (body !== undefined) {
    const csrf = await getCsrfToken()
    options.headers['Content-Type'] = 'application/json'
    options.headers[csrf.headerName] = csrf.token
    options.body = JSON.stringify(body)
  } else if (method !== 'GET' && method !== 'HEAD') {
    const csrf = await getCsrfToken()
    options.headers[csrf.headerName] = csrf.token
  }
  const response = await fetch(`${API_BASE_URL}${path}`, options)
  return readResponse(response)
}

export async function loginStudent({ accountId, password }) {
  const result = await postWithCsrf('/api/student/auth/login', { accountId, password })
  if (result.authenticated) await getCsrfToken()
  return result
}

export function registerStudent({ fullName, program, password }) {
  return postWithCsrf('/api/student/auth/register', { fullName, program, password })
}

export async function getCurrentStudent() {
  const response = await fetch(`${API_BASE_URL}/api/student/auth/me`, { credentials: 'include' })
  if (response.status === 401 || response.status === 403) return null
  return readResponse(response)
}

export function logoutStudent() {
  return postWithCsrf('/api/student/auth/logout')
}

export async function isStudentSessionEnded() {
  const response = await fetch(`${API_BASE_URL}/api/student/auth/me`, { credentials: 'include' })
  if (response.status === 401) return true
  if (response.ok) return false
  return readResponse(response).then(() => false)
}
