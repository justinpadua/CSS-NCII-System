const API_BASE_URL = import.meta.env?.VITE_API_BASE_URL ?? ''

async function readResponse(response) {
  if (response.status === 401) {
    const error = new Error('Your Admin session has expired. Please log in again.')
    error.status = 401
    throw error
  }

  if (!response.ok) {
    const defaultMessages = {
      400: 'Please review the information and try again.',
      404: 'The requested record could not be found.',
      409: 'This record conflicts with an existing record.',
    }
    let message = defaultMessages[response.status] ?? 'The request could not be completed. Please try again.'
    try {
      const body = await response.json()
      if (typeof body.message === 'string') message = body.message
    } catch {
      // Keep the generic message when the server response is not JSON.
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  if (response.status === 204) return null
  return response.json()
}

async function getCsrfToken() {
  const response = await fetch(`${API_BASE_URL}/api/admin/auth/csrf`, {
    credentials: 'include',
  })
  return readResponse(response)
}

async function requestWithCsrf(path, method, body) {
  for (let attempt = 0; attempt < 2; attempt += 1) {
    const csrf = await getCsrfToken()
    const isMultipart = typeof FormData !== 'undefined' && body instanceof FormData
    const headers = { [csrf.headerName]: csrf.token }
    if (!isMultipart) headers['Content-Type'] = 'application/json'
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method,
      credentials: 'include',
      headers,
      body: body === undefined ? undefined : isMultipart ? body : JSON.stringify(body),
    })

    if (response.status === 403 && attempt === 0) continue
    return readResponse(response)
  }
}

async function postWithCsrf(path, body) {
  return requestWithCsrf(path, 'POST', body)
}

export async function requestAdminApi(path, { method = 'GET', body } = {}) {
  const normalizedMethod = method.toUpperCase()
  if (['GET', 'HEAD', 'OPTIONS'].includes(normalizedMethod)) {
    const response = await fetch(`${API_BASE_URL}${path}`, {
      method: normalizedMethod,
      credentials: 'include',
    })
    return readResponse(response)
  }

  return requestWithCsrf(path, normalizedMethod, body)
}

export async function getCurrentAdmin() {
  const response = await fetch(`${API_BASE_URL}/api/admin/auth/me`, {
    credentials: 'include',
  })
  // This is an authentication probe: 401 means no Admin session and 403
  // commonly means the browser holds an authenticated Student session.
  if (response.status === 401 || response.status === 403) return null
  return readResponse(response)
}

export async function isAdminSessionEnded() {
  const response = await fetch(`${API_BASE_URL}/api/admin/auth/me`, {
    credentials: 'include',
  })
  if (response.status === 401 || response.status === 403) return true
  if (response.ok) return false
  await readResponse(response)
  return false
}

export async function loginAdmin(username, password) {
  const admin = await postWithCsrf('/api/admin/auth/login', { username, password })
  // Authentication clears the previous CSRF token, so establish one for the session.
  await getCsrfToken()
  return admin
}

export async function logoutAdmin() {
  return postWithCsrf('/api/admin/auth/logout')
}
