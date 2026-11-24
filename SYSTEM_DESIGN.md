# CSS NC II COC 1 and COC 2 Management System

## Complete System Design

**Document status:** Revised system design\
**Project type:** Academic / educational management system\
**Deployment target:** Online deployment using Vercel and Railway

------------------------------------------------------------------------

## 1. Project Overview

The CSS NC II COC 1 and COC 2 Management System is a web-based learning
and assessment platform. It provides students with structured learning
materials, assessments, activities, progress tracking, and
competency-level evaluations.

The system has two competencies:

-   **COC 1**
-   **COC 2**

Each competency is organized into Discussions. Each Discussion contains
its corresponding topics or subtopics and follows the same seven-stage
learning flow.

The Admin manages student records, learning content, assessments, and
retake permissions. Students access the system through a website and
complete the required learning stages and assessments.

The actual definitions, explanations, examples, videos, activities, and
assessment questions will be added later by the Admin. The initial
development should establish the system structure and content-management
features so that the materials can be populated afterward.

## 2. Project Goals

The system aims to:

1.  Organize COC 1 and COC 2 learning materials into Discussions and
    topics.
2.  Provide a consistent seven-stage learning flow for every Discussion.
3.  Allow students to complete assessments and activities and review
    their results.
4.  Track student progress for each Discussion and competency.
5.  Provide a Generalized Post-test for each competency.
6.  Enforce the 70% passing grade and three-attempt limit for each
    Generalized Post-test.
7.  Allow the Admin to authorize additional Generalized Post-test
    retakes.
8.  Allow the Admin to manage students, course content, assessments, and
    student progress.
9.  Make the system accessible online through a deployed website.

## 3. Technology Stack and Deployment

### 3.1 Application Stack

  Component          Technology
  ------------------ ---------------------------------------------------
  Frontend           React + Vite
  Styling            Tailwind CSS 4
  Backend            Java + Spring Boot
  Database           MySQL
  Authentication     Spring Security with session-based authentication
  Frontend hosting   Vercel
  Backend hosting    Railway
  Database hosting   MySQL service on Railway
  Code repository    GitHub

The existing React, Java + Spring Boot, MySQL, and Tailwind CSS stack will
be retained.

### 3.2 Deployment Architecture

The application will use separate frontend and backend hosting:

1.  **Vercel** hosts the React + Vite frontend.
2.  **Railway** hosts the Spring Boot backend.
3.  **Railway MySQL** stores student records, learning content,
    assessments, results, and progress.
4.  Learning images and videos must use online-accessible resources or
    an appropriate online file/object storage service.

The React frontend communicates with Spring Boot through API requests over
HTTPS.

### 3.3 Development and Deployment Workflow

Development and testing will first be performed locally. After the
application is stable:

1.  Push the project code to GitHub.
2.  Deploy the React frontend to Vercel.
3.  Deploy the Spring Boot backend to Railway.
4.  Configure the Railway MySQL database.
5.  Configure production environment variables, API URLs, CORS, session
    cookies, Spring Security, and HTTPS.
6.  Configure online storage for uploaded learning resources, if needed.
7.  Run production migrations and verify the deployed application.

Hosting plans, usage limits, and costs must be checked before
deployment. Do not assume that all required hosting services will remain
free.

## 4. User Roles and Authentication

The system has two user roles: Admin and Student.

### 4.1 Admin

The Admin is responsible for managing the system.

Admin login requirements:

-   Username
-   Password

The Admin account is created and managed through the backend or an
authorized administrative process. Students cannot register themselves
as Admins.

### 4.2 Student

Students can create their own accounts. Registration requires:

-   Full Name
-   Program
-   Password

An official Student ID and email address are not required. The system
generates a unique Account ID for each registration using `STU-` followed
by a number padded to a minimum of six digits (for example,
`STU-000001`). Numbers above six digits continue without truncation.
Students use that Account ID and their password to log in. The internal
`students.id` remains the database primary key and reference used by
progress and assessment records.

Each new account is saved with `PENDING` status. An Admin reviews the
registration and approves it (`ACTIVE`) or rejects it (`REJECTED`). Admin
deactivation changes an approved account to `DEACTIVATED`; reactivation
returns it to `ACTIVE`. Only `ACTIVE` accounts can authenticate. Pending,
rejected, and deactivated accounts cannot log in.

Passwords must be at least eight characters and securely hashed with
BCrypt. No default password is assigned. An Admin can reset a student's
password; the new password is hashed and all existing sessions for that
student are invalidated.

