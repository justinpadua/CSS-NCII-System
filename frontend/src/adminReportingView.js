export function progressPercentage(completed, total) {
  return total > 0 ? Math.round((completed * 100) / total) : 0
}

export function submittedAttemptCount(attempts = []) {
  return attempts.filter(attempt => attempt.status === 'SUBMITTED').length
}

export function attemptResultText(attempt) {
  if (attempt.status === 'IN_PROGRESS') return 'In progress; not counted as submitted'
  if (attempt.status !== 'SUBMITTED' || attempt.percentage == null) return 'No score recorded'
  const result = attempt.passed == null ? 'Submitted' : attempt.passed ? 'Passed' : 'Failed'
  return `${attempt.score}/${attempt.totalPossibleScore} (${attempt.percentage}%) · ${result}`
}
