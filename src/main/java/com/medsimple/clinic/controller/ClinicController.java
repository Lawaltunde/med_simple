package com.medsimple.clinic.controller;

import com.medsimple.clinic.dto.*;
import com.medsimple.clinic.service.ClinicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    public ResponseEntity<ClinicResponse> createClinic(@Valid @RequestBody CreateClinicRequest request,
                                                        HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicService.createClinic(request, httpRequest.getRemoteAddr()));
    }

    @GetMapping
    public ResponseEntity<List<ClinicResponse>> getAllClinics() {
        return ResponseEntity.ok(clinicService.getAllClinics());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClinicResponse> getClinic(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicService.getClinic(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClinicResponse> updateClinic(@PathVariable UUID id,
                                                        @RequestBody UpdateClinicRequest request) {
        return ResponseEntity.ok(clinicService.updateClinic(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateClinic(@PathVariable UUID id) {
        clinicService.deactivateClinic(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/staff")
    public ResponseEntity<StaffResponse> addStaff(@PathVariable UUID id,
                                                   @Valid @RequestBody AddStaffRequest request,
                                                   HttpServletRequest httpRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clinicService.addStaff(id, request, httpRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}/staff")
    public ResponseEntity<List<StaffResponse>> getStaff(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicService.getStaff(id));
    }

    @DeleteMapping("/{id}/staff/{staffId}")
    public ResponseEntity<Void> removeStaff(@PathVariable UUID id, @PathVariable UUID staffId) {
        clinicService.removeStaff(id, staffId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/capabilities")
    public ResponseEntity<CapabilityResponse> setCapability(@PathVariable UUID id,
                                                             @Valid @RequestBody SetCapabilityRequest request) {
        return ResponseEntity.ok(clinicService.setCapability(id, request));
    }

    @GetMapping("/{id}/capabilities")
    public ResponseEntity<List<CapabilityResponse>> getCapabilities(@PathVariable UUID id) {
        return ResponseEntity.ok(clinicService.getCapabilities(id));
    }
}
