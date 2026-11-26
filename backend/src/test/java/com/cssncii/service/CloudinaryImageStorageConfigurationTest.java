package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.http5.UploaderStrategy;
import com.cssncii.config.CloudinaryImageStorageConfiguration;
import org.junit.jupiter.api.Test;

class CloudinaryImageStorageConfigurationTest {
    @Test
    void rejectsMissingCloudinaryCredentialsWithoutIncludingSecretValuesInError() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> CloudinaryImageStorageConfiguration.validateRequiredConfiguration("cloud-name", "api-key", " "));

        assertTrue(error.getMessage().contains("CLOUDINARY_API_SECRET"));
        assertFalse(error.getMessage().contains("api-key"));
    }

    @Test
    void acceptsCompleteCloudinaryConfiguration() {
        assertDoesNotThrow(() -> CloudinaryImageStorageConfiguration.validateRequiredConfiguration(
            "cloud-name", "api-key", "api-secret"));
    }

    @Test
    void rejectsMalformedCloudNameBeforeCreatingTheSdkClient() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> CloudinaryImageStorageConfiguration.validateRequiredConfiguration("https://host/path", "key", "secret"));

        assertTrue(error.getMessage().contains("CLOUDINARY_CLOUD_NAME"));
        assertFalse(error.getMessage().contains("https://host/path"));
    }

    @Test
    void configuresFiniteCloudinarySdkTimeoutsForUploads() {
        Cloudinary cloudinary = new Cloudinary(
            CloudinaryImageStorageConfiguration.cloudinaryOptions("cloud", "key", "secret"));
        UploaderStrategy strategy = new UploaderStrategy();
        strategy.init(cloudinary.uploader());

        assertEquals(CloudinarySdkImageClient.SDK_TIMEOUT_SECONDS, cloudinary.config.timeout);
        assertEquals(CloudinarySdkImageClient.SDK_TIMEOUT_SECONDS,
            strategy.buildRequestConfig().getResponseTimeout().toSeconds());
        assertEquals(CloudinarySdkImageClient.SDK_TIMEOUT_SECONDS,
            strategy.buildRequestConfig().getConnectTimeout().toSeconds());
    }
}