Registration workflow:

1. The student submits Full Name, Program, and Password.
2. The backend validates the fields, including the eight-character
   minimum, and hashes the password with BCrypt.
3. The system generates a unique Account ID.
4. The account is saved with `PENDING` status and its registration time.
5. The Admin reviews the registration and any possible-duplicate warning.
6. The Admin approves it as `ACTIVE` or rejects it as `REJECTED`; the
   decision time and reviewing Admin are recorded.
7. Only approved `ACTIVE` accounts can log in with Account ID and
   Password.

The system checks Full Name and Program for possible duplicate
registrations. Normalize Unicode, trim leading/trailing whitespace,
collapse repeated whitespace, and compare case-insensitively while
preserving accents. This pair is not unique: two different people can
have the same name and program, so an exact normalized match raises an
Admin review warning and must not automatically reject the registration.
Admins can search for near matches as part of review. Since no official
ID or email is collected, software cannot reliably prove whether two
registrations represent the same person. Admins should review matches
and, when needed, verify identity through an appropriate offline
process before approval. Rate-limit registration and login to reduce
automated abuse. Do not claim the duplicate check can guarantee one
account per person.

An Admin may also create a student account using Full Name, Program, and
an initial password of at least eight characters. The system generates
the same Account ID, hashes the password with BCrypt, and sets status to
`PENDING`. Admin-created accounts follow the same approval flow and
cannot log in until approved as `ACTIVE`. The initial password must be
communicated to the student through a trusted channel outside the
application; it must never be returned in an API response or logged.

Existing student records have no password hashes. During migration,
`is_active = true` records become `ACTIVE` and `is_active = false`
records become `DEACTIVATED`. No default passwords are assigned. A
legacy student cannot log in until an Admin resets their password.
Every migrated account receives an Account ID derived from its preserved
internal `students.id` using the same `STU-` plus minimum-six-digit
format.
Student passwords must be securely hashed using BCrypt and never stored
or compared as plain text. Admin and Student authentication remain
separate.

### 4.3 Authentication and Authorization

-   Admin and Student access must be separated.
-   Admin-only API routes must reject Student sessions.
-   Student-only API routes must reject Admin sessions unless a route
    explicitly permits both roles.
-   Protected backend operations must be validated by Spring Boot, not only
    by frontend route guards.
-   Session cookies, CORS, Spring Security, and HTTPS must be configured for
    the actual deployed frontend and backend domains.
-   Do not expose database credentials or secret keys in the frontend.

## 5. Course Structure

The system hierarchy is:

**Competency → Discussion → Topics / Subtopics → Discussion Learning
Flow**

A Discussion is the main learning unit. Its topics or subtopics are the
contents covered within that Discussion.

The seven-stage learning flow applies once to the entire Discussion. It
does not repeat separately for every topic or subtopic.

### 5.1 COC 1

COC 1 contains six Discussions and eighteen listed topics/subtopics.

#### Discussion 1: Discuss fundamental concepts of computer systems servicing

-   1.1 Fundamentals concepts of computers (software, hardware PC)
-   1.2 Occupational Health Standards (OHS)
-   1.3 Introduce the needed tools and equipment
-   1.4 Business Costings (Repair, Installing, Reformat, etc.)
-   1.5 Creating a small computer shop (needs/structure to have a
    standard computer shop by size)
-   1.6 Costing computation, tools and equipment checklist, safety
    compliance plan, and a short career pathway reflection

#### Discussion 2: Discuss elements of computer systems

-   2.1 Computer parts, software, peopleware, and dataware
-   2.2 Identify and differentiate system and application software

#### Discussion 3: Perform disassembly and assembly of computer hardware

-   3.1 Internal parts of system unit
-   3.2 How to disassemble system unit PC (step-by-step procedure)
-   3.3 How to assemble system unit PC (step-by-step procedure)
-   3.4 How to troubleshoot a malfunctioning computer system

#### Discussion 4: Create portable bootable device

-   4.1 How to create a functional ISO image and bootable device
-   4.2 How to create ISO image using third-party application or DOS
    commands

#### Discussion 5: Perform installation of system software and application software

-   5.1 Apply BIOS/UEFI configuration, disk management, and driver
    installation to set up a fully functional system
-   5.2 Configure BIOS/UEFI settings, install an operating system,
    manage disk partitions, and install all necessary system and device
    drivers

