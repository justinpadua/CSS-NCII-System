import { requestAdminApi } from './adminAuth.js'

const basePath = '/api/admin/competencies'

export function listContentCompetencies() {
  return requestAdminApi(basePath)
}

export function listDiscussionStages(competencyId, discussionId) {
  return requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/learning-stages`)
}

export function previewDiscussionStageSource(competencyId, discussionId, stageCode) {
  return requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/learning-stages/${stageCode}/source-preview`)
}

export function updateDiscussionStage(competencyId, discussionId, stageCode, body) {
  return requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/learning-stages/${stageCode}`, {
    method: 'PUT',
    body,
  })
}
