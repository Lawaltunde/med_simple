package com.medsimple.careplan.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Deterministic post-filter on an AI-suggested plan of care. This is the latent->deterministic
 * meta-loop (CLAUDE.md): the model proposes the plan, this code-level screen guarantees the model
 * cannot cross the line. It does NOT trust the model to resist prompt injection — it screens the
 * OUTPUT regardless of what the prompt did.
 *
 * If a plan trips the screen it is blocked: the suggestion is recorded FAILED/GUARDRAIL_BLOCKED and
 * the plan text is never stored. The suggestion layer proposes a plan of care, never a prescription;
 * prescribing stays in the physician-driven prescription module behind its own safety check.
 */
@Component
public class SuggestionGuardrail {

    public static final String BLOCK_REASON = "GUARDRAIL_BLOCKED";

    // Controlled substances and high-risk agents the AI must never propose.
    private static final List<String> CONTROLLED_SUBSTANCES = List.of(
            "oxycodone", "oxycontin", "percocet", "hydromorphone", "dilaudid", "morphine",
            "fentanyl", "codeine", "tramadol", "hydrocodone", "methadone", "buprenorphine",
            "alprazolam", "xanax", "lorazepam", "ativan", "diazepam", "valium",
            "clonazepam", "klonopin", "zolpidem", "ambien",
            "adderall", "amphetamine", "methylphenidate", "ritalin", "vyvanse", "lisdexamfetamine",
            "ketamine", "barbiturate", "phenobarbital", "narcotic", "opioid", "opiate"
    );

    // Directives that mean the AI tried to prescribe / dose rather than propose a plan of care.
    private static final Pattern PRESCRIBE_VERB = Pattern.compile(
            "\\b(prescrib|i am prescribing|start the patient on|dispense|administer)\\w*",
            Pattern.CASE_INSENSITIVE);

    // A specific drug dose in mg/mcg/units is a prescription, not a plan of care.
    private static final Pattern SPECIFIC_DOSE = Pattern.compile(
            "\\b\\d+(\\.\\d+)?\\s?(mg|mcg|microgram|milligram|units|iu|ml)\\b",
            Pattern.CASE_INSENSITIVE);

    /** True if the plan must be blocked (controlled substance, prescribing directive, or a dose). */
    public boolean isBlocked(String plan) {
        if (plan == null || plan.isBlank()) {
            return false; // nothing to screen; the no-plan path is handled separately
        }
        String lower = plan.toLowerCase();
        if (CONTROLLED_SUBSTANCES.stream().anyMatch(lower::contains)) {
            return true;
        }
        return PRESCRIBE_VERB.matcher(plan).find() || SPECIFIC_DOSE.matcher(plan).find();
    }
}
