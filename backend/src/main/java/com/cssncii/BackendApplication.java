package com.cssncii;

import com.cssncii.config.ApprovedAssessmentBulkImportRunner;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BackendApplication {
    public static void main(String[] args) {
        if (assessmentImportProfileRequested(args)) {
            if (!ApprovedAssessmentBulkImportRunner.isExplicitRehearsalUrl(System.getenv("SPRING_DATASOURCE_URL"))) {
                throw new IllegalStateException("Assessment import startup refused before Spring initialization: "
                    + "SPRING_DATASOURCE_URL must explicitly target the local rehearsal database.");
            }
            boolean provisioningEnv = "true".equalsIgnoreCase(System.getenv("APP_ADMIN_PROVISIONING_ENABLED"));
            boolean provisioningArg = Arrays.stream(args)
                .anyMatch(arg -> arg.equalsIgnoreCase("--app.admin.provisioning.enabled=true"));
            if (provisioningEnv || provisioningArg) {
                throw new IllegalStateException("Assessment import startup refused while initial Admin provisioning is enabled.");
            }
        }
        SpringApplication.run(BackendApplication.class, args);
    }

    private static boolean assessmentImportProfileRequested(String[] args) {
        String environmentProfiles = System.getenv("SPRING_PROFILES_ACTIVE");
        if (environmentProfiles != null && Arrays.stream(environmentProfiles.split(","))
            .anyMatch(profile -> profile.trim().equals("assessment-import"))) return true;
        return Arrays.stream(args).anyMatch(arg -> arg.startsWith("--spring.profiles.active=")
            && Arrays.stream(arg.substring(arg.indexOf('=') + 1).split(","))
                .anyMatch(profile -> profile.trim().equals("assessment-import")));
    }
}
