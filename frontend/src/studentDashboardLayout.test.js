import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import test from 'node:test'
import postcss from 'postcss'

const css = postcss.parse(await readFile(new URL('./index.css', import.meta.url), 'utf8'))

function declarationsFor(selector, width, height) {
  const declarations = new Map()

  function visit(container, mediaMatches = true) {
    container.each(node => {
      if (node.type === 'rule' && mediaMatches && node.selectors.includes(selector)) {
        node.walkDecls(declaration => declarations.set(declaration.prop, declaration.value))
      } else if (node.type === 'atrule' && node.name === 'media') {
        const maxWidth = node.params.match(/max-width:\s*(\d+)px/)
        const minWidth = node.params.match(/min-width:\s*(\d+)px/)
        const maxHeight = node.params.match(/max-height:\s*(\d+)px/)
        const matches = (!maxWidth || width <= Number(maxWidth[1]))
          && (!minWidth || width >= Number(minWidth[1]))
          && (!maxHeight || height <= Number(maxHeight[1]))
        visit(node, mediaMatches && matches)
      }
    })
  }

  visit(css)
  return declarations
}

test('short narrow mobile stacks progress details below a compact ring', () => {
  const viewport = { width: 320, height: 640 }
  const layout = declarationsFor('.student-dashboard-progress-layout', viewport.width, viewport.height)
  const ring = declarationsFor('.student-dashboard-progress-ring', viewport.width, viewport.height)
  const copy = declarationsFor('.student-dashboard-progress-copy', viewport.width, viewport.height)
  const description = declarationsFor('.student-dashboard-progress-description', viewport.width, viewport.height)

  assert.equal(layout.get('flex-direction'), 'column')
  assert.equal(layout.get('flex-wrap'), 'nowrap')
  assert.equal(ring.get('width'), '4rem')
  assert.equal(copy.get('width'), '100%')
  assert.equal(description.get('font-size'), '0.7rem')
})

test('short wider screens retain the existing compact side-by-side progress layout', () => {
  const layout = declarationsFor('.student-dashboard-progress-layout', 768, 640)

  assert.equal(layout.get('flex-wrap'), 'nowrap')
  assert.equal(layout.has('flex-direction'), false)
})

test('taller mobile screens do not receive the short-height overrides', () => {
  const layout = declarationsFor('.student-dashboard-progress-layout', 320, 800)
  const ring = declarationsFor('.student-dashboard-progress-ring', 320, 800)

  assert.equal(layout.has('flex-direction'), false)
  assert.equal(ring.has('width'), false)
})
