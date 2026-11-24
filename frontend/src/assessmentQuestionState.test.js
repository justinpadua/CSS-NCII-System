import assert from 'node:assert/strict'
import test from 'node:test'
import { getCorrectChoiceIndexes, updateCorrectChoice } from './assessmentQuestionState.js'

const choices = [
  { choiceText: 'A', correct: true },
  { choiceText: 'B', correct: false },
  { choiceText: 'C', correct: false },
]

test('selecting a multiple-choice answer clears the previous correct answer', () => {
  const updated = updateCorrectChoice(choices, 1, true, 'MULTIPLE_CHOICE')

  assert.deepEqual(updated.map(choice => choice.correct), [false, true, false])
  assert.deepEqual(getCorrectChoiceIndexes(updated), [1])
})

test('multiple-select keeps all independently selected answers', () => {
  const first = updateCorrectChoice(choices, 1, true, 'MULTIPLE_SELECT')
  const second = updateCorrectChoice(first, 2, true, 'MULTIPLE_SELECT')

  assert.deepEqual(second.map(choice => choice.correct), [true, true, true])
  assert.deepEqual(getCorrectChoiceIndexes(second), [0, 1, 2])
})

test('multiple-select can clear one answer without clearing the others', () => {
  const selected = updateCorrectChoice(choices, 1, true, 'MULTIPLE_SELECT')
  const selectedTwo = updateCorrectChoice(selected, 2, true, 'MULTIPLE_SELECT')
  const updated = updateCorrectChoice(selectedTwo, 1, false, 'MULTIPLE_SELECT')

  assert.deepEqual(updated.map(choice => choice.correct), [true, false, true])
})
