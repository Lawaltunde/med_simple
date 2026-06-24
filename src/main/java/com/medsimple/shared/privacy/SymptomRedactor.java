package com.medsimple.shared.privacy;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic PHI scrubber for free-text symptom strings.
 *
 * <p>PHIPA rule (CLAUDE.md): "PHI must be stripped or tokenized before any data reaches AI model
 * providers." Symptom free-text is the residual risk the design doc flagged: a patient may type
 * their name, OHIP/health-card number, phone, email, or postal code into the symptom box, and that
 * text is forwarded verbatim to the model. This component removes the obvious identifiers before
 * the string leaves the trust boundary.
 *
 * <p>Pure function: {@link #redact(String)} maps a String to a String with no I/O, no model call,
 * no shared state. Same input always yields the same output — this is deterministic work, so it
 * lives in code (regex), never in latent space. It is intentionally conservative: it targets
 * high-precision structured identifiers (numbers, emails, postal codes) plus explicit
 * self-identification phrases ("my name is X", "I am X"). It does NOT attempt open-ended NER on
 * arbitrary names — that is a latent-space task and would risk shredding clinical terms.
 *
 * <p>Each match is replaced with a typed placeholder ([NAME], [HEALTH_CARD], [PHONE], [EMAIL],
 * [POSTAL_CODE]) so the model still sees that an identifier was present (preserving sentence shape)
 * without seeing its value.
 *
 * <p><b>Wiring (owned by the triage orchestrator, not this class):</b> in
 * {@code AiTriageService.triageEpisode}, map each collected symptom string through this redactor
 * immediately before building {@code EpisodeContext} / calling {@code aiTriageDao.analyze}, e.g.
 *
 * <pre>{@code
 *   List<String> symptoms = collectSymptomTexts(episodeId).stream()
 *           .map(symptomRedactor::redact)
 *           .toList();
 * }</pre>
 *
 * Inject {@code SymptomRedactor} into {@code AiTriageService} (constructor field) to do so. The
 * exemplar text retrieved by {@code PlanRetriever} should be redacted the same way if it ever
 * carries free-text PHI.
 */
@Component
public class SymptomRedactor {

    private static final String NAME = "[NAME]";
    private static final String HEALTH_CARD = "[HEALTH_CARD]";
    private static final String PHONE = "[PHONE]";
    private static final String EMAIL = "[EMAIL]";
    private static final String POSTAL_CODE = "[POSTAL_CODE]";

    // Email — RFC-ish, good enough to catch real addresses. Run BEFORE phone/number rules so the
    // digits inside an address don't get partially eaten.
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");

    // Explicit self-identification. Captures the name token(s) after the lead-in phrase, stopping
    // at sentence punctuation or a clause break. Case-insensitive. Examples:
    //   "my name is Jane Doe"      -> "my name is [NAME]"
    //   "I am John Smith,"         -> "I am [NAME],"
    //   "this is Mary O'Brien and" -> "this is [NAME] and"
    // The name is 1-3 capitalised-ish words; we stop before lowercase filler ("and", "but") and
    // before "a"/"an"/"the" so "I am a 45 year old" is left intact (not a name).
    private static final Pattern SELF_ID_PATTERN = Pattern.compile(
            "(?i)\\b(my name is|i am|i'm|this is|name's)\\s+"
                    + "(?!a |an |the |feeling|having|experiencing|so |very |really |not |in |on |at |with |having|getting|going)"
                    + "([A-Z][\\p{L}'\\-]+(?:\\s+[A-Z][\\p{L}'\\-]+){0,2})");

    // Canadian provincial health-card / OHIP-style numbers: 10 digits, optionally with a 2-letter
    // version code and optional spaces/dashes between groups. Examples:
    //   "1234567890", "1234-567-890", "1234 567 890 AB", "OHIP 1234567890AB".
    // Requires >= 10 digits so ordinary small numbers ("for 3 days", "twice a day") survive.
    private static final Pattern HEALTH_CARD_PATTERN = Pattern.compile(
            "\\b\\d{4}[\\s\\-]?\\d{3}[\\s\\-]?\\d{3}(?:[\\s\\-]?[A-Za-z]{2})?\\b");

    // North American phone numbers: optional +1, optional parens, 3-3-4 grouping with space, dot
    // or dash separators. Examples: "416-555-0199", "(416) 555 0199", "+1 416 555 0199",
    // "4165550199". Run AFTER the health-card rule so a 10-digit card isn't mislabelled as a phone.
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "(?<!\\d)(?:\\+?1[\\s.\\-]?)?(?:\\(\\d{3}\\)|\\d{3})[\\s.\\-]\\d{3}[\\s.\\-]\\d{4}(?!\\d)");

    // Canadian postal code: A1A 1A1 (optional space). Excludes letters D,F,I,O,Q,U which Canada
    // Post never uses in the first position group, but we keep it permissive on the value letters.
    private static final Pattern POSTAL_CODE_PATTERN = Pattern.compile(
            "(?i)\\b[A-Za-z]\\d[A-Za-z][ \\-]?\\d[A-Za-z]\\d\\b");

    /**
     * Redact obvious PHI from a free-text symptom string.
     *
     * @param input raw symptom text (may be null or blank)
     * @return the input with identifiers replaced by typed placeholders; null/blank passed through
     *         unchanged
     */
    public String redact(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String out = input;

        // Order matters. Email first (its dots/digits would confuse number rules). Then explicit
        // self-identification. Then health card before phone (a 10-digit card must not be eaten by
        // the phone rule). Postal code last.
        out = EMAIL_PATTERN.matcher(out).replaceAll(Matcher.quoteReplacement(EMAIL));
        out = SELF_ID_PATTERN.matcher(out).replaceAll(mr ->
                Matcher.quoteReplacement(mr.group(1) + " " + NAME));
        out = HEALTH_CARD_PATTERN.matcher(out).replaceAll(Matcher.quoteReplacement(HEALTH_CARD));
        out = PHONE_PATTERN.matcher(out).replaceAll(Matcher.quoteReplacement(PHONE));
        out = POSTAL_CODE_PATTERN.matcher(out).replaceAll(Matcher.quoteReplacement(POSTAL_CODE));

        return out;
    }
}