#### Discussion 6: Perform computer testing and documentation

-   6.1 Discuss how to apply stress testing techniques to evaluate
    system performance and stability
-   6.2 Perform a stress or burning test on a computer system, record
    the results, and analyze any issues observed during testing

### 5.2 COC 2

COC 2 contains three Discussions and six listed topics/subtopics.

#### Discussion 1: Discuss the concepts of computer network

-   1.1 Different types of networking, topologies, architectural design
-   1.2 Assigning IP Addresses (IPv4 or IPv6)
-   1.3 Difference between peer-to-peer computer and client-server
    computer

#### Discussion 2: Perform setting up of computer network

-   2.1 Different types of LAN cable (Straight through-A, Straight
    through-B, Cross-over cable) and how to create them
-   2.2 How to apply network planning and implementation skills using
    proper tools, devices, and cable management techniques

#### Discussion 3: Perform network testing and documentation

-   3.1 How to apply connectivity test using PING

## 6. Seven-Stage Learning Flow

Every Discussion in both COC 1 and COC 2 follows the same seven stages:

1.  Pre-test
2.  Definition of Terms
3.  Explanation of the Discussion
4.  Examples
5.  Video Lessons
6.  Activity
7.  Post-test

### 6.1 Pre-test

-   The Pre-test assesses the student's initial knowledge of the
    Discussion.
-   The student must submit the Pre-test to proceed to the next stage.
-   A passing score is not required.
-   After submission, the system displays the student's score and
    identifies incorrectly answered questions.
-   The system may also display the correct answers as feedback.

### 6.2 Definition of Terms

-   Presents important terms and their definitions for the Discussion.
-   The Admin supplies and manages the terms and definitions.
-   The student must complete the stage before proceeding.

### 6.3 Explanation of the Discussion

-   Presents the main learning explanation for the Discussion.
-   The explanation must cover the topics or subtopics assigned to that
    Discussion.
-   The Admin supplies and manages the learning content.
-   The student must complete the stage before proceeding.

### 6.4 Examples

-   Presents examples that help explain the Discussion.
-   Examples may include text, images, or other supported learning
    resources.
-   The Admin supplies and manages the examples.
-   The student must complete the stage before proceeding.

### 6.5 Video Lessons

-   Provides video lessons through inserted video links or supported
    embedded video resources.
-   The Admin supplies and manages the video links.
-   Videos must be accessible online after deployment.
-   The student must complete the stage before proceeding.

### 6.6 Activity

-   Provides an activity related to the Discussion.
-   The student must submit the activity to proceed.
-   A passing score is not required.
-   The student can submit regardless of whether the answers are
    correct.
-   After submission, the system displays the student's score and
    identifies incorrectly answered questions.
-   The system displays the correct answers as activity feedback.

### 6.7 Discussion Post-test

-   Evaluates the student's understanding of the Discussion.
-   The student must submit the Discussion Post-test to complete the
    Discussion and proceed to the next Discussion.
-   A passing score is not required.
-   After submission, the system displays the student's score and
    identifies incorrectly answered questions.
-   The system displays the correct answers as feedback.
-   The student can revisit a completed Discussion without losing its
    saved progress.

## 7. Assessment and Grading Rules

### 7.1 Discussion Assessments

The Pre-test, Activity, and Discussion Post-test have the following
rules:

  -------------------------------------------------------------------------
  Assessment       Submission required        Passing score Results shown
                           to proceed?            required? 
  --------------- -------------------- -------------------- ---------------
  Pre-test                         Yes                   No Score and
                                                            incorrectly
                                                            answered
                                                            questions

  Activity                         Yes                   No Score and
                                                            incorrectly
                                                            answered
                                                            questions

  Discussion                       Yes                   No Score and
  Post-test                                                 incorrectly
                                                            answered
                                                            questions
  -------------------------------------------------------------------------

The student may proceed after submitting the assessment, regardless of
the score.

The system must save submitted answers, scores, and assessment results
so that student progress and history can be tracked.

The exact number of questions, question types, and content for each
Discussion assessment will be configured or supplied later. The
previously discussed defaults of 5 Pre-test questions and 10 Activity
and Discussion Post-test items should only be used if retained in the
final assessment configuration.

### 7.2 Generalized Post-tests

In addition to the individual Discussion Post-tests, each competency has
one Generalized Post-test.

-   COC 1 has one COC 1 Generalized Post-test covering all six COC 1
    Discussions and their topics.
