package com.cssncii.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

/** Cloudinary provider; assets are authenticated and only retrieved after application authorization. */
public class CloudinaryImageStorageService implements LearningImageStore {
    private static final String AUTHENTICATED = "authenticated";
    private final CloudinaryImageClient client;

    public CloudinaryImageStorageService(CloudinaryImageClient client) {
        this.client = client;
    }

    @Override
    public UploadedImage store(MultipartFile file) {
        LearningImageValidation.PreparedImage image = LearningImageValidation.prepare(file);
        try {
            client.upload(image.bytes(), image.publicId(), image.format(), AUTHENTICATED);
            return new UploadedImage(image.filename(), image.contentType());
        } catch (RuntimeException exception) {
            throw new IllegalStateException("The image could not be stored.");
        }
    }

    @Override
    public StoredImage get(String filename) {
        if (!LearningImageValidation.isValidFilename(filename)) throw new EntityNotFoundException("Learning image not found.");
        String contentType = LearningImageValidation.contentTypeForFilename(filename);
        String publicId = filename.substring(0, filename.lastIndexOf('.'));
        String format = filename.substring(filename.lastIndexOf('.') + 1);
        try {
            byte[] bytes = client.download(publicId, format, AUTHENTICATED);
            LearningImageValidation.validateContent(bytes, contentType);
            return new StoredImage(filename, contentType, new ByteArrayResource(bytes));
        } catch (IllegalArgumentException exception) {
            throw new EntityNotFoundException("Learning image not found.");
        } catch (RuntimeException exception) {
            throw new IllegalStateException("The image could not be retrieved.");
        }
    }
}
