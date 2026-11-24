# Database Design — CSS NC II COC 1 and COC 2 Management System

## 1. Purpose and scope

This document defines the relational design for the CSS NC II COC 1 and COC 2 Management System in MySQL database `css_ncii_java_db`. A guarded, manual-only migration preparation script is at `docs/student_account_redesign_test_db_migration.sql`; it targets only `css_ncii_java_db_migration_test` and has not been executed. It is not run automatically by the application.

`SYSTEM_DESIGN.md` is the source of system requirements. This design applies the approved decisions that Discussion assessments allow one submission and can be resumed, each learning stage has one editable content body, students self-register with a generated Account ID and password and require Admin approval, Admin accounts use username and password without required email, and retake grants are audited. The migration test database was restored from the existing 14-table MySQL schema and is prepared for the student-account change; no migration has been run.

### Conventions

- MySQL 8 with InnoDB; `BIGINT UNSIGNED` generated identifiers for internal primary and foreign keys.
- `DATETIME(6)` timestamps in UTC; application code supplies or normalizes UTC consistently.
- Text lengths below are proposed practical limits and can be adjusted before implementation.
- Currency is not stored; scores use `DECIMAL` to avoid floating point rounding.
- `JSON` is used only for flexible question response data, not as a replacement for relational course and assessment relationships.
- Physical media files are stored with an online-accessible storage provider; the database stores their URLs and descriptive text.

## 2. Table summary

| Table | Purpose |
|---|---|
| `admins` | Admin login accounts and password hashes. |
| `students` | Student credentials, registration status, private Admin notes, and latest review information. |
| `competencies` | The COC 1 and COC 2 course containers. |
| `discussions` | Ordered Discussions within each competency. |
| `topics` | Ordered topics or subtopics within each Discussion. |
| `learning_stage_contents` | One editable content record for each of the seven stages of a Discussion. |
| `assessments` | Distinguishes the three Discussion assessments from competency Generalized Post-tests. |
| `questions` | Ordered questions and scoring configuration for assessments. |
| `answer_choices` | Optional selectable choices and correct-choice configuration for choice-based questions. |
| `assessment_attempts` | Resumable Discussion attempts and immutable submitted assessment results/history. |
| `attempt_answers` | A student's saved response and evaluated result per question within an attempt. |
| `student_stage_progress` | Completion state for each student's seven stages in each Discussion. |
| `student_competency_progress` | Per-student competency state, including pass status used for unlocking. |
| `retake_authorizations` | Auditable history of Admin grants of additional Generalized Post-test attempt blocks. |

The expected initial course data is two competency rows, nine Discussion rows, twenty-four topic rows, and seven learning-stage rows for every Discussion. These are proposed seed/content records, not schema objects.

### Student-account schema delta

The registration requirements change only the existing `students` table; the design remains at 14 tables. Replace the current human-facing `student_id` column and `is_active` flag with unique `account_id` and a `status` field. Add `password_hash`, nullable `admin_notes`, nullable `reviewed_by_admin_id` (FK to `admins.id`), and nullable `reviewed_at`. Retain the internal `id`, `full_name`, `program`, `created_at`, and `updated_at`. Remove the old unique constraint on `students.student_id` and old `(full_name, program, is_active)` index; add the unique constraint on `account_id` and non-unique `(full_name, program, status)` index. Add no new table. Existing progress and assessment foreign keys continue to use `students.id` and do not change.

For migration, assign every existing row a unique Account ID in the approved format, map legacy `is_active = true` to `status = ACTIVE` and `is_active = false` to `status = DEACTIVATED`, and preserve the row's internal `students.id`. Existing records have no password hashes: do not assign default passwords, and require an Admin to reset each legacy student's password before that account can use Account ID/password login. During a staged migration, `password_hash` must permit NULL for those legacy rows; it can become required after every legacy account has been assigned a hash. Preserve all existing dependent rows and foreign keys.

## 3. Tables and columns

### 3.1 `admins`

