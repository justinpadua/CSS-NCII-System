import assert from 'node:assert/strict'
import test from 'node:test'
import {
  assessmentCreationRequest,
  assessmentTypeForScope,
  DISCUSSION_ASSESSMENT_TYPES,
  GENERALIZED_ASSESSMENT_TYPE,
} from './assessmentManagementState.js'

test('switching to Generalized Post-test selects its required type', () => {
  assert.equal(assessmentTypeForScope('generalized', 'PRE_TEST'), GENERALIZED_ASSESSMENT_TYPE)
})

test('switching back to Discussion preserves a valid Discussion type or chooses Pre-test', () => {
  assert.equal(assessmentTypeForScope('discussion', 'DISCUSSION_POST_TEST'), 'DISCUSSION_POST_TEST')
  assert.equal(assessmentTypeForScope('discussion', GENERALIZED_ASSESSMENT_TYPE), 'PRE_TEST')
  assert.ok(DISCUSSION_ASSESSMENT_TYPES.includes(assessmentTypeForScope('discussion', 'stale type')))
})

test('Generalized creation overrides stale type state and targets either competency', () => {
  for (const competencyId of [1, 2]) {
    const request = assessmentCreationRequest('generalized', competencyId, 41, 'PRE_TEST', {
      title: 'Generalized Post-test', instructions: null, active: false,
    })
    assert.equal(request.scope, 'generalized')
    assert.equal(request.competencyId, competencyId)
    assert.equal(request.body.assessmentType, GENERALIZED_ASSESSMENT_TYPE)
  }
})

test('Discussion creation keeps the selected Discussion assessment type and target', () => {
  const request = assessmentCreationRequest('discussion', 1, 41, 'ACTIVITY', {
    title: 'Practical activity', instructions: 'Complete the task', active: false,
  })
  assert.equal(request.scope, 'discussion')
  assert.equal(request.discussionId, 41)
  assert.equal(request.body.assessmentType, 'ACTIVITY')
})
