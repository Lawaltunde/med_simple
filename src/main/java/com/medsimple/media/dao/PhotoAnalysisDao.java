package com.medsimple.media.dao;

public interface PhotoAnalysisDao {

    // Analyzes the image at mediaUrl: whether it shows a medical symptom, plus a factual
    // description. Never throws on a non-medical image — it returns NO_SYMPTOM, so a junk photo is
    // flagged rather than turned into a fabricated clinical finding.
    PhotoAnalysisResult analyze(String mediaUrl);
}
