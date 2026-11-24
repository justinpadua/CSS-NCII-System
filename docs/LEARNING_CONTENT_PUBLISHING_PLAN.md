# Learning Content Publishing Plan

## Purpose and decision

This plan prepares a controlled, Admin-reviewed publication of the user-approved Markdown learning materials into existing learning_stage_contents records. It does not authorize or perform publication. No live MySQL connection was made, no application code or source Markdown was changed, and no database records were changed.

**Recommended method:** use the existing Admin Learning Content Management page and its per-stage source preview, editor, and explicit save. Do not add a batch importer or run direct SQL. This preserves the Admin review step, existing authorization/CSRF, and update service.

The prior comparison export records 63 active stage rows with empty contentBody values. That snapshot describes only its export time and does not prove the current records remain empty. Recheck the current stage in the Admin UI immediately before each save.

## Existing application flow inspected

### Admin API and security

- GET /api/admin/competencies lists COC competency records.
- GET /api/admin/competencies/{competencyId}/discussions lists ordered Discussions through the course-structure API.
- GET /api/admin/competencies/{competencyId}/discussions/{discussionId}/learning-stages returns the seven current stage rows.
- GET /api/admin/competencies/{competencyId}/discussions/{discussionId}/learning-stages/{stageCode}/source-preview reads packaged Markdown and returns a preview; it does not write.
- PUT /api/admin/competencies/{competencyId}/discussions/{discussionId}/learning-stages/{stageCode} updates the existing stage title/body through LearningContentService. It replaces the submitted whole body and updates its timestamp; this is the write/publish action.
- AdminSecurityConfiguration restricts /api/admin/** to ROLE_ADMIN and retains CSRF. The frontend uses the existing Admin cookie/session/CSRF helper.
- Maven packages learning-content/COC-1/**/*.md and COC-2/**/*.md as backend classpath resources for source preview.
- The Student Dashboard reads active database stage content. Saving an active stage body can make it student-visible immediately; the current model has no separate draft/publish status.

### Current Admin interface

The page selects COC → competency → Discussion → stage. It shows configured/empty state, previews source read-only, loads a candidate into an unsaved editor draft, then saves only after Admin submission. It is a manual one-stage-at-a-time workflow. The content_body column is LONGTEXT and the editor supports Markdown; no schema change is indicated.

The source preview validates COC, Discussion order/title, stage code and path. If existing content is nonblank it disables loading the source candidate, and it identifies exact duplicates. The subsequent PUT replaces the whole body. Preserve any nonempty live record unless an Admin reviews and explicitly approves replacement.

## Exact 63-file source-to-record mapping

The identifiers below are from the supplied JSONL snapshot, not live database queries. Verify current records through the Admin API just before publishing. Each Discussion has seven records; stage codes retain canonical order.

