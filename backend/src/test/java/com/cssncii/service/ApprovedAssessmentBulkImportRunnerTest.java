package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.cssncii.config.ApprovedAssessmentBulkImportRunner;

class ApprovedAssessmentBulkImportRunnerTest {
    private static final String SAFE_URL =
        "jdbc:mysql://127.0.0.1:3306/css_ncii_java_db_repair_test_20261123";

    @Test
    void acceptsOnlyExplicitRehearsalUrlAndConfirmation() {
        assertTrue(ApprovedAssessmentBulkImportRunner.isExplicitRehearsalUrl(SAFE_URL));
        assertFalse(ApprovedAssessmentBulkImportRunner.isExplicitRehearsalUrl(
            "jdbc:mysql://127.0.0.1:3306/css_ncii_java_db"));
        assertFalse(ApprovedAssessmentBulkImportRunner.isExplicitRehearsalUrl(null));
        ApprovedAssessmentBulkImportRunner.validateTarget(SAFE_URL, SAFE_URL, false,
            "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123");
    }

    @Test
    void rejectsMissingOrMismatchedDatasourceAndUnsafeStartupOptions() {
        assertThrows(IllegalStateException.class, () -> ApprovedAssessmentBulkImportRunner.validateTarget(
            null, SAFE_URL, false, "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123"));
        assertThrows(IllegalStateException.class, () -> ApprovedAssessmentBulkImportRunner.validateTarget(
            "jdbc:mysql://127.0.0.1:3306/css_ncii_java_db", "jdbc:mysql://127.0.0.1:3306/css_ncii_java_db",
            false, "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123"));
        assertThrows(IllegalStateException.class, () -> ApprovedAssessmentBulkImportRunner.validateTarget(
            SAFE_URL, "jdbc:mysql://127.0.0.1:3306/css_ncii_java_db", false,
            "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123"));
        assertThrows(IllegalStateException.class, () -> ApprovedAssessmentBulkImportRunner.validateTarget(
            SAFE_URL, SAFE_URL, true, "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123"));
        assertThrows(IllegalStateException.class, () -> ApprovedAssessmentBulkImportRunner.validateTarget(
            SAFE_URL, SAFE_URL, false, ""));
    }
}
