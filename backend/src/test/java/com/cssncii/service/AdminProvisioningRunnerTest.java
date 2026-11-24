package com.cssncii.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cssncii.config.InitialAdminProvisioningRunner;
import com.cssncii.entity.Admin;
import com.cssncii.repository.AdminRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminProvisioningRunnerTest {
    private static final String INITIAL_PASSWORD = "a-long-test-password";

    private final AdminRepository adminRepository = mock(AdminRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void createsInitialAdminWithBcryptHash() {
        when(adminRepository.count()).thenReturn(0L);
        when(adminRepository.save(any(Admin.class))).thenAnswer(invocation -> invocation.getArgument(0));
        InitialAdminProvisioningRunner runner = runner("first-admin", INITIAL_PASSWORD);

        runner.run(emptyArguments());

        org.mockito.ArgumentCaptor<Admin> savedAdmin = org.mockito.ArgumentCaptor.forClass(Admin.class);
        verify(adminRepository).save(savedAdmin.capture());
        Admin admin = savedAdmin.getValue();
        assertEquals("first-admin", admin.getUsername());
        assertNotEquals(INITIAL_PASSWORD, admin.getPasswordHash());
        assertTrue(admin.getPasswordHash().matches("^\\$2[aby]\\$\\d{2}\\$.*"));
        assertTrue(passwordEncoder.matches(INITIAL_PASSWORD, admin.getPasswordHash()));
        assertNotNull(admin.getCreatedAt());
        assertNotNull(admin.getUpdatedAt());
    }

    @Test
    void refusesProvisioningWhenAnAdminAlreadyExistsWithoutChangingIt() {
        Admin existing = new Admin();
        existing.setUsername("existing-admin");
        existing.setPasswordHash("existing-bcrypt-hash");
        when(adminRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> runner("new-admin", INITIAL_PASSWORD).run(emptyArguments()));

        verify(adminRepository, never()).save(any(Admin.class));
        assertEquals("existing-admin", existing.getUsername());
        assertEquals("existing-bcrypt-hash", existing.getPasswordHash());
    }

    @Test
    void rejectsMissingUsername() {
        when(adminRepository.count()).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> runner("  ", INITIAL_PASSWORD).run(emptyArguments()));

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void rejectsMissingPassword() {
        when(adminRepository.count()).thenReturn(0L);

        assertThrows(IllegalArgumentException.class, () -> runner("first-admin", "").run(emptyArguments()));

        verify(adminRepository, never()).save(any(Admin.class));
    }

    @Test
    void provisioningRunnerIsDisabledByDefault() {
        contextRunner().run(context -> assertThat(context).doesNotHaveBean(InitialAdminProvisioningRunner.class));
    }

    @Test
    void provisioningRunnerLoadsOnlyWhenExplicitlyEnabled() {
        contextRunner()
            .withPropertyValues("app.admin.provisioning.enabled=true")
            .run(context -> assertThat(context).hasSingleBean(InitialAdminProvisioningRunner.class));
    }

    private InitialAdminProvisioningRunner runner(String username, String password) {
        return new InitialAdminProvisioningRunner(adminRepository, passwordEncoder, username, password);
    }

    private ApplicationArguments emptyArguments() {
        return new DefaultApplicationArguments(new String[0]);
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner().withUserConfiguration(ProvisioningTestConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(InitialAdminProvisioningRunner.class)
    static class ProvisioningTestConfiguration {
        @Bean
        AdminRepository adminRepository() {
            return mock(AdminRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
