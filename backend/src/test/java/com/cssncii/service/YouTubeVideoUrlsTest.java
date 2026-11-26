package com.cssncii.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class YouTubeVideoUrlsTest {
    private static final String VIDEO_ID = "abcdefghijk";

    @Test
    void acceptsSupportedYouTubeUrlFormats() {
        assertEquals(VIDEO_ID, YouTubeVideoUrls.videoId("https://www.youtube.com/watch?v=" + VIDEO_ID));
        assertEquals(VIDEO_ID, YouTubeVideoUrls.videoId("https://youtu.be/" + VIDEO_ID));
        assertEquals(VIDEO_ID, YouTubeVideoUrls.videoId("https://www.youtube.com/embed/" + VIDEO_ID));
        assertEquals(VIDEO_ID, YouTubeVideoUrls.videoId("https://www.youtube.com/shorts/" + VIDEO_ID));
    }

    @Test
    void rejectsNonYouTubeAndMalformedUrls() {
        assertThrows(IllegalArgumentException.class,
            () -> YouTubeVideoUrls.videoId("https://example.com/embed/" + VIDEO_ID));
        assertThrows(IllegalArgumentException.class,
            () -> YouTubeVideoUrls.videoId("http://www.youtube.com/watch?v=" + VIDEO_ID));
        assertThrows(IllegalArgumentException.class,
            () -> YouTubeVideoUrls.videoId("https://www.youtube.com/watch?v=short"));
        assertThrows(IllegalArgumentException.class,
            () -> YouTubeVideoUrls.videoId("<iframe src=\"https://youtube.com/embed/" + VIDEO_ID + "\"></iframe>"));
    }

    @Test
    void optionalUrlListAllowsEmptyValuesAndValidatesPopulatedValues() {
        assertEquals(List.of(), YouTubeVideoUrls.validate(List.of("", "  ")));
        assertEquals(List.of("https://youtu.be/" + VIDEO_ID),
            YouTubeVideoUrls.validate(List.of(" https://youtu.be/" + VIDEO_ID + " ")));
    }
}