-   COC 2 has one COC 2 Generalized Post-test covering all three COC 2
    Discussions and their topics.

The Generalized Post-test is the competency-level assessment. It is
separate from the Post-test at the end of each Discussion.

### 7.3 Generalized Post-test Passing Grade

The passing grade for both Generalized Post-tests is **70%**.

A student passes when the final score is at least 70%.

Examples:

-   70% --- Passed
-   80% --- Passed
-   69% --- Failed

The system must calculate the score and determine the result on the
backend.

### 7.4 Generalized Post-test Attempts

Each Generalized Post-test has its own attempt allowance:

  Generalized Post-test           Initial attempts
  ----------------------------- ------------------
  COC 1 Generalized Post-test                    3
  COC 2 Generalized Post-test                    3

Rules:

1.  The student receives three attempts for the COC 1 Generalized
    Post-test.
2.  The student separately receives three attempts for the COC 2
    Generalized Post-test.
3.  Each submitted attempt must be recorded.
4.  If the student passes within the available attempts, the competency
    is marked as passed.
5.  If the student uses all three attempts without reaching 70%, the
    student is marked as failed and cannot start another attempt.
6.  The Admin must grant permission before the student can retake a
    failed Generalized Post-test.
7.  Admin approval grants another three attempts for that specific
    Generalized Post-test.
8.  Previous attempts and scores must remain in the assessment history
    after a retake is granted.
9.  The COC 1 and COC 2 attempt counters must be independent.
10. A student who has passed a Generalized Post-test should retain the
    passed status.

The system must prevent students from bypassing the attempt limit
through repeated requests, browser refreshes, or frontend changes.
Attempt counting and retake authorization must be enforced by Spring Boot.

The exact Admin interface and audit details for granting retakes will be
implemented in the Admin area.

## 8. Competency Unlocking and Student Progression

### 8.1 Discussion Unlocking

Discussions are completed in order.

-   The first Discussion of a competency is available when that
    competency is unlocked.
-   The student must complete all seven stages of the current
    Discussion.
-   Submission of the Pre-test, Activity, and Discussion Post-test is
    required.
-   Passing the individual Discussion assessments is not required.
-   Once a Discussion is completed, the next Discussion becomes
    available.
-   Previously completed Discussions can be revisited.
-   Revisiting a completed Discussion must not erase its saved progress
    or assessment history.

### 8.2 COC 1 to COC 2 Unlocking

COC 2 remains locked until the student passes the COC 1 Generalized
Post-test with at least 70%.

The required progression is:

1.  Complete all six COC 1 Discussions.
2.  Unlock and take the COC 1 Generalized Post-test.
3.  Pass the COC 1 Generalized Post-test with at least 70%.
4.  Unlock COC 2.
5.  Complete all three COC 2 Discussions.
6.  Unlock and take the COC 2 Generalized Post-test.
7.  Pass the COC 2 Generalized Post-test with at least 70%.

Failing the COC 1 Generalized Post-test prevents access to COC 2 until
the student passes it. Admin-authorized retakes follow the rules in
Section 7.4.

### 8.3 Progress Tracking

The system must track progress at these levels:

-   Student's overall progress.
-   COC 1 progress.
-   COC 2 progress.
-   Discussion completion status.
-   Completion status of each of the seven stages.
-   Assessment submission status.
-   Assessment scores and answer history.
-   Generalized Post-test attempts and results.
-   Generalized Post-test retake permissions.
-   Competency pass/fail status.

Progress must be stored in the database and retrieved from the backend.
It must not rely only on browser state or local storage.

## 9. Admin Features

The Admin dashboard must provide tools to manage the system.

### 9.1 Student Management

The Admin can:

-   View registered student records and search or organize them.
-   Review pending registrations and approve or reject them.
-   Reconsider a `REJECTED` registration and approve it without requiring
    a new registration.
-   Create student accounts that start as `PENDING` and follow the same
    approval and password rules as self-registrations.
-   Add private notes to student records.
-   Deactivate approved student accounts and reactivate deactivated
    accounts as `ACTIVE`.
-   Reset student passwords; resetting invalidates the student's
    existing authenticated sessions.
-   Grant Generalized Post-test retake permissions under Section 7.4.
-   View student progress and assessment results.

Student account records include:

