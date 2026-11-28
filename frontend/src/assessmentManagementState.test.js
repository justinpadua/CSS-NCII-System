import assert from 'node:assert/strict'
import test from 'node:test'
import {
  assessmentCreationRequest,
  assessmentListKey,
  assessmentListReady,
  assessmentTypeForScope,
  DISCUSSION_ASSESSMENT_TYPES,
  GENERALIZED_ASSESSMENT_TYPE,
  questionTypesForAssessment,
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

test('all five question types are available to Activities, Pre-tests, and Discussion Post-tests', () => {
  const expected = ['MULTIPLE_CHOICE', 'MULTIPLE_SELECT', 'TRUE_FALSE', 'MATCHING_TYPE', 'ENUMERATION']
  for (const type of ['ACTIVITY', 'PRE_TEST', 'DISCUSSION_POST_TEST']) {
    assert.deepEqual(questionTypesForAssessment(type), expected)
  }
  assert.equal(questionTypesForAssessment('ACTIVITY').includes('IDENTIFICATION'), false)
})

test('assessment creation remains blocked until the selected Discussion list loaded successfully', () => {
  const previousDiscussion = assessmentListKey('discussion', 1, 41)
  const selectedDiscussion = assessmentListKey('discussion', 1, 42)
  assert.equal(assessmentListReady(previousDiscussion, selectedDiscussion, 'loaded'), false)
  assert.equal(assessmentListReady('', selectedDiscussion, 'loading'), false)
  assert.equal(assessmentListReady('', selectedDiscussion, 'error'), false)
  assert.equal(assessmentListReady(selectedDiscussion, selectedDiscussion, 'loaded'), true)
})
