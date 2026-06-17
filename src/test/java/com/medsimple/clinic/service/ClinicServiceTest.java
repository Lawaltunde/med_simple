package com.medsimple.clinic.service;

import com.medsimple.audit.service.AuditService;
import com.medsimple.clinic.dto.*;
import com.medsimple.clinic.entity.*;
import com.medsimple.clinic.exception.ClinicNotFoundException;
import com.medsimple.clinic.repository.*;
import com.medsimple.patient.repository.PatientRepository;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicServiceTest {

    @Mock ClinicRepository clinicRepository;
    @Mock ClinicCapabilityRepository capabilityRepository;
    @Mock ClinicStaffRepository staffRepository;
    @Mock PatientRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;

    @InjectMocks ClinicService clinicService;

    private Clinic sampleClinic;
    private UUID clinicId;

    @BeforeEach
    void setUp() {
        clinicId = UUID.randomUUID();
        sampleClinic = Clinic.builder()
                .id(clinicId)
                .name("Maple Clinic")
                .type(ClinicType.FAMILY_PRACTICE)
                .city("Toronto")
                .province("ON")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();
    }

    // ── getClinicsForUser (console "my clinics") ─────────────────────────────

    @Test
    void getClinicsForUser_returnsActiveMemberships() {
        UUID userId = UUID.randomUUID();
        ClinicStaff membership = ClinicStaff.builder()
                .id(UUID.randomUUID()).clinic(sampleClinic)
                .user(User.builder().id(userId).role(UserRole.PHYSICIAN).build())
                .role(StaffRole.PHYSICIAN).active(true).build();
        when(staffRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(membership));

        List<ClinicResponse> clinics = clinicService.getClinicsForUser(userId);

        assertThat(clinics).singleElement()
                .satisfies(c -> assertThat(c.id()).isEqualTo(clinicId));
    }

    @Test
    void getClinicsForUser_noMemberships_returnsEmpty() {
        UUID userId = UUID.randomUUID();
        when(staffRepository.findByUserIdAndActiveTrue(userId)).thenReturn(List.of());

        assertThat(clinicService.getClinicsForUser(userId)).isEmpty();
    }

    // ── createClinic ─────────────────────────────────────────────────────────

    @Test
    void createClinic_success() {
        CreateClinicRequest request = new CreateClinicRequest(
                "Maple Clinic", ClinicType.FAMILY_PRACTICE,
                "123 Main St", null, "Toronto", "ON", "M5V 1A1",
                "416-555-0100", null, "maple@clinic.ca");

        when(clinicRepository.save(any())).thenReturn(sampleClinic);

        ClinicResponse response = clinicService.createClinic(request, "127.0.0.1");

        assertThat(response.name()).isEqualTo("Maple Clinic");
        assertThat(response.type()).isEqualTo(ClinicType.FAMILY_PRACTICE);
        verify(auditService).log(null, "CLINIC_CREATED", "clinics", clinicId, "127.0.0.1");
    }

    // ── getClinic ─────────────────────────────────────────────────────────────

    @Test
    void getClinic_found() {
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(sampleClinic));

        ClinicResponse response = clinicService.getClinic(clinicId);

        assertThat(response.id()).isEqualTo(clinicId);
        assertThat(response.name()).isEqualTo("Maple Clinic");
    }

    @Test
    void getClinic_notFound_throws() {
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicService.getClinic(clinicId))
                .isInstanceOf(ClinicNotFoundException.class)
                .hasMessageContaining(clinicId.toString());
    }

    // ── updateClinic ──────────────────────────────────────────────────────────

    @Test
    void updateClinic_partialUpdate() {
        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(sampleClinic));
        when(clinicRepository.save(any())).thenReturn(sampleClinic);

        ClinicResponse response = clinicService.updateClinic(clinicId,
                new UpdateClinicRequest("New Name", null, null, null, null, null, null, null, null, null));

        verify(clinicRepository).save(argThat(c -> c.getName().equals("New Name")));
    }

    // ── addStaff ──────────────────────────────────────────────────────────────

    @Test
    void addStaff_createsUserAndStaff() {
        AddStaffRequest request = new AddStaffRequest(
                "dr.smith@clinic.ca", "password123", "John", "Smith",
                "416-555-0200", StaffRole.PHYSICIAN, "ON-12345", "General Practice");

        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .email("dr.smith@clinic.ca")
                .firstName("John")
                .lastName("Smith")
                .role(UserRole.PHYSICIAN)
                .createdAt(OffsetDateTime.now())
                .build();

        ClinicStaff savedStaff = ClinicStaff.builder()
                .id(UUID.randomUUID())
                .clinic(sampleClinic)
                .user(savedUser)
                .role(StaffRole.PHYSICIAN)
                .licenseNumber("ON-12345")
                .specialization("General Practice")
                .active(true)
                .createdAt(OffsetDateTime.now())
                .build();

        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(sampleClinic));
        when(userRepository.existsByEmail("dr.smith@clinic.ca")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(savedUser);
        when(staffRepository.save(any())).thenReturn(savedStaff);

        StaffResponse response = clinicService.addStaff(clinicId, request, "127.0.0.1");

        assertThat(response.email()).isEqualTo("dr.smith@clinic.ca");
        assertThat(response.role()).isEqualTo(StaffRole.PHYSICIAN);
        assertThat(response.licenseNumber()).isEqualTo("ON-12345");
    }

    // ── setCapability ─────────────────────────────────────────────────────────

    @Test
    void setCapability_enablesNewCapability() {
        SetCapabilityRequest request = new SetCapabilityRequest(CapabilityType.PRESCRIBIT, true, null);

        ClinicCapability saved = ClinicCapability.builder()
                .id(UUID.randomUUID())
                .clinic(sampleClinic)
                .capability(CapabilityType.PRESCRIBIT)
                .enabled(true)
                .build();

        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(sampleClinic));
        when(clinicRepository.getReferenceById(clinicId)).thenReturn(sampleClinic);
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT))
                .thenReturn(Optional.empty());
        when(capabilityRepository.save(any())).thenReturn(saved);

        CapabilityResponse response = clinicService.setCapability(clinicId, request);

        assertThat(response.capability()).isEqualTo(CapabilityType.PRESCRIBIT);
        assertThat(response.enabled()).isTrue();
    }

    @Test
    void setCapability_updatesExisting() {
        ClinicCapability existing = ClinicCapability.builder()
                .id(UUID.randomUUID())
                .clinic(sampleClinic)
                .capability(CapabilityType.PRESCRIBIT)
                .enabled(false)
                .build();

        when(clinicRepository.findById(clinicId)).thenReturn(Optional.of(sampleClinic));
        when(clinicRepository.getReferenceById(clinicId)).thenReturn(sampleClinic);
        when(capabilityRepository.findByClinicIdAndCapability(clinicId, CapabilityType.PRESCRIBIT))
                .thenReturn(Optional.of(existing));
        when(capabilityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CapabilityResponse response = clinicService.setCapability(clinicId,
                new SetCapabilityRequest(CapabilityType.PRESCRIBIT, true, null));

        assertThat(response.enabled()).isTrue();
    }
}
