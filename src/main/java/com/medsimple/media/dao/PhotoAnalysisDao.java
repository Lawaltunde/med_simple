package com.medsimple.media.dao;

public interface PhotoAnalysisDao {

    // Produces a clinical description of the image at mediaUrl (e.g. a rash, swelling, wound).
    String analyze(String mediaUrl);
}
