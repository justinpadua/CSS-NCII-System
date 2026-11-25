package com.cssncii.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LearningImageStorageService {
    public static final long MAX_IMAGE_BYTES = 5L * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 40_000_000L;
    private static final String IMAGE_FILENAME = "^[a-f0-9]{32}\\.(?:jpg|png|webp)$";

    private final Path storageDirectory;

    public LearningImageStorageService(@Value("${app.upload-dir:./uploads/learning-images}") String uploadDirectory) {
        this.storageDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepareStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Learning image storage is unavailable.", exception);
        }
    }

    public UploadedImage store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose an image file to upload.");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("Image files must be 5 MB or smaller.");
        }

        String extension = extensionOf(file.getOriginalFilename());
        String contentType = contentTypeForExtension(extension);
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) {
                throw new IllegalArgumentException("Image files must be non-empty and 5 MB or smaller.");
            }
            String detectedType = detectImageType(bytes);
            if (!contentType.equals(detectedType)) {
                throw new IllegalArgumentException("The selected file extension does not match its image content.");
            }
            validateDecodableImage(bytes);

            String storedExtension = "image/jpeg".equals(contentType) ? "jpg" : extension;
            String filename;
            Path destination;
            do {
                filename = UUID.randomUUID().toString().replace("-", "") + "." + storedExtension;
                destination = storageDirectory.resolve(filename).normalize();
            } while (Files.exists(destination, LinkOption.NOFOLLOW_LINKS));
            if (!destination.startsWith(storageDirectory)) {
                throw new IllegalStateException("Generated image destination is invalid.");
            }
            Files.write(destination, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new UploadedImage(filename, contentType);
        } catch (IOException exception) {
            throw new IllegalStateException("The image could not be stored.", exception);
        }
    }

    public StoredImage get(String filename) {
        if (filename == null || !filename.matches(IMAGE_FILENAME)) {
            throw new EntityNotFoundException("Learning image not found.");
        }
        Path image = storageDirectory.resolve(filename).normalize();
        if (!image.startsWith(storageDirectory)
            || !Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS)
            || Files.isSymbolicLink(image)) {
            throw new EntityNotFoundException("Learning image not found.");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        String contentType = switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new EntityNotFoundException("Learning image not found.");
        };
        return new StoredImage(filename, image, contentType);
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("Image filename must end in JPG, JPEG, PNG, or WEBP.");
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            throw new IllegalArgumentException("Image filename must end in JPG, JPEG, PNG, or WEBP.");
        }
        String extension = originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!extension.equals("jpg") && !extension.equals("jpeg")
            && !extension.equals("png") && !extension.equals("webp")) {
            throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP images are supported.");
        }
        return extension;
    }

    private String contentTypeForExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new IllegalArgumentException("Only JPG, JPEG, PNG, and WEBP images are supported.");
        };
    }

    private String detectImageType(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8
            && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length >= png.length && startsWith(bytes, png)) return "image/png";
        if (bytes.length >= 12 && ascii(bytes, 0, 4).equals("RIFF")
            && ascii(bytes, 8, 4).equals("WEBP")) return "image/webp";
        throw new IllegalArgumentException("The selected file is not a supported image.");
    }

    private void validateDecodableImage(byte[] bytes) throws IOException {
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
        }
    }

    private boolean startsWith(byte[] bytes, byte[] prefix) {
        for (int index = 0; index < prefix.length; index++) {
            if (bytes[index] != prefix[index]) return false;
        }
        return true;
    }

    private String ascii(byte[] bytes, int offset, int length) {
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    public record UploadedImage(String filename, String contentType) {}
    public record StoredImage(String filename, Path path, String contentType) {}
}
