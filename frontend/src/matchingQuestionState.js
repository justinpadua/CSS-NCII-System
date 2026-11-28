export function buildMatchingQuestionConfig(matchingPairs, matchingOptions, editing, distractorsText = '') {
  const pairs = matchingPairs.map(pair => ({
    prompt: pair.prompt.trim(),
    answerText: pair.answerText.trim(),
  }))
  const options = (matchingOptions || []).map(option => ({ ...option, optionText: option.optionText.trim() }))
  const correctTexts = new Set(pairs.map(pair => pair.answerText.toLowerCase()))
  const config = {
    matchingPairs: pairs,
    distractors: editing
      ? options.map(option => option.optionText).filter(optionText => !correctTexts.has(optionText.toLowerCase()))
      : distractorsText.split(/\r?\n/).map(value => value.trim()).filter(Boolean),
  }
  if (editing) config.sharedOptions = options.map(({ id, optionText }) => ({ id, optionText }))
  return config
}

export function renameMatchingOption(draft, index, optionText) {
  const previousText = draft.matchingOptions[index].optionText
  return {
    ...draft,
    matchingOptions: draft.matchingOptions.map((option, optionIndex) => optionIndex === index
      ? { ...option, optionText } : option),
    matchingPairs: draft.matchingPairs.map(pair => pair.answerText === previousText
      ? { ...pair, answerText: optionText } : pair),
  }
}

export function removeMatchingDistractor(draft, index) {
  const option = draft.matchingOptions[index]
  if (draft.matchingPairs.some(pair => pair.answerText === option.optionText)) return draft
  return {
    ...draft,
    matchingOptions: draft.matchingOptions.filter((_, optionIndex) => optionIndex !== index),
  }
}

export function appendMatchingPair(pairs) {
  return [...pairs, { prompt: '', answerText: '' }]
}

export function matchingSelectionsWithChange(matches, promptCount, promptIndex, selectedChoiceId) {
  return Array.from({ length: promptCount }, (_, itemIndex) => ({
    promptIndex: itemIndex,
    choiceId: itemIndex === promptIndex
      ? (selectedChoiceId === '' ? null : Number(selectedChoiceId))
      : matches.find(match => match.promptIndex === itemIndex)?.choiceId ?? null,
  }))
}

export function isMatchingOptionDisabled(matches, promptIndex, choiceId, selectedChoiceId = null) {
  if (selectedChoiceId !== null && String(selectedChoiceId) === String(choiceId)) return false
  return matches.some(match => match.promptIndex !== promptIndex && match.choiceId === choiceId)
}
