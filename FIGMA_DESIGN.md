# Figma Design and UI/UX Guidelines

## Prototype

- **Prototype:** [CSS NC II COC 1 and COC 2 prototype](https://www.figma.com/proto/zItPqmZNrjV2l5xHa4Rf7Z/Untitled?node-id=22-181&t=b6sgpGrieTvnHppe-1&scaling=scale-down&content-scaling=fixed&page-id=0%3A1&starting-point-node-id=22%3A181)
- **Starting frame requested:** node `22:181` on page `0:1`.
- **Inspection source:** 19 user-provided screenshots of prototype screens, supplied 2026-11-19.
- **Scope note:** The screenshots show screens inside a MacBook Pro 16-inch presentation mockup. Descriptions below refer to the app interface, not the surrounding browser or device frame.

## Scope and Design Authority

Figma is a **visual starting point and general reference, not a strict implementation specification**. Preserve its useful visual identity where it fits, but improve, simplify, reorganize, or make a layout responsive when needed to support the actual product clearly.

Use this priority when making decisions:

1. `SYSTEM_DESIGN.md` defines functionality, user roles, workflows, business rules, and intended interactions.
2. `DATABASE_DESIGN.md` defines the stored records, relationships, fields, and constraints. The UI must not present unsupported information as if it were stored or manageable.
3. `FIGMA_DESIGN.md` guides visual appearance, layouts, and components. Adapt it when necessary to support the requirements above.
4. `AGENTS.md` provides development instructions and coding guidelines.

If the prototype conflicts with the system requirements or database design, follow the system and database documents. **UI improvements are encouraged** when they make the system more functional, readable, responsive, consistent, or easier to use. Keep the implementation simple and avoid features that the system does not require.

This file documents visual references and practical UI/UX guidance. It does not change system behavior or database requirements.

## Prototype Visual Reference

The screenshots provide a visual language to draw from, not a requirement to reproduce every detail or screen literally.

### Overall Style and Layout

- The entry screen uses a school/campus photograph. Student dashboard and learning screens use a dark computer-circuit-board photograph.
- Interior views layer translucent deep blue/teal navigation and rounded dark teal-green content panels over the background image.
- The student learning composition places a persistent left sidebar beside a large content area.
- Text is primarily white or off-white. Active navigation uses a darker green/teal highlight.
- The entry form uses a translucent light panel, gray fields, and a green action button.
- Progress and assessment results use circular score/progress indicators; certificate text uses a muted gold/orange accent.
- The screenshots are desktop compositions inside a MacBook Pro 16-inch presentation. They do not show mobile/tablet behavior.

### Colors and Typography

Use the visible palette as a starting point:

| UI role | Screenshot reference |
|---|---|
| Sidebar | Deep blue/teal, with image/texture visible through the treatment. |
| Main panels | Dark teal-green, translucent, rounded. |
| Active navigation | Darker green/teal highlight. |
| Primary text | White or off-white. |
| Score rings and small controls | Pale gray/white with dark teal centers. |
| Overall progress ring | Muted gold/tan around a dark teal center. |
| Certificate emphasis | Orange/gold. |
| Entry form | Translucent light-gray/white container, medium-gray fields, dark text. |
| Entry action | Green button with dark text. |

The screenshots suggest a sans-serif typeface, bold headings, and mostly white body text. The exact font family, color values, font sizes, weights, and line heights are not available; choose and apply a consistent accessible type scale rather than treating screenshot dimensions as exact tokens.

### Navigation, Headers, and Components

- Student screenshots show a hamburger icon and student-name label near the top of a blue/teal sidebar. The navigation includes a dashboard link, a course link, and stage links on learning pages.
- Active stage links use a full-width darker highlight. Some long labels wrap to a second line.
- The Admin student-record screen uses a sidebar with an `ADMIN` heading, a `Students Records` link, and `Log out` near the bottom.
- Main content uses large rounded translucent panels. Small pale buttons are used for proceeding between stages; a compact arrow control appears at the lower right on some learning screens.
- The student entry screen shows a campus image, institutional logos, a welcome heading, and a centered translucent form panel.
- No modal is visible in the screenshots. A modal is not required by the visual reference; use a simple page or dialog only where the actual workflow benefits from it.

### Screens Shown in the Supplied Reference

The screenshots show: student entry; a student dashboard in incomplete and completed states; course introduction; term definition; topic list; examples; video; Pre-Test, Activity, and Post-Test question/result screens; and an Admin student-records table. The Admin screenshot shows columns labeled Student Name, Student No., Program, Year Level, Remarks, and View/Edit. It has thin grid lines over a translucent teal panel and eye icons for row actions.

The screenshot inventory appears at the end of this document. No project image assets were copied; the inventory refers to the user-provided screenshot numbers.

## Project UI/UX Guidelines

### General Layout and Responsive Behavior

- Keep the visual language consistent: blue/teal navigation, dark teal content areas, restrained use of green and gold accents, and clear high-contrast text.
- Favor readable content over excessive transparency or photographic detail. If a background image reduces legibility, strengthen the panel overlay, simplify the background, or omit it on that screen.
- Use reusable React components for shared navigation, stage status, progress, assessment questions, score summaries, forms, tables, and confirmation/error feedback.
- Design for desktop, tablet, and mobile, even though the screenshots only show a desktop device. At narrow widths, adapt the sidebar and multi-column dashboard into compact navigation and stacked content; do not assume a specific breakpoint from the prototype.
- Use clear labels, visible focus states, keyboard-accessible controls, and text labels/icons in addition to color for status.
- Provide simple loading, empty, validation, and server-error states where the workflow needs them. Do not invent additional business features to fill empty states.

### Student Entry and Login

- Student access follows `SYSTEM_DESIGN.md`: Full Name + Program are the normal lookup fields; ask for Student ID when needed to distinguish multiple active matches. Use the database field/label **Student ID** (`student_id`), not a separate School ID field.
- Students are registered and managed by Admins. The prototype's `Sign Up` label should not be copied as student self-registration unless system requirements are changed. The interface may be improved to make student entry/login clear and consistent with the approved flow.
- Admin access is a separate Username + Password login. The screenshots do not show an Admin login form, so its visual details remain open; keep it consistent with the reference palette without mixing the Admin and Student flows.
- Do not suggest that Full Name + Program is a password or strong identity proof. Follow the system's safeguards and communicate errors without exposing private account information.

### Admin Area

The system design requires clear Admin navigation for:

- Dashboard.
- Student Management.
- Competency Management.
- Discussion and Topic Management.
- Learning Content Management.
- Assessment Management.
- Student Progress and Results.
- Generalized Post-test Retake Management.

The existing student-record table is a useful layout reference, but adapt its columns to stored data. Student records store Student ID, Full Name, Program, active status, and timestamps. `Year Level` is not a field in the approved `students` table, so do not present it as saved student data unless the system/database design is formally updated. Progress/remarks should be derived from actual progress and assessment records, not entered as an unsupported student attribute.

Admin workflows should use the existing record relationships:

- Student management: create, view, edit, activate, and deactivate students; review student progress/results.
- Course management: select COC 1 or COC 2, then manage ordered Discussions and their ordered topics.
- Content management: edit the single stage-content record for each stage of a Discussion, including title, body, and display order; support text, image references, and video links as designed.
- Assessment management: choose the correct Discussion or competency assessment, manage questions and choices, and configure the stored answer key without exposing it to Students.
- Retake management: show the student, competency, attempts/results, remaining or exhausted allowance, and the Admin authorization history supported by the database. Each authorization is tied to an Admin and timestamp and grants a block of three attempts.

Use confirmation or feedback states for consequential actions such as deactivation or retake authorization only as needed for clarity. Do not add fields or controls that imply unsupported records or business rules.

### Student Dashboard and COC 1 / COC 2

The dashboard may borrow the prototype's progress ring, course card, and assessment-score cards, but must represent the real two-competency system:

- Show COC 1 and COC 2, not COC 3.
- Clearly distinguish available, locked, in-progress, completed, passed, and failed states using text and visual treatment.
- Show COC 2 as locked until the student passes the COC 1 Generalized Post-test at 70% or higher.
- Represent progress using stored stage/discussion/competency progress; do not rely on browser-only progress or invent extra scores.
- Allow access to completed Discussions without implying that previous progress or assessment history will reset.
- Use only student information and progress/results returned by the backend.

The course structure is six ordered Discussions in COC 1 and three ordered Discussions in COC 2. Each Discussion contains its topics/subtopics and one ordered seven-stage flow. Course labels and content must come from the stored competency, Discussion, topic, and stage records; do not use the prototype's COC 3/server lesson labels for COC 1 or COC 2 content.

### Discussion Learning Stages

Every Discussion uses the canonical stages, in this order:

1. `PRE_TEST` — Pre-test.
2. `DEFINITIONS` — Definition of Terms.
3. `EXPLANATION` — Explanation of the Discussion.
4. `EXAMPLES` — Examples.
5. `VIDEO_LESSONS` — Video Lessons.
6. `ACTIVITY` — Activity.
7. `POST_TEST` — Discussion Post-test.

The prototype's `Introduction`, `COC 3 Lesson`, `Video`, and other labels are visual examples only. Map the real stage names and content to the canonical sequence above. Show locked, available, and completed stage states clearly. A student must submit the Pre-test, Activity, and Discussion Post-test to proceed past those assessment stages; passing those individual assessments is not required. All seven stages must be completed before the next Discussion becomes available. Previously completed stages and assessment history remain intact when revisiting.

Content panels should accommodate the single editable stage body stored per Discussion/stage, including text, image URLs, and video links. Do not create UI expectations for multiple independent content records per stage or binary media stored in the database.

### Assessment Interfaces and Feedback

- Keep Pre-test, Activity, and Discussion Post-test identifiable as separate assessment stages. A Generalized Post-test is a separate competency-level assessment, not the Discussion Post-test stage.
- Present questions, answer choices, progress through the form, and submission controls clearly. Use only configured question data and supported question types; do not hardcode sample questions from the prototype.
- Discussion assessments may be interrupted and resumed. Each may be submitted once. Preserve saved answers and submitted results.
- After a Pre-test submission, show the score and incorrectly answered questions; correct answers may be shown.
- After Activity and Discussion Post-test submissions, show the score, incorrectly answered questions, and correct answers.
- A Generalized Post-test becomes available after all Discussions in its competency are complete. Show its 70% pass threshold and the student's remaining submitted attempts when available.
- Generalized Post-tests start with three submitted attempts; each Admin authorization adds exactly three. Interrupted attempts are resumable and do not use an allowance. Keep COC 1 and COC 2 allowances separate.
- Show Generalized Post-test result and remaining attempts using backend results. On pass, show the competency as passed and preserve that state. On exhaustion, show the failed/locked state and any available Admin-authorized retake status without discarding history.
- Never show stored answer keys or correctness flags before the feedback point allowed by the system. Do not invent question counts, randomization, or review timing.

The prototype's circular result indicators and simple next-step buttons are suitable visual references, but result copy, score values, pass states, navigation, and available actions must reflect actual assessment type and backend state.

### Data Display and Database Fit

Use `DATABASE_DESIGN.md` to determine which values can be shown, edited, or derived:

- Admin and Student accounts are separate. Admin has username/password hash; Student has Student ID, Full Name, Program, and active status, with no email/password requirement.
- Competencies, Discussions, and topics have display ordering and active state; show these consistently in Admin and Student views.
- Each Discussion has exactly one content record for each of the seven canonical stages.
- Assessment types and scopes distinguish Discussion assessments (`PRE_TEST`, `ACTIVITY`, `DISCUSSION_POST_TEST`) from competency-level `GENERALIZED_POST_TEST`.
- Questions, choices, attempts, saved answers, scores, stage progress, competency progress, and retake authorization history are shown only where relevant and according to their relationships.
- Overall/competency progress percentages may be derived from progress rows; do not imply that a duplicate progress percentage is independently stored.
- Do not display unsupported student columns such as `Year Level`, or invent timestamps, statuses, contact details, or metrics not present or derived from the approved schema.

## Reference Coverage and Open Visual Details

The supplied prototype screenshots show a student entry page, student dashboard, learning-stage pages, assessment questions/results, and one Admin student-records table. However, all course-learning examples in the screenshots are labeled **COC 3** and include server-related copy. They are not COC 1 or COC 2 designs. Use their visual treatment only; implement the actual COC 1/COC 2 structure and content defined by `SYSTEM_DESIGN.md` and the approved database design.

The screenshots do not show an Admin login screen, full Admin management pages, a retake-authorization workflow, COC 1/COC 2 course screens, mobile/tablet variants, edit forms/modals, or question selection and submission states. Exact color codes and font family/metrics are also unavailable. These details can be designed later to meet the requirements and should not be assumed to be present in the prototype.

## Screen Inventory

| Screenshot(s) | Visible reference | Use as a visual reference for |
|---|---|---|
| 1, 17 | Student entry with campus image, logos, welcome text, fields, Sign Up, Admin link | Photo-led entry, centered translucent form card; adapt inputs/actions to approved student login and Admin login. |
| 2, 16 | Student dashboard with progress ring, certificate panel, assessment score rings | Progress and summary-card composition; replace COC 3 with COC 1/COC 2 and actual stored progress. |
| 3–4 | Course introduction and overview, labeled COC 3 | Text-led course content panel; use actual COC 1/COC 2 records and content. |
| 5, 10, 18 | Numbered Pre-Test question examples | Assessment question typography and vertical content rhythm; controls/submit state not shown. |
| 6 | Pre-Test result | Centered score ring and next-stage action. |
| 7 | Definition of Terms | Term heading, definition, illustration, next arrow. |
| 8 | Lesson topics | Heading and explanatory topic list. |
| 9 | Example | Image plus explanatory copy. |
| 11 | Video | Embedded-video preview and next arrow. |
| 12, 14 | Activity and Post-Test question examples | Assessment question layout. |
| 13 | Activity result | Score, pass message, next action. |
| 15 | Post-Test result | Score and dashboard-return action. |
| 19 | Admin student-record table | Sidebar and table treatment; adapt columns to stored Student and progress data. |

## References and Assets

- **Prototype link:** See the link at the top of this document.
- **Screenshot references:** User-provided screenshots 1–19 in the conversation. No image files were copied into the project.
- **Visible imagery:** Campus photograph and institutional logos on entry; circuit-board background, server/network illustrations, and a video preview on student learning screens.
- **Visual tokens:** No Figma color tokens or inspectable font specifications were provided. Palette and typography descriptions are approximate visual observations, not fixed implementation values.

## Follow-up

- Use the Figma source or updated screenshots for COC 1/COC 2, Admin login/management, retake history, and responsive variants if they become available.
- Confirm exact font and color tokens from the editable Figma file if precise matching is needed.
- Resolve any design questions against `SYSTEM_DESIGN.md` and `DATABASE_DESIGN.md`; adapt prototype details instead of changing approved behavior or implying unsupported data.
