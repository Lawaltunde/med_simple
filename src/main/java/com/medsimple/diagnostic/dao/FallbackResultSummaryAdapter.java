package com.medsimple.diagnostic.dao;

import org.springframework.stereotype.Component;

/**
 * Non-AI result summary — deterministic, no external calls.
 * Default adapter; active when ai.result.adapter is not set to openai.
 * Produces a neutral, non-interpretive placeholder. It deliberately does NOT
 * attempt clinical interpretation — that is the physician's job before release.
 */
@Component
public class FallbackResultSummaryAdapter implements ResultSummaryDao {

    @Override
    public String summarize(String testType, String rawResultText) {
        return "Your " + testType + " results are available. "
                + "Your physician will review them and add an interpretation before they are released to you.";
    }
}
