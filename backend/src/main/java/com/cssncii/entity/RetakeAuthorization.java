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
import java.time.LocalDateTime;

@Entity
@Table(name = "retake_authorizations", indexes = {
    @Index(name = "idx_retake_authorizations_student_competency", columnList = "student_id, competency_id"),
    @Index(name = "idx_retake_authorizations_admin", columnList = "admin_id")
})
public class RetakeAuthorization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "competency_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Competency competency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Admin admin;

    @Column(name = "authorized_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime authorizedAt;

    public RetakeAuthorization() {}
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Competency getCompetency() { return competency; }
    public void setCompetency(Competency competency) { this.competency = competency; }
    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }
    public LocalDateTime getAuthorizedAt() { return authorizedAt; }
    public void setAuthorizedAt(LocalDateTime authorizedAt) { this.authorizedAt = authorizedAt; }
}
