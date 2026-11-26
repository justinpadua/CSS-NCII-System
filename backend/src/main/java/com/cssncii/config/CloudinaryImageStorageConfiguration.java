package com.cssncii.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.cssncii.service.CloudinaryImageClient;
import com.cssncii.service.CloudinaryImageStorageService;
import com.cssncii.service.CloudinarySdkImageClient;
import com.cssncii.service.LearningImageStore;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.image-storage.provider", havingValue = "cloudinary")
public class CloudinaryImageStorageConfiguration {
    @Bean
    CloudinaryImageClient cloudinaryImageClient(
        @Value("${CLOUDINARY_CLOUD_NAME:}") String cloudName,
        @Value("${CLOUDINARY_API_KEY:}") String apiKey,
        @Value("${CLOUDINARY_API_SECRET:}") String apiSecret
    ) {
        validateRequiredConfiguration(cloudName, apiKey, apiSecret);
        Cloudinary cloudinary = new Cloudinary(cloudinaryOptions(cloudName, apiKey, apiSecret));
        return new CloudinarySdkImageClient(cloudinary);
    }

    public static Map<String, Object> cloudinaryOptions(String cloudName, String apiKey, String apiSecret) {
        return ObjectUtils.asMap(
            "cloud_name", cloudName,
            "api_key", apiKey,
            "api_secret", apiSecret,
            "secure", true,
            "timeout", CloudinarySdkImageClient.SDK_TIMEOUT_SECONDS);
    }

    @Bean
    LearningImageStore cloudinaryImageStore(CloudinaryImageClient client) {
        return new CloudinaryImageStorageService(client);
    }

    public static void validateRequiredConfiguration(String cloudName, String apiKey, String apiSecret) {
        requireValue("CLOUDINARY_CLOUD_NAME", cloudName);
        requireValue("CLOUDINARY_API_KEY", apiKey);
        requireValue("CLOUDINARY_API_SECRET", apiSecret);
        if (!cloudName.matches("[A-Za-z0-9_-]{1,255}")) {
            throw new IllegalStateException("CLOUDINARY_CLOUD_NAME contains unsupported characters.");
        }
        if (!apiKey.equals(apiKey.trim()) || !apiSecret.equals(apiSecret.trim())) {
            throw new IllegalStateException("Cloudinary credential environment variables must not contain surrounding whitespace.");
        }
    }

    private static void requireValue(String name, String value) {
        if (value == null || value.isBlank()) throw new IllegalStateException("Cloudinary storage requires the " + name + " environment variable.");
    }
}
