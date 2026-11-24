import assert from 'node:assert/strict'
import test from 'node:test'
import { attemptResultText, progressPercentage, submittedAttemptCount } from './src/adminReportingView.js'

test('progress display handles normal and empty denominators', () => {
  assert.equal(progressPercentage(2, 7), 29)
  assert.equal(progressPercentage(7, 7), 100)
  assert.equal(progressPercentage(0, 0), 0)
})

test('in-progress attempts are not counted as submissions or scored results', () => {
  const attempts = [
    { status: 'SUBMITTED', score: 7, totalPossibleScore: 10, percentage: 70, passed: true },
    { status: 'IN_PROGRESS', score: null, totalPossibleScore: null, percentage: null, passed: null },
  ]
  assert.equal(submittedAttemptCount(attempts), 1)
  assert.equal(attemptResultText(attempts[1]), 'In progress; not counted as submitted')
  assert.equal(attemptResultText(attempts[0]), '7/10 (70%) · Passed')
})

test('failed submitted attempts keep their actual score and result', () => {
  assert.equal(attemptResultText({
    status: 'SUBMITTED', score: 6, totalPossibleScore: 10, percentage: 60, passed: false,
  }), '6/10 (60%) · Failed')
})