| Competency code | Competency ID | Discussion ID | Discussion order | Stage record ID | Stage code | Source file |
|---|---:|---:|---:|---:|---|---|
| COC1 | 1 | 1 | 1 | 1 | PRE_TEST | learning-content/COC-1/Lesson-01/01-Pretest.md |
| COC1 | 1 | 1 | 1 | 2 | DEFINITIONS | learning-content/COC-1/Lesson-01/02-Definition-of-Terms.md |
| COC1 | 1 | 1 | 1 | 3 | EXPLANATION | learning-content/COC-1/Lesson-01/03-Discussion.md |
| COC1 | 1 | 1 | 1 | 4 | EXAMPLES | learning-content/COC-1/Lesson-01/04-Examples.md |
| COC1 | 1 | 1 | 1 | 5 | VIDEO_LESSONS | learning-content/COC-1/Lesson-01/05-Video-Lessons.md |
| COC1 | 1 | 1 | 1 | 6 | ACTIVITY | learning-content/COC-1/Lesson-01/06-Activity.md |
| COC1 | 1 | 1 | 1 | 7 | POST_TEST | learning-content/COC-1/Lesson-01/07-Posttest.md |
| COC1 | 1 | 2 | 2 | 8 | PRE_TEST | learning-content/COC-1/Lesson-02/01-Pretest.md |
| COC1 | 1 | 2 | 2 | 9 | DEFINITIONS | learning-content/COC-1/Lesson-02/02-Definition-of-Terms.md |
| COC1 | 1 | 2 | 2 | 10 | EXPLANATION | learning-content/COC-1/Lesson-02/03-Discussion.md |
| COC1 | 1 | 2 | 2 | 11 | EXAMPLES | learning-content/COC-1/Lesson-02/04-Examples.md |
| COC1 | 1 | 2 | 2 | 12 | VIDEO_LESSONS | learning-content/COC-1/Lesson-02/05-Video-Lessons.md |
| COC1 | 1 | 2 | 2 | 13 | ACTIVITY | learning-content/COC-1/Lesson-02/06-Activity.md |
| COC1 | 1 | 2 | 2 | 14 | POST_TEST | learning-content/COC-1/Lesson-02/07-Posttest.md |
| COC1 | 1 | 3 | 3 | 15 | PRE_TEST | learning-content/COC-1/Lesson-03/01-Pretest.md |
| COC1 | 1 | 3 | 3 | 16 | DEFINITIONS | learning-content/COC-1/Lesson-03/02-Definition-of-Terms.md |
| COC1 | 1 | 3 | 3 | 17 | EXPLANATION | learning-content/COC-1/Lesson-03/03-Discussion.md |
| COC1 | 1 | 3 | 3 | 18 | EXAMPLES | learning-content/COC-1/Lesson-03/04-Examples.md |
| COC1 | 1 | 3 | 3 | 19 | VIDEO_LESSONS | learning-content/COC-1/Lesson-03/05-Video-Lessons.md |
| COC1 | 1 | 3 | 3 | 20 | ACTIVITY | learning-content/COC-1/Lesson-03/06-Activity.md |
| COC1 | 1 | 3 | 3 | 21 | POST_TEST | learning-content/COC-1/Lesson-03/07-Posttest.md |
| COC1 | 1 | 4 | 4 | 22 | PRE_TEST | learning-content/COC-1/Lesson-04/01-Pretest.md |
| COC1 | 1 | 4 | 4 | 23 | DEFINITIONS | learning-content/COC-1/Lesson-04/02-Definition-of-Terms.md |
| COC1 | 1 | 4 | 4 | 24 | EXPLANATION | learning-content/COC-1/Lesson-04/03-Discussion.md |
| COC1 | 1 | 4 | 4 | 25 | EXAMPLES | learning-content/COC-1/Lesson-04/04-Examples.md |
| COC1 | 1 | 4 | 4 | 26 | VIDEO_LESSONS | learning-content/COC-1/Lesson-04/05-Video-Lessons.md |
| COC1 | 1 | 4 | 4 | 27 | ACTIVITY | learning-content/COC-1/Lesson-04/06-Activity.md |
| COC1 | 1 | 4 | 4 | 28 | POST_TEST | learning-content/COC-1/Lesson-04/07-Posttest.md |
| COC1 | 1 | 5 | 5 | 29 | PRE_TEST | learning-content/COC-1/Lesson-05/01-Pretest.md |
| COC1 | 1 | 5 | 5 | 30 | DEFINITIONS | learning-content/COC-1/Lesson-05/02-Definition-of-Terms.md |
| COC1 | 1 | 5 | 5 | 31 | EXPLANATION | learning-content/COC-1/Lesson-05/03-Discussion.md |
| COC1 | 1 | 5 | 5 | 32 | EXAMPLES | learning-content/COC-1/Lesson-05/04-Examples.md |
| COC1 | 1 | 5 | 5 | 33 | VIDEO_LESSONS | learning-content/COC-1/Lesson-05/05-Video-Lessons.md |
| COC1 | 1 | 5 | 5 | 34 | ACTIVITY | learning-content/COC-1/Lesson-05/06-Activity.md |
| COC1 | 1 | 5 | 5 | 35 | POST_TEST | learning-content/COC-1/Lesson-05/07-Posttest.md |
| COC1 | 1 | 6 | 6 | 36 | PRE_TEST | learning-content/COC-1/Lesson-06/01-Pretest.md |
| COC1 | 1 | 6 | 6 | 37 | DEFINITIONS | learning-content/COC-1/Lesson-06/02-Definition-of-Terms.md |
| COC1 | 1 | 6 | 6 | 38 | EXPLANATION | learning-content/COC-1/Lesson-06/03-Discussion.md |
| COC1 | 1 | 6 | 6 | 39 | EXAMPLES | learning-content/COC-1/Lesson-06/04-Examples.md |
| COC1 | 1 | 6 | 6 | 40 | VIDEO_LESSONS | learning-content/COC-1/Lesson-06/05-Video-Lessons.md |
| COC1 | 1 | 6 | 6 | 41 | ACTIVITY | learning-content/COC-1/Lesson-06/06-Activity.md |
| COC1 | 1 | 6 | 6 | 42 | POST_TEST | learning-content/COC-1/Lesson-06/07-Posttest.md |
| COC2 | 2 | 7 | 1 | 43 | PRE_TEST | learning-content/COC-2/Lesson-01/01-Pretest.md |
| COC2 | 2 | 7 | 1 | 44 | DEFINITIONS | learning-content/COC-2/Lesson-01/02-Definition-of-Terms.md |
| COC2 | 2 | 7 | 1 | 45 | EXPLANATION | learning-content/COC-2/Lesson-01/03-Discussion.md |
| COC2 | 2 | 7 | 1 | 46 | EXAMPLES | learning-content/COC-2/Lesson-01/04-Examples.md |
| COC2 | 2 | 7 | 1 | 47 | VIDEO_LESSONS | learning-content/COC-2/Lesson-01/05-Video-Lessons.md |
| COC2 | 2 | 7 | 1 | 48 | ACTIVITY | learning-content/COC-2/Lesson-01/06-Activity.md |
| COC2 | 2 | 7 | 1 | 49 | POST_TEST | learning-content/COC-2/Lesson-01/07-Posttest.md |
| COC2 | 2 | 8 | 2 | 50 | PRE_TEST | learning-content/COC-2/Lesson-02/01-Pretest.md |
| COC2 | 2 | 8 | 2 | 51 | DEFINITIONS | learning-content/COC-2/Lesson-02/02-Definition-of-Terms.md |
| COC2 | 2 | 8 | 2 | 52 | EXPLANATION | learning-content/COC-2/Lesson-02/03-Discussion.md |
| COC2 | 2 | 8 | 2 | 53 | EXAMPLES | learning-content/COC-2/Lesson-02/04-Examples.md |
| COC2 | 2 | 8 | 2 | 54 | VIDEO_LESSONS | learning-content/COC-2/Lesson-02/05-Video-Lessons.md |
| COC2 | 2 | 8 | 2 | 55 | ACTIVITY | learning-content/COC-2/Lesson-02/06-Activity.md |
| COC2 | 2 | 8 | 2 | 56 | POST_TEST | learning-content/COC-2/Lesson-02/07-Posttest.md |
| COC2 | 2 | 9 | 3 | 57 | PRE_TEST | learning-content/COC-2/Lesson-03/01-Pretest.md |
| COC2 | 2 | 9 | 3 | 58 | DEFINITIONS | learning-content/COC-2/Lesson-03/02-Definition-of-Terms.md |
| COC2 | 2 | 9 | 3 | 59 | EXPLANATION | learning-content/COC-2/Lesson-03/03-Discussion.md |
| COC2 | 2 | 9 | 3 | 60 | EXAMPLES | learning-content/COC-2/Lesson-03/04-Examples.md |
| COC2 | 2 | 9 | 3 | 61 | VIDEO_LESSONS | learning-content/COC-2/Lesson-03/05-Video-Lessons.md |
| COC2 | 2 | 9 | 3 | 62 | ACTIVITY | learning-content/COC-2/Lesson-03/06-Activity.md |
| COC2 | 2 | 9 | 3 | 63 | POST_TEST | learning-content/COC-2/Lesson-03/07-Posttest.md |