Stores Admin accounts separately from students. Students have their own Account ID and password hash in `students`; their credentials must not be represented as Admin accounts.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `username` | `VARCHAR(100)` | Required, unique login name. |
| `password_hash` | `VARCHAR(255)` | Required hash produced by an approved password-hashing algorithm; never store a plaintext password. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

### 3.2 `students`

Stores self-registered Student accounts and their current approval/activation state. Students do not need an official Student ID or email. `id` remains the internal primary key referenced by progress and assessment tables; `account_id` is the generated identifier students use to log in.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key and internal student reference. |
| `account_id` | `VARCHAR(50)` | Required, unique Account ID generated for login as `STU-` plus a decimal number padded to at least six digits (for example, `STU-000001`). The digit count grows beyond six as needed. |
| `full_name` | `VARCHAR(200)` | Required. |
| `program` | `VARCHAR(150)` | Required. |
| `password_hash` | `VARCHAR(255)` | BCrypt password hash; required for accounts with a set password. It may be NULL only for legacy accounts awaiting an Admin password reset during migration. Never store or return a plain-text password or hash to clients. |
| `status` | `VARCHAR(20)` | Required; `PENDING`, `ACTIVE`, `REJECTED`, or `DEACTIVATED`. New registrations are `PENDING`; only `ACTIVE` accounts may log in. |
| `admin_notes` | `TEXT` | Nullable private Admin notes; never included in student responses. |
| `reviewed_by_admin_id` | `BIGINT UNSIGNED` | Nullable FK to `admins.id`; Admin who made the latest approval/rejection decision. |
| `reviewed_at` | `DATETIME(6)` | Nullable time of the latest approval/rejection decision. |
| `created_at` | `DATETIME(6)` | Required registration time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

The application generates unique Account IDs in the `STU-` plus the internal auto-increment `students.id`, formatted to a minimum of six decimal digits (for example, `STU-000001`; values above 999999 use more digits). To obtain the generated primary key without a separate sequence table, create the row inside one transaction with a private unique temporary Account ID, flush to obtain `id`, replace the placeholder with `STU-` plus the formatted ID, and commit only after replacement. The placeholder is never returned, logged, or visible outside the transaction. Enforce final Account ID uniqueness in the database. The internal `students.id` remains the primary key and all relationship references continue to use it. Passwords are at least eight characters and are hashed with BCrypt before persistence. Approval changes status from `PENDING` to `ACTIVE`; rejection changes it to `REJECTED`. An Admin may reconsider a rejected registration and approve or reject it again. Admin deactivation changes an approved account to `DEACTIVATED`, and reactivation returns it to `ACTIVE`. Pending, rejected, and deactivated accounts cannot authenticate. Deactivation and reactivation preserve progress, assessment attempts, and retake history.

Admin-created student accounts use the same unique Account ID generation, password validation and BCrypt hashing, and `PENDING` initial status as self-registration. The Admin supplies an initial password and communicates it to the student through a trusted channel outside the application; it is never logged or returned by an API. The account must pass the same approval step before login is allowed. Admin password resets replace the hash and invalidate all authenticated sessions for that student. Deactivation also invalidates all of that student's authenticated sessions.

Full Name + Program is intentionally non-unique. A non-unique `(full_name, program, status)` index supports Admin review of possible duplicates. The application flags exact matches after Unicode normalization, trimming, repeated-whitespace collapse, and case folding while preserving accents; Admin search can be used to inspect near matches. Do not store a duplicate flag or automatically reject a registration only because the pair matches. Since no official ID or email is collected, the database cannot prove whether two registrations belong to the same person. Admin review, suitable offline identity verification where needed, and registration/login rate limiting are the reasonable safeguards; they do not guarantee identity uniqueness. `created_at` serves as the registration timestamp, so a separate registration-time column is unnecessary.

`reviewed_by_admin_id` has a nullable foreign key because unreviewed registrations have no reviewer. Together with `reviewed_at` and current `status`, these fields identify the latest review decision. This intentionally does not preserve a timeline of every approval/rejection action; a separate review-history table is not proposed because full decision history was not required.

