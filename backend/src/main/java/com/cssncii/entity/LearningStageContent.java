package com.cssncii.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "learning_stage_contents",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_stage_contents_discussion_stage", columnNames = {"discussion_id", "stage_code"}),
        @UniqueConstraint(name = "uq_stage_contents_discussion_order", columnNames = {"discussion_id", "display_order"})
    })
public class LearningStageContent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Discussion discussion;

    @Column(name = "stage_code", nullable = false, length = 30)
    private String stageCode;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content_body", nullable = false, columnDefinition = "LONGTEXT")
    private String contentBody;

    @Column(name = "youtube_video_urls", columnDefinition = "LONGTEXT")
    private String youtubeVideoUrls;

    @Column(name = "display_order", nullable = false, columnDefinition = "TINYINT UNSIGNED")
    private short displayOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    public LearningStageContent() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Discussion getDiscussion() { return discussion; }
    public void setDiscussion(Discussion discussion) { this.discussion = discussion; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContentBody() { return contentBody; }
    public void setContentBody(String contentBody) { this.contentBody = contentBody; }
    public List<String> getYoutubeVideoUrls() {
        if (youtubeVideoUrls == null || youtubeVideoUrls.isBlank()) return List.of();
        return Arrays.stream(youtubeVideoUrls.split("\\R"))
            .filter(url -> !url.isBlank())
            .toList();
    }
    public void setYoutubeVideoUrls(List<String> urls) {
        youtubeVideoUrls = urls == null || urls.isEmpty() ? null : String.join("\n", urls);
    }
    public short getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(short displayOrder) { this.displayOrder = displayOrder; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
