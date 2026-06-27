package com.medsimple.symptom.entity;

import com.medsimple.shared.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// FHIR: Observation — individual symptom entry accumulating into an episode.
// Voice/photo media stored in Supabase Storage; only the URL is persisted here.
@Entity
@Table(name = "symptom_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SymptomSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private SymptomEpisode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false)
    private InputType inputType;

    @Column(name = "text_content")
    private String textContent;

    @Column(name = "media_url")
    private String mediaUrl;

    private String transcription;

    @Column(name = "ai_analysis_result")
    private String aiAnalysisResult;

    // For a PHOTO: whether the image actually shows a medical symptom. Drives the patient prompt
    // (retake / describe / send anyway) and the physician-side flag. NULL for text/voice.
    @Enumerated(EnumType.STRING)
    @Column(name = "ai_analysis_relevance")
    private MediaRelevance aiAnalysisRelevance;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    @Builder.Default
    private ProcessingStatus processingStatus = ProcessingStatus.PENDING;

    @CreationTimestamp
    @Column(name = "submitted_at", nullable = false, updatable = false)
    private OffsetDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
