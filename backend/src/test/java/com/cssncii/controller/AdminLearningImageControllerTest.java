package com.cssncii.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cssncii.config.AdminSecurityConfiguration;
import com.cssncii.repository.AdminRepository;
import com.cssncii.security.AdminUserDetailsService;
import com.cssncii.service.LearningImageStore;
import com.cssncii.service.LearningImageStorageService;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminLearningImageController.class)
@Import({AdminSecurityConfiguration.class, AdminUserDetailsService.class, AdminLearningImageControllerTest.TestBeans.class})
class AdminLearningImageControllerTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private LearningImageStore imageStorage;

    @Test
    void uploadRequiresAdminAndCsrfAndReturnsStudentReference() throws Exception {
        MockMultipartFile file = pngFile();

        mockMvc.perform(multipart("/api/admin/learning-images").file(file).with(csrf()))
            .andExpect(status().isUnauthorized());
        mockMvc.perform(multipart("/api/admin/learning-images").file(file).with(csrf())
                .with(authentication(auth("ROLE_STUDENT"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/admin/learning-images").file(file)
                .with(authentication(auth("ROLE_ADMIN"))))
            .andExpect(status().isForbidden());
        mockMvc.perform(multipart("/api/admin/learning-images").file(file).with(csrf())
                .with(authentication(auth("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.matchesRegex(
                "/api/student/learning/images/[a-f0-9]{32}\\.png")))
            .andExpect(jsonPath("$.contentType").value("image/png"));
    }

    @Test
    void adminCanPreviewStoredImageWithSafeInlineHeaders() throws Exception {
        var uploaded = imageStorage.store(pngFile());

        mockMvc.perform(get("/api/admin/learning-images/" + uploaded.filename())
                .with(authentication(auth("ROLE_ADMIN"))))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.IMAGE_PNG))
            .andExpect(header().string("X-Content-Type-Options", "nosniff"))
            .andExpect(header().string("Content-Disposition", "inline; filename=\"" + uploaded.filename() + "\""));
    }

    private MockMultipartFile pngFile() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return new MockMultipartFile("file", "board.png", "image/png", output.toByteArray());
        } finally {
            image.flush();
        }
    }

    private UsernamePasswordAuthenticationToken auth(String role) {
        return new UsernamePasswordAuthenticationToken("test", "", List.of(new SimpleGrantedAuthority(role)));
    }

    @TestConfiguration
    static class TestBeans {
        private static final Path STORAGE = Path.of(System.getProperty("java.io.tmpdir"),
            "css-ncii-admin-image-tests-" + java.util.UUID.randomUUID());

        @Bean AdminRepository adminRepository() {
            return proxy(AdminRepository.class, (method, args) -> {
                if (method.equals("findByUsername")) return java.util.Optional.empty();
                throw new UnsupportedOperationException(method);
            });
        }

        @Bean LearningImageStore learningImageStorageService() {
            return new LearningImageStorageService(STORAGE.toString());
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxy(Class<T> type, RepositoryCall call) {
            return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        case "toString" -> type.getSimpleName() + " test proxy";
                        default -> null;
                    };
                }
                return call.invoke(method.getName(), args == null ? new Object[0] : args);
            });
        }

        @FunctionalInterface
        private interface RepositoryCall { Object invoke(String method, Object[] args); }
    }
}