### 3.3 `competencies`

Stores the COC course containers.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `code` | `VARCHAR(20)` | Required unique code, such as `COC1` or `COC2`. |
| `title` | `VARCHAR(200)` | Required display title. |
| `description` | `TEXT` | Optional description. |
| `display_order` | `INT UNSIGNED` | Required order in the UI. |
| `is_active` | `BOOLEAN` | Required; defaults true. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

### 3.4 `discussions`

Stores ordered Discussions within a competency.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `competency_id` | `BIGINT UNSIGNED` | Required FK to `competencies.id`. |
| `title` | `VARCHAR(250)` | Required. |
| `description` | `TEXT` | Optional. |
| `display_order` | `INT UNSIGNED` | Required order within the competency. |
| `is_active` | `BOOLEAN` | Required; defaults true. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

### 3.5 `topics`

Stores the topics/subtopics listed under each Discussion.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `discussion_id` | `BIGINT UNSIGNED` | Required FK to `discussions.id`. |
| `title` | `VARCHAR(250)` | Required topic text/title. |
| `description` | `TEXT` | Optional supplemental text. |
| `display_order` | `INT UNSIGNED` | Required order within the Discussion. |
| `is_active` | `BOOLEAN` | Required; defaults true. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

### 3.6 `learning_stage_contents`

Stores one Admin-editable content body for each standard stage of each Discussion. Media references are URLs in the body or structured body content; binary media is kept outside MySQL. The body format may be plain text or a structured rich-content representation so it can contain text, image URLs, and video links.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `discussion_id` | `BIGINT UNSIGNED` | Required FK to `discussions.id`. |
| `stage_code` | `VARCHAR(30)` | Required; one of `PRE_TEST`, `DEFINITIONS`, `EXPLANATION`, `EXAMPLES`, `VIDEO_LESSONS`, `ACTIVITY`, `POST_TEST`. |
| `title` | `VARCHAR(200)` | Required stage/content title. |
| `content_body` | `LONGTEXT` | Required editable body; may be empty for assessment instructions where those are maintained with the assessment. |
| `display_order` | `TINYINT UNSIGNED` | Required, 1 through 7. |
| `is_active` | `BOOLEAN` | Required; defaults true. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Use a unique constraint on (`discussion_id`, `stage_code`) so each Discussion has exactly one record per stage, plus a unique constraint on (`discussion_id`, `display_order`). The seven allowed stage codes and their fixed sequence are a shared application rule; seed/setup validation must ensure all seven rows exist per Discussion. The assessment definitions and question content remain in the assessment tables; this content table represents the stage's editable instructional body/title/order.

### 3.7 `assessments`

Stores both Discussion assessments and competency-level Generalized Post-tests. Exactly one scope is set: either a Discussion or a competency. `assessment_type` distinguishes the assessment role.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `discussion_id` | `BIGINT UNSIGNED` | Nullable FK to `discussions.id`; set for Discussion assessments only. |
| `competency_id` | `BIGINT UNSIGNED` | Nullable FK to `competencies.id`; set for Generalized Post-tests only. |
| `assessment_type` | `VARCHAR(30)` | Required enum-like value: `PRE_TEST`, `ACTIVITY`, `DISCUSSION_POST_TEST`, or `GENERALIZED_POST_TEST`. |
| `title` | `VARCHAR(200)` | Required display title. |
| `instructions` | `TEXT` | Optional instructions. |
| `pass_percentage` | `DECIMAL(5,2)` | Nullable; Generalized Post-tests use `70.00`; Discussion assessments have no pass threshold. |
| `is_active` | `BOOLEAN` | Required; defaults true. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Required integrity rules:

