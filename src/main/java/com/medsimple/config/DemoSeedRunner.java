package com.medsimple.config;

import com.medsimple.auth.repository.UserAccountRepository;
import com.medsimple.clinic.entity.Clinic;
import com.medsimple.clinic.entity.ClinicStaff;
import com.medsimple.clinic.entity.ClinicType;
import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.repository.ClinicRepository;
import com.medsimple.clinic.repository.ClinicStaffRepository;
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

/**
 * Local-only demo data so there is a clinic to attach to and a physician to log in as.
 * Idempotent: keyed on the physician email, so it runs once and is a no-op thereafter.
 * Never active outside the {@code local} profile — no demo credentials reach staging/prod.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DemoSeedRunner implements ApplicationRunner {

    public static final String DEMO_PHYSICIAN_EMAIL = "physician@demo.medsimple";
    private static final String DEMO_PHYSICIAN_PASSWORD = "Physician123!";
    private static final String DEMO_CLINIC_NAME = "MedSimple Demo Family Practice";

    private final ClinicRepository clinicRepository;
    private final UserAccountRepository userRepository;
    private final ClinicStaffRepository staffRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(DEMO_PHYSICIAN_EMAIL).isPresent()) {
            return; // already seeded
        }

        Clinic clinic = clinicRepository.save(Clinic.builder()
                .name(DEMO_CLINIC_NAME)
                .type(ClinicType.FAMILY_PRACTICE)
                .addressLine1("100 Health Way")
                .city("Toronto")
                .province("ON")
                .postalCode("M5H 2N2")
                .phone("416-555-0100")
                .build());

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
                .active(true)
                .build());

        log.info("Seeded demo clinic '{}' and physician login {} / {}",
                DEMO_CLINIC_NAME, DEMO_PHYSICIAN_EMAIL, DEMO_PHYSICIAN_PASSWORD);
    }
}
