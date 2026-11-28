import assert from 'node:assert/strict'
import { after, before, test } from 'node:test'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import react from '@vitejs/plugin-react'
import { createServer } from 'vite'
import os from 'node:os'
import path from 'node:path'
import process from 'node:process'

let vite
let AssessmentForm

before(async () => {
  vite = await createServer({
    configFile: false,
    root: process.cwd(),
    cacheDir: path.join(os.tmpdir(), 'css-ncii-assessment-form-vite-cache'),
    appType: 'custom',
    plugins: [react()],
    server: { middlewareMode: true },
  })
  ;({ AssessmentForm } = await vite.ssrLoadModule('/src/StudentLearningDashboard.jsx'))
})

after(async () => {
  await vite?.close()
})

test('renders readable matching and enumeration student inputs', () => {
  const html = renderToStaticMarkup(React.createElement(AssessmentForm, {
    title: 'Pre-test',
    questions: [
      { questionId: 1, questionType: 'MATCHING_TYPE', questionText: 'Match terms.',
        choices: [{ choiceId: 8, choiceText: 'Central Processor' }], matchingPrompts: ['CPU'],
        questionConfig: '{"matchingPrompts":["CPU"]}' },
      { questionId: 2, questionType: 'ENUMERATION', questionText: 'List two parts.', choices: [],
        questionConfig: '{"requiredAnswerCount":2,"ordered":false}' },
    ],
    answers: {}, onAnswer() {}, onSubmit() {}, busy: false,
  }))

  assert.match(html, /Central Processor/)
  assert.match(html, /<select/)
  assert.match(html, /Answer 1/)
  assert.match(html, /Answer 2/)
  assert.ok(html.split('text-slate-900').length - 1 >= 3)
})

test('matching dropdowns disable options selected elsewhere but keep their own selection enabled', () => {
  const html = renderToStaticMarkup(React.createElement(AssessmentForm, {
    title: 'Pre-test',
    questions: [{ questionId: 1, questionType: 'MATCHING_TYPE', questionText: 'Match terms.',
      choices: [
        { choiceId: 8, choiceText: 'Processor' },
        { choiceId: 9, choiceText: 'Memory' },
        { choiceId: 10, choiceText: 'Distractor' },
      ], matchingPrompts: ['CPU', 'RAM'], questionConfig: '{"matchingPrompts":["CPU","RAM"]}' }],
    answers: { 1: { matches: [{ promptIndex: 0, choiceId: 8 }, { promptIndex: 1, choiceId: 10 }] } },
    onAnswer() {}, onSubmit() {}, busy: false,
  }))
  const selects = [...html.matchAll(/<select[^>]*>(.*?)<\/select>/g)].map(match => match[1])
  assert.equal(selects.length, 2)
  assert.match(selects[0], /value="8" selected=""/)
  assert.doesNotMatch(selects[0], /value="8" disabled=""/)
  assert.match(selects[0], /value="10" disabled=""/)
  assert.match(selects[1], /value="10" selected=""/)
  assert.match(selects[1], /value="8" disabled=""/)
})