- Exactly one of `discussion_id` and `competency_id` is non-null.
- `PRE_TEST`, `ACTIVITY`, and `DISCUSSION_POST_TEST` require `discussion_id`; `GENERALIZED_POST_TEST` requires `competency_id`.
- Unique (`discussion_id`, `assessment_type`) prevents duplicate assessment kinds within one Discussion.
- Unique (`competency_id`, `assessment_type`) ensures one Generalized Post-test per competency.
- The application validates that the competency of a Generalized Post-test corresponds to the covered Discussions. MySQL cannot express that cross-table membership rule with a simple FK.

Attempt rules are fixed by the system requirements and enforced in Spring Boot transactions: one submission for each Discussion assessment, and three initial submissions for each Generalized Post-test.

### 3.8 `questions`

Stores assessment questions and scoring configuration. The question type can be expanded as formats are finalized; the relational table does not assume a fixed number of choices.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `assessment_id` | `BIGINT UNSIGNED` | Required FK to `assessments.id`. |
| `question_type` | `VARCHAR(40)` | Required configured type for the question format. |
| `question_text` | `LONGTEXT` | Required prompt. |
| `correct_answer_data` | `JSON` | Required backend-only answer key in a format appropriate to `question_type`; for choice questions it identifies the correct choice or choices. |
| `points` | `DECIMAL(8,2)` | Required, non-negative score weight. |
| `display_order` | `INT UNSIGNED` | Required order within the assessment. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`assessment_id`, `display_order`) prevents duplicate positions. The backend uses `correct_answer_data` to evaluate responses; it is never sent to student clients before allowed by the feedback rules.

### 3.9 `answer_choices`

Stores displayed choices for questions that use selectable answers. Questions that do not use choices have no rows here; correctness is defined once in `questions.correct_answer_data`.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `question_id` | `BIGINT UNSIGNED` | Required FK to `questions.id`. |
| `choice_text` | `TEXT` | Required displayed choice. |
| `display_order` | `SMALLINT UNSIGNED` | Required order within the question. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`question_id`, `display_order`) prevents duplicate choice positions.

### 3.10 `assessment_attempts`

Stores both resumable in-progress work and submitted results. A resumed attempt retains its original row and saved responses. A submitted attempt is retained as history and is not overwritten.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `student_id` | `BIGINT UNSIGNED` | Required FK to `students.id`. |
| `assessment_id` | `BIGINT UNSIGNED` | Required FK to `assessments.id`. |
| `attempt_number` | `SMALLINT UNSIGNED` | Required sequence number within the student and assessment. |
| `status` | `VARCHAR(20)` | Required `IN_PROGRESS` or `SUBMITTED` status. Only submitted attempts count against a Generalized Post-test allowance. |
| `score` | `DECIMAL(9,2)` | Null until submitted; then awarded points. |
| `total_possible_score` | `DECIMAL(9,2)` | Null until submitted; then maximum points for the administered assessment. |
| `percentage` | `DECIMAL(5,2)` | Null until submitted; backend-calculated result. |
| `passed` | `BOOLEAN` | Null for Discussion assessments or in-progress attempts; backend-calculated for Generalized Post-tests. |
| `started_at` | `DATETIME(6)` | Required time attempt began. |
| `submitted_at` | `DATETIME(6)` | Null until submission. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`student_id`, `assessment_id`, `attempt_number`) identifies a sequence number. For a Discussion assessment, only attempt number 1 is valid and only one attempt row may exist per student/assessment; a Spring Boot transaction checks for and reuses the existing `IN_PROGRESS` row, or rejects a new attempt if it is already submitted. For Generalized Post-tests, multiple attempt numbers are permitted, subject to the student's computed allowance.

**Attempt limits and retakes:** a Generalized Post-test allowance is three initial submissions plus three for each recorded authorization. Count only `SUBMITTED` attempts. Before creating an in-progress attempt and before submission, the backend checks available allowance in a transaction. Serialize attempt creation/submission and retake grants for the same student and competency so concurrent requests cannot exceed the allowance or create duplicate in-progress attempts. A passed competency stays passed and cannot be downgraded by a later administrative action. The schema records submitted sequence/history; authorization history is stored separately.

### 3.11 `attempt_answers`

