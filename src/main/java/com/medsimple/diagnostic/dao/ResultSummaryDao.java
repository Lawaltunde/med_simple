package com.medsimple.diagnostic.dao;

public interface ResultSummaryDao {

    // rawResultText is PHI-stripped — lab values / report text only, no patient identity.
    // Returns a plain-language summary to help the patient understand the result once released.
    String summarize(String testType, String rawResultText);
}
