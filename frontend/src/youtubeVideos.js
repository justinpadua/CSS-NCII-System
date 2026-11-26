const VIDEO_ID_PATTERN = /^[A-Za-z0-9_-]{11}$/

export function extractYouTubeVideoId(value) {
  try {
    const url = new URL(value)
    if (url.protocol !== 'https:' || url.username || url.password || url.port) return null

    let videoId = null
    if (url.hostname === 'youtu.be') {
      videoId = url.pathname.slice(1)
    } else if (['youtube.com', 'www.youtube.com'].includes(url.hostname)) {
      const segments = url.pathname.split('/').filter(Boolean)
      if (url.pathname === '/watch') videoId = url.searchParams.get('v')
      else if (segments.length === 2 && ['embed', 'shorts'].includes(segments[0])) videoId = segments[1]
    }
    return videoId && VIDEO_ID_PATTERN.test(videoId) ? videoId : null
  } catch {
    return null
  }
}

export function youtubeEmbedUrl(videoId) {
  if (!VIDEO_ID_PATTERN.test(videoId)) return null
  return `https://www.youtube.com/embed/${videoId}`
}
