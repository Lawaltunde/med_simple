package com.medsimple.ai.dao;

import com.medsimple.ai.dto.EpisodeContext;
import com.medsimple.ai.dto.TriageResult;

public interface AiTriageDao {

    TriageResult analyze(EpisodeContext context);

    // Provenance recorded on a care suggestion so "which adapter/model produced this" is queryable.
    default String adapterName() {
        return "fallback";
    }

    default String modelName() {
        return null;
    }
}
