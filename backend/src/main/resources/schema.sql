-- CSS NC II COC 1 and COC 2 Management System
-- Schema only. This script intentionally contains no seed data or DROP statements.

CREATE TABLE admins (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_admins_username (username)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE students (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    account_id VARCHAR(50) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    program VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NULL,
    status VARCHAR(20) NOT NULL,
    admin_notes TEXT NULL,
    reviewed_by_admin_id BIGINT UNSIGNED NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_students_account_id (account_id),
    KEY idx_students_login (full_name, program, status),
    CONSTRAINT chk_students_status CHECK (status IN ('PENDING', 'ACTIVE', 'REJECTED', 'DEACTIVATED')),
    CONSTRAINT fk_students_reviewed_by_admin FOREIGN KEY (reviewed_by_admin_id) REFERENCES admins (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE competencies (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    display_order INT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_competencies_code (code)
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE discussions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    competency_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT NULL,
    display_order INT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_discussions_competency_order (competency_id, display_order),
    CONSTRAINT fk_discussions_competencies
        FOREIGN KEY (competency_id) REFERENCES competencies (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE topics (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    discussion_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(250) NOT NULL,
    description TEXT NULL,
    display_order INT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_topics_discussion_order (discussion_id, display_order),
    CONSTRAINT fk_topics_discussions
        FOREIGN KEY (discussion_id) REFERENCES discussions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE learning_stage_contents (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    discussion_id BIGINT UNSIGNED NOT NULL,
    stage_code VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content_body LONGTEXT NOT NULL,
    youtube_video_urls LONGTEXT NULL,
    display_order TINYINT UNSIGNED NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stage_contents_discussion_stage (discussion_id, stage_code),
    UNIQUE KEY uq_stage_contents_discussion_order (discussion_id, display_order),
    CONSTRAINT fk_stage_contents_discussions
        FOREIGN KEY (discussion_id) REFERENCES discussions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessments (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    discussion_id BIGINT UNSIGNED NULL,
    competency_id BIGINT UNSIGNED NULL,
    assessment_type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NULL,
    pass_percentage DECIMAL(5,2) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_assessments_discussion_type (discussion_id, assessment_type),
    UNIQUE KEY uq_assessments_competency_type (competency_id, assessment_type),
    CONSTRAINT chk_assessments_scope CHECK (
        (discussion_id IS NOT NULL AND competency_id IS NULL
            AND assessment_type IN ('PRE_TEST', 'ACTIVITY', 'DISCUSSION_POST_TEST'))
        OR
        (discussion_id IS NULL AND competency_id IS NOT NULL
            AND assessment_type = 'GENERALIZED_POST_TEST')
    ),
    CONSTRAINT fk_assessments_discussions
        FOREIGN KEY (discussion_id) REFERENCES discussions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_assessments_competencies
        FOREIGN KEY (competency_id) REFERENCES competencies (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE questions (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assessment_id BIGINT UNSIGNED NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    question_text LONGTEXT NOT NULL,
    correct_answer_data JSON NOT NULL,
    points DECIMAL(8,2) NOT NULL,
    display_order INT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_questions_assessment_order (assessment_id, display_order),
    CONSTRAINT fk_questions_assessments
        FOREIGN KEY (assessment_id) REFERENCES assessments (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE answer_choices (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    choice_text TEXT NOT NULL,
    display_order SMALLINT UNSIGNED NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_answer_choices_question_order (question_id, display_order),
    CONSTRAINT fk_answer_choices_questions
        FOREIGN KEY (question_id) REFERENCES questions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE assessment_attempts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    assessment_id BIGINT UNSIGNED NOT NULL,
    attempt_number SMALLINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    score DECIMAL(9,2) NULL,
    total_possible_score DECIMAL(9,2) NULL,
    percentage DECIMAL(5,2) NULL,
    passed BOOLEAN NULL,
    started_at DATETIME(6) NOT NULL,
    submitted_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attempts_student_assessment_number (student_id, assessment_id, attempt_number),
    KEY idx_attempts_student_assessment_status (student_id, assessment_id, status),
    KEY idx_attempts_assessment (assessment_id),
    CONSTRAINT fk_attempts_students
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_attempts_assessments
        FOREIGN KEY (assessment_id) REFERENCES assessments (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE attempt_answers (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    response_data JSON NOT NULL,
    is_correct BOOLEAN NULL,
    awarded_points DECIMAL(8,2) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attempt_answers_attempt_question (attempt_id, question_id),
    KEY idx_attempt_answers_question (question_id),
    CONSTRAINT fk_attempt_answers_attempts
        FOREIGN KEY (attempt_id) REFERENCES assessment_attempts (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_attempt_answers_questions
        FOREIGN KEY (question_id) REFERENCES questions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_stage_progress (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    discussion_id BIGINT UNSIGNED NOT NULL,
    stage_code VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_stage_progress_student_discussion_stage (student_id, discussion_id, stage_code),
    KEY idx_stage_progress_discussion (discussion_id),
    CONSTRAINT fk_stage_progress_students
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_stage_progress_discussions
        FOREIGN KEY (discussion_id) REFERENCES discussions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_competency_progress (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    competency_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL,
    passed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_competency_progress_student_competency (student_id, competency_id),
    KEY idx_competency_progress_competency (competency_id),
    CONSTRAINT fk_competency_progress_students
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_competency_progress_competencies
        FOREIGN KEY (competency_id) REFERENCES competencies (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE retake_authorizations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_id BIGINT UNSIGNED NOT NULL,
    competency_id BIGINT UNSIGNED NOT NULL,
    admin_id BIGINT UNSIGNED NOT NULL,
    authorized_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_retake_authorizations_student_competency (student_id, competency_id),
    KEY idx_retake_authorizations_admin (admin_id),
    CONSTRAINT fk_retake_authorizations_students
        FOREIGN KEY (student_id) REFERENCES students (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_retake_authorizations_competencies
        FOREIGN KEY (competency_id) REFERENCES competencies (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_retake_authorizations_admins
        FOREIGN KEY (admin_id) REFERENCES admins (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARACTER SET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
