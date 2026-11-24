import { requestAdminApi } from './adminAuth.js'

const basePath = '/api/admin/competencies'
const initializationPath = '/api/admin/course-structure/initialize'

export const initializeCourseStructure = () => requestAdminApi(initializationPath, { method: 'POST' })

export const listCompetencies = () => requestAdminApi(basePath)
export const createCompetency = body => requestAdminApi(basePath, { method: 'POST', body })
export const updateCompetency = (id, body) => requestAdminApi(`${basePath}/${id}`, { method: 'PUT', body })
export const setCompetencyStatus = (id, active) => requestAdminApi(`${basePath}/${id}/status`, { method: 'PATCH', body: { active } })
export const listDiscussions = competencyId => requestAdminApi(`${basePath}/${competencyId}/discussions`)
export const createDiscussion = (competencyId, body) => requestAdminApi(`${basePath}/${competencyId}/discussions`, { method: 'POST', body })
export const updateDiscussion = (competencyId, discussionId, body) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}`, { method: 'PUT', body })
export const setDiscussionStatus = (competencyId, discussionId, active) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/status`, { method: 'PATCH', body: { active } })
export const listTopics = (competencyId, discussionId) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/topics`)
export const createTopic = (competencyId, discussionId, body) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/topics`, { method: 'POST', body })
export const updateTopic = (competencyId, discussionId, topicId, body) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/topics/${topicId}`, { method: 'PUT', body })
export const setTopicStatus = (competencyId, discussionId, topicId, active) => requestAdminApi(`${basePath}/${competencyId}/discussions/${discussionId}/topics/${topicId}/status`, { method: 'PATCH', body: { active } })
