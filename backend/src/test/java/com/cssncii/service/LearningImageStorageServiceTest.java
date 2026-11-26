package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LearningImageStorageServiceTest {
    @TempDir Path uploadDirectory;
    private LearningImageStorageService storage;

    @BeforeEach
    void setUp() {
        storage = new LearningImageStorageService(uploadDirectory.toString());
    }

    @Test
    void acceptsValidPngAndUsesSafeUniqueNames() throws Exception {
        byte[] png = imageBytes("png");
        var first = storage.store(file("../../unsafe name.png", "image/png", png));
        var second = storage.store(file("../../unsafe name.png", "image/png", png));

        assertTrue(first.filename().matches("[a-f0-9]{32}\\.png"));
        assertNotEquals(first.filename(), second.filename());
        assertTrue(Files.isRegularFile(uploadDirectory.resolve(first.filename())));
        assertEquals("image/png", storage.get(first.filename()).contentType());
    }

    @Test
    void acceptsJpegAndCanReadItAfterStorageServiceReinitializes() throws Exception {
        var uploaded = storage.store(file("photo.jpeg", "image/jpeg", imageBytes("jpg")));
        var restartedStorage = new LearningImageStorageService(uploadDirectory.toString());

        assertTrue(Files.isRegularFile(uploadDirectory.resolve(restartedStorage.get(uploaded.filename()).filename())));
        assertEquals("image/jpeg", uploaded.contentType());
    }

    @Test
    void acceptsWebpOnlyWhenTheImageCanActuallyBeDecoded() {
        byte[] onePixelWebp = java.util.Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEAAUAmJaQAA3AA/v89WAAAAA==");

        var uploaded = storage.store(file("pixel.webp", "application/octet-stream", onePixelWebp));

        assertEquals("image/webp", uploaded.contentType());
        assertTrue(Files.isRegularFile(uploadDirectory.resolve(uploaded.filename())));
    }

    @Test
    void rejectsUnsupportedCorruptedAndMismatchedFiles() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> storage.store(file("photo.gif", "image/gif", new byte[] {1, 2, 3})));
        assertThrows(IllegalArgumentException.class, () -> storage.store(file("broken.png", "image/png", new byte[] {(byte) 0x89, 'P', 'N', 'G'})));
        assertThrows(IllegalArgumentException.class, () -> storage.store(file("photo.jpg", "image/jpeg", imageBytes("png"))));
    }

    @Test
    void rejectsFilesOverFiveMegabytes() {
        byte[] tooLarge = new byte[(int) LearningImageStorageService.MAX_IMAGE_BYTES + 1];
        assertThrows(IllegalArgumentException.class,
            () -> storage.store(file("large.png", "image/png", tooLarge)));
    }

    private MockMultipartFile file(String name, String type, byte[] content) {
        return new MockMultipartFile("file", name, type, content);
    }

    private byte[] imageBytes(String format) throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertTrue(ImageIO.write(image, format, output));
            return output.toByteArray();
        } finally {
            image.flush();
        }
    }
}