Stores the student's saved answer for each question in an attempt. It supports saving partial answers while an attempt is in progress and records the evaluated answer/result at submission.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `attempt_id` | `BIGINT UNSIGNED` | Required FK to `assessment_attempts.id`. |
| `question_id` | `BIGINT UNSIGNED` | Required FK to `questions.id`. |
| `response_data` | `JSON` | Student's selected choice ID(s), text, or other format-specific response. |
| `is_correct` | `BOOLEAN` | Null until evaluated; then backend-calculated. |
| `awarded_points` | `DECIMAL(8,2)` | Null until evaluated; then backend-calculated points for this answer. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`attempt_id`, `question_id`) gives one current saved response per question in an attempt. Response rows are updated while the attempt is `IN_PROGRESS`; after submission they and the recorded attempt score are retained as history. Assessment content with attempt history should be deactivated rather than deleted.

### 3.12 `student_stage_progress`

Stores a student's completion state for each of the seven stages within each Discussion. It supports stage completion and ordered progression without storing redundant overall percentages.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `student_id` | `BIGINT UNSIGNED` | Required FK to `students.id`. |
| `discussion_id` | `BIGINT UNSIGNED` | Required FK to `discussions.id`. |
| `stage_code` | `VARCHAR(30)` | Required one of the seven standard stage codes used by `learning_stage_contents`. |
| `status` | `VARCHAR(20)` | Required `LOCKED`, `AVAILABLE`, or `COMPLETED`. |
| `completed_at` | `DATETIME(6)` | Null until completed. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`student_id`, `discussion_id`, `stage_code`) provides one current progress row for each stage. The backend advances stages in sequence; the Pre-test, Activity, and Discussion Post-test stages become complete only after their corresponding assessment is submitted. A Discussion is complete when all seven stage rows are complete. The next Discussion is made available only after the current Discussion is complete. Revisit behavior preserves these rows and all attempt history.

### 3.13 `student_competency_progress`

Stores each student's current state for each competency. This is the authoritative pass state used for COC 1 to COC 2 unlocking; overall progress percentages can be calculated from stage rows and attempts rather than duplicated here.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `student_id` | `BIGINT UNSIGNED` | Required FK to `students.id`. |
| `competency_id` | `BIGINT UNSIGNED` | Required FK to `competencies.id`. |
| `status` | `VARCHAR(20)` | Required `LOCKED`, `AVAILABLE`, `IN_PROGRESS`, `FAILED`, or `PASSED`. |
| `passed_at` | `DATETIME(6)` | Null until the competency Generalized Post-test is passed. |
| `created_at` | `DATETIME(6)` | Required creation time. |
| `updated_at` | `DATETIME(6)` | Required last update time. |

Unique (`student_id`, `competency_id`) ensures one current state per student/competency. COC 1 starts available; COC 2 stays locked until the student's COC 1 status is `PASSED`. The backend sets a competency to `PASSED` only when its Generalized Post-test attempt reaches at least 70%. Do not store a separate copied attempt counter here: the allowance is the initial three attempts plus three for each retake authorization row.

### 3.14 `retake_authorizations`

Stores every Admin-authorized additional block of Generalized Post-test attempts. Rows are append-only audit history; previous grants are not overwritten when another grant is made.

| Column | Type | Rules / purpose |
|---|---|---|
| `id` | `BIGINT UNSIGNED` | Primary key. |
| `student_id` | `BIGINT UNSIGNED` | Required FK to `students.id`. |
| `competency_id` | `BIGINT UNSIGNED` | Required FK to `competencies.id`; identifies which competency's Generalized Post-test is affected. |
| `admin_id` | `BIGINT UNSIGNED` | Required FK to `admins.id`; Admin who granted authorization. |
| `authorized_at` | `DATETIME(6)` | Required UTC date and time when the authorization was granted. |
Each row represents one authorization and grants exactly three additional attempts for that student and competency. No uniqueness constraint should prevent repeated grants over time. The backend permits a grant only when the relevant Generalized Post-test is exhausted and not already passed, and records the Admin and authorization timestamp in the same transaction as the approval action.

