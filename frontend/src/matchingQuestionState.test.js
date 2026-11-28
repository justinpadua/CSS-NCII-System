import assert from 'node:assert/strict'
import test from 'node:test'
import {
  appendMatchingPair,
  buildMatchingQuestionConfig,
  isMatchingOptionDisabled,
  matchingSelectionsWithChange,
  removeMatchingDistractor,
  renameMatchingOption,
} from './matchingQuestionState.js'

test('edit payload preserves every shared option and separates unused distractors from mappings', () => {
  const config = buildMatchingQuestionConfig(
    [{ prompt: 'CPU', answerText: 'Processor' }, { prompt: 'RAM', answerText: 'Memory' }],
    [
      { id: 10, optionText: 'Processor' },
      { id: 11, optionText: 'Memory' },
      { id: 12, optionText: 'Storage' },
    ],
    true,
  )
  assert.deepEqual(config.matchingPairs, [
    { prompt: 'CPU', answerText: 'Processor' },
    { prompt: 'RAM', answerText: 'Memory' },
  ])
  assert.deepEqual(config.distractors, ['Storage'])
  assert.deepEqual(config.sharedOptions.map(option => option.id), [10, 11, 12])
})

test('renaming a shared option retains its correct prompt mapping', () => {
  const draft = {
    matchingPairs: [{ prompt: 'CPU', answerText: 'Processor' }],
    matchingOptions: [{ id: 10, optionText: 'Processor' }, { id: 11, optionText: 'Storage' }],
  }
  const renamed = renameMatchingOption(draft, 0, 'Central Processor')
  assert.equal(renamed.matchingPairs[0].answerText, 'Central Processor')
  assert.equal(renamed.matchingOptions[0].optionText, 'Central Processor')
})

test('instructors can add pairs and remove unused distractors only', () => {
  const pairs = appendMatchingPair([{ prompt: 'CPU', answerText: 'Processor' }])
  assert.deepEqual(pairs[1], { prompt: '', answerText: '' })
  const draft = {
    matchingPairs: [{ prompt: 'CPU', answerText: 'Processor' }],
    matchingOptions: [{ id: 10, optionText: 'Processor' }, { id: 11, optionText: 'Storage' }],
  }
  assert.equal(removeMatchingDistractor(draft, 0), draft)
  assert.deepEqual(removeMatchingDistractor(draft, 1).matchingOptions, [{ id: 10, optionText: 'Processor' }])
})

test('new matching question payload keeps its existing pair and distractor format', () => {
  const config = buildMatchingQuestionConfig(
    [{ prompt: 'CPU', answerText: 'Processor' }], [], false, 'Storage\nMemory',
  )
  assert.deepEqual(config, {
    matchingPairs: [{ prompt: 'CPU', answerText: 'Processor' }],
    distractors: ['Storage', 'Memory'],
  })
})

test('a selected option stays available in its own prompt and is disabled in other prompts', () => {
  const matches = [{ promptIndex: 0, choiceId: 10 }, { promptIndex: 1, choiceId: 11 }]
  assert.equal(isMatchingOptionDisabled(matches, 0, 10), false)
  assert.equal(isMatchingOptionDisabled(matches, 1, 11), false)
  assert.equal(isMatchingOptionDisabled(matches, 1, 10), true)
  assert.equal(isMatchingOptionDisabled(matches, 0, 11), true)
  const legacyDuplicate = [{ promptIndex: 0, choiceId: 10 }, { promptIndex: 1, choiceId: 10 }]
  assert.equal(isMatchingOptionDisabled(legacyDuplicate, 0, 10, 10), false)
  assert.equal(isMatchingOptionDisabled(legacyDuplicate, 1, 10, 10), false)
})

test('changing a selection releases its previous option and reserves the new one', () => {
  const matches = [{ promptIndex: 0, choiceId: 10 }, { promptIndex: 1, choiceId: 11 }]
  const changed = matchingSelectionsWithChange(matches, 2, 0, '12')
  assert.deepEqual(changed, [{ promptIndex: 0, choiceId: 12 }, { promptIndex: 1, choiceId: 11 }])
  assert.equal(isMatchingOptionDisabled(changed, 1, 10), false)
  assert.equal(isMatchingOptionDisabled(changed, 1, 12), true)
  assert.equal(isMatchingOptionDisabled(changed, 0, 12), false)
})
