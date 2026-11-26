import { youtubeEmbedUrl } from './youtubeVideos.js'

export default function YouTubeVideoPlayers({ videoIds = [], titlePrefix = 'Video lesson' }) {
  if (!videoIds.length) return null

  return (
    <div className="space-y-4" aria-label="YouTube video lessons">
      {videoIds.map((videoId, index) => {
        const embedUrl = youtubeEmbedUrl(videoId)
        if (!embedUrl) return null
        return (
          <div key={`${videoId}-${index}`} className="aspect-video w-full overflow-hidden rounded-xl bg-slate-950 shadow-sm">
            <iframe
              title={`${titlePrefix} ${index + 1}`}
              src={embedUrl}
              loading="lazy"
              referrerPolicy="strict-origin-when-cross-origin"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              allowFullScreen
              className="size-full"
            />
          </div>
        )
      })}
    </div>
  )
}
