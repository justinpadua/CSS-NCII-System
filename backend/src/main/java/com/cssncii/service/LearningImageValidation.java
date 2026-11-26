package com.cssncii.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.web.multipart.MultipartFile;

final class LearningImageValidation {
    static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final String IMAGE_FILENAME = "^[a-f0-9]{32}\\.(?:jpg|png|webp)$";

    private LearningImageValidation() {}

    static PreparedImage prepare(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Choose an image file to upload.");
        if (file.getSize() > MAX_IMAGE_BYTES) throw new IllegalArgumentException("Image files must be 5 MB or smaller.");
        String extension = extensionOf(file.getOriginalFilename());
        String contentType = contentTypeForExtension(extension);
        try {
            byte[] bytes = file.getBytes();
            validateContent(bytes, contentType);
            String storedExtension = "image/jpeg".equals(contentType) ? "jpg" : extension;
            String filename = UUID.randomUUID().toString().replace("-", "") + "." + storedExtension;
            return new PreparedImage(filename, filename.substring(0, filename.lastIndexOf('.')),
                storedExtension, contentType, bytes);
        } catch (IOException exception) {
            throw new IllegalStateException("The image could not be read.");
        }
    }

    static boolean isValidFilename(String filename) {
        return filename != null && filename.matches(IMAGE_FILENAME);
    }

    static String contentTypeForFilename(String filename) {
        if (!isValidFilename(filename)) throw new IllegalArgumentException("Learning image not found.");
        return switch (filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT)) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new IllegalArgumentException("Learning image not found.");
        };
    }

    static void validateContent(byte[] bytes, String expectedType) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image files must be non-empty and 5 MB or smaller.");
        }
        String detectedType = detectImageType(bytes);
        if (!expectedType.equals(detectedType)) {
            throw new IllegalArgumentException("The selected file extension does not match its image content.");
        }
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) throw new IllegalArgumentException("The image file is corrupted or unsupported.");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IllegalArgumentException("The image file is corrupted or unsupported.");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions must be valid and no larger than 40 megapixels.");
                }
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new IllegalArgumentException("The image file is corrupted or unsupported.");
                decoded.flush();
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("The image file is corrupted or unsupported.");
        }
    }

    private static String extensionOf(String originalFilename) {
        if (originalFilename == null) throw new IllegalArgumentException("Image filename must end in JPG, JPEG, PNG, or WEBP.");
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new IllegalArgumentException("Image filename must end in JPG, JPEG, PNG, or WEBP.");
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("jpg") && !extension.equals("jpeg") && !extension.equals("png") && !extension.equals("webp")) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP images are supported.");
        }
        return extension;
    }

    private static String contentTypeForExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP images are supported.");
        };
    }

    private static String detectImageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length >= png.length && startsWith(bytes, png)) return "image/png";
        if (bytes.length >= 12 && ascii(bytes, 0, 4).equals("RIFF") && ascii(bytes, 8, 4).equals("WEBP")) return "image/webp";
        throw new IllegalArgumentException("The selected file is not a supported image.");
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int index = 0; index < prefix.length; index++) if (bytes[index] != prefix[index]) return false;
        return true;
    }

    private static String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    record PreparedImage(String filename, String publicId, String format, String contentType, byte[] bytes) {}
}
