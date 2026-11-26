import assert from 'node:assert/strict'
import test from 'node:test'
import { getCurrentAdmin, isAdminSessionEnded } from './adminAuth.js'

test('Admin session probe treats 401 and role-mismatch 403 as no Admin session', async t => {
  let status = 403
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status }))
  assert.equal(await getCurrentAdmin(), null)

  status = 401
  assert.equal(await getCurrentAdmin(), null)
})

test('Admin session probe does not swallow unrelated server errors', async t => {
  t.mock.method(globalThis, 'fetch', async () => new Response('{}', {
    status: 500,
    headers: { 'Content-Type': 'application/json' },
  }))
  await assert.rejects(getCurrentAdmin(), error => error.status === 500)
})

test('Admin logout verification treats 403 as ended Admin access', async t => {
  t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 403 }))
  assert.equal(await isAdminSessionEnded(), true)
})