-   Generated Account ID
-   Full Name
-   Program
-   Password hash (never displayed to Admins or students)
-   Account status (`PENDING`, `ACTIVE`, `REJECTED`, or `DEACTIVATED`)
-   Private Admin notes
-   Registration and latest approval/rejection reviewer and time, where applicable
-   Latest review decision, reviewer, and timestamp; reconsidering a
    rejected record updates these fields rather than creating history.
-   Created and updated timestamps

Permanent deletion should be avoided so assessment history and progress
remain available. Admin notes are private and must not be returned in
student-facing responses.

Full Name and Program are not unique. The system should surface possible
duplicate registrations for Admin review without automatically refusing
registrations solely because the pair matches.

### 9.2 Discussion and Topic Management

The Admin can:

-   View COC 1 and COC 2.
-   View the Discussions under each competency.
-   Manage Discussion titles and descriptions.
-   Manage the topics or subtopics assigned to each Discussion.
-   Set the display order of Discussions and topics.
-   Activate or deactivate content when appropriate.

The initial COC 1 and COC 2 Discussion and topic structures should be
seeded or entered based on Section 5.

### 9.3 Learning Content Management

The Admin can manage the content for each Discussion's learning stages:

-   Definition of Terms
-   Explanation
-   Examples
-   Video Lessons

The content-management structure should support adding and editing
learning materials later without requiring code changes for every new
paragraph, example, image, or video link.

### 9.4 Assessment Management

The Admin can manage:

-   Pre-test questions and answer choices.
-   Activity questions and answer choices.
-   Discussion Post-test questions and answer choices.
-   COC 1 Generalized Post-test questions and answer choices.
-   COC 2 Generalized Post-test questions and answer choices.
-   Correct answers and scoring configuration.

The system must associate each assessment with the correct Discussion or
competency.

### 9.5 Retake Management

The Admin can:

-   View students who have exhausted their Generalized Post-test
    attempts.
-   Grant another three attempts for the selected competency's
    Generalized Post-test.
-   View previous attempts and scores.
-   Preserve the student's previous assessment history.

A retake permission for COC 1 must not change the COC 2 attempt
allowance, and vice versa.

### 9.6 Student Progress and Results

The Admin can review:

-   Student Discussion completion.
-   Stage completion.
-   Pre-test, Activity, and Discussion Post-test scores.
-   Incorrectly answered questions.
-   Generalized Post-test attempts.
-   Generalized Post-test passing or failing status.
-   Competency completion status.

## 10. Student Features

The Student area must allow students to:

1.  Register using Full Name, Program, and Password without an official
    Student ID or email address.
2.  See the generated Account ID after registration so it can be used
    for login.
3.  Log in using Account ID and Password only after Admin approval and
    while the account is `ACTIVE`.
4.  View their available competencies.
5.  Access unlocked Discussions.
6.  View the topics or subtopics included in each Discussion.
7.  Complete the seven learning stages in order.
8.  Submit the Pre-test, Activity, and Discussion Post-test.
9.  View assessment scores and incorrectly answered questions after
    submission.
10. Revisit completed Discussions.
11. View their progress.
12. Take the Generalized Post-test after completing all Discussions in
    that competency.
13. View Generalized Post-test results and remaining attempts.
14. Access COC 2 only after passing the COC 1 Generalized Post-test.
15. Retake a failed Generalized Post-test only after the Admin grants
    permission.

## 11. Suggested Database Design

The database must support the course hierarchy, content management,
assessment history, student progress, and retake authorization.

The following is the logical data structure. Exact table names,
relationships, indexes, and migration details may be refined during
implementation while preserving the requirements.

### 11.1 Users

Stores Admin accounts.

Expected fields include:

-   id
-   username
-   email, if retained by the existing Admin users table
-   password
-   role
-   timestamps

Students must not be stored as Admin users.

### 11.2 Students

Stores student accounts, including self-registrations and their current
approval/activation state.

Expected fields:

-   id
-   account_id (unique, generated by the system)
-   Account ID format: `STU-` plus a number padded to a minimum of six
    digits; the digit count grows when required.
-   full_name
-   program
-   password_hash (BCrypt; minimum input password length is eight)
-   status (PENDING, ACTIVE, REJECTED, or DEACTIVATED)
-   admin_notes (private; optional)
-   reviewed_by_admin_id and reviewed_at (nullable until reviewed)
-   timestamps

Use a non-unique index for full_name, program, and status to help Admins
review possible duplicates. Do not make Full Name + Program unique.
Only ACTIVE accounts authenticate. Enforce Account ID uniqueness in the
database. Hash passwords with BCrypt before storing them.

