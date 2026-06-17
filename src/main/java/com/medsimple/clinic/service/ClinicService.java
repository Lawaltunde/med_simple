package com.medsimple.clinic.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.dto.*;
import com.medsimple.clinic.entity.*;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.exception.StaffAlreadyExistsException;
import com.medsimple.clinic.repository.*;
import com.medsimple.patient.exception.EmailAlreadyInUseException;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;
    private final ClinicCapabilityRepository capabilityRepository;
    private final ClinicStaffRepository staffRepository;
    private final PatientRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public ClinicResponse createClinic(CreateClinicRequest request, String ipAddress) {
        Clinic clinic = Clinic.builder()
                .name(request.name())
                .type(request.type())
                .addressLine1(request.addressLine1())
                .addressLine2(request.addressLine2())
                .city(request.city())
                .province(request.province())
                .postalCode(request.postalCode())
                .phone(request.phone())
                .fax(request.fax())
                .email(request.email())
                .build();
        Clinic saved = clinicRepository.save(clinic);
        auditService.log(null, "CLINIC_CREATED", "clinics", saved.getId(), ipAddress);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ClinicResponse> getAllClinics() {
        return clinicRepository.findAll().stream().map(this::toResponse).toList();
    }

    // Clinics the given user is active staff at. Empty for patients (they aren't staff),
    // which is how the console knows which clinic's queue to load.
    @Transactional(readOnly = true)
    public List<ClinicResponse> getClinicsForUser(UUID userId) {
        return staffRepository.findByUserIdAndActiveTrue(userId).stream()
                .map(staff -> toResponse(staff.getClinic()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ClinicResponse getClinic(UUID id) {
        return toResponse(findClinicById(id));
    }

    @Transactional
    public ClinicResponse updateClinic(UUID id, UpdateClinicRequest request) {
        Clinic clinic = findClinicById(id);
        if (request.name() != null) clinic.setName(request.name());
        if (request.type() != null) clinic.setType(request.type());
        if (request.addressLine1() != null) clinic.setAddressLine1(request.addressLine1());
        if (request.addressLine2() != null) clinic.setAddressLine2(request.addressLine2());
        if (request.city() != null) clinic.setCity(request.city());
        if (request.province() != null) clinic.setProvince(request.province());
        if (request.postalCode() != null) clinic.setPostalCode(request.postalCode());
        if (request.phone() != null) clinic.setPhone(request.phone());
        if (request.fax() != null) clinic.setFax(request.fax());
        if (request.email() != null) clinic.setEmail(request.email());
        return toResponse(clinicRepository.save(clinic));
    }

    @Transactional
    public void deactivateClinic(UUID id) {
        Clinic clinic = findClinicById(id);
        clinic.setActive(false);
        clinicRepository.save(clinic);
    }

    // ── Staff ─────────────────────────────────────────────────────────────────

    @Transactional
    public StaffResponse addStaff(UUID clinicId, AddStaffRequest request, String ipAddress) {
        Clinic clinic = findClinicById(clinicId);

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyInUseException(request.email());
        }

        UserRole userRole = switch (request.role()) {
            case PHYSICIAN -> UserRole.PHYSICIAN;
            case NURSE -> UserRole.NURSE;
            case RECEPTIONIST -> UserRole.RECEPTIONIST;
            case CLINIC_ADMIN -> UserRole.CLINIC_ADMIN;
        };

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(userRole)
                .firstName(request.firstName())
                .lastName(request.lastName())
                .phone(request.phone())
                .build();
        User savedUser = userRepository.save(user);

        ClinicStaff staff = ClinicStaff.builder()
                .clinic(clinic)
                .user(savedUser)
                .role(request.role())
                .licenseNumber(request.licenseNumber())
                .specialization(request.specialization())
                .build();
        ClinicStaff savedStaff = staffRepository.save(staff);

        auditService.log(savedUser.getId(), "STAFF_ADDED", "clinic_staff", savedStaff.getId(), ipAddress);
        return toStaffResponse(savedStaff);
    }

    @Transactional(readOnly = true)
    public List<StaffResponse> getStaff(UUID clinicId) {
        findClinicById(clinicId);
        return staffRepository.findByClinicIdAndActiveTrue(clinicId)
                .stream().map(this::toStaffResponse).toList();
    }

    @Transactional
    public void removeStaff(UUID clinicId, UUID staffId) {
        ClinicStaff staff = staffRepository.findByIdAndClinicId(staffId, clinicId)
                .orElseThrow(() -> new ClinicNotFoundException(clinicId));
        staff.setActive(false);
        staffRepository.save(staff);
    }

    // ── Capabilities ──────────────────────────────────────────────────────────

    @Transactional
    public CapabilityResponse setCapability(UUID clinicId, SetCapabilityRequest request) {
        findClinicById(clinicId);
        Clinic clinicRef = clinicRepository.getReferenceById(clinicId);

        ClinicCapability capability = capabilityRepository
                .findByClinicIdAndCapability(clinicId, request.capability())
                .orElseGet(() -> ClinicCapability.builder()
                        .clinic(clinicRef)
                        .capability(request.capability())
                        .build());

        capability.setEnabled(request.enabled());
        capability.setConfig(request.config());
        return toCapabilityResponse(capabilityRepository.save(capability));
    }

    @Transactional(readOnly = true)
    public List<CapabilityResponse> getCapabilities(UUID clinicId) {
        findClinicById(clinicId);
        return capabilityRepository.findByClinicId(clinicId)
                .stream().map(this::toCapabilityResponse).toList();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Clinic findClinicById(UUID id) {
        return clinicRepository.findById(id)
                .orElseThrow(() -> new ClinicNotFoundException(id));
    }

    private ClinicResponse toResponse(Clinic c) {
        return new ClinicResponse(c.getId(), c.getName(), c.getType(),
                c.getAddressLine1(), c.getAddressLine2(), c.getCity(),
                c.getProvince(), c.getPostalCode(), c.getPhone(),
                c.getFax(), c.getEmail(), c.isActive(), c.getCreatedAt());
    }

    private StaffResponse toStaffResponse(ClinicStaff s) {
        return new StaffResponse(s.getId(), s.getUser().getId(),
                s.getUser().getFirstName(), s.getUser().getLastName(),
                s.getUser().getEmail(), s.getRole(),
                s.getLicenseNumber(), s.getSpecialization(),
                s.isActive(), s.getCreatedAt());
    }

    private CapabilityResponse toCapabilityResponse(ClinicCapability c) {
        return new CapabilityResponse(c.getId(), c.getCapability(), c.isEnabled());
    }
}