## 4. Relationships and cardinality

| Parent | Child | Cardinality / delete behavior |
|---|---|---|
| `competencies` | `discussions` | One competency has many Discussions; each Discussion belongs to one competency. Restrict deleting a competency with dependent content/history. |
| `discussions` | `topics` | One Discussion has many ordered topics; each topic belongs to one Discussion. Restrict deletion where progress/history exists. |
| `discussions` | `learning_stage_contents` | One Discussion has exactly seven stage content rows; each row belongs to one Discussion. |
| `discussions` | `assessments` | One Discussion has one each of Pre-test, Activity, and Discussion Post-test. |
| `competencies` | `assessments` | One competency has one Generalized Post-test. |
| `assessments` | `questions` | One assessment has many ordered questions. |
| `questions` | `answer_choices` | One question has zero or more ordered choices. |
| `admins` | `students.reviewed_by_admin_id` | One Admin may review many student registrations; each student has zero or one latest reviewer. |
| `students` | `assessment_attempts` | One student has many assessment attempts. |
| `assessments` | `assessment_attempts` | One assessment has many student attempts; Discussion assessments are limited to one attempt record per student. |
| `assessment_attempts` | `attempt_answers` | One attempt has one saved answer per question. |
| `questions` | `attempt_answers` | One question can appear in many student answer records. |
| `students` | `student_stage_progress` | One student has progress rows across their Discussions. |
| `discussions` | `student_stage_progress` | One Discussion has progress rows for each student and stage. |
| `students` | `student_competency_progress` | One student has one current row per competency. |
| `competencies` | `student_competency_progress` | One competency has progress rows for students. |
| `students`, `competencies`, `admins` | `retake_authorizations` | Each grant belongs to one student, one competency, and one authorizing Admin; each referenced record may have many grants. |

Use restrictive foreign keys for course/assessment content and records that have attempts or progress. Prefer deactivation to deletion so audit and historical records remain explainable. Student deactivation similarly preserves their history.

## 5. Constraints and indexes

### Unique constraints

- `admins.username` unique.
- `students.account_id` unique.
- `competencies.code` unique.
- `discussions(competency_id, display_order)` unique.
- `topics(discussion_id, display_order)` unique.
- `learning_stage_contents(discussion_id, stage_code)` and `(discussion_id, display_order)` unique.
- `assessments(discussion_id, assessment_type)` unique and `assessments(competency_id, assessment_type)` unique; null scope is permitted for the other assessment scope.
- `questions(assessment_id, display_order)` unique.
- `answer_choices(question_id, display_order)` unique.
- `assessment_attempts(student_id, assessment_id, attempt_number)` unique.
- Discussion assessment attempts: enforce one attempt per `(student_id, assessment_id)` with a Spring Boot transaction. Reuse the one in-progress attempt to resume; reject further starts or submissions after it has been submitted.
- `attempt_answers(attempt_id, question_id)` unique.
- `student_stage_progress(student_id, discussion_id, stage_code)` unique.
- `student_competency_progress(student_id, competency_id)` unique.

### Indexes

- Keep indexes needed for foreign keys and the unique constraints above.
- Keep the non-unique `students(full_name, program, status)` index for possible-duplicate review and Admin student search. Do not make Full Name + Program unique.
- Add `assessment_attempts(student_id, assessment_id, status)` to find resumable attempts and count submissions.
- Add `retake_authorizations(student_id, competency_id)` to calculate additional allowance and review grants.

Use primary keys, foreign keys, and the listed unique constraints for core relational integrity. Enforce assessment scope/type consistency, stage order, correct competency scope, attempt limits, and one-time Discussion submission in Spring Boot transactions. This avoids specialized database constraints for business rules.

## 6. Assessment handling

### Discussion assessments