`created_at` records registration time. `reviewed_by_admin_id` references
`admins.id` and `reviewed_at` records the latest approval or rejection
decision. A nullable foreign key is needed because new accounts start
PENDING. Re-reviewing a rejected registration replaces the latest
reviewer/time and status; no review-history table is required. `admin_notes`
stores private Admin context without a separate notes table. Admin-created
and self-registered accounts both start PENDING.

### 11.3 Competencies

Stores COC 1 and COC 2.

Expected fields:

-   id
-   name or code
-   title
-   description
-   display_order
-   is_active
-   timestamps

### 11.4 Discussions

Stores the Discussions under each competency.

Expected fields:

-   id
-   competency_id
-   title
-   description
-   display_order
-   is_active
-   timestamps

### 11.5 Topics

Stores topics or subtopics belonging to a Discussion.

Expected fields:

-   id
-   discussion_id
-   title
-   description, if needed
-   display_order
-   timestamps

### 11.6 Learning Content

Stores content for the seven stages of each Discussion.

The design must support:

-   Definition entries.
-   Explanation content.
-   Examples.
-   Video links.
-   Stage ordering and availability.

The Pre-test, Activity, and Discussion Post-test are assessment records
and should be associated with their corresponding Discussion.

### 11.7 Assessments and Questions

The database must distinguish:

-   Discussion Pre-tests.
-   Discussion Activities.
-   Discussion Post-tests.
-   COC 1 Generalized Post-test.
-   COC 2 Generalized Post-test.

Assessment records should identify their assessment type and their
associated Discussion or competency.

Question records should support question text, question type, choices,
correct answers, scoring value, and display order.

### 11.8 Student Assessment Attempts and Answers

Store each submitted assessment attempt, including:

-   Student.
-   Assessment.
-   Attempt number or sequence.
-   Submitted answers.
-   Score.
-   Total possible score.
-   Percentage.
-   Submission time.
-   Result or status.

Keep assessment history instead of overwriting previous submissions.

### 11.9 Student Discussion Progress

Track each student's progress through each Discussion and its seven
stages.

The data model must prevent a student from unlocking the next Discussion
before submitting the required stages and assessments.

### 11.10 Student Competency Progress and Retake Permissions

Track:

-   Student and competency.
-   Competency status.
-   Generalized Post-test attempt allowance and usage.
-   Generalized Post-test pass/fail status.
-   Retake permission granted by the Admin.
-   Retake authorization history.

The backend must enforce attempt limits and prevent duplicate or
unauthorized retake grants.

## 12. Security and Data Integrity

The system must:

-   Validate all inputs on the Spring Boot backend.
-   Use password hashing for Admin passwords.
-   Hash Student passwords before storing them; never store plain-text
    passwords.
-   Permit Student authentication only for ACTIVE accounts and use the
    generated Account ID with the password.
-   Keep pending, rejected, and deactivated Student accounts from
    authenticating.
-   Keep private Admin notes and password hashes out of student-facing
    responses.
-   Separate Admin and Student authorization.
-   Protect all Admin management routes.
-   Protect Student records and progress from unauthorized access.
-   Restrict authentication to ACTIVE student accounts; pending, rejected,
    and deactivated accounts must be refused.
-   Invalidate all existing Student sessions after account deactivation or
    password reset. Use the server-side session registry; do not persist
    session IDs in the student table.
-   Rate-limit Student registration and Account ID/password login, and
    protect registration from automated abuse.
-   Use HTTPS in production.
-   Configure CORS and Spring Security for the deployed domains.
-   Apply rate limiting to login and assessment submission endpoints.
-   Enforce Generalized Post-test attempt limits on the backend.
-   Prevent students from changing scores, correct answers, completion
    status, or retake permissions through frontend requests.
-   Keep database credentials and application secrets out of frontend
    code and public repositories.
-   Preserve submitted assessment history.
-   Use appropriate database constraints and transactions for important
    progress and attempt updates.

## 13. User Interface Requirements

The interface should be simple and understandable for students and the
Admin.

### 13.1 Admin Interface

The Admin dashboard should provide clear navigation for:

-   Dashboard
-   Student Management
-   Pending student registration review and approval/rejection
-   Competency Management
-   Discussion and Topic Management
-   Learning Content Management
-   Assessment Management
-   Student Progress and Results
-   Generalized Post-test Retake Management

