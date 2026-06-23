package com.medsimple.careplan.entity;

import com.medsimple.shared.entity.User;
import com.medsimple.symptom.entity.SymptomEpisode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

// An AI-proposed plan of care, advisory only. Separate from the signed care_plans record:
// the patient never reads this table. A care_plans row links back via source_suggestion_id.
@Entity
@Table(name = "care_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CareSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private SymptomEpisode episode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    // Advisory plan text. Null for FAILED / guardrail-blocked rows — a blocked proposal is
    // never stored, only the fact that one was blocked.
    @Column(name = "suggested_plan")
    private String suggestedPlan;

    private String rationale;

    @Column(nullable = false)
    private String adapter;   // 'openai' | 'fallback'

    private String model;     // e.g. 'gpt-4o'; null for the fallback adapter

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CareSuggestionStatus status = CareSuggestionStatus.PROPOSED;

    @Column(name = "reject_reason")
    private String rejectReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;   // the physician who approved/edited/rejected

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    // Set when an approved/edited suggestion becomes a care plan.
    @Column(name = "care_plan_id")
    private UUID carePlanId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
