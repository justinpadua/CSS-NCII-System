package com.cssncii.config;

import com.cssncii.entity.Admin;
import com.cssncii.repository.AdminRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Runs only when explicitly enabled for a one-time initial Admin setup. */
@Component
@ConditionalOnProperty(
    prefix = "app.admin.provisioning",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class InitialAdminProvisioningRunner implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(InitialAdminProvisioningRunner.class);
    private static final int MIN_PASSWORD_CHARACTERS = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;
    private static final int MAX_USERNAME_CHARACTERS = 100;

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String configuredUsername;
    private final String configuredPassword;

    public InitialAdminProvisioningRunner(
        AdminRepository adminRepository,
        PasswordEncoder passwordEncoder,
        @Value("${ADMIN_USERNAME:}") String configuredUsername,
        @Value("${ADMIN_PASSWORD:}") String configuredPassword
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.configuredUsername = configuredUsername;
        this.configuredPassword = configuredPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments arguments) {
        if (adminRepository.count() > 0) {
            throw new IllegalStateException(
                "Initial Admin provisioning refused because an Admin account already exists.");
        }

        String username = validateUsername(configuredUsername);
        String password = validatePassword(configuredPassword);
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        Admin admin = new Admin();
        admin.setUsername(username);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        adminRepository.save(admin);

        LOGGER.info("Initial Admin account provisioned successfully.");
    }

    private String validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("ADMIN_USERNAME is required for initial Admin provisioning.");
        }
        String normalized = username.trim();
        if (normalized.codePointCount(0, normalized.length()) > MAX_USERNAME_CHARACTERS) {
            throw new IllegalArgumentException("ADMIN_USERNAME must be at most 100 characters.");
        }
        return normalized;
    }

    private String validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("ADMIN_PASSWORD is required for initial Admin provisioning.");
        }
        int characterCount = password.codePointCount(0, password.length());
        if (characterCount < MIN_PASSWORD_CHARACTERS) {
            throw new IllegalArgumentException("ADMIN_PASSWORD must contain at least 12 characters.");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new IllegalArgumentException("ADMIN_PASSWORD must not exceed BCrypt's 72-byte input limit.");
        }
        return password;
    }
}
