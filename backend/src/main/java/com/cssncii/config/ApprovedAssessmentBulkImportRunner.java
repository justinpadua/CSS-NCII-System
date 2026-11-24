package com.cssncii.config;

import com.cssncii.service.ApprovedAssessmentBulkImportService;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** One-shot import entry point. Disabled unless the dedicated profile and explicit confirmation are supplied. */
@Component
@Profile("assessment-import")
@ConditionalOnProperty(prefix = "app.assessment-import", name = "enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApprovedAssessmentBulkImportRunner implements ApplicationRunner {
    public static final String TARGET_DATABASE = "css_ncii_java_db_repair_test_20261123";
    private static final Pattern MYSQL_URL = Pattern.compile(
        "^jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):3306/([^?]+)(?:\\?.*)?$");
    private static final String REQUIRED_CONFIRMATION = "IMPORT_TO_CSS_NCII_REPAIR_TEST_20261123";

    private final Environment environment;
    private final JdbcTemplate jdbcTemplate;
    private final ApprovedAssessmentBulkImportService importService;

    public ApprovedAssessmentBulkImportRunner(
        Environment environment, JdbcTemplate jdbcTemplate, ApprovedAssessmentBulkImportService importService
    ) {
        this.environment = environment;
        this.jdbcTemplate = jdbcTemplate;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) {
        String explicitUrl = System.getenv("SPRING_DATASOURCE_URL");
        String effectiveUrl = environment.getProperty("spring.datasource.url");
        validateTarget(explicitUrl, effectiveUrl,
            environment.getProperty("app.admin.provisioning.enabled", Boolean.class, false),
            environment.getProperty("app.assessment-import.confirm", ""));

        // This SELECT is the first database operation performed by the importer.
        String selectedDatabase = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (!TARGET_DATABASE.equals(selectedDatabase)) {
            throw new IllegalStateException("Import stopped: SELECT DATABASE() returned an unexpected database.");
        }
        Path root = Path.of(environment.getProperty("app.assessment-import.source-root", "../learning-content"))
            .toAbsolutePath().normalize();
        ApprovedAssessmentBulkImportService.ImportSummary summary = importService.importApproved(root);
        System.out.printf("Assessment import complete. db=%s, scopes=%d, assessmentsCreated=%d, questionsCreated=%d, "
                + "questionsPreserved=%d%n", selectedDatabase, summary.assessmentScopes(), summary.assessmentsCreated(),
            summary.questionsCreated(), summary.questionsPreserved());
        summary.scopeResults().forEach(System.out::println);
    }

    public static void validateTarget(String explicitUrl, String effectiveUrl, boolean adminProvisioningEnabled,
                                      String confirmation) {
        if (explicitUrl == null || explicitUrl.isBlank()) {
            throw new IllegalStateException("Set SPRING_DATASOURCE_URL explicitly to the rehearsal database URL.");
        }
        if (!isExplicitRehearsalUrl(explicitUrl)) {
            throw new IllegalStateException("SPRING_DATASOURCE_URL must target only " + TARGET_DATABASE + " on local MySQL.");
        }
        if (!explicitUrl.equals(effectiveUrl)) {
            throw new IllegalStateException("Spring's effective datasource URL differs from the explicit rehearsal URL.");
        }
        if (adminProvisioningEnabled) {
            throw new IllegalStateException("Disable initial Admin provisioning before running the assessment importer.");
        }
        if (!REQUIRED_CONFIRMATION.equals(confirmation)) {
            throw new IllegalStateException("Set the explicit assessment-import confirmation property to proceed.");
        }
    }

    public static boolean isExplicitRehearsalUrl(String url) {
        if (url == null) return false;
        Matcher matcher = MYSQL_URL.matcher(url);
        return matcher.matches() && TARGET_DATABASE.equals(matcher.group(1));
    }
}
