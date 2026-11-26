import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { listContentCompetencies, listDiscussionStages, previewDiscussionStageSource, updateDiscussionStage, uploadLearningImage } from './learningContentApi.js'
import { listDiscussions } from './courseStructureApi.js'
import SafeMarkdown from './SafeMarkdown.jsx'
import { buildLearningImageMarkdown, findLearningImageReferences, removeLearningImageReference } from './learningImageMarkdown.js'
import { extractYouTubeVideoId } from './youtubeVideos.js'
import YouTubeVideoPlayers from './YouTubeVideoPlayers.jsx'

const STAGES = [
  { code: 'PRE_TEST', title: 'Pre-test' },
  { code: 'DEFINITIONS', title: 'Definitions' },
  { code: 'EXPLANATION', title: 'Explanation' },
  { code: 'EXAMPLES', title: 'Examples' },
  { code: 'VIDEO_LESSONS', title: 'Video Lessons' },
  { code: 'ACTIVITY', title: 'Activity' },
  { code: 'POST_TEST', title: 'Post-test' },
]

function errorMessage(error) {
  if (error.status === 401) return 'Your Admin session has expired. Please log in again.'
  return error.message || 'The request could not be completed. Please try again.'
}

function LearningContentManagement({ onSessionExpired }) {
  const [competencies, setCompetencies] = useState([])
  const [selectedCode, setSelectedCode] = useState('COC1')
  const [discussions, setDiscussions] = useState([])
  const [selectedDiscussionId, setSelectedDiscussionId] = useState('')
  const [stages, setStages] = useState([])
  const [selectedStageCode, setSelectedStageCode] = useState('PRE_TEST')
  const [draftTitle, setDraftTitle] = useState(null)
  const [draftBody, setDraftBody] = useState(null)
  const [draftYoutubeVideoUrls, setDraftYoutubeVideoUrls] = useState(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [notice, setNotice] = useState('')
  const [sourcePreview, setSourcePreview] = useState(null)
  const [sourceLoading, setSourceLoading] = useState(false)
  const [imageFile, setImageFile] = useState(null)
  const [imagePreviewUrl, setImagePreviewUrl] = useState('')
  const [imageAltText, setImageAltText] = useState('')
  const [imageCaption, setImageCaption] = useState('')
  const [uploadedImage, setUploadedImage] = useState(null)
  const [imageUploading, setImageUploading] = useState(false)
  const [showLessonPreview, setShowLessonPreview] = useState(false)
  const editorRef = useRef(null)
  const fileInputRef = useRef(null)

  const competency = useMemo(() => competencies.find(item => item.code === selectedCode) ?? null,
    [competencies, selectedCode])
  const selectedDiscussion = discussions.find(item => String(item.id) === String(selectedDiscussionId)) ?? null
  const selectedStage = stages.find(item => item.stageCode === selectedStageCode) ?? null
  const editorBody = draftBody ?? selectedStage?.contentBody ?? ''
  const editorYoutubeVideoUrls = draftYoutubeVideoUrls
    ?? (selectedStage?.youtubeVideoUrls?.length ? selectedStage.youtubeVideoUrls : [''])
  const editorYoutubeVideoIds = editorYoutubeVideoUrls.map(extractYouTubeVideoId).filter(Boolean)
  const lessonImages = findLearningImageReferences(editorBody)

  const handleError = useCallback((requestError) => {
    setError(errorMessage(requestError))
    if (requestError.status === 401) onSessionExpired()
  }, [onSessionExpired])

  useEffect(() => () => {
    if (imagePreviewUrl) URL.revokeObjectURL(imagePreviewUrl)
  }, [imagePreviewUrl])

  function resetImageUpload() {
    setImageFile(null)
    setImagePreviewUrl('')
    setImageAltText('')
    setImageCaption('')
    setUploadedImage(null)
  }

  function chooseImage(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    const extension = file.name.split('.').pop()?.toLowerCase()
    if (!['jpg', 'jpeg', 'png', 'webp'].includes(extension)) {
      setError('Choose a JPG, PNG, or WEBP image.')
      return
    }
    if (file.size > 5 * 1024 * 1024) {
      setError('Image files must be 5 MB or smaller.')
      return
    }
    setError('')
    resetImageUpload()
    setImageFile(file)
    setImagePreviewUrl(URL.createObjectURL(file))
  }

  async function uploadSelectedImage() {
    if (!imageFile) return
    setImageUploading(true)
    setError('')
    try {
      setUploadedImage(await uploadLearningImage(imageFile))
      setNotice('Image uploaded. Review its text and insert it into the lesson when ready.')
    } catch (requestError) {
      handleError(requestError)
    } finally {
      setImageUploading(false)
    }
  }

  function insertUploadedImage() {
    if (!uploadedImage || !selectedStage) return
    let markdown
    try {
      markdown = buildLearningImageMarkdown({
        url: uploadedImage.url,
        altText: imageAltText,
        caption: imageCaption,
      })
    } catch (insertError) {
      setError(insertError.message)
      return
    }
    const editor = editorRef.current
    const source = editorBody
    const start = editor?.selectionStart ?? source.length
    const end = editor?.selectionEnd ?? start
    const prefix = start > 0 && source[start - 1] !== '\n' ? '\n\n' : ''
    const suffix = end < source.length && source[end] !== '\n' ? '\n\n' : ''
    const updated = source.slice(0, start) + prefix + markdown + suffix + source.slice(end)
    const caret = start + prefix.length + markdown.length
    setDraftBody(updated)
    setNotice('Image reference inserted into the lesson draft. Save the stage to publish it.')
    requestAnimationFrame(() => {
      if (!editorRef.current) return
      editorRef.current.focus()
      editorRef.current.setSelectionRange(caret, caret)
    })
    resetImageUpload()
  }

  function removeImageReference(reference) {
    setDraftBody(currentBody => removeLearningImageReference(currentBody ?? selectedStage?.contentBody ?? '', reference))
    setNotice('Image removed from this lesson draft. Save the stage to publish the change.')
  }

  const loadCompetencies = useCallback(async () => {
    try {
      const items = await listContentCompetencies()
      setCompetencies(items)
      if (!items.some(item => item.code === selectedCode)) {
        setSelectedCode(items.find(item => item.code === 'COC1')?.code ?? items[0]?.code ?? 'COC1')
      }
    } catch (requestError) {
      handleError(requestError)
    }
  }, [handleError, selectedCode])

  const loadDiscussionsForCompetency = useCallback(async () => {
    if (!competency) {
      setDiscussions([])
      setSelectedDiscussionId('')
      setStages([])
      return
    }
    try {
      const items = await listDiscussions(competency.id)
      setDiscussions(items)
      if (!items.some(item => String(item.id) === String(selectedDiscussionId))) {
        setSelectedDiscussionId(items[0] ? String(items[0].id) : '')
      }
    } catch (requestError) {
      handleError(requestError)
    }
  }, [competency, handleError, selectedDiscussionId])

  const loadStages = useCallback(async () => {
    if (!competency || !selectedDiscussion) {
      setStages([])
      return
    }
    setLoading(true)
    try {
      const items = await listDiscussionStages(competency.id, selectedDiscussion.id)
      setStages(items)
    } catch (requestError) {
      handleError(requestError)
    } finally {
      setLoading(false)
    }
  }, [competency, handleError, selectedDiscussion])

  useEffect(() => {
    const timer = window.setTimeout(() => { void loadCompetencies() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadCompetencies])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadDiscussionsForCompetency() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadDiscussionsForCompetency])
  useEffect(() => {
    const timer = window.setTimeout(() => { void loadStages() }, 0)
    return () => window.clearTimeout(timer)
  }, [loadStages])

  async function saveStage(event) {
    event.preventDefault()
    if (!competency || !selectedDiscussion || !selectedStage) return
    setBusy(true)
    setError('')
    setNotice('')
    const youtubeVideoUrls = selectedStage.stageCode === 'VIDEO_LESSONS'
      ? editorYoutubeVideoUrls.map(url => url.trim()).filter(Boolean)
      : undefined
    if (youtubeVideoUrls?.some(url => !extractYouTubeVideoId(url))) {
      setBusy(false)
      setError('Enter a valid HTTPS YouTube video URL or remove the invalid video field.')
      return
    }
    try {
      const updated = await updateDiscussionStage(competency.id, selectedDiscussion.id, selectedStage.stageCode, {
        title: (draftTitle ?? selectedStage.title).trim(),
        contentBody: draftBody ?? selectedStage.contentBody,
        ...(youtubeVideoUrls !== undefined ? { youtubeVideoUrls } : {}),
      })
      setStages(items => items.map(item => item.stageCode === updated.stageCode ? updated : item))
      setDraftTitle(null)
      setDraftBody(null)
      setDraftYoutubeVideoUrls(null)
      setSourcePreview(null)
      resetImageUpload()
      setNotice(`${selectedStage.title} content saved.`)
    } catch (requestError) {
      handleError(requestError)
    } finally {
      setBusy(false)
    }
  }

  const setCourse = event => {
    setSelectedCode(event.target.value)
    setSelectedDiscussionId('')
    setStages([])
    setDraftTitle(null)
    setDraftBody(null)
    setDraftYoutubeVideoUrls(null)
    setSourcePreview(null)
    resetImageUpload()
    setShowLessonPreview(false)
    setError('')
    setNotice('')
  }

  return (
    <section className="space-y-5">
      <div>
        <p className="text-sm font-semibold uppercase tracking-wider text-teal-700">Admin workspace</p>
        <h1 className="mt-1 text-2xl font-bold text-slate-900">Learning Content</h1>
        <p className="mt-2 text-sm text-slate-600">Choose a COC competency, Discussion, and stage to edit its instructional content.</p>
      </div>

      {error && <p role="alert" className="rounded-lg border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}</p>}
      {notice && <p role="status" className="rounded-lg border border-emerald-200 bg-emerald-50 p-3 text-sm text-emerald-900">{notice}</p>}

      <div className="grid gap-4 rounded-2xl border border-slate-200 bg-white p-5 shadow-sm md:grid-cols-3">
        <label className="block text-sm font-semibold text-slate-700">COC
          <select value={selectedCode} onChange={setCourse} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal text-slate-900">
            <option value="COC1">COC 1</option><option value="COC2">COC 2</option>
          </select>
        </label>
        <div className="text-sm font-semibold text-slate-700">Competency
          <div className="mt-1 flex min-h-11 items-center rounded-lg border border-slate-200 bg-slate-50 px-3 py-2.5 font-normal text-slate-900">
            {competency ? competency.title : 'No competency record configured'}
          </div>
          <span className="mt-1 block text-xs font-normal text-slate-500">In the current schema, each COC is stored as its competency record.</span>
        </div>
        <label className="block text-sm font-semibold text-slate-700">Discussion
          <select value={selectedDiscussionId} onChange={event => { setSelectedDiscussionId(event.target.value); setDraftTitle(null); setDraftBody(null); setDraftYoutubeVideoUrls(null); setSourcePreview(null); setNotice(''); resetImageUpload(); setShowLessonPreview(false) }} disabled={!competency || discussions.length === 0} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 font-normal text-slate-900">
            {discussions.length === 0 ? <option value="">No Discussions available</option> : discussions.map(item => <option key={item.id} value={item.id}>{item.displayOrder}. {item.title}</option>)}
          </select>
        </label>
      </div>

      {!competency ? (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">No {selectedCode} competency record exists. Create it in Course Structure before adding learning content.</div>
      ) : !selectedDiscussion ? (
        <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Select a Discussion with its existing course structure before editing stages.</div>
      ) : loading ? (
        <div className="rounded-xl border border-slate-200 bg-white p-5 text-sm text-slate-600">Loading the seven learning stages…</div>
      ) : stages.length !== STAGES.length ? (
        <div role="alert" className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-900">This Discussion does not have a complete set of seven learning-stage records. The API did not create or duplicate stage rows.</div>
      ) : (
        <div className="grid gap-5 lg:grid-cols-[minmax(15rem,0.8fr)_minmax(0,1.6fr)]">
          <section className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
            <h2 className="px-2 text-lg font-bold text-slate-900">Seven stages</h2>
            <ol className="mt-3 space-y-1">
              {STAGES.map((stage, index) => {
                const item = stages.find(value => value.stageCode === stage.code)
                return (
                  <li key={stage.code}>
                    <button type="button" onClick={() => { setSelectedStageCode(stage.code); setDraftTitle(null); setDraftBody(null); setDraftYoutubeVideoUrls(null); setSourcePreview(null); setNotice(''); resetImageUpload(); setShowLessonPreview(false) }} className={`flex w-full items-center justify-between gap-3 rounded-lg px-3 py-3 text-left ${selectedStageCode === stage.code ? 'bg-teal-50 text-teal-950 ring-1 ring-teal-200' : 'text-slate-700 hover:bg-slate-50'}`}>
                      <span><span className="font-semibold">{index + 1}. {stage.title}</span><span className="mt-0.5 block text-xs text-slate-500">{item?.active ? 'Active' : 'Inactive'}</span></span>
                      <span className={`shrink-0 rounded-full px-2 py-1 text-[11px] font-semibold ${item?.hasContent ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-600'}`}>{item?.hasContent ? 'Configured' : 'Empty'}</span>
                    </button>
                  </li>
                )
              })}
            </ol>
          </section>

          <section className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
            {!selectedStage ? <p className="text-sm text-slate-600">Choose a learning stage to edit.</p> : (
              <form onSubmit={saveStage} className="space-y-4">
                {selectedStage.stageCode === 'VIDEO_LESSONS' && <section aria-label="YouTube video lessons" className="space-y-3 rounded-xl border border-red-200 bg-red-50/50 p-4">
                  <div>
                    <h3 className="text-sm font-semibold text-slate-800">YouTube videos <span className="font-normal text-slate-500">(optional)</span></h3>
                    <p className="mt-1 text-xs leading-5 text-slate-600">Add one or more HTTPS YouTube watch, short, embed, or Shorts links. Videos appear above this stage’s written lesson.</p>
                  </div>
                  <div className="space-y-2">
                    {editorYoutubeVideoUrls.map((url, index) => <div key={index} className="flex items-start gap-2">
                      <label className="sr-only" htmlFor={`youtube-video-url-${index}`}>YouTube video URL {index + 1}</label>
                      <input id={`youtube-video-url-${index}`} type="url" value={url}
                        onChange={event => setDraftYoutubeVideoUrls(current => {
                          const values = [...(current ?? editorYoutubeVideoUrls)]
                          values[index] = event.target.value
                          return values
                        })}
                        placeholder="https://www.youtube.com/watch?v=…"
                        aria-invalid={Boolean(url.trim()) && !extractYouTubeVideoId(url)}
                        className="min-w-0 flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-sm text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" />
                      <button type="button" onClick={() => setDraftYoutubeVideoUrls(current =>
                        (current ?? editorYoutubeVideoUrls).filter((_, itemIndex) => itemIndex !== index))}
                        className="rounded-lg border border-red-300 bg-white px-3 py-2 text-sm font-semibold text-red-800 hover:bg-red-50">Remove</button>
                    </div>)}
                  </div>
                  <button type="button" onClick={() => setDraftYoutubeVideoUrls(current => [...(current ?? editorYoutubeVideoUrls), ''])}
                    className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">Add another video</button>
                  {editorYoutubeVideoUrls.some(url => url.trim() && !extractYouTubeVideoId(url))
                    && <p role="alert" className="text-sm text-red-800">Use a valid HTTPS YouTube URL in each populated field.</p>}
                </section>}
                <div>
                  <p className="text-xs font-semibold uppercase tracking-wider text-teal-700">{selectedStage.stageCode} · Stage {selectedStage.displayOrder} of 7</p>
                  <h2 className="mt-1 text-xl font-bold text-slate-900">Edit {selectedStage.title}</h2>
                  <p className="mt-1 text-sm text-slate-600">The stage code and position are fixed to preserve the Discussion learning flow.</p>
                </div>
                <div className="rounded-xl border border-sky-200 bg-sky-50 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div><h3 className="font-semibold text-sky-950">Existing Markdown source</h3><p className="mt-1 text-xs leading-5 text-sky-900">Preview is read-only. Review it, load it into the editor, then explicitly save to publish. Existing stage content is never replaced.</p></div>
                    <button type="button" disabled={sourceLoading || sourcePreview?.existingContentPreserved} onClick={async () => {
                      setSourceLoading(true)
                      setError('')
                      try {
                        setSourcePreview(await previewDiscussionStageSource(competency.id, selectedDiscussion.id, selectedStage.stageCode))
                      } catch (requestError) {
                        handleError(requestError)
                      } finally {
                        setSourceLoading(false)
                      }
                    }} className="rounded-lg border border-sky-300 bg-white px-3 py-2 text-sm font-semibold text-sky-950 disabled:opacity-60">{sourceLoading ? 'Loading preview…' : 'Preview source Markdown'}</button>
                  </div>
                  {sourcePreview && <div className="mt-3 space-y-3 border-t border-sky-200 pt-3">
                    <p className="text-xs text-sky-950"><span className="font-semibold">Source:</span> {sourcePreview.sourcePath}</p>
                    <p role="status" className="text-sm text-sky-950">{sourcePreview.message}</p>
                    {sourcePreview.assessmentItemsRemoved && <p className="rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs leading-5 text-amber-950">Assessment questions and answer keys are not included in this student-visible stage draft. Use Admin Assessment Management to review and enter them.</p>}
                    {sourcePreview.stageBodyPreview && <pre className="max-h-72 overflow-auto whitespace-pre-wrap rounded-lg border border-sky-200 bg-white p-3 text-xs leading-5 text-slate-800">{sourcePreview.stageBodyPreview}</pre>}
                    {sourcePreview.canLoadIntoEditor && <button type="button" disabled={draftBody !== null} onClick={() => { setDraftBody(sourcePreview.stageBodyPreview); setNotice('Source loaded as an unsaved draft. Save the stage to publish it.') }} className="rounded-lg bg-teal-800 px-3 py-2 text-sm font-semibold text-white disabled:opacity-60">{draftBody !== null ? 'Draft already loaded or edited' : 'Load reviewed source into editor'}</button>}
                  </div>}
                </div>
                <label className="block text-sm font-semibold text-slate-700">Stage title<input value={draftTitle ?? selectedStage.title} onChange={event => setDraftTitle(event.target.value)} required maxLength={200} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-2.5 font-normal text-slate-900" /></label>
                <div>
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <label htmlFor="learning-stage-content" className="text-sm font-semibold text-slate-700">Content body</label>
                    <button type="button" onClick={() => fileInputRef.current?.click()} className="rounded-lg border border-teal-700 px-3 py-2 text-sm font-semibold text-teal-900 hover:bg-teal-50">Upload Image</button>
                    <input ref={fileInputRef} type="file" accept=".jpg,.jpeg,.png,.webp,image/jpeg,image/png,image/webp" onChange={chooseImage} className="hidden" />
                  </div>
                  <textarea id="learning-stage-content" ref={editorRef} value={editorBody} onChange={event => setDraftBody(event.target.value)} rows={14} className="mt-1 w-full rounded-lg border border-slate-300 px-3 py-3 font-mono text-sm font-normal text-slate-900 focus:border-teal-700 focus:outline-none focus:ring-2 focus:ring-teal-700/20" placeholder="Write instructional text here. Use Upload Image to add lesson pictures." />
                </div>

                {imageFile && <section aria-label="Image upload preview" className="space-y-4 rounded-xl border border-sky-200 bg-sky-50 p-4">
                  <div className="flex flex-wrap items-start gap-4">
                    <img src={imagePreviewUrl} alt="Selected image preview" className="max-h-48 max-w-full rounded-lg border border-slate-200 bg-white object-contain" />
                    <div className="min-w-48 flex-1 space-y-3">
                      <p className="break-all text-xs text-slate-600">{imageFile.name} · {(imageFile.size / 1024).toFixed(0)} KB</p>
                      <label className="block text-sm font-medium text-slate-700">Alternative text <span className="font-normal text-slate-500">(optional)</span><input value={imageAltText} onChange={event => setImageAltText(event.target.value)} maxLength={300} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" placeholder="Describe the image for screen readers" /></label>
                      <label className="block text-sm font-medium text-slate-700">Caption <span className="font-normal text-slate-500">(optional)</span><input value={imageCaption} onChange={event => setImageCaption(event.target.value)} maxLength={300} className="mt-1 w-full rounded-lg border border-slate-300 bg-white px-3 py-2" placeholder="Caption shown below the image" /></label>
                    </div>
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {!uploadedImage ? <button type="button" onClick={uploadSelectedImage} disabled={imageUploading} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white disabled:opacity-60">{imageUploading ? 'Uploading…' : 'Upload image'}</button> : <button type="button" onClick={insertUploadedImage} className="rounded-lg bg-teal-800 px-4 py-2 text-sm font-semibold text-white">Insert at cursor</button>}
                    <button type="button" onClick={resetImageUpload} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700">Cancel</button>
                  </div>
                  {uploadedImage && <p role="status" className="text-xs text-emerald-800">Image uploaded and ready to insert. It is not part of the lesson until you insert and save the stage.</p>}
                </section>}

                {lessonImages.length > 0 && <section className="rounded-xl border border-slate-200 bg-white p-4">
                  <h3 className="text-sm font-semibold text-slate-800">Images in this lesson ({lessonImages.length})</h3>
                  <ul className="mt-2 space-y-2">{lessonImages.map((image, index) => <li key={`${image.url}-${image.start}`} className="flex min-w-0 items-center justify-between gap-3 rounded-lg bg-slate-50 px-3 py-2"><span className="min-w-0 truncate text-sm text-slate-700">{image.altText || `Image ${index + 1}`}{image.caption ? ` · ${image.caption}` : ''}</span><button type="button" onClick={() => removeImageReference(image)} className="shrink-0 text-sm font-semibold text-red-700 underline underline-offset-2">Remove</button></li>)}</ul>
                </section>}

                <div className="flex flex-wrap items-center gap-3">
                  <button type="button" onClick={() => setShowLessonPreview(value => !value)} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-semibold text-sky-950 hover:bg-slate-50">{showLessonPreview ? 'Hide lesson preview' : 'Preview lesson'}</button>
                </div>
                {showLessonPreview && <section aria-label="Lesson preview" className="rounded-xl border border-slate-200 bg-white p-5"><h3 className="mb-4 text-sm font-semibold text-slate-700">Student lesson preview</h3><div className="space-y-5"><YouTubeVideoPlayers videoIds={selectedStage.stageCode === 'VIDEO_LESSONS' ? editorYoutubeVideoIds : []} titlePrefix="Preview video" /><SafeMarkdown source={editorBody} imageUrlPrefix="/api/admin/learning-images/" allowYoutubeVideos={selectedStage.stageCode === 'VIDEO_LESSONS'} /></div></section>}
                <div className="rounded-lg bg-sky-50 p-3 text-xs leading-5 text-sky-950">
                  <p className="font-semibold">Text and media references</p>
                  <p>Use Upload Image to preview and insert JPG, PNG, or WEBP files. Images are stored outside the database; the stage body stores a safe image reference. YouTube embeds are available only in Stage 5 – Video Lessons.</p>
                </div>
                <div className="flex flex-wrap items-center gap-3">
                  <button type="submit" disabled={busy} className="rounded-lg bg-teal-800 px-5 py-2.5 text-sm font-semibold text-white hover:bg-teal-900 disabled:cursor-wait disabled:opacity-60">{busy ? 'Saving…' : 'Save stage content'}</button>
                  <span className="text-xs text-slate-500">{selectedStage.hasContent ? 'This stage has content configured.' : 'This stage is currently empty.'}</span>
                </div>
              </form>
            )}
          </section>
        </div>
      )}
    </section>
  )
}

export default LearningContentManagement
