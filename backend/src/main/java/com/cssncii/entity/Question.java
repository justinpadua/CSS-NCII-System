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
@Table(name = "questions", uniqueConstraints = @UniqueConstraint(name = "uq_questions_assessment_order", columnNames = {"assessment_id", "display_order"}))
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Assessment assessment;

    @Column(name = "question_type", nullable = false, length = 40)
    private String questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "LONGTEXT")
    private String questionText;

    @Column(name = "correct_answer_data", nullable = false, columnDefinition = "JSON")
    private String correctAnswerData;

    @Column(name = "question_config", columnDefinition = "JSON")
    private String questionConfig;

    @Column(name = "points", nullable = false, precision = 8, scale = 2)
    private BigDecimal points;

    @Column(name = "display_order", nullable = false, columnDefinition = "INT UNSIGNED")
    private long displayOrder;

    @Column(name = "created_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    public Question() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Assessment getAssessment() { return assessment; }
    public void setAssessment(Assessment assessment) { this.assessment = assessment; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getCorrectAnswerData() { return correctAnswerData; }
    public void setCorrectAnswerData(String correctAnswerData) { this.correctAnswerData = correctAnswerData; }
    public String getQuestionConfig() { return questionConfig; }
    public void setQuestionConfig(String questionConfig) { this.questionConfig = questionConfig; }
    public BigDecimal getPoints() { return points; }
    public void setPoints(BigDecimal points) { this.points = points; }
    public long getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(long displayOrder) { this.displayOrder = displayOrder; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
