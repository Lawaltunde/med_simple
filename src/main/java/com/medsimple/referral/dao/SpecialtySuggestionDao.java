package com.medsimple.referral.dao;

public interface SpecialtySuggestionDao {

    // clinicalSummary is PHI-stripped — symptom/condition text only, no patient identity.
    SpecialtySuggestion suggest(String clinicalSummary);
}
