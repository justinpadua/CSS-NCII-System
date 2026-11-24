export function updateCorrectChoice(choices, selectedIndex, checked, questionType) {
  if (questionType === 'MULTIPLE_CHOICE') {
    return choices.map((choice, index) => ({
      ...choice,
      correct: checked && index === selectedIndex,
    }))
  }

  return choices.map((choice, index) => index === selectedIndex
    ? { ...choice, correct: checked }
    : choice)
}

export function getCorrectChoiceIndexes(choices) {
  return choices.flatMap((choice, index) => choice.correct ? [index] : [])
}
