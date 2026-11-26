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
let SafeMarkdown

before(async () => {
  vite = await createServer({
    configFile: false,
    root: process.cwd(),
    cacheDir: path.join(os.tmpdir(), 'css-ncii-safe-markdown-vite-cache'),
    appType: 'custom',
    plugins: [react()],
    server: { middlewareMode: true },
  })
  ;({ default: SafeMarkdown } = await vite.ssrLoadModule('/src/SafeMarkdown.jsx'))
})

after(async () => {
  await vite?.close()
})

function renderedMarkdown(source, imageUrlPrefix) {
  return renderToStaticMarkup(React.createElement(SafeMarkdown, { source, imageUrlPrefix }))
}

function assertNoParagraphContainsFigure(html) {
  const stack = []
  for (const [, closing, tag] of html.matchAll(/<(\/)?(p|figure)(?:\s[^>]*)?>/g)) {
    if (closing) {
      const opened = stack.pop()
      assert.equal(opened, tag, `unexpected closing </${tag}> in rendered HTML`)
    } else {
      assert.equal(tag === 'figure' && stack.includes('p'), false, 'figure must not be nested inside a paragraph')
      assert.equal(tag === 'p' && stack.includes('figure'), false, 'paragraph must not be nested inside a figure')
      stack.push(tag)
    }
  }
  assert.deepEqual(stack, [])
}

test('renders lesson images as block siblings of paragraphs and preserves surrounding Markdown', () => {
  const studentImageUrl = '/api/student/learning/images/0123456789abcdef0123456789abcdef.png'
  const source = [
    '# Lesson heading',
    '',
    `Read **carefully** and view ![Motherboard](${studentImageUrl} "Board caption") after.`,
    '',
    '- Keep the board level',
    '',
    '[Safety guide](https://example.test/safety)',
  ].join('\n')

  const html = renderedMarkdown(source)

  assertNoParagraphContainsFigure(html)
  assert.match(html, /<h2[^>]*>Lesson heading<\/h2>/)
  assert.match(html, /<p[^>]*>Read <strong>carefully<\/strong> and view <\/p>/)
  assert.match(html, /<figure[^>]*><img src="\/api\/student\/learning\/images\/0123456789abcdef0123456789abcdef\.png" alt="Motherboard"[^>]*><figcaption[^>]*>Board caption<\/figcaption><\/figure>/)
  assert.match(html, /<p[^>]*> after\.<\/p>/)
  assert.match(html, /<ul[^>]*><li>Keep the board level<\/li><\/ul>/)
  assert.match(html, /<a href="https:\/\/example\.test\/safety"/)
})

test('Admin preview prefix is used only when explicitly supplied', () => {
  const studentReference = '/api/student/learning/images/fedcba9876543210fedcba9876543210.webp'
  const markdown = `![Router](${studentReference} "Router view")`

  const studentHtml = renderedMarkdown(markdown)
  const adminHtml = renderedMarkdown(markdown, '/api/admin/learning-images/')

  assert.match(studentHtml, /src="\/api\/student\/learning\/images\/fedcba9876543210fedcba9876543210\.webp"/)
  assert.match(adminHtml, /src="\/api\/admin\/learning-images\/fedcba9876543210fedcba9876543210\.webp"/)
  assert.doesNotMatch(studentHtml, /\/api\/admin\/learning-images\//)
  assertNoParagraphContainsFigure(studentHtml)
})
