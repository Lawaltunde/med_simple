package com.medsimple.config;

import com.medsimple.auth.repository.UserAccountRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.clinic.entity.ClinicStaff;
import com.medsimple.clinic.entity.ClinicType;
import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.entity.VerificationStatus;
import com.medsimple.clinic.repository.ClinicRepository;
import com.medsimple.clinic.repository.ClinicStaffRepository;
import com.medsimple.patient.dto.CreatePatientRequest;
import com.medsimple.patient.dto.PatientResponse;
import com.medsimple.patient.dto.RegisterToClinicRequest;
import com.medsimple.patient.service.PatientService;
import com.medsimple.shared.entity.User;
import com.medsimple.shared.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Local-only demo data so there is a clinic to attach to, a physician to log in as, and a patient
 * to log in as. Each entity is seeded idempotently (keyed on its own name/email), so the runner is
 * a no-op once everything exists and stays correct even if only some rows are present.
 * Never active outside the {@code local} profile — no demo credentials reach staging/prod.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DemoSeedRunner implements ApplicationRunner {

    public static final String DEMO_PHYSICIAN_EMAIL = "physician@demo.medsimple";
    private static final String DEMO_PHYSICIAN_PASSWORD = "Physician123!";
    public static final String DEMO_ADMIN_EMAIL = "admin@demo.medsimple";
    private static final String DEMO_ADMIN_PASSWORD = "Admin123!";
    public static final String DEMO_PATIENT_EMAIL = "patient@demo.medsimple";
    private static final String DEMO_PATIENT_PASSWORD = "Patient123!";
    private static final String DEMO_CLINIC_NAME = "MedSimple Demo Family Practice";

    private final ClinicRepository clinicRepository;
    private final UserAccountRepository userRepository;
    private final ClinicStaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientService patientService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Clinic clinic = clinicRepository.findByName(DEMO_CLINIC_NAME)
                .orElseGet(() -> clinicRepository.save(Clinic.builder()
                        .name(DEMO_CLINIC_NAME)
                        .type(ClinicType.FAMILY_PRACTICE)
                        .addressLine1("100 Health Way")
                        .city("Toronto")
                        .province("ON")
                        .postalCode("M5H 2N2")
                        .phone("416-555-0100")
                        .build()));

        if (userRepository.findByEmail(DEMO_PHYSICIAN_EMAIL).isEmpty()) {
            User physician = userRepository.save(User.builder()
                    .email(DEMO_PHYSICIAN_EMAIL)
                    .passwordHash(passwordEncoder.encode(DEMO_PHYSICIAN_PASSWORD))
                    .role(UserRole.PHYSICIAN)
                    .firstName("Dana")
                    .lastName("Okafor")
                    .active(true)
                    .emailVerified(true)
                    .build());

            staffRepository.save(ClinicStaff.builder()
                    .clinic(clinic)
                    .user(physician)
                    .role(StaffRole.PHYSICIAN)
                    .specialization("Family Medicine")
                    .college("CPSO")
                    .registrationNumber("90210")
                    .licenseProvince("ON")
                    // Seeded VERIFIED so the demo clinical loop works out of the box. Onboarding a
                    // NEW physician via the admin console starts UNVERIFIED, where the verify flow
                    // (and the assignment gate) is demoable.
                    .verificationStatus(VerificationStatus.VERIFIED)
                    .verificationSource("MANUAL_REVIEW")
                    .verifiedAt(java.time.OffsetDateTime.now())
                    .active(true)
                    .build());

            log.info("Seeded demo physician login {} / {}", DEMO_PHYSICIAN_EMAIL, DEMO_PHYSICIAN_PASSWORD);
        }

        if (userRepository.findByEmail(DEMO_ADMIN_EMAIL).isEmpty()) {
            // A clinic admin who belongs to the demo clinic — the only role that may onboard staff.
            User admin = userRepository.save(User.builder()
                    .email(DEMO_ADMIN_EMAIL)
                    .passwordHash(passwordEncoder.encode(DEMO_ADMIN_PASSWORD))
                    .role(UserRole.CLINIC_ADMIN)
                    .firstName("Alex")
                    .lastName("Admin")
                    .active(true)
                    .emailVerified(true)
                    .build());

            staffRepository.save(ClinicStaff.builder()
                    .clinic(clinic)
                    .user(admin)
                    .role(StaffRole.CLINIC_ADMIN)
                    .active(true)
                    .build());

            log.info("Seeded demo clinic admin login {} / {}", DEMO_ADMIN_EMAIL, DEMO_ADMIN_PASSWORD);
        }

        if (userRepository.findByEmail(DEMO_PATIENT_EMAIL).isEmpty()) {
            // Same path a real signup takes (User + PatientProfile + audit), then attach to the demo
            // clinic so the patient's episodes route to the physician queue (an orphan patient with
            // no clinic registration never appears on the console).
            PatientResponse patient = patientService.createPatient(
                    new CreatePatientRequest(DEMO_PATIENT_EMAIL, DEMO_PATIENT_PASSWORD,
                            "Sam", "Rivera", null, LocalDate.of(1990, 5, 12)),
                    "demo-seed");
            patientService.registerToClinic(patient.id(),
                    new RegisterToClinicRequest(clinic.getId()), "demo-seed");

            log.info("Seeded demo patient login {} / {} (registered to '{}')",
                    DEMO_PATIENT_EMAIL, DEMO_PATIENT_PASSWORD, DEMO_CLINIC_NAME);
        }
    }
}