Discussion titles from SYSTEM_DESIGN.md and the export:

- **COC1 Discussion 1 (Discussion ID 1):** Discuss fundamental concepts of computer systems servicing
- **COC1 Discussion 2 (Discussion ID 2):** Discuss elements of computer systems
- **COC1 Discussion 3 (Discussion ID 3):** Perform disassembly and assembly of computer hardware
- **COC1 Discussion 4 (Discussion ID 4):** Create portable bootable device
- **COC1 Discussion 5 (Discussion ID 5):** Perform installation of system software and application software
- **COC1 Discussion 6 (Discussion ID 6):** Perform computer testing and documentation
- **COC2 Discussion 1 (Discussion ID 7):** Discuss the concepts of computer network
- **COC2 Discussion 2 (Discussion ID 8):** Perform setting up of computer network
- **COC2 Discussion 3 (Discussion ID 9):** Perform network testing and documentation

## Content types and handling

### Instructional content

- Definitions, Explanation, Examples and Video Lessons are ordinary Markdown learning content. Preview, inspect the full candidate, preserve Markdown, links and external media references, then save through the Admin UI.
- Activity files include student task prompts. COC 1 Activity files also contain Instructor Answer Key sections. The current Markdown preview service treats ACTIVITY as ordinary content and returns the whole file, so a key could be saved to an active student-visible stage.
- Safe Markdown rendering does not make instructor answers private. Before any Activity is published, the preview/import path must exclude the Instructor Answer Key and everything after it, and regression tests must prove the student-facing body has no answer material. If the boundary is uncertain, do not publish that Activity content.

