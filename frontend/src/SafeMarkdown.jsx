import { Fragment } from 'react'

function safeUrl(value) {
  try {
    const url = new URL(value)
    return url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
}

function inline(text, keyPrefix) {
  const parts = []
  const pattern = /(!?)\[([^\]]*)\]\(([^)\s]+)(?:\s+"([^"]*)")?\)|`([^`]+)`|\*\*([^*]+)\*\*/g
  let cursor = 0
  let match
  let index = 0
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > cursor) parts.push(text.slice(cursor, match.index))
    const key = `${keyPrefix}-${index++}`
    const url = safeUrl(match[3] || '')
    if (match[1] === '!' && url) {
      parts.push(<img key={key} src={url} alt={match[2]} loading="lazy" className="my-4 max-h-96 max-w-full rounded-lg object-contain" />)
    } else if (!match[1] && match[2] !== undefined && url) {
      parts.push(<a key={key} href={url} target="_blank" rel="noreferrer" className="font-medium text-teal-800 underline underline-offset-2">{match[2]}</a>)
    } else if (match[5]) {
      parts.push(<code key={key} className="rounded bg-slate-100 px-1.5 py-0.5 text-[0.9em] text-slate-800">{match[5]}</code>)
    } else if (match[6]) {
      parts.push(<strong key={key}>{match[6]}</strong>)
    } else {
      parts.push(match[0])
    }
    cursor = pattern.lastIndex
  }
  if (cursor < text.length) parts.push(text.slice(cursor))
  return parts.map((part, index) => <Fragment key={`${keyPrefix}-part-${index}`}>{part}</Fragment>)
}

function renderTable(lines, key) {
  const rows = lines.map(line => line.trim().replace(/^\|/, '').replace(/\|$/, '').split('|').map(cell => cell.trim()))
  const contentRows = rows.filter(row => !row.every(cell => /^:?-{3,}:?$/.test(cell)))
  if (!contentRows.length) return null
  return (
    <div key={key} className="my-4 overflow-x-auto rounded-lg border border-slate-200">
      <table className="min-w-full divide-y divide-slate-200 text-left text-sm">
        <thead className="bg-slate-50"><tr>{contentRows[0].map((cell, index) => <th key={index} className="px-3 py-2 font-semibold text-slate-800">{inline(cell, `${key}-head-${index}`)}</th>)}</tr></thead>
        <tbody className="divide-y divide-slate-100">{contentRows.slice(1).map((row, rowIndex) => <tr key={rowIndex}>{row.map((cell, index) => <td key={index} className="px-3 py-2 align-top text-slate-700">{inline(cell, `${key}-row-${rowIndex}-${index}`)}</td>)}</tr>)}</tbody>
      </table>
    </div>
  )
}

function videoEmbed(text) {
  const match = text.match(/^\s*(?:[-*]\s*)?(?:Video(?:\s+link)?\s*:\s*)?(https:\/\/\S+)\s*$/i)
  if (!match) return null
  const url = safeUrl(match[1])
  if (!url) return null
  let embed = null
  try {
    const parsed = new URL(url)
    const youtubeId = parsed.hostname === 'youtu.be'
      ? parsed.pathname.slice(1)
      : ['youtube.com', 'www.youtube.com', 'm.youtube.com'].includes(parsed.hostname) && parsed.pathname === '/watch'
        ? parsed.searchParams.get('v')
        : null
    if (youtubeId && /^[A-Za-z0-9_-]{11}$/.test(youtubeId)) embed = `https://www.youtube-nocookie.com/embed/${youtubeId}`
    if (parsed.hostname === 'vimeo.com' && /^\/[0-9]+$/.test(parsed.pathname)) embed = `https://player.vimeo.com/video${parsed.pathname}`
  } catch {
    return null
  }
  return embed ? <div className="my-4 aspect-video overflow-hidden rounded-xl bg-slate-950"><iframe title="Learning video" src={embed} loading="lazy" referrerPolicy="strict-origin-when-cross-origin" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share" allowFullScreen className="size-full" /></div> : null
}

export default function SafeMarkdown({ source }) {
  if (!source) return null
  const lines = source.replace(/\r/g, '').split('\n')
  const blocks = []
  let index = 0
  while (index < lines.length) {
    const line = lines[index]
    if (!line.trim()) { index += 1; continue }
    if (/^\s*\|.*\|\s*$/.test(line) && index + 1 < lines.length && /^\s*\|?\s*:?-{3,}/.test(lines[index + 1])) {
      const table = []
      while (index < lines.length && /^\s*\|.*\|\s*$/.test(lines[index])) table.push(lines[index++])
      blocks.push(renderTable(table, `table-${blocks.length}`))
      continue
    }
    const heading = line.match(/^\s*(#{1,6})\s+(.+)$/)
    if (heading) {
      const Tag = `h${Math.min(heading[1].length + 1, 6)}`
      blocks.push(<Tag key={`heading-${blocks.length}`} className="mb-2 mt-5 font-bold text-slate-900 first:mt-0">{inline(heading[2], `heading-${blocks.length}`)}</Tag>)
      index += 1
      continue
    }
    const video = videoEmbed(line)
    if (video) { blocks.push(<div key={`video-${blocks.length}`}>{video}</div>); index += 1; continue }
    if (/^\s*[-*+]\s+/.test(line)) {
      const items = []
      while (index < lines.length && /^\s*[-*+]\s+/.test(lines[index])) items.push(lines[index++].replace(/^\s*[-*+]\s+/, ''))
      blocks.push(<ul key={`ul-${blocks.length}`} className="my-3 list-disc space-y-1 pl-6 text-slate-700">{items.map((item, i) => <li key={i}>{inline(item, `ul-${blocks.length}-${i}`)}</li>)}</ul>)
      continue
    }
    if (/^\s*\d+[.)]\s+/.test(line)) {
      const items = []
      while (index < lines.length && /^\s*\d+[.)]\s+/.test(lines[index])) items.push(lines[index++].replace(/^\s*\d+[.)]\s+/, ''))
      blocks.push(<ol key={`ol-${blocks.length}`} className="my-3 list-decimal space-y-1 pl-6 text-slate-700">{items.map((item, i) => <li key={i}>{inline(item, `ol-${blocks.length}-${i}`)}</li>)}</ol>)
      continue
    }
    if (/^\s*>\s?/.test(line)) {
      blocks.push(<blockquote key={`quote-${blocks.length}`} className="my-3 border-l-4 border-teal-600 pl-4 italic text-slate-600">{inline(line.replace(/^\s*>\s?/, ''), `quote-${blocks.length}`)}</blockquote>)
      index += 1
      continue
    }
    const paragraph = []
    while (index < lines.length && lines[index].trim() && !/^\s*(#{1,6}\s+|[-*+]\s+|\d+[.)]\s+|>\s?|\|.*\|\s*$)/.test(lines[index])) paragraph.push(lines[index++].trim())
    if (paragraph.length) blocks.push(<p key={`p-${blocks.length}`} className="my-3 whitespace-pre-wrap leading-7 text-slate-700">{inline(paragraph.join(' '), `p-${blocks.length}`)}</p>)
    else index += 1
  }
  return <div className="break-words">{blocks}</div>
}
