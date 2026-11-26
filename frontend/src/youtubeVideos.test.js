import assert from 'node:assert/strict'
import test from 'node:test'
import { extractYouTubeVideoId, youtubeEmbedUrl } from './youtubeVideos.js'

const id = 'abcdefghijk'

test('extracts ids from the supported YouTube URL formats', () => {
  assert.equal(extractYouTubeVideoId(`https://www.youtube.com/watch?v=${id}`), id)
  assert.equal(extractYouTubeVideoId(`https://youtu.be/${id}`), id)
  assert.equal(extractYouTubeVideoId(`https://www.youtube.com/embed/${id}`), id)
  assert.equal(extractYouTubeVideoId(`https://www.youtube.com/shorts/${id}`), id)
})

test('rejects insecure, malformed, and external URLs', () => {
  assert.equal(extractYouTubeVideoId(`http://www.youtube.com/watch?v=${id}`), null)
  assert.equal(extractYouTubeVideoId(`https://example.com/embed/${id}`), null)
  assert.equal(extractYouTubeVideoId('https://youtube.com/watch?v=short'), null)
  assert.equal(extractYouTubeVideoId(`<iframe src="https://youtube.com/embed/${id}"></iframe>`), null)
})

test('constructs only official YouTube embed URLs from valid ids', () => {
  assert.equal(youtubeEmbedUrl(id), `https://www.youtube.com/embed/${id}`)
  assert.equal(youtubeEmbedUrl('invalid'), null)
})