### 13.2 Student Interface

The Student dashboard should clearly show:

-   Student registration using Full Name, Program, and Password.
-   The generated Account ID and pending-approval status after
    registration.
-   A clear indication when a pending or rejected account cannot log in.
-   Student information.
-   Available and locked competencies.
-   Discussion list and completion status.
-   Seven-stage learning navigation.
-   Assessment instructions and results.
-   Generalized Post-test availability.
-   Generalized Post-test score and attempts remaining.
-   Competency pass/fail status.

The interface must clearly distinguish completed, available, locked,
failed, and passed states.

## 14. Functional Requirements Summary

  -----------------------------------------------------------------------
  ID                                  Requirement
  ----------------------------------- -----------------------------------
  FR-01                               Admin can log in using username and
                                      password.

  FR-02                               Student can self-register using
                                      Full Name, Program, and Password;
                                      the system generates a unique
                                      Account ID and creates the account
                                      as PENDING.

  FR-03                               Admin can manage student records.

  FR-03A                              Admin can review pending student
                                      registrations, approve or reject
                                      them, add private notes, and
                                      activate or deactivate approved
                                      accounts.

  FR-03B                              Only ACTIVE students can log in
                                      using Account ID and Password.

  FR-03C                              Possible Full Name + Program
                                      duplicates are flagged for Admin
                                      review but are not automatically
                                      rejected.

  FR-04                               The system contains COC 1 and COC
                                      2.

  FR-05                               Each competency contains its
                                      assigned Discussions.

  FR-06                               Each Discussion contains its
                                      assigned topics or subtopics.

  FR-07                               Each Discussion follows the
                                      seven-stage learning flow.

  FR-08                               Student must submit the Pre-test to
                                      proceed.

  FR-09                               Student must complete the
                                      learning-content stages to proceed.

  FR-10                               Student must submit the Activity to
                                      proceed.

  FR-11                               Student must submit the Discussion
                                      Post-test to complete the
                                      Discussion.

  FR-12                               Passing is not required for
                                      Discussion assessments.

  FR-13                               Student can view scores and
                                      incorrectly answered questions
                                      after Discussion assessments.

  FR-14                               Student can revisit completed
                                      Discussions.

  FR-15                               Each competency has one Generalized
                                      Post-test.

  FR-16                               Generalized Post-tests cover all
                                      Discussions and topics in their
                                      competency.

  FR-17                               Generalized Post-tests require at
                                      least 70% to pass.

  FR-18                               Each Generalized Post-test
                                      initially allows three attempts.

  FR-19                               After three failed attempts, the
                                      Generalized Post-test is locked.

  FR-20                               Admin can grant another three
                                      attempts for a failed Generalized
                                      Post-test.

  FR-21                               Previous Generalized Post-test
                                      attempts remain recorded.

  FR-22                               COC 2 remains locked until the
                                      student passes the COC 1
                                      Generalized Post-test.

  FR-23                               Student progress is saved and
                                      tracked in the database.

  FR-24                               Admin can manage learning materials
                                      and assessment questions.

  FR-25                               Admin can view student progress and
                                      assessment results.
  -----------------------------------------------------------------------

## 15. Non-Functional Requirements

### Performance

-   Pages and learning content should load within a reasonable time
    under normal use.
-   Database queries should be structured efficiently.
-   Large images and videos should use appropriate online delivery
    methods.

### Reliability

-   Submitted assessment results must be saved reliably.
-   Progress must remain available after logout or when the student uses
    another device.
-   Attempt limits and competency unlocking must be enforced
    consistently.

### Usability

-   The interface should use clear labels and straightforward
    navigation.
-   Students should understand which stage or Discussion they need to
    complete next.
-   Admin content management should be practical for a non-technical
    user after setup.

### Maintainability

-   Keep frontend, backend, and database responsibilities organized.
-   Use Spring Boot-compatible database migration and seeding tools for database structure and
    initial course data.
-   Keep learning content editable through the Admin interface.
-   Keep validation and assessment rules in the backend.

### Availability

-   The deployed application requires internet access.
-   Availability depends on the selected hosting services and their
    plans.

## 16. Development Phases

A recommended implementation order is:

### Phase 1: Project Foundation

-   Confirm the existing React + Vite and Java + Spring Boot project structure.
-   Confirm MySQL connection.
-   Confirm Spring Security and session configuration.
-   Preserve and review the existing Admin and Student database
    foundations.

### Phase 2: Authentication