Each Discussion has separate assessment rows for Pre-test, Activity, and Discussion Post-test. Each student gets one attempt record per assessment. That row can remain `IN_PROGRESS`; `attempt_answers` are saved as responses are made, allowing the student to resume. Submission evaluates the stored responses on the backend, fills score/percentage and submitted time, updates answer evaluation fields, marks the corresponding stage complete, and prevents any further attempt for that assessment. Passing is not required for progression.

### Generalized Post-tests

Each competency has its own `GENERALIZED_POST_TEST` assessment and independent attempt sequence. Start with three available submissions. Every retake authorization row contributes a further block of three. Only `SUBMITTED` attempts consume the allowance. The backend validates allowance transactionally and records every submission; previous attempts remain unchanged. A score of at least 70% marks the related competency passed. Passing remains permanent and COC 1's passed state unlocks COC 2.

## 7. Progress and progression

- Create one `student_stage_progress` row per student, Discussion, and one of seven fixed stage codes. Track `LOCKED`, `AVAILABLE`, and `COMPLETED` state with completion timestamps.
- Enforce sequence in backend service logic. The initial stage of the first Discussion is available only if its competency is available. The required assessment submission completes its assessment stage; completion of the remaining content stages is recorded when the student completes them.
- A Discussion is complete only after all seven rows are complete. The next ordered Discussion becomes available then. Revisiting does not reset rows or attempts.
- Generalized Post-test availability is derived from completion of all Discussions in its competency and remaining attempt allowance.
- `student_competency_progress.status` records current competency state and authoritative pass state. COC 2 availability depends on COC 1 being `PASSED`.
- Overall and competency progress percentages are calculated from the progress rows, rather than stored redundantly.

## 8. Learning content and seven-stage consistency

The seven canonical stages have fixed codes and display order:

| Order | Stage code | Stage |
|---:|---|---|
| 1 | `PRE_TEST` | Pre-test |
| 2 | `DEFINITIONS` | Definition of Terms |
| 3 | `EXPLANATION` | Explanation of the Discussion |
| 4 | `EXAMPLES` | Examples |
| 5 | `VIDEO_LESSONS` | Video Lessons |
| 6 | `ACTIVITY` | Activity |
| 7 | `POST_TEST` | Discussion Post-test |

`learning_stage_contents` stores a single title/body/order per stage and Discussion. Assessment stages have their actual assessment questions in `assessments` and `questions`; the stage body can contain stage description/instructions. Text and media links are in the editable content body. Store external image/video URLs rather than binaries. A setup/seed operation must create exactly one content row for each of the seven stage codes for every Discussion; uniqueness prevents duplicates but completeness is validated during setup.

## 9. Account structure and privacy

- Admin credentials live only in `admins`; `password_hash` contains a strong one-way hash. No email is required.
- Students live only in `students`; their Account ID and password hash are distinct from Admin credentials. Account ID is unique; official Student ID and email are not required.
- Only `ACTIVE` student accounts may authenticate. Registration creates `PENDING` records; Admin approval/rejection and private notes use the student row and nullable reviewer FK.
- Full Name + Program remains non-unique. Possible duplicate registrations are surfaced for Admin review, never automatically rejected solely because the pair matches. Registration/login must have backend rate limiting and other safeguards; without an official identifier or email, exact person-level duplicate prevention cannot be guaranteed.
- Do not return correct-choice flags or answer keys in student-facing payloads before the configured feedback point.

### Student registration and approval flow

1. Accept required Full Name, Program, and Password over the backend API; validate them server-side.
2. Generate a unique `STU-` Account ID using a decimal number padded to at least six digits (e.g. `STU-000001`); allow additional digits when the sequence exceeds six. Save it with the BCrypt password hash and `PENDING` status in the existing `students` table. Use `created_at` as registration time. Admin-created accounts use the same generation and initial status rules.
3. Surface pending registrations with an exact Full Name + Program match after trimming and case normalization to Admin reviewers; Admins may search for near matches. This is a non-unique candidate check, not a rejection rule. Do not persist a duplicate flag because the candidate can change as accounts are added or edited.
4. On approval or rejection, update `status`, `reviewed_by_admin_id`, and `reviewed_at`. Approval sets status to `ACTIVE`; rejection sets it to `REJECTED`. Keep reviewer/time null until the first decision.
5. Store optional private Admin notes in `admin_notes`. Ensure student-facing API responses omit this column and `password_hash`.
6. Authenticate only an `ACTIVE` account using Account ID and password. Keep Student authentication separate from Admin authentication.

