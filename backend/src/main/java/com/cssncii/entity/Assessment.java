package com.cssncii.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessments", uniqueConstraints = {
    @UniqueConstraint(name = "uq_assessments_discussion_type", columnNames = {"discussion_id", "assessment_type"}),
    @UniqueConstraint(name = "uq_assessments_competency_type", columnNames = {"competency_id", "assessment_type"})
})
public class Assessment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discussion_id", columnDefinition = "BIGINT UNSIGNED")
    private Discussion discussion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competency_id", columnDefinition = "BIGINT UNSIGNED")
    private Competency competency;

    @Column(name = "assessment_type", nullable = false, length = 30)
    private String assessmentType;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "instructions", columnDefinition = "TEXT")
    private String instructions;

    @Column(name = "pass_percentage", precision = 5, scale = 2)
    private BigDecimal passPercentage;

    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    public Assessment() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Discussion getDiscussion() { return discussion; }
    public void setDiscussion(Discussion discussion) { this.discussion = discussion; }
    public Competency getCompetency() { return competency; }
    public void setCompetency(Competency competency) { this.competency = competency; }
    public String getAssessmentType() { return assessmentType; }
    public void setAssessmentType(String assessmentType) { this.assessmentType = assessmentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getInstructions() { return instructions; }
    public void setInstructions(String instructions) { this.instructions = instructions; }
    public BigDecimal getPassPercentage() { return passPercentage; }
    public void setPassPercentage(BigDecimal passPercentage) { this.passPercentage = passPercentage; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
