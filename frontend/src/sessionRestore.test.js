import assert from 'node:assert/strict'
import test from 'node:test'
import { restoreSession } from './sessionRestore.js'

test('an active Student session is restored without probing the Admin endpoint', async () => {
  const student = { accountId: 'STU-000001' }
  let adminChecks = 0
  let studentChecks = 0

  const result = await restoreSession(
    { admin: null, student },
    {
      getCurrentAdmin: async () => { adminChecks += 1; return null },
      getCurrentStudent: async () => { studentChecks += 1; return student },
    },
  )

  assert.deepEqual(result, { admin: null, student })
  assert.equal(studentChecks, 1)
  assert.equal(adminChecks, 0)
})

test('initial restore checks Student first and skips Admin when Student is signed in', async () => {
  const student = { accountId: 'STU-000002' }
  const calls = []

  const result = await restoreSession(
    { admin: null, student: null },
    {
      getCurrentStudent: async () => { calls.push('student'); return student },
      getCurrentAdmin: async () => { calls.push('admin'); return null },
    },
  )

  assert.deepEqual(result, { admin: null, student })
  assert.deepEqual(calls, ['student'])
})

test('an active Admin session is checked directly without probing Student', async () => {
  const admin = { username: 'admin' }
  const calls = []

  const result = await restoreSession(
    { admin, student: null },
    {
      getCurrentAdmin: async () => { calls.push('admin'); return admin },
      getCurrentStudent: async () => { calls.push('student'); return null },
    },
  )

  assert.deepEqual(result, { admin, student: null })
  assert.deepEqual(calls, ['admin'])
})

test('after a remembered Student session ends, checks once for a role switch to Admin', async () => {
  const admin = { username: 'admin' }
  const calls = []

  const result = await restoreSession(
    { admin: null, student: { accountId: 'STU-000003' } },
    {
      getCurrentStudent: async () => { calls.push('student'); return null },
      getCurrentAdmin: async () => { calls.push('admin'); return admin },
    },
  )

  assert.deepEqual(result, { admin, student: null })
  assert.deepEqual(calls, ['student', 'admin'])
})
