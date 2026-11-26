package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class CloudinaryImageStorageServiceTest {
    private byte[] png;
    private FakeCloudinaryClient client;
    private CloudinaryImageStorageService storage;

    @BeforeEach
    void setUp() throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            png = output.toByteArray();
        } finally {
            image.flush();
        }
        client = new FakeCloudinaryClient(png);
        storage = new CloudinaryImageStorageService(client);
    }

    @Test
    void uploadsWithAuthenticatedDeliveryAndPreservesTheInternalReferenceFormat() {
        var uploaded = storage.store(new MockMultipartFile("file", "diagram.png", "image/png", png));

        assertTrue(uploaded.filename().matches("[a-f0-9]{32}\\.png"));
        assertEquals("image/png", uploaded.contentType());
        assertEquals(uploaded.filename().substring(0, 32), client.publicId);
        assertEquals("png", client.format);
        assertEquals("authenticated", client.deliveryType);
    }

    @Test
    void retrievesAuthenticatedAssetAsBytesForTheExistingBackendEndpoint() throws Exception {
        var uploaded = storage.store(new MockMultipartFile("file", "diagram.png", "image/png", png));
        var retrieved = storage.get(uploaded.filename());

        assertEquals("authenticated", client.downloadDeliveryType);
        assertEquals(uploaded.filename(), retrieved.filename());
        assertEquals("image/png", retrieved.contentType());
        assertArrayEquals(png, retrieved.resource().getContentAsByteArray());
    }

    @Test
    void rejectsInvalidFilenameWithoutCallingCloudinary() {
        assertThrows(jakarta.persistence.EntityNotFoundException.class, () -> storage.get("../../private.png"));
        assertNull(client.downloadDeliveryType);
    }

    @Test
    void retainsExistingUploadValidationBeforeCloudinaryIsCalled() {
        assertThrows(IllegalArgumentException.class,
            () -> storage.store(new MockMultipartFile("file", "wrong.jpg", "image/jpeg", png)));
        assertNull(client.publicId);
    }

    private static final class FakeCloudinaryClient implements CloudinaryImageClient {
        private final byte[] content;
        private String publicId;
        private String format;
        private String deliveryType;
        private String downloadDeliveryType;

        private FakeCloudinaryClient(byte[] content) { this.content = content; }

        @Override
        public void upload(byte[] bytes, String publicId, String format, String deliveryType) {
            assertArrayEquals(content, bytes);
            this.publicId = publicId;
            this.format = format;
            this.deliveryType = deliveryType;
        }

        @Override
        public byte[] download(String publicId, String format, String deliveryType) {
            this.downloadDeliveryType = deliveryType;
            return content;
        }
    }
}