### Pre-tests and post-tests

- Actual questions, answer choices, correct answers and explanations belong in the existing Admin Assessment Management and assessment tables, not in student-facing learning-stage content. Keep assessment type, correct-answer secrecy, scoring, 70% Generalized Post-test pass rule, retakes and historical attempts unchanged.
- Current preview removes material starting at the first detected question line or answer-key marker for PRE_TEST/POST_TEST. Treat this as a draft filter, inspect the candidate and ensure it retains appropriate instructions while excluding every item/key. Do not rely on it without tests.
- COC 2 Discussion 1 PRE_TEST is a composite learner module rather than a clean standalone pre-test. Manually select/confirm only appropriate instructions; do not load the whole module into the stage.
- The supplied learning-stage export omitted assessments/questions/choices, so their existence and completeness remain unverified.

## Recheck of COC 2 Discussion 1 sources (2026-11-22)

Both source files now contain actual content and neither contains the missing-content marker.

- **ACTIVITY** maps to competency ID 2, Discussion ID 7 (order 1), stage record ID 48, stage code ACTIVITY, from learning-content/COC-2/Lesson-01/06-Activity.md. It contains five student-facing practical task parts: component matching, T568B pin order, cable-making steps, safe installation, and cable troubleshooting. The file has no missing marker and no instructor answer-key section. This file is suitable for the ACTIVITY stage after Admin preview and review. The current preview service does not filter Activity files, so continue to block other Activity files that contain instructor keys until sanitization is fixed.
- **POST_TEST** maps to competency ID 2, Discussion ID 7 (order 1), stage record ID 49, stage code POST_TEST, from learning-content/COC-2/Lesson-01/07-Posttest.md. It contains directions, ten multiple-choice questions, and an Answer Key. It has no missing marker. The ten questions and key are assessment data and must be entered only through Admin Assessment Management. The source-preview implementation stops at the first numbered question for POST_TEST, so the stage candidate should contain only the heading/directions. Inspect the complete preview and verify it contains no question, choice, or answer-key text before saving.
- The supplied export was a prior snapshot; it showed stage records 48 and 49 with empty content bodies. Current live stage contents remain unverified because this task makes no database connection.

The materials are user-approved, but source approval does not change their data classification: the post-test questions and key remain private assessment data and must not be copied into student-facing stage content.

## Safeguards required before publication

1. Use only Admin preview and update endpoints; no direct SQL, startup import, or batch endpoint.
2. Re-read each live stage before saving. If its body is nonempty, compare and preserve it unless replacement is explicitly reviewed and approved. Do not rely on the older export.
3. COC 2 Discussion 1 ACTIVITY may be reviewed through the existing stage preview; the inspected source contains student instructions and no answer key. For other Activity sources, especially all six COC 1 Activities, first close and test the instructor-answer-key exclusion gap. The current service returns Activity source verbatim.
4. Keep all actual PRE_TEST/POST_TEST questions, choices, correct answers, and explanations in Admin Assessment Management. For COC 2 Discussion 1 POST_TEST, only save the previewed directions to the stage body; verify preview ends before the first question and contains no key.
5. Admin checks source path, competency, Discussion, stage, and the full candidate. Saving is publication, not a draft operation.
6. Keep stage code/order fixed and preserve progress rows, assessment attempts, and results.
7. After a successful save, refetch the stage by API and record completion in a checklist.

## Recommended later implementation and test sequence

