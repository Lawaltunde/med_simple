package com.medsimple.careplan.controller;

import com.medsimple.careplan.dto.*;
import com.medsimple.careplan.service.CarePlanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CarePlanController {

    private final CarePlanService carePlanService;

    @PostMapping("/api/v1/care-plans")
    public ResponseEntity<CarePlanResponse> createCarePlan(@Valid @RequestBody CreateCarePlanRequest request,
                                                           HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.createCarePlan(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/api/v1/care-plans/{id}")
    public ResponseEntity<CarePlanResponse> getCarePlan(@PathVariable UUID id) {
        return ResponseEntity.ok(carePlanService.getCarePlan(id));
    }

    @GetMapping("/api/v1/patients/{patientId}/care-plans")
    public ResponseEntity<List<CarePlanResponse>> getPatientCarePlans(@PathVariable UUID patientId) {
        return ResponseEntity.ok(carePlanService.getPatientCarePlans(patientId));
    }

    @PatchMapping("/api/v1/care-plans/{id}")
    public ResponseEntity<CarePlanResponse> updateCarePlan(@PathVariable UUID id,
                                                          @RequestBody UpdateCarePlanRequest request,
                                                          HttpServletRequest httpRequest) {
        return ResponseEntity.ok(carePlanService.updateCarePlan(id, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/care-plans/{id}/follow-ups")
    public ResponseEntity<FollowUpResponse> addFollowUp(@PathVariable UUID id,
                                                        @Valid @RequestBody AddFollowUpRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carePlanService.addFollowUp(id, request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/care-plans/{id}/follow-ups/{followUpId}/respond")
    public ResponseEntity<FollowUpResponse> respondToFollowUp(@PathVariable UUID id,
                                                             @PathVariable UUID followUpId,
                                                             @Valid @RequestBody RespondFollowUpRequest request,
                                                             HttpServletRequest httpRequest) {
        return ResponseEntity.ok(
                carePlanService.respondToFollowUp(id, followUpId, request, httpRequest.getRemoteAddr()));
    }
}
