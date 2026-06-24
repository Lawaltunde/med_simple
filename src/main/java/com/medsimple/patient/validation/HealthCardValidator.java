package com.medsimple.patient.validation;

import com.medsimple.patient.entity.CoverageType;
import com.medsimple.patient.exception.InvalidHealthCardException;
import org.springframework.stereotype.Component;

/**
 * Validates provincial health-card numbers before they are encrypted and stored.
 *
 * <p>Per-province seam: today only Ontario (OHIP) has a published, machine-checkable algorithm, so
 * that is what is enforced. Other provinces and non-provincial coverage (private, Veterans, NIHB)
 * pass through with the basic non-blank check the request already enforces — the {@link #validate}
 * switch is where each province's rule slots in as it is added. This is deterministic work: same
 * card, same verdict, every time.
 *
 * <p>OHIP format: 10 digits, optionally followed by a 2-letter version code. The 10 digits carry a
 * Luhn (mod-10) check digit, so a transposed or mistyped digit is caught here, not at the desk.
 */
@Component
public class HealthCardValidator {

    /**
     * @throws InvalidHealthCardException if the number is malformed or fails its check digit.
     */
    public void validate(CoverageType coverageType, String province, String cardNumber) {
        if (coverageType == CoverageType.PROVINCIAL && isOntario(province)) {
            validateOhip(cardNumber);
        }
    }

    private boolean isOntario(String province) {
        return province != null && "ON".equalsIgnoreCase(province.trim());
    }

    private void validateOhip(String cardNumber) {
        String normalized = cardNumber == null ? "" : cardNumber.replaceAll("[\\s-]", "").toUpperCase();
        if (!normalized.matches("\\d{10}([A-Z]{2})?")) {
            throw new InvalidHealthCardException(
                    "Ontario health card must be 10 digits with an optional 2-letter version code.");
        }
        if (!luhnValid(normalized.substring(0, 10))) {
            throw new InvalidHealthCardException(
                    "Ontario health card number failed its check digit — please re-check the number.");
        }
    }

    /** Standard Luhn mod-10 over the digit string. */
    private boolean luhnValid(String digits) {
        int sum = 0;
        boolean doubleIt = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (doubleIt) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubleIt = !doubleIt;
        }
        return sum % 10 == 0;
    }
}
