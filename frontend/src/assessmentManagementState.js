export const GENERALIZED_ASSESSMENT_TYPE = 'GENERALIZED_POST_TEST'

export const DISCUSSION_ASSESSMENT_TYPES = [
  'PRE_TEST',
  'ACTIVITY',
  'DISCUSSION_POST_TEST',
]

export const SUPPORTED_QUESTION_TYPES = [
  'MULTIPLE_CHOICE',
  'MULTIPLE_SELECT',
  'TRUE_FALSE',
  'MATCHING_TYPE',
  'ENUMERATION',
]

export function questionTypesForAssessment() {
  return [...SUPPORTED_QUESTION_TYPES]
}

export function assessmentListKey(scope, competencyId, discussionId) {
  if (scope === 'generalized') return `generalized:${competencyId}`
  return `discussion:${competencyId}:${discussionId}`
}

export function assessmentListReady(loadedKey, currentKey, status) {
  return status === 'loaded' && Boolean(currentKey) && loadedKey === currentKey
}

export function assessmentTypeForScope(scope, currentType) {
  if (scope === 'generalized') return GENERALIZED_ASSESSMENT_TYPE
  return DISCUSSION_ASSESSMENT_TYPES.includes(currentType) ? currentType : DISCUSSION_ASSESSMENT_TYPES[0]
}

export function assessmentCreationRequest(scope, competencyId, discussionId, selectedType, details) {
  const generalized = scope === 'generalized'
  return {
    scope: generalized ? 'generalized' : 'discussion',
    competencyId,
    discussionId,
    body: {
      ...details,
      assessmentType: generalized ? GENERALIZED_ASSESSMENT_TYPE : selectedType,
    },
  }
}
