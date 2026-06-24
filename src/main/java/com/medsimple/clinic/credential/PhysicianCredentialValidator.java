package com.medsimple.clinic.credential;

import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.exception.InvalidPhysicianCredentialException;
import org.springframework.stereotype.Component;

/**
 * Deterministic onboarding checks for physician credentials. A PHYSICIAN must present a College,
 * a registration number, and a province of licensure; the registration number is format-checked
 * where the College's scheme is known (Ontario CPSO = 5-6 digits). Non-physician staff are not
 * required to carry College credentials.
 *
 * <p>This is the cheap, same-input-same-answer gate; the live "in good standing" confirmation is
 * the separate {@link PhysicianCredentialVerificationDao} step.
 */
@Component
public class PhysicianCredentialValidator {

    public void validateForOnboarding(StaffRole role, String college, String registrationNumber,
                                      String licenseProvince) {
        if (role != StaffRole.PHYSICIAN) {
            return;
        }
        require(college, "College (e.g. CPSO)");
        require(registrationNumber, "registration number");
        require(licenseProvince, "province of licensure");

        if (!licenseProvince.trim().matches("(?i)[A-Za-z]{2}")) {
            throw new InvalidPhysicianCredentialException(
                    "Province of licensure must be a 2-letter code, e.g. ON.");
        }
        // Ontario CPSO registration numbers are 5-6 digits. Other Colleges: presence-only for now.
        if ("CPSO".equalsIgnoreCase(college.trim())
                && !registrationNumber.replaceAll("\\s", "").matches("\\d{5,6}")) {
            throw new InvalidPhysicianCredentialException(
                    "CPSO registration number must be 5 or 6 digits.");
        }
    }

    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidPhysicianCredentialException(
                    "A physician requires a " + field + ".");
        }
    }
}
