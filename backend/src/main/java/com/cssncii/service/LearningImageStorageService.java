package com.cssncii.service;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Local filesystem provider, selected by default for development. */
@Service
@ConditionalOnProperty(name = "app.image-storage.provider", havingValue = "local", matchIfMissing = true)
public class LearningImageStorageService implements LearningImageStore {
    public static final long MAX_IMAGE_BYTES = LearningImageValidation.MAX_IMAGE_BYTES;
    private final Path storageDirectory;

    public LearningImageStorageService(@Value("${app.upload-dir:./uploads/learning-images}") String uploadDirectory) {
        this.storageDirectory = Paths.get(uploadDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepareStorageDirectory() {
        try {
            Files.createDirectories(storageDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Learning image storage is unavailable.");
        }
    }

    @Override
    public UploadedImage store(MultipartFile file) {
        LearningImageValidation.PreparedImage image = LearningImageValidation.prepare(file);
        Path destination = storageDirectory.resolve(image.filename()).normalize();
        if (!destination.startsWith(storageDirectory)) throw new IllegalStateException("Generated image destination is invalid.");
        try {
            Files.write(destination, image.bytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return new UploadedImage(image.filename(), image.contentType());
        } catch (IOException exception) {
            throw new IllegalStateException("The image could not be stored.");
        }
    }

    @Override
    public StoredImage get(String filename) {
        if (!LearningImageValidation.isValidFilename(filename)) throw new EntityNotFoundException("Learning image not found.");
        Path image = storageDirectory.resolve(filename).normalize();
        if (!image.startsWith(storageDirectory) || !Files.isRegularFile(image, LinkOption.NOFOLLOW_LINKS)
            || Files.isSymbolicLink(image)) {
            throw new EntityNotFoundException("Learning image not found.");
        }
        return new StoredImage(filename, LearningImageValidation.contentTypeForFilename(filename), new FileSystemResource(image));
    }
}