For migration, preserve every internal student primary key and dependent foreign key. Assign Account IDs from each existing internal ID; convert legacy `is_active` values to the corresponding `ACTIVE` or `DEACTIVATED` status. Since legacy records have no hashes, do not assign default passwords; an Admin must reset each password before the account can log in. Invalidate a student's active sessions when an Admin deactivates the account or resets its password. Use Spring Security's existing server-side session mechanism and a session registry; do not store session identifiers in the database or add authentication tables. The in-memory registry supports this single backend instance; if the application is later deployed across multiple instances, session invalidation must use a shared session store/registry.

The prepared manual SQL script is deliberately guarded to the migration test database and checks for the original 14-table shape, the four existing foreign keys to `students.id`, and an already-applied or partial migration before changing the table. It adds and populates replacement fields, validates generated IDs and statuses, and only then removes the old student columns. The script has not been run. Before it is run manually, verify the active database name and take a test-database backup. Do not run it against `css_ncii_java_db`.

Registration and approval do not need another table for the currently specified workflow. `students` retains current status and latest decision metadata; it does not provide a full decision audit log. Submitted assessment and progress foreign keys continue referencing the internal `students.id`, so changing the human-facing identifier does not alter those relationships.

## 10. Design review checklist

- **Relationships:** every course, assessment, progress, answer, and retake record has a foreign key to its owner; no polymorphic untyped owner references are used.
- **Discussion vs Generalized assessments:** separate assessment types plus mutually exclusive Discussion/competency FKs distinguish the scopes and allow one Generalized Post-test per competency.
- **Attempt rules:** one resumable row for each Discussion assessment per student; sequenced, unlimited-history rows for Generalized tests within allowance; score and answers are retained after submission.
- **Retake audit:** each additional three-attempt grant references the student, competency, granting Admin, and authorization timestamp; grants are append-only.
- **Progression:** stage rows represent all seven stages; Discussion ordering and competency pass state support sequential unlocks and COC 1 → COC 2 gating.
- **No unnecessary counters:** attempt allowance is the fixed initial three plus three for each authorization row, compared with submitted attempts; progress percentages are derived from completion rows.
- **History:** submitted answers and scores remain stored; assessment content with attempt history should be deactivated rather than deleted.
- **No conflicting constraints:** Full Name + Program is intentionally non-unique; only generated Account ID and the existing scoped course/assessment/progress identifiers are unique.

## 11. Feedback rules and remaining configuration

`SYSTEM_DESIGN.md` establishes these feedback requirements: the Pre-test displays a score and incorrectly answered questions, and may also show correct answers; the Activity and Discussion Post-test display the score, incorrectly answered questions, and correct answers. For Generalized Post-tests, the design requires recording attempts and results and showing the student results and attempts remaining, but does not specify whether correct answers or incorrectly answered questions are shown. The timing and duration of answer review (only immediately after submission or on later review) remain to be configured. Do not treat the established Discussion assessment feedback rules as undecided.

Other details left for later configuration are exact question counts/types, whether questions are randomized, and the final external media provider. The question type field, ordered choices, saved response data, and URL-capable content body accommodate those decisions without changing table relationships. Resumable Discussion assessments are represented by `IN_PROGRESS` attempts and saved answer rows.

Student-account implementation details still requiring care include a secure password-reset delivery procedure for students without email, identity checks for possible duplicate registrations, and cross-instance session invalidation if the backend is deployed on multiple instances. Reconsideration of rejected applications uses the existing current-status and latest-review fields; a complete decision history and an additional audit table are not required by the approved design.

