import { requestAdminApi } from './adminAuth.js'

const competenciesPath = '/api/admin/competencies'

export const listAssessmentCompetencies = () => requestAdminApi(competenciesPath)
export const listAssessmentDiscussions = competencyId => requestAdminApi(`${competenciesPath}/${competencyId}/discussions`)
export const listDiscussionAssessments = (competencyId, discussionId) => requestAdminApi(
  `${competenciesPath}/${competencyId}/discussions/${discussionId}/assessments`,
)
export const createDiscussionAssessment = (competencyId, discussionId, body) => requestAdminApi(
  `${competenciesPath}/${competencyId}/discussions/${discussionId}/assessments`, { method: 'POST', body },
)
export const listGeneralizedAssessments = competencyId => requestAdminApi(`${competenciesPath}/${competencyId}/assessments`)
export const createGeneralizedAssessment = (competencyId, body) => requestAdminApi(
  `${competenciesPath}/${competencyId}/assessments`, { method: 'POST', body },
)
export const updateAssessment = (assessmentId, body) => requestAdminApi(`/api/admin/assessments/${assessmentId}`, {
  method: 'PUT', body,
})
export const listAssessmentQuestions = assessmentId => requestAdminApi(`/api/admin/assessments/${assessmentId}/questions`)
export const createAssessmentQuestion = (assessmentId, body) => requestAdminApi(
  `/api/admin/assessments/${assessmentId}/questions`, { method: 'POST', body },
)
export const updateAssessmentQuestion = (assessmentId, questionId, body) => requestAdminApi(
  `/api/admin/assessments/${assessmentId}/questions/${questionId}`, { method: 'PUT', body },
)
export const deleteAssessmentQuestion = (assessmentId, questionId) => requestAdminApi(
  `/api/admin/assessments/${assessmentId}/questions/${questionId}`, { method: 'DELETE' },
)
export const createAnswerChoice = (questionId, body) => requestAdminApi(`/api/admin/questions/${questionId}/choices`, {
  method: 'POST', body,
})
export const updateAnswerChoice = (questionId, choiceId, body) => requestAdminApi(
  `/api/admin/questions/${questionId}/choices/${choiceId}`, { method: 'PUT', body },
)
export const deleteAnswerChoice = (questionId, choiceId) => requestAdminApi(
  `/api/admin/questions/${questionId}/choices/${choiceId}`, { method: 'DELETE' },
)
