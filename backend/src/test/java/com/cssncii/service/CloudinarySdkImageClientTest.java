package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Map;
import javax.net.ssl.HttpsURLConnection;
import org.junit.jupiter.api.Test;

class CloudinarySdkImageClientTest {
    private static final String SIGNED_URL = "https://api.cloudinary.com/v1_1/test/image/download?signature=internal";

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void sdkUploadExplicitlyUsesAuthenticatedDeliveryAndUniqueNonOverwritingId() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(), anyMap())).thenReturn(Map.of(
            "public_id", "0123456789abcdef0123456789abcdef",
            "type", "authenticated",
            "format", "png"));
        CloudinarySdkImageClient client = new CloudinarySdkImageClient(cloudinary);

        client.upload(new byte[] {1}, "0123456789abcdef0123456789abcdef", "png", "authenticated");

        org.mockito.ArgumentCaptor<Map> options = org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(), options.capture());
        assertEquals("authenticated", options.getValue().get("type"));
        assertEquals("image", options.getValue().get("resource_type"));
        assertEquals(false, options.getValue().get("overwrite"));
        assertEquals(false, options.getValue().get("unique_filename"));
    }

    @Test
    void privateDownloadIsShortLivedAndAuthenticated() {
        Map<String, Object> options = CloudinarySdkImageClient.privateDownloadOptions("authenticated", 123L);

        assertEquals("authenticated", options.get("type"));
        assertEquals("image", options.get("resource_type"));
        assertEquals(123L, options.get("expires_at"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void successfulDownloadUsesTlsTimeoutsNoRedirectAndReturnsImageBytes() throws Exception {
        byte[] expected = new byte[] {11, 22, 33};
        HttpsURLConnection connection = connection(200, new ByteArrayInputStream(expected));
        Cloudinary cloudinary = cloudinaryWithSignedUrl();
        CloudinarySdkImageClient client = client(cloudinary, connection);

        assertArrayEquals(expected, client.download("image-id", "png", "authenticated"));
        verify(connection).setConnectTimeout(5_000);
        verify(connection).setReadTimeout(10_000);
        verify(connection).setInstanceFollowRedirects(false);
        verify(connection).disconnect();
    }

    @Test
    void stalledResponseBodyFailsWithSafeGenericError() throws Exception {
        InputStream stalled = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new SocketTimeoutException("provider stream stalled");
            }
        };
        HttpsURLConnection connection = connection(200, stalled);
        CloudinarySdkImageClient client = client(cloudinaryWithSignedUrl(), connection);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> client.download("image-id", "png", "authenticated"));

        assertEquals("Cloudinary image download failed.", error.getMessage());
        assertNull(error.getCause());
        verify(connection).setReadTimeout(10_000);
        verify(connection).disconnect();
    }

    @Test
    void responseLargerThanFiveMegabytesIsRejectedWhileStreaming() throws Exception {
        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];
        HttpsURLConnection connection = connection(200, new ByteArrayInputStream(oversized));
        CloudinarySdkImageClient client = client(cloudinaryWithSignedUrl(), connection);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> client.download("image-id", "png", "authenticated"));

        assertEquals("Cloudinary image download failed.", error.getMessage());
        assertNull(error.getCause());
        verify(connection).disconnect();
    }

    @Test
    void redirectResponseIsRejectedAndNeverFollowed() throws Exception {
        HttpsURLConnection connection = connection(302, InputStream.nullInputStream());
        CloudinarySdkImageClient client = client(cloudinaryWithSignedUrl(), connection);

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> client.download("image-id", "png", "authenticated"));

        assertEquals("Cloudinary image download failed.", error.getMessage());
        verify(connection).setInstanceFollowRedirects(false);
        verify(connection, never()).getInputStream();
        verify(connection).disconnect();
    }

    @Test
    void rejectsNonHttpsOrUnexpectedCloudinaryHostBeforeOpeningConnection() throws Exception {
        Cloudinary cloudinary = cloudinaryWithUrl("https://attacker.example/image?signature=hidden");
        CloudinarySdkImageClient client = new CloudinarySdkImageClient(cloudinary,
            url -> fail("Unexpected host must not be opened: " + url));

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> client.download("image-id", "png", "authenticated"));

        assertEquals("Cloudinary image download failed.", error.getMessage());
        assertNull(error.getCause());
    }

    private CloudinarySdkImageClient client(Cloudinary cloudinary, HttpsURLConnection connection) throws Exception {
        return new CloudinarySdkImageClient(cloudinary, url -> connection);
    }

    private HttpsURLConnection connection(int status, InputStream body) throws Exception {
        HttpsURLConnection connection = mock(HttpsURLConnection.class);
        when(connection.getResponseCode()).thenReturn(status);
        when(connection.getContentLengthLong()).thenReturn(-1L);
        when(connection.getInputStream()).thenReturn(body);
        return connection;
    }

    private Cloudinary cloudinaryWithSignedUrl() throws Exception {
        return cloudinaryWithUrl(SIGNED_URL);
    }

    private Cloudinary cloudinaryWithUrl(String signedUrl) throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        when(cloudinary.privateDownload(any(), any(), anyMap())).thenReturn(signedUrl);
        return cloudinary;
    }
}
