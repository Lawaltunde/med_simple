package com.medsimple.diagnostic.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.auth.security.AccessGuard;
import com.medsimple.careplan.entity.CarePlan;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.repository.CarePlanRepository;
import com.medsimple.diagnostic.dao.*;
import com.medsimple.diagnostic.dto.*;
import com.medsimple.diagnostic.entity.*;
import com.medsimple.diagnostic.exception.*;
import com.medsimple.diagnostic.repository.DiagnosticRequisitionRepository;
import com.medsimple.diagnostic.repository.DiagnosticResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiagnosticService {

    private final DiagnosticRequisitionRepository requisitionRepository;
    private final DiagnosticResultRepository resultRepository;
    private final CarePlanRepository carePlanRepository;
    private final RequisitionDeliveryResolver deliveryResolver;
    private final ResultSummaryDao resultSummaryDao;
    private final AuditService auditService;
    private final com.medsimple.notification.service.NotificationService notificationService;
    private final AccessGuard accessGuard;

    // ── Requisition lifecycle ───────────────────────────────────────────────────

    @Transactional
    public RequisitionResponse createRequisition(CreateRequisitionRequest request, String ipAddress) {
        CarePlan carePlan = carePlanRepository.findById(request.carePlanId())
                .orElseThrow(() -> new CarePlanNotFoundException(request.carePlanId()));

        DiagnosticRequisition requisition = DiagnosticRequisition.builder()
                .carePlan(carePlan)
                .patient(carePlan.getPatient())
                .orderingPhysician(carePlan.getPhysician())
                .testType(request.testType())
                .testCodes(request.testCodes() != null ? request.testCodes() : List.of())
                .clinicalIndication(request.clinicalIndication())
                .urgency(request.urgency())
                .status(RequisitionStatus.DRAFT)
                .build();

        DiagnosticRequisition saved = requisitionRepository.save(requisition);
        auditService.log(carePlan.getPhysician().getId(), "REQUISITION_CREATED",
                "diagnostic_requisitions", saved.getId(), ipAddress);
        return toRequisitionResponse(saved);
    }

    @Transactional
    public RequisitionResponse sendRequisition(UUID id, String ipAddress) {
        DiagnosticRequisition req = findRequisition(id);
        if (req.getStatus() != RequisitionStatus.DRAFT) {
            throw new InvalidRequisitionStateException(
                    "Only a DRAFT requisition can be sent (was " + req.getStatus() + ")");
        }

        UUID clinicId = req.getCarePlan().getClinic().getId();
        RequisitionDeliveryDao delivery = deliveryResolver.resolve(clinicId);
        RequisitionDeliveryResult result = delivery.deliver(req);

        req.setStatus(result.status());
        req.setDeliveryMethod(result.deliveryMethod());
        req.setDocumentUrl(result.documentUrl());
        req.setSentAt(OffsetDateTime.now());

        DiagnosticRequisition saved = requisitionRepository.save(req);
        auditService.log(req.getOrderingPhysician().getId(), "REQUISITION_SENT",
                "diagnostic_requisitions", id, ipAddress);
        return toRequisitionResponse(saved);
    }

    @Transactional
    public RequisitionResponse bookRequisition(UUID id, BookRequisitionRequest request, String ipAddress) {
        DiagnosticRequisition req = findRequisition(id);
        if (req.getStatus() != RequisitionStatus.SENT) {
            throw new InvalidRequisitionStateException(
                    "Only a SENT requisition can be booked (was " + req.getStatus() + ")");
        }
        req.setStatus(RequisitionStatus.BOOKED);
        req.setDiagnosticCentreId(request.diagnosticCentreId());
        req.setAppointmentAt(request.appointmentAt());
        req.setBookedAt(OffsetDateTime.now());
        return toRequisitionResponse(requisitionRepository.save(req));
    }

    @Transactional(readOnly = true)
    public RequisitionResponse getRequisition(UUID id) {
        return toRequisitionResponse(findRequisition(id));
    }

    @Transactional(readOnly = true)
    public List<RequisitionResponse> getCarePlanRequisitions(UUID carePlanId) {
        return requisitionRepository.findByCarePlanIdOrderByCreatedAtDesc(carePlanId)
                .stream().map(this::toRequisitionResponse).toList();
    }

    @Transactional
    public RequisitionResponse cancelRequisition(UUID id, String ipAddress) {
        DiagnosticRequisition req = findRequisition(id);
        if (req.getStatus() == RequisitionStatus.RESULTED) {
            throw new InvalidRequisitionStateException("A resulted requisition cannot be cancelled");
        }
        req.setStatus(RequisitionStatus.CANCELLED);
        return toRequisitionResponse(requisitionRepository.save(req));
    }

    // ── Result lifecycle ────────────────────────────────────────────────────────

    @Transactional
    public PhysicianResultResponse recordResult(UUID requisitionId, RecordResultRequest request, String ipAddress) {
        DiagnosticRequisition req = findRequisition(requisitionId);

        // Plain-language summary (PHI-stripped: only test type + result text).
        String summary = resultSummaryDao.summarize(req.getTestType(), request.rawResultText());

        DiagnosticResult result = DiagnosticResult.builder()
                .requisition(req)
                .patient(req.getPatient())
                .orderingPhysician(req.getOrderingPhysician())
                .rawResultText(request.rawResultText())
                .rawResultData(request.rawResultData())
                .aiSummary(summary)
                .build(); // releasedToPatientAt stays NULL — the gate is closed by default

        DiagnosticResult saved = resultRepository.save(result);

        req.setStatus(RequisitionStatus.RESULTED);
        requisitionRepository.save(req);

        auditService.log(req.getOrderingPhysician().getId(), "RESULT_RECORDED",
                "diagnostic_results", saved.getId(), ipAddress);
        return toPhysicianResponse(saved);
    }

    @Transactional
    public PhysicianResultResponse addInterpretation(UUID resultId, AddInterpretationRequest request, String ipAddress) {
        DiagnosticResult result = findResult(resultId);
        result.setPhysicianInterpretation(request.physicianInterpretation());
        DiagnosticResult saved = resultRepository.save(result);
        auditService.log(result.getOrderingPhysician().getId(), "RESULT_INTERPRETED",
                "diagnostic_results", resultId, ipAddress);
        return toPhysicianResponse(saved);
    }

    @Transactional
    public PhysicianResultResponse releaseResult(UUID resultId, String ipAddress) {
        DiagnosticResult result = findResult(resultId);
        if (result.getReleasedToPatientAt() != null) {
            throw new InvalidRequisitionStateException("Result is already released");
        }
        // Releasing without an interpretation defeats physician review — require it.
        if (result.getPhysicianInterpretation() == null || result.getPhysicianInterpretation().isBlank()) {
            throw new InvalidRequisitionStateException(
                    "A physician interpretation is required before releasing a result to the patient");
        }
        result.setReleasedToPatientAt(OffsetDateTime.now());
        DiagnosticResult saved = resultRepository.save(result);
        auditService.log(result.getOrderingPhysician().getId(), "RESULT_RELEASED",
                "diagnostic_results", resultId, ipAddress);

        // Now that the physician has released it, tell the patient the result is available.
        notificationService.notify(result.getPatient().getId(),
                com.medsimple.notification.entity.NotificationType.RESULT_READY,
                "Your test result is ready",
                "Your " + result.getRequisition().getTestType()
                        + " result has been reviewed by your physician and is now available to view.");

        return toPhysicianResponse(saved);
    }

    // Physician view — full result regardless of release status.
    @Transactional(readOnly = true)
    public PhysicianResultResponse getResultForPhysician(UUID resultId) {
        return toPhysicianResponse(findResult(resultId));
    }

    // ── PATIENT-SCOPED READS — the PHIPA gate is enforced here, in the service ──

    @Transactional(readOnly = true)
    public PatientResultResponse getResultForPatient(UUID resultId) {
        DiagnosticResult result = findResult(resultId);
        if (result.getReleasedToPatientAt() == null) {
            throw new ResultNotReleasedException(resultId);
        }
        return toPatientResponse(result);
    }

    @Transactional(readOnly = true)
    public List<PatientResultResponse> getPatientReleasedResults(UUID patientId) {
        // Patient-scoped read: a patient may see only their own released results;
        // staff may see any. The release gate (releasedToPatientAt) controls WHAT is
        // visible; this controls WHOSE — without it any patient could read another's.
        accessGuard.requirePatientSelfOrStaff(patientId);
        return resultRepository
                .findByPatientIdAndReleasedToPatientAtIsNotNullOrderByReceivedAtDesc(patientId)
                .stream().map(this::toPatientResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private DiagnosticRequisition findRequisition(UUID id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> new RequisitionNotFoundException(id));
    }

    private DiagnosticResult findResult(UUID id) {
        return resultRepository.findById(id)
                .orElseThrow(() -> new ResultNotFoundException(id));
    }

    private RequisitionResponse toRequisitionResponse(DiagnosticRequisition r) {
        return new RequisitionResponse(
                r.getId(), r.getCarePlan().getId(), r.getPatient().getId(),
                r.getOrderingPhysician().getId(), r.getTestType(), r.getTestCodes(),
                r.getClinicalIndication(), r.getUrgency(), r.getStatus(),
                r.getDeliveryMethod(), r.getDiagnosticCentreId(), r.getDocumentUrl(),
                r.getAppointmentAt(), r.getSentAt(), r.getBookedAt(), r.getCreatedAt());
    }

    private PhysicianResultResponse toPhysicianResponse(DiagnosticResult r) {
        return new PhysicianResultResponse(
                r.getId(), r.getRequisition().getId(), r.getPatient().getId(),
                r.getOrderingPhysician().getId(), r.getRawResultText(), r.getRawResultData(),
                r.getAiSummary(), r.getPhysicianInterpretation(),
                r.getReleasedToPatientAt() != null, r.getReleasedToPatientAt(), r.getReceivedAt());
    }

    private PatientResultResponse toPatientResponse(DiagnosticResult r) {
        return new PatientResultResponse(
                r.getId(), r.getRequisition().getId(), r.getRequisition().getTestType(),
                r.getAiSummary(), r.getPhysicianInterpretation(), r.getReleasedToPatientAt());
    }
}
