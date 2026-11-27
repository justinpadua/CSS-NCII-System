import assert from 'node:assert/strict'
import test from 'node:test'
import { completedStageCount, overallProgressPercent } from './studentDashboardMetrics.js'

const dashboard = {
  competencies: [
    { code: 'COC1', discussionCount: 6, completedDiscussions: 2, discussions: [
      { stages: [{ stageCode: 'PRE_TEST', status: 'COMPLETED' }, { stageCode: 'ACTIVITY', status: 'IN_PROGRESS' }] },
      { stages: [{ stageCode: 'PRE_TEST', status: 'IN_PROGRESS' }, { stageCode: 'POST_TEST', status: 'COMPLETED' }] },
    ] },
    { code: 'COC2', discussionCount: 3, completedDiscussions: 1, discussions: [
      { stages: [{ stageCode: 'PRE_TEST', status: 'COMPLETED' }, { stageCode: 'ACTIVITY', status: 'COMPLETED' }] },
    ] },
    { code: 'COC3', discussionCount: 100, completedDiscussions: 100, discussions: [
      { stages: [{ stageCode: 'PRE_TEST', status: 'COMPLETED' }] },
    ] },
  ],
}

test('calculates overall progress from the existing COC 1 and COC 2 discussion totals', () => {
  assert.equal(overallProgressPercent(dashboard), 33)
})

test('counts only completed stage records from COC 1 and COC 2', () => {
  assert.equal(completedStageCount(dashboard, 'PRE_TEST'), 2)
  assert.equal(completedStageCount(dashboard, 'ACTIVITY'), 1)
  assert.equal(completedStageCount(dashboard, 'POST_TEST'), 1)
})

test('returns zero when progress has no available discussion data', () => {
  assert.equal(overallProgressPercent({ competencies: [] }), 0)
  assert.equal(completedStageCount(null, 'PRE_TEST'), 0)
})
