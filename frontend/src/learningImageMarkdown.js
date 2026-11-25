const imageReferencePattern = /!\[([^\]]*)\]\((\/api\/student\/learning\/images\/[a-f0-9]{32}\.(?:jpg|png|webp))(?:\s+"([^"]*)")?\)/g

function cleanMarkdownLabel(value) {
  return String(value ?? '').replace(/[\r\n]+/g, ' ').replace(/\[/g, '(').replace(/\]/g, ')').trim()
}

export function buildLearningImageMarkdown({ url, altText = '', caption = '' }) {
  if (!/^\/api\/student\/learning\/images\/[a-f0-9]{32}\.(?:jpg|png|webp)$/.test(url ?? '')) {
    throw new Error('The uploaded image reference is invalid.')
  }
  const alt = cleanMarkdownLabel(altText)
  const title = cleanMarkdownLabel(caption).replace(/"/g, "'")
  return `![${alt}](${url}${title ? ` "${title}"` : ''})`
}

export function findLearningImageReferences(content = '') {
  return [...String(content).matchAll(imageReferencePattern)].map(match => ({
    markdown: match[0],
    url: match[2],
    altText: match[1],
    caption: match[3] ?? '',
    start: match.index,
    end: match.index + match[0].length,
  }))
}

export function removeLearningImageReference(content, reference) {
  const source = String(content ?? '')
  return source.slice(0, reference.start) + source.slice(reference.end)
}
