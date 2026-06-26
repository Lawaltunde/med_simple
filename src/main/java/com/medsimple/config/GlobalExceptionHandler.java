package com.medsimple.config;

import com.medsimple.auth.exception.InvalidCredentialsException;
import com.medsimple.auth.exception.InvalidTokenException;
import com.medsimple.auth.exception.TooManyAttemptsException;
import com.medsimple.appointment.exception.AppointmentNotFoundException;
import com.medsimple.careplan.exception.CarePlanNotFoundException;
import com.medsimple.careplan.exception.CareSuggestionNotFoundException;
import com.medsimple.careplan.exception.InvalidSuggestionStateException;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.exception.InvalidPhysicianCredentialException;
import com.medsimple.clinic.exception.StaffAlreadyExistsException;
import com.medsimple.prescription.exception.InvalidPrescriptionStateException;
import com.medsimple.prescription.exception.PrescriptionNotFoundException;
import com.medsimple.diagnostic.exception.InvalidRequisitionStateException;
import com.medsimple.diagnostic.exception.RequisitionNotFoundException;
import com.medsimple.diagnostic.exception.ResultNotFoundException;
import com.medsimple.diagnostic.exception.ResultNotReleasedException;
import com.medsimple.media.exception.MediaNotFoundException;
import com.medsimple.media.exception.MediaTooLargeException;
import com.medsimple.media.exception.SubmissionNotFoundException;
import com.medsimple.media.exception.UnsupportedMediaTypeException;
import com.medsimple.notification.exception.NotificationNotFoundException;
import com.medsimple.referral.exception.InvalidReferralStateException;
import com.medsimple.referral.exception.ReferralNotFoundException;
import com.medsimple.shared.exception.BusinessRuleException;
import com.medsimple.symptom.exception.EpisodeNotFoundException;
import com.medsimple.patient.exception.EmailAlreadyInUseException;
import com.medsimple.patient.exception.InvalidHealthCardException;
import com.medsimple.patient.exception.PatientAlreadyRegisteredException;
import com.medsimple.patient.exception.PatientNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    record ApiError(int status, String error, String message, Instant timestamp) {}

    @ExceptionHandler({PatientNotFoundException.class, ClinicNotFoundException.class, EpisodeNotFoundException.class, CarePlanNotFoundException.class, PrescriptionNotFoundException.class, ReferralNotFoundException.class, RequisitionNotFoundException.class, ResultNotFoundException.class, NotificationNotFoundException.class, SubmissionNotFoundException.class, MediaNotFoundException.class, CareSuggestionNotFoundException.class, AppointmentNotFoundException.class})
    public ResponseEntity<ApiError> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, "NOT_FOUND", ex.getMessage(), Instant.now()));
    }

    // Upload rejected: content type not in the image/audio allow-list.
    @ExceptionHandler(UnsupportedMediaTypeException.class)
    public ResponseEntity<ApiError> handleUnsupportedMedia(UnsupportedMediaTypeException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ApiError(415, "UNSUPPORTED_MEDIA_TYPE", ex.getMessage(), Instant.now()));
    }

    // Upload rejected: file exceeds the configured maximum size.
    @ExceptionHandler(MediaTooLargeException.class)
    public ResponseEntity<ApiError> handleMediaTooLarge(MediaTooLargeException ex) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ApiError(413, "PAYLOAD_TOO_LARGE", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler({InvalidPrescriptionStateException.class, InvalidReferralStateException.class, InvalidRequisitionStateException.class, InvalidSuggestionStateException.class, BusinessRuleException.class})
    public ResponseEntity<ApiError> handleInvalidState(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "INVALID_STATE", ex.getMessage(), Instant.now()));
    }

    // Unknown route / missing static resource. Without this it falls through to the catch-all and
    // surfaces as a 500; an unmatched path is a 404.
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError(404, "NOT_FOUND", "No endpoint " + ex.getResourcePath(), Instant.now()));
    }

    // PHIPA gate violation — the patient asked for a result that is not yet released.
    @ExceptionHandler(ResultNotReleasedException.class)
    public ResponseEntity<ApiError> handleNotReleased(ResultNotReleasedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "NOT_RELEASED", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler({InvalidCredentialsException.class, InvalidTokenException.class})
    public ResponseEntity<ApiError> handleAuthFailure(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError(401, "UNAUTHORIZED", ex.getMessage(), Instant.now()));
    }

    // Login throttle tripped — too many failed attempts for this identifier.
    @ExceptionHandler(TooManyAttemptsException.class)
    public ResponseEntity<ApiError> handleTooManyAttempts(TooManyAttemptsException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ApiError(429, "TOO_MANY_REQUESTS", ex.getMessage(), Instant.now()));
    }

    // @PreAuthorize denial — authenticated but lacking the required role.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError(403, "FORBIDDEN", "You do not have permission to perform this action",
                        Instant.now()));
    }

    @ExceptionHandler({PatientAlreadyRegisteredException.class, EmailAlreadyInUseException.class, StaffAlreadyExistsException.class})
    public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(409, "CONFLICT", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(InvalidHealthCardException.class)
    public ResponseEntity<ApiError> handleInvalidHealthCard(InvalidHealthCardException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "INVALID_HEALTH_CARD", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(InvalidPhysicianCredentialException.class)
    public ResponseEntity<ApiError> handleInvalidCredential(InvalidPhysicianCredentialException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "INVALID_PHYSICIAN_CREDENTIAL", ex.getMessage(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "VALIDATION_ERROR", message, Instant.now()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "BAD_REQUEST", message, Instant.now()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(400, "MALFORMED_REQUEST", "Request body is malformed or has an invalid field type",
                        Instant.now()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError(500, "INTERNAL_ERROR", "An unexpected error occurred", Instant.now()));
    }
}
