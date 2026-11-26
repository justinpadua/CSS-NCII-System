package com.cssncii.service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class YouTubeVideoUrls {
    private static final String VIDEO_ID_PATTERN = "[A-Za-z0-9_-]{11}";

    private YouTubeVideoUrls() {}

    public static List<String> validate(List<String> urls) {
        if (urls == null) return null;
        List<String> validated = new ArrayList<>();
        for (String value : urls) {
            if (value == null || value.isBlank()) continue;
            String url = value.trim();
            videoId(url);
            validated.add(url);
        }
        return List.copyOf(validated);
    }

    public static String videoId(String value) {
        try {
            URI uri = URI.create(value.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getPort() != -1) {
                throw invalidUrl();
            }
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath();
            String id = null;
            if ("youtu.be".equals(host)) {
                id = path.startsWith("/") ? path.substring(1) : path;
            } else if ("youtube.com".equals(host) || "www.youtube.com".equals(host)) {
                if ("/watch".equals(path)) id = queryValue(uri.getRawQuery(), "v");
                else if (path.startsWith("/embed/")) id = path.substring("/embed/".length());
                else if (path.startsWith("/shorts/")) id = path.substring("/shorts/".length());
            }
            if (id == null || !id.matches(VIDEO_ID_PATTERN)) throw invalidUrl();
            return id;
        } catch (IllegalArgumentException exception) {
            throw invalidUrl();
        }
    }

    private static String queryValue(String query, String name) {
        if (query == null) return null;
        for (String parameter : query.split("&")) {
            String[] pair = parameter.split("=", 2);
            if (pair.length == 2 && name.equals(URLDecoder.decode(pair[0], StandardCharsets.UTF_8))) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private static IllegalArgumentException invalidUrl() {
        return new IllegalArgumentException("Enter a valid HTTPS YouTube video URL.");
    }
}
