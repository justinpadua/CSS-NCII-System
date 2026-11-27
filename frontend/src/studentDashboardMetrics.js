const TRACKED_COC_CODES = new Set(['COC1', 'COC2'])

function discussionsFrom(dashboard) {
  return (dashboard?.competencies ?? [])
    .filter(competency => TRACKED_COC_CODES.has(competency.code))
    .flatMap(competency => competency.discussions ?? [])
}

export function overallProgressPercent(dashboard) {
  const competencies = (dashboard?.competencies ?? [])
    .filter(competency => TRACKED_COC_CODES.has(competency.code))
  const discussionCount = competencies.reduce((total, competency) => total + (competency.discussionCount ?? 0), 0)
  if (!discussionCount) return 0
  const completedCount = competencies.reduce((total, competency) => total + (competency.completedDiscussions ?? 0), 0)
  return Math.floor((completedCount * 100) / discussionCount)
}

export function completedStageCount(dashboard, stageCode) {
  return discussionsFrom(dashboard).reduce((count, discussion) => count + (discussion.stages ?? [])
    .filter(stage => stage.stageCode === stageCode && stage.status === 'COMPLETED').length, 0)
}
