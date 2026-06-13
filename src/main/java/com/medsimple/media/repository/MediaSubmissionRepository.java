package com.medsimple.media.repository;

import com.medsimple.symptom.entity.InputType;
import com.medsimple.symptom.entity.ProcessingStatus;
import com.medsimple.symptom.entity.SymptomSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

// Media module's own view of symptom_submissions — the rows that need media processing.
@Repository
public interface MediaSubmissionRepository extends JpaRepository<SymptomSubmission, UUID> {

    List<SymptomSubmission> findByProcessingStatusAndInputTypeIn(
            ProcessingStatus status, List<InputType> inputTypes);
}