-   Complete Admin authentication.
-   Implement Student self-registration, Admin approval/rejection, and
    Account ID + password login for ACTIVE accounts.
-   Separate Admin and Student access.
-   Test protected backend routes.

### Phase 3: Course Structure

-   Create COC 1 and COC 2 records.
-   Create the six COC 1 Discussions and their eighteen
    topics/subtopics.
-   Create the three COC 2 Discussions and their six topics/subtopics.
-   Implement Discussion ordering and competency relationships.

### Phase 4: Learning Content Management

-   Implement the seven stages for each Discussion.
-   Implement Admin content management.
-   Support definitions, explanations, examples, and video links.
-   Use placeholders until the actual learning materials are provided.

### Phase 5: Discussion Assessments

-   Implement Pre-tests, Activities, and Discussion Post-tests.
-   Save submitted answers and scores.
-   Show incorrectly answered questions and correct-answer feedback.
-   Require submission, not a passing score, for progression.

### Phase 6: Student Progression

-   Implement stage completion.
-   Implement sequential Discussion unlocking.
-   Allow revisiting completed Discussions.
-   Save progress in MySQL.

### Phase 7: Generalized Post-tests

-   Implement the COC 1 Generalized Post-test.
-   Implement the COC 2 Generalized Post-test.
-   Implement 70% passing.
-   Implement three initial attempts for each assessment.
-   Implement Admin-authorized additional three-attempt retakes.
-   Preserve attempt history.
-   Unlock COC 2 only after passing the COC 1 Generalized Post-test.

### Phase 8: Testing

-   Test Admin and Student authentication.
-   Test Discussion progression.
-   Test assessment submission and feedback.
-   Test progress persistence.
-   Test Generalized Post-test scoring and attempt limits.
-   Test Admin retake authorization.
-   Test competency unlocking.
-   Test access control and invalid requests.

### Phase 9: Deployment

-   Push the project to GitHub.
-   Deploy React to Vercel.
-   Deploy Spring Boot and MySQL to Railway.
-   Configure production environment variables, HTTPS, CORS, Spring Security,
    and online learning-resource storage.
-   Run production migrations.
-   Test the complete system using the deployed URLs.

## 17. Items to Populate or Confirm Later

The system structure can be developed before the actual learning
materials are ready.

The following content will be supplied or finalized later:

-   Definitions of terms for each Discussion.
-   Discussion explanations.
-   Examples.
-   Video links.
-   Pre-test questions and correct answers.
-   Activity instructions, questions, and correct answers.
-   Discussion Post-test questions and correct answers.
-   COC 1 Generalized Post-test questions and correct answers.
-   COC 2 Generalized Post-test questions and correct answers.
-   Exact question counts and question types where not yet finalized.

The following implementation details should also be confirmed when their
related features are developed:

-   Whether assessment questions are randomized.
-   Whether answer review is available after every assessment or only
    immediately after submission.
-   The operational process for verifying a possible duplicate when
    identity cannot be determined from the collected fields alone.
-   The trusted out-of-application channel for giving Admin-created
    accounts their initial password and for student password resets.
-   Admin interface details for reviewing registrations and retakes.
-   Final production hosting plans and online storage provider.

These details must not be silently assumed during implementation.

## 18. Final System Workflow

### COC 1

1.  Student logs in.
2.  Student opens COC 1.
3.  Student completes Discussion 1 through its seven stages.
4.  Student proceeds through Discussions 2--6 in order.
5.  Student may revisit completed Discussions.
6.  After all six Discussions are complete, the COC 1 Generalized
    Post-test becomes available.
7.  Student takes the Generalized Post-test.
8.  If the student scores at least 70%, COC 1 is marked as passed and
    COC 2 becomes available.
9.  If the student fails three attempts, the assessment is locked until
    the Admin grants another three attempts.

### COC 2

1.  Student accesses COC 2 after passing the COC 1 Generalized
    Post-test.
2.  Student completes Discussion 1 through its seven stages.
3.  Student proceeds through Discussions 2--3 in order.
4.  Student may revisit completed Discussions.
5.  After all three Discussions are complete, the COC 2 Generalized
    Post-test becomes available.
6.  Student takes the Generalized Post-test.
7.  If the student scores at least 70%, COC 2 is marked as passed.
8.  If the student fails three attempts, the assessment is locked until
    the Admin grants another three attempts.

------------------------------------------------------------------------

**End of System Design**