1. Add a focused Activity-source sanitization rule and tests proving COC 1 instructor-key text cannot be loaded or saved into student-visible content. Keep student task instructions intact. This is required before publishing the six COC 1 Activity sources.
2. Add/extend tests for all 63 COC/order/stage/file mappings, canonical ordering, assessment question/key exclusion, existing-content preservation, duplicate detection, and no write during preview. Update source tests so COC 2 Discussion 1 ACTIVITY and POST_TEST are present and importable as appropriate.
3. Verify Admin authorization and CSRF on preview/update. Any MySQL integration tests must be isolated to css_ncii_java_db_migration_test; never use css_ncii_java_db.
4. Through Admin Learning Content Management, review and save ordinary instructional stage content one record at a time. The COC 2 Discussion 1 Activity can be published as student instructions after preview. For PRE_TEST/POST_TEST, keep only appropriate directions in the stage body.
5. Enter the COC 2 Discussion 1 ten-question assessment and its answer key only through Admin Assessment Management. Audit the assessment records separately; the supplied learning-stage export cannot show whether they are already configured.
6. For all published stages, check the Student Dashboard for correct content, safe links/media, no instructor keys or assessment answers, and clear empty states.
7. Export/compare records after publication and verify the seven stage codes/order per Discussion, saved content at beginning/middle/end, and unchanged student progress and assessment history.

## Recommendation

**The source-to-database map covers all 63 stage files, and the two formerly missing COC 2 Discussion 1 files now contain material. The set is not ready for a one-to-one raw-file publication.** Use the existing Admin Learning Content Management flow for reviewed instructional content, one stage at a time. Before publishing COC 1 Activities, add and test filtering for instructor answer keys. Keep all real assessment questions and answer keys in Assessment Management; for the COC 2 Discussion 1 Post-test stage, publish only the safe directions candidate after inspecting the preview. Separately verify/enter its ten-question test through the assessment feature. COC 2 Discussion 1 Activity is now eligible for Admin preview and stage publication. No code or database work is performed by this plan update.

## Scope of this plan

No SQL, MySQL connection, migration, import, application-code change, Markdown edit, or database write was performed. This plan does not claim any content has been published.
## Final source-readiness reconciliation (2026-11-23)

This section supersedes the earlier statements that COC 1 Activity files still contain instructor answer sections or that COC 2 Discussion 1 Activity/Post-test contain missing placeholders. The approved Activity drafts are now in the source files, answer keys/rubrics are separate, and a scan found no instructor-key/rubric markers in the 63 stage sources. The COC 2 Discussion 1 Activity contains five complete task parts; its Post-test contains assessment questions but they are filtered from the stage preview and must be entered only through Assessment Management.

The existing `MarkdownLearningContentImportService.preview()` maps the nine documented Discussion titles and seven canonical stage files, protects non-empty stage content, detects exact duplicates, and for PRE_TEST/POST_TEST stops the candidate at the first numbered question or answer-key marker. It does not write or publish; Admin must explicitly save a candidate through the existing update endpoint. The preview test covers one representative assessment filter and preservation behavior, not all 63 mappings. Admin must inspect the complete preview and keep any questions, choices, answer keys, explanations, and rubrics out of the saved stage body.

Current readiness: Definitions, Explanation, Examples, and Activity sources are prepared for individual Admin preview; the Sample Shop subsection is now an illustrative text layout. All nine Video Lesson sources are previewable, but no video link is approved until an instructor checks playback and suitability. The former COC 2 D1 search link and weak-passphrase demonstration links in COC 2 D2/D3 were withheld. The COC 2 D1 Pre-test source now contains only directions and its original quiz questions; the proposed question-to-Discussion split and the three-question Discussion 1 coverage gap are documented in the instructor answer-key file. The two target-facing COC 2 D3 test titles have been corrected.

The 220 keyed Discussion question rows remain in the instructor-only key companion. Their rationales remain generic; instructor editing is required before assessment activation. The two Generalized Post-test drafts remain separate Admin-review sources (20 questions each, 70% pass rule unchanged); no file in this task imported them. The historical 63-row JSONL database comparison is not evidence of current live database state. Recheck each database stage in Admin immediately before saving, and preserve every existing Admin-edited body.

No preview, save, database query, import, or publication was performed in this content update.
