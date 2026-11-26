package com.cssncii.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public final class CloudinarySdkImageClient implements CloudinaryImageClient {
    private static final long MAX_IMAGE_BYTES = LearningImageStorageService.MAX_IMAGE_BYTES;
    static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    static final int READ_TIMEOUT_MILLIS = 10_000;
    public static final int SDK_TIMEOUT_SECONDS = 10;
    private final Cloudinary cloudinary;
    private final DownloadConnectionFactory connectionFactory;

    public CloudinarySdkImageClient(Cloudinary cloudinary) {
        this(cloudinary, url -> (HttpsURLConnection) url.openConnection());
    }

    CloudinarySdkImageClient(Cloudinary cloudinary, DownloadConnectionFactory connectionFactory) {
        this.cloudinary = cloudinary;
        this.connectionFactory = connectionFactory;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void upload(byte[] bytes, String publicId, String format, String deliveryType) {
        try {
            Map result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "resource_type", "image",
                "type", deliveryType,
                "public_id", publicId,
                "overwrite", false,
                "unique_filename", false,
                "use_filename", false));
            if (!publicId.equals(result.get("public_id")) || !deliveryType.equals(result.get("type"))
                || !format.equals(result.get("format"))) {
                throw new IllegalStateException("Cloudinary returned unexpected image metadata.");
            }
        } catch (Exception exception) {
            // Do not retain provider exception messages: they can include signed request details.
            throw new IllegalStateException("Cloudinary image upload failed.");
        }
    }

    @Override
    public byte[] download(String publicId, String format, String deliveryType) {
        try {
            String signedUrl = cloudinary.privateDownload(publicId, format,
                privateDownloadOptions(deliveryType, Instant.now().plusSeconds(60).getEpochSecond()));
            URI uri = URI.create(signedUrl);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"api.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
                throw new IllegalStateException("Cloudinary returned an unexpected download host.");
            }
            HttpsURLConnection connection = connectionFactory.open(uri.toURL());
            try {
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
                connection.setReadTimeout(READ_TIMEOUT_MILLIS);
                connection.setInstanceFollowRedirects(false);
                int status = connection.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    throw new IllegalStateException("Cloudinary image download failed.");
                }
                long contentLength = connection.getContentLengthLong();
                if (contentLength > MAX_IMAGE_BYTES) {
                    throw new IllegalStateException("Cloudinary image exceeds the supported size.");
                }
                try (InputStream body = connection.getInputStream()) {
                    ByteArrayOutputStream output = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int total = 0;
                    int read;
                    while ((read = body.read(buffer)) != -1) {
                        total += read;
                        if (total > MAX_IMAGE_BYTES) {
                            throw new IllegalStateException("Cloudinary image exceeds the supported size.");
                        }
                        output.write(buffer, 0, read);
                    }
                    return output.toByteArray();
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception exception) {
            // Never expose or log the signed Cloudinary download URL.
            throw new IllegalStateException("Cloudinary image download failed.");
        }
    }

    static Map<String, Object> privateDownloadOptions(String deliveryType, long expiresAt) {
        return ObjectUtils.asMap("resource_type", "image", "type", deliveryType, "expires_at", expiresAt);
    }

    @FunctionalInterface
    interface DownloadConnectionFactory {
        HttpsURLConnection open(URL url) throws IOException;
    }
}
