package com.medsimple.clinic.credential;

import com.medsimple.clinic.entity.StaffRole;
import com.medsimple.clinic.exception.InvalidPhysicianCredentialException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhysicianCredentialValidatorTest {

    private final PhysicianCredentialValidator validator = new PhysicianCredentialValidator();

    @Test
    void physicianWithValidCpsoCredentials_passes() {
        assertThatCode(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "67890", "ON"))
                .doesNotThrowAnyException();
        // 6-digit CPSO numbers are also valid.
        assertThatCode(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "123456", "on"))
                .doesNotThrowAnyException();
    }

    @Test
    void physicianMissingCollegeOrRegistrationOrProvince_isRejected() {
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, null, "67890", "ON"))
                .isInstanceOf(InvalidPhysicianCredentialException.class)
                .hasMessageContaining("College");
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "  ", "ON"))
                .isInstanceOf(InvalidPhysicianCredentialException.class)
                .hasMessageContaining("registration number");
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "67890", null))
                .isInstanceOf(InvalidPhysicianCredentialException.class)
                .hasMessageContaining("province");
    }

    @Test
    void cpsoRegistrationWrongFormat_isRejected() {
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "12", "ON"))
                .isInstanceOf(InvalidPhysicianCredentialException.class)
                .hasMessageContaining("5 or 6 digits");
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "ABCDE", "ON"))
                .isInstanceOf(InvalidPhysicianCredentialException.class);
    }

    @Test
    void badProvinceCode_isRejected() {
        assertThatThrownBy(() ->
                validator.validateForOnboarding(StaffRole.PHYSICIAN, "CPSO", "67890", "Ontario"))
                .isInstanceOf(InvalidPhysicianCredentialException.class)
                .hasMessageContaining("2-letter");
    }

    @Test
    void nonPhysicianStaff_skipCredentialCheck() {
        // A receptionist or nurse needs no College credentials.
        assertThatCode(() ->
                validator.validateForOnboarding(StaffRole.RECEPTIONIST, null, null, null))
                .doesNotThrowAnyException();
        assertThatCode(() ->
                validator.validateForOnboarding(StaffRole.NURSE, null, null, null))
                .doesNotThrowAnyException();
    }
}
