package com.yunesh.digitalwallet.kyc;

import com.yunesh.digitalwallet.user.KycDocumentType;
import com.yunesh.digitalwallet.user.MaritalStatus;
import com.yunesh.digitalwallet.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "kyc_details")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KycDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "father_name", length = 100)
    private String fatherName;

    @Column(name = "mother_name", length = 100)
    private String motherName;

    @Column(name = "grandfather_name", length = 100)
    private String grandfatherName;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "spouse_name", length = 100)
    private String spouseName;

    @Column(name = "spouse_phone", length = 10)
    private String spousePhone;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 50)
    private KycDocumentType documentType;

    @Column(name = "document_id", length = 100)
    private String documentId;

    @Column(name = "document_issue_date")
    private LocalDate documentIssueDate;

    @Column(name = "document_issued_place", length = 150)
    private String documentIssuedPlace;

    @Column(name = "document_picture_path", length = 500)
    private String documentPicturePath;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}