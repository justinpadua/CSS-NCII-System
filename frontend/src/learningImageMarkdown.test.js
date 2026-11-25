import test from 'node:test'
import assert from 'node:assert/strict'
import { buildLearningImageMarkdown, findLearningImageReferences, removeLearningImageReference } from './learningImageMarkdown.js'

const firstUrl = '/api/student/learning/images/0123456789abcdef0123456789abcdef.png'
const secondUrl = '/api/student/learning/images/fedcba9876543210fedcba9876543210.webp'

test('builds safe Markdown with optional alternative text and caption', () => {
  assert.equal(buildLearningImageMarkdown({ url: firstUrl, altText: 'Board', caption: 'Network board' }),
    `![Board](${firstUrl} "Network board")`)
  assert.equal(buildLearningImageMarkdown({ url: firstUrl }), `![](${firstUrl})`)
  assert.throws(() => buildLearningImageMarkdown({ url: 'https://example.test/image.png' }), /reference is invalid/)
})

test('finds and removes one image reference without disturbing other images', () => {
  const markdown = `Text\n\n${buildLearningImageMarkdown({ url: firstUrl, altText: 'First' })}\n\n${buildLearningImageMarkdown({ url: secondUrl, altText: 'Second' })}`
  const images = findLearningImageReferences(markdown)
  assert.equal(images.length, 2)
  assert.equal(images[0].altText, 'First')
  const updated = removeLearningImageReference(markdown, images[0])
  assert.equal(findLearningImageReferences(updated).length, 1)
  assert.match(updated, /Second/)
  assert.doesNotMatch(updated, /First/)
})
